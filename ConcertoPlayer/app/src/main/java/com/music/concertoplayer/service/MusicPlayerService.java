package com.music.concertoplayer.service;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.manage.MusicPlayManage;
import com.music.concertoplayer.receiver.PlayerManagerReceiver;
import com.music.concertoplayer.service.notification.PlayingNotification;
import com.music.concertoplayer.service.notification.PlayingNotificationImpl;
import com.music.concertoplayer.service.notification.PlayingNotificationImpl24;
import com.music.concertoplayer.utils.RxBus;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.media.AudioManager.AUDIOFOCUS_LOSS;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static com.music.concertoplayer.fragment.PlayBarFragment.ACTION_UPDATE_UI_PlayBar;
import static com.music.concertoplayer.receiver.PlayerManagerReceiver.status;


public class MusicPlayerService extends Service {
    private static final String TAG = MusicPlayerService.class.getName();
    public static final String PHONOGRAPH_PACKAGE_NAME = "com.music.concertoplayer";
    public static final String PLAYER_MANAGER_ACTION = "com.music.concertoplayer.service.MusicPlayerService.player.action";
    public static final String ACTION_QUIT = PHONOGRAPH_PACKAGE_NAME + ".quitservice";
    public static final String ACTION_REWIND = PHONOGRAPH_PACKAGE_NAME + ".rewind";
    public static final String ACTION_TOGGLE_PAUSE = PHONOGRAPH_PACKAGE_NAME + ".togglepause";
    public static final String ACTION_SKIP = PHONOGRAPH_PACKAGE_NAME + ".skip";
    public static final String ACTION_PAUSE = PHONOGRAPH_PACKAGE_NAME + ".pause";
    public static final String ACTION_STOP = PHONOGRAPH_PACKAGE_NAME + ".stop";
    public static final String ACTION_PLAY = PHONOGRAPH_PACKAGE_NAME + ".play";
    private PlayerManagerReceiver mReceiver;
    private PowerManager.WakeLock wakeLock;
    private PlayingNotification playingNotification;
    private MediaSessionCompat mediaSession;
    private int current;
    private HomeReceiver mUiReceiver;
    private boolean isRegist = false;

    private boolean pausedByTransientLossOfFocus;
    private final AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(final int focusChange) {
            //playerHandler.obtainMessage(FOCUS_CHANGE, focusChange, 0).sendToTarget();
            //监听系统播放状态的改变
            //Log.e("MyOnAudioFocus", "focusChange=" + focusChange);
            //暂时失去AudioFocus，可以很快再次获取AudioFocus，可以不释放播放资源
            if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                if (status == Constant.STATUS_PLAY) {
                    //暂时失去AudioFocus，可以很快再次获取AudioFocus，可以不释放播放资源,只需暂停播放
                    MusicPlayManage.pause(MusicPlayerService.this);
                    pausedByTransientLossOfFocus = true;
                }
            } else if (focusChange == AUDIOFOCUS_GAIN) {
                //获取了AudioFocus，如果当前处于播放暂停状态，并且这个暂停状态不是用户手动点击的暂停，才会继续播放
                if (status == Constant.STATUS_PAUSE && pausedByTransientLossOfFocus) {
                    pausedByTransientLossOfFocus = false;
                    play();
                }
            } else if (focusChange == AUDIOFOCUS_LOSS) {
                // 会长时间的失去AudioFoucs,就不在监听远程播放
                /*if (AudioPlayerService.this!=null){
                    PlayerController.stopService(AudioPlayerService.this);
                    audioManager.abandonAudioFocus(mListener);//不再监听播放焦点的变化
                }*/
                if (status == Constant.STATUS_PLAY) {
                    //暂时失去AudioFocus，可以很快再次获取AudioFocus，可以不释放播放资源,只需暂停播放
                    MusicPlayManage.pause(MusicPlayerService.this);
                    pausedByTransientLossOfFocus = true;
                }
            }
        }
    };
    private AudioManager audioManager;
    private Handler uiThreadHandler;
    private RemoteControlClient myRemoteControlClient;



    public MusicPlayerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: ");
        register();
        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.setReferenceCounted(false);
        uiThreadHandler = new Handler();
        setupMediaSession();
        initNotification();
        RxBus.getDefault().toObservable(String.class)
                //在io线程进行订阅，可以执行一些耗时操作
                .subscribeOn(Schedulers.io())
                //在主线程进行观察，可做UI更新操作
                .observeOn(AndroidSchedulers.mainThread())
                //观察的对象
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String msg) {
                        if (msg.equalsIgnoreCase("刷新")) {
                            updateNotification();
                            //updateMediaSessionMetaData();
                            if (getCurrentSong() == null) {
                                return;
                            }
                            myRemoteControlClient.setPlaybackState(isPlaying() ? RemoteControlClient.PLAYSTATE_PLAYING : RemoteControlClient.PLAYSTATE_PAUSED);
                            myRemoteControlClient.editMetadata(true).putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getCurrentSong().getFolderName())
                                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getCurrentSong().getName())
                                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, Long.parseLong(getCurrentSong().getDuration()))
                                    .putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, getCurrentSong().getId())
                                    .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, App.getDbManager().getFolderNameByName(getCurrentSong().getFolderName()).getBitmap())
                                    .apply();

                        } else if ("增加播放次数".equalsIgnoreCase(msg)) {
                            onTrackEnded();
                        }

                    }
                });
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
        setupBluetooth();
        initPhoneListener();
    }

    private void initPhoneListener() {
        // 请求AudioFocus,注册监听
        int result = audioManager.requestAudioFocus(audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //能打印出这句话标识已监听成功
            Log.e("MyOnAudioFocus", "requestAudioFocus successfully.");
        } else {
            Log.e("MyOnAudioFocus", "requestAudioFocus failed.");
        }
    }
    private class MyOnAudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {

        }
    }

    private void setupBluetooth() {
        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));
    }

    private void initNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            playingNotification = new PlayingNotificationImpl24();
        } else {
            playingNotification = new PlayingNotificationImpl();
        }
        playingNotification.init(this);
    }

    public void updateNotification() {
        if (playingNotification != null && getCurrentSong() != null && getCurrentSong().getId() != -1) {
            playingNotification.update();
        }
    }


    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() != null) {
                String action = intent.getAction();
                switch (action) {
                    case ACTION_TOGGLE_PAUSE:

                        if (isPlaying()) {
                            MusicPlayManage.pause(this);
                        } else {
                            play();
                        }
                        break;
                    case ACTION_REWIND:
                        MusicPlayManage.playPreMusic(this);
                        break;
                    case ACTION_SKIP:
                        //MusicPlayManage.playNextMusic(this);
                        MusicPlayManage.playNextMusic(this, 0);
                        break;
                    case ACTION_QUIT:
                        quit();
                        break;
                }
            }
        }

        return START_NOT_STICKY;
    }

    private AudioManager getAudioManager() {
        if (audioManager == null) {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        return audioManager;
    }


    public void runOnUiThread(Runnable runnable) {
        uiThreadHandler.post(runnable);
    }

    private void setupMediaSession() {
        ComponentName mediaButtonReceiverComponentName = new ComponentName(getApplicationContext(), MediaButtonIntentReceiver.class);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mediaButtonReceiverComponentName);

        PendingIntent mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);

        mediaSession = new MediaSessionCompat(this, "Phonograph", mediaButtonReceiverComponentName, mediaButtonReceiverPendingIntent);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                MusicPlayManage.pause(MusicPlayerService.this);
            }

            @Override
            public void onSkipToNext() {
                MusicPlayManage.playNextMusic(App.getContext(), 0);
            }

            @Override
            public void onSkipToPrevious() {
                MusicPlayManage.playPreMusic(MusicPlayerService.this);
            }

            @Override
            public void onStop() {
                quit();
            }

            @Override
            public void onSeekTo(long pos) {
                MusicPlayManage.skipTo(MusicPlayerService.this, (int) pos);
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {

                return MediaButtonIntentReceiver.handleIntent(MusicPlayerService.this, mediaButtonEvent);
            }
        });

        ComponentName myEventReceiver = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        getAudioManager().registerMediaButtonEventReceiver(myEventReceiver);
        // build the PendingIntent for the remote control client
        //Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(myEventReceiver);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
        // create and register the remote control client
        myRemoteControlClient = new RemoteControlClient(mediaPendingIntent);
        getAudioManager().registerRemoteControlClient(myRemoteControlClient);

        getAudioManager().registerRemoteControlClient(myRemoteControlClient);


        getAudioManager().requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {

            @Override
            public void onAudioFocusChange(int focusChange) {
                System.out.println("focusChange = " + focusChange);
            }
        }, AudioManager.STREAM_MUSIC, AUDIOFOCUS_GAIN);
        myRemoteControlClient.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                        RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                        RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                        RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS);
        if (getCurrentSong() == null) {
            return;
        }
        myRemoteControlClient.editMetadata(true).putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getCurrentSong().getFolderName())
                .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getCurrentSong().getName())
                .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, Long.parseLong(getCurrentSong().getDuration()))
                .putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, getCurrentSong().getId())
                .putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, App.getDbManager().getFolderNameByName(getCurrentSong().getFolderName()).getBitmap())
                .apply();

    }


    private void play() {
        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        if (musicId == -1 || musicId == 0) {
            Intent intent = new Intent(Constant.MP_FILTER);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_STOP);
            this.sendBroadcast(intent);
            Toast.makeText(this, "歌曲不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        //如果当前媒体在播放音乐状态，则图片显示暂停图片，按下播放键，则发送暂停媒体命令，图片显示播放图片。以此类推。
        if (status == Constant.STATUS_PAUSE) {
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_PLAY);
            this.sendBroadcast(intent);
        } else if (status == Constant.STATUS_PLAY) {
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_PAUSE);
            this.sendBroadcast(intent);
        } else {
            //为停止状态时发送播放命令，并发送将要播放歌曲的路径
            String path = App.getDbManager().getMusicPath(musicId);
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_PLAY);
            intent.putExtra(Constant.KEY_PATH, path);
            if (PlayerManagerReceiver.status == Constant.STATUS_STOP) {
                intent.putExtra(Constant.KEY_CURRENT, MusicPlayManage.getIntShared(Constant.PLAY_CURRENT));
            }
            this.sendBroadcast(intent);
        }
    }

     BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0);
            if(action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) && bluetoothState==BluetoothAdapter.STATE_DISCONNECTED){
                MusicPlayManage.pause(MusicPlayerService.this);
            }
        }
    };

    class HomeReceiver extends BroadcastReceiver {

        int status;

        @Override
        public void onReceive(Context context, Intent intent) {
            status = intent.getIntExtra(Constant.STATUS, 0);
            current = intent.getIntExtra(Constant.KEY_CURRENT, 0);

        }
    }


    private void quit() {
        MusicPlayManage.pause(this);
        playingNotification.stop();
        getAudioManager().abandonAudioFocus(audioFocusListener);

        stopSelf();
    }

    @Override
    public void onDestroy() {
        unRegister();
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        mediaSession.release();
        mediaSession.setActive(false);
        quit();
        wakeLock.release();
    }


    public void releaseWakeLock() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void onTrackEnded() {
        acquireWakeLock(30000);
    }

    public void acquireWakeLock(long milli) {
        wakeLock.acquire(milli);
    }

    private void register() {
        mReceiver = new PlayerManagerReceiver(MusicPlayerService.this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAYER_MANAGER_ACTION);
        registerReceiver(mReceiver, intentFilter);
        mUiReceiver = new HomeReceiver();
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter.addAction(ACTION_UPDATE_UI_PlayBar);
        App.getContext().registerReceiver(mReceiver, intentFilter2);
        isRegist = true;
    }

    private void unRegister() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);

        }
        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver);
        }
        /*if (mUiReceiver != null && mUiReceiver.) {
            try {
                App.getContext().unregisterReceiver(mUiReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isRegist = false;
        }*/
    }

    public boolean isPlaying() {
        return PlayerManagerReceiver.status == Constant.STATUS_PLAY;
    }

    public MusicInfo getCurrentSong() {
        return App.getDbManager().getMusicFromAllById(MusicPlayManage.getIntShared(Constant.KEY_ID));
    }

    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }
}

