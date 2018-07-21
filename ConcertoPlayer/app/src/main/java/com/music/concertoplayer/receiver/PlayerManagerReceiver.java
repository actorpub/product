package com.music.concertoplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.database.DBManager;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.fragment.PlayBarFragment;
import com.music.concertoplayer.manage.MusicPlayManage;
import com.music.concertoplayer.net.LogUtils;
import com.music.concertoplayer.utils.RxBus;
import com.orhanobut.logger.Logger;
import com.smp.soundtouchandroid.OnProgressChangedListener;
import com.smp.soundtouchandroid.SoundStreamAudioPlayer;

import java.io.File;


public class PlayerManagerReceiver extends BroadcastReceiver {

    private static final String TAG = PlayerManagerReceiver.class.getName();
    public static final String ACTION_UPDATE_UI_ADAPTER = "com.lijunyan.blackmusic.receiver.PlayerManagerReceiver:action_update_ui_adapter_broad_cast";
    //private MediaPlayer mediaPlayer;
    private SoundStreamAudioPlayer player;
    private DBManager dbManager;
    public static int status = Constant.STATUS_STOP;
    private int threadNumber;
    private Context context;
    private float tempo = 1.0f;//这个是速度，1.0表示正常设置新的速度控制值，
    private float pitchSemi = 1.0f;//这个是音调，1.0表示正常，
    private float rate = 1.0f;//这个参数是变速又变声的，这个参数大于0，否则会报错

    public PlayerManagerReceiver() {
    }

    public PlayerManagerReceiver(Context context) {
        super();
        this.context = context;
        dbManager = DBManager.getInstance(context);

        Log.d(TAG, "create");
        initMediaPlayer();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int cmd = intent.getIntExtra(Constant.COMMAND, Constant.COMMAND_INIT);
        Log.d(TAG, "cmd = " + cmd);
        switch (cmd) {
            case Constant.COMMAND_INIT:    //已经在创建的时候初始化了，可以撤销了
                Log.d(TAG, "COMMAND_INIT");
                break;
            case Constant.COMMAND_PLAY:
                Log.d(TAG, "COMMAND_PLAY");
                String musicPath = intent.getStringExtra(Constant.KEY_PATH);


                if (!TextUtils.isEmpty(musicPath)) {
                    playMusic(musicPath);
                } else {
                    player.start();
                }
                //打开读取上次播放的位置
                int preProgress = intent.getIntExtra(Constant.KEY_CURRENT, -1);
                if (preProgress != -1) {
                    player.seekToSecond(context, preProgress);
                }
                status = Constant.STATUS_PLAY;
                RxBus.getDefault().post("刷新");
                String speed = MusicPlayManage.getStringShared(Constant.KEY_SPEED);
                if (!TextUtils.isEmpty(speed)) {
                    if (speed.equalsIgnoreCase("0.5")) {
                        speed = "-50";
                    } else if (speed.equalsIgnoreCase("1.0")) {
                        speed = "0";
                    } else if (speed.equalsIgnoreCase("1.5")) {
                        speed = "50";
                    } else if (speed.equalsIgnoreCase("2.0")) {
                        speed = "100";
                    }
                    String pitchSemiStr = MusicPlayManage.getStringShared(Constant.KEY_PITCHSEMI);
                    if (!TextUtils.isEmpty(pitchSemiStr)) {
                        pitchSemi = Float.valueOf(pitchSemiStr);
                    }
                    String speedRange = MusicPlayManage.getStringShared(Constant.KEY_SPEED_RANGE);

                    if (!TextUtils.isEmpty(speedRange)) {
                        if (speedRange.equalsIgnoreCase("speedToAllSong")) {
                            //当前播放速度适用于所有音乐
                            tempo = Float.parseFloat(speed);
                            player.setTempoChange(tempo);
                            player.setPitchSemi(pitchSemi);

                        } else {
                            //单个文件夹
                            MusicInfo musicInfo = MusicPlayManage.getCurrentMusic();
                            if (musicInfo.getFolderName().equalsIgnoreCase(speedRange)) {
                                tempo = Float.parseFloat(speed);
                                player.setTempoChange(tempo);
                                player.setPitchSemi(pitchSemi);
                            } else {
                                tempo = 1.0f;
                                pitchSemi = 1.0f;
                                player.setTempoChange(tempo);
                                player.setPitchSemi(pitchSemi);
                            }

                        }

                    }
                }


                break;
            case Constant.COMMAND_PAUSE:
                if (player != null) {
                    player.pause();
                }
                status = Constant.STATUS_PAUSE;
                RxBus.getDefault().post("刷新");
                break;
            case Constant.COMMAND_STOP: //本程序停止状态都是删除当前播放音乐触发
                status = Constant.STATUS_STOP;
                if (player != null) {
                    player.stop();
                }

                initStopOperate();
                break;
            case Constant.COMMAND_PROGRESS://拖动进度
                if (player == null) {
                    return;
                }
                int curProgress = intent.getIntExtra(Constant.KEY_CURRENT, 0);
                //异步的，可以设置完成监听来获取真正定位完成的时候
                player.seekToSecond(context, curProgress);
                break;
            case Constant.COMMAND_COMPLETE://拖动进度
                if (player == null) {
                    return;
                }
                String path = intent.getStringExtra(Constant.KEY_PATH);
                playMusic(path);
                player.pause();
                status = Constant.STATUS_PAUSE;
                break;
            case Constant.COMMAND_RELEASE:
                status = Constant.STATUS_STOP;
                if (player != null) {
                    player.stop();
                    player = null;
                }
                break;
            case Constant.COMMAND_SPEEED:
                String speed2 = intent.getStringExtra(Constant.KEY_SPEED);
                if (speed2.equalsIgnoreCase("0.5")) {
                    speed2 = "-50";
                } else if (speed2.equalsIgnoreCase("1.0")) {
                    speed2 = "0";
                } else if (speed2.equalsIgnoreCase("1.5")) {
                    speed2 = "50";
                } else if (speed2.equalsIgnoreCase("2.0")) {
                    speed2 = "100";
                }
                tempo = Float.parseFloat(speed2);
                if (player != null) {
                    Logger.d("setTempoChange " + tempo);
                    player.setTempoChange(tempo);
                }
                break;
        }
        updateUI();
    }

    private void initStopOperate() {
        MusicPlayManage.setShared(Constant.KEY_ID, dbManager.getFirstId(Constant.LIST_ALLMUSIC));
    }

    private void playMusic(String musicPath) {
        //NumberRandom();
        //Logger.d("开始播放音乐=" + musicPath);
        if (player != null) {
            player.stop();
            player = null;
        }
        try {
            player = new SoundStreamAudioPlayer(0, musicPath, tempo, pitchSemi);

            player.setOnProgressChangedListener(new OnProgressChangedListener() {

                @Override
                public void onProgressChanged(int track, double currentPercentage, long position) {

                }

                @Override
                public void onProgressChanged(int track, long current, long total) {
                    //Log.d("chen", "onProgressChanged　　track=" + track + " current=" + current + " total=" + total);
                    int currentMillis = (int) (current / 1000);
                    int totalMillis = (int) (total / 1000);
                    Intent intent = new Intent(PlayBarFragment.ACTION_UPDATE_UI_PlayBar);
                    if (!(player.isPaused() || player.isFinished())) {
                        intent.putExtra(Constant.STATUS, Constant.STATUS_PLAY);
                    }
                    intent.putExtra(Constant.KEY_DURATION, totalMillis);
                    intent.putExtra(Constant.KEY_CURRENT, currentMillis);
                    context.sendBroadcast(intent);
                }

                @Override
                public void onTrackEnd(int track) {
                    Log.d("chen", "onTrackEnd = " + track);
                    int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
                    dbManager.addPlayCount(musicId);
                    MusicInfo musicInfo = dbManager.getMusicFromAllById(musicId);
                    dbManager.updateFolderPlayProgress(musicInfo.getFolderName(), 0);
                    MusicPlayManage.playNextMusic(App.getContext(), 0);//下一首
                    updateUI();                //更新界面
                    RxBus.getDefault().post("增加播放次数");
                    LogUtils.insertPlayLog(musicInfo, "完成", "0");
                }

                @Override
                public void onExceptionThrown(String string) {
                    Log.d("chen", "onExceptionThrown" + string);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                //Logger.d("playMusic onCompletion: ");
                int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
                App.getDbManager().addPlayCount(musicId);
                NumberRandom();                //切换线程
                //onComplete();     //调用音乐切换模块，进行相应操作
                MusicPlayManage.playNextMusic(context,dbManager.getMusicFromAllById(musicId),0); //下一首
                updateUI();                //更新界面
                RxBus.getDefault().post("增加播放次数");

            }
        });*/

        try {
            int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
            MusicInfo musicInfo = dbManager.getMusicFromAllById(musicId);

            File file = new File(musicPath);
            if (!file.exists()) {
                Toast.makeText(context, "歌曲文件不存在，请重新扫描", Toast.LENGTH_SHORT).show();
                //MusicPlayManage.playNextMusic(context);
                LogUtils.insertPlayLog(musicInfo, "开始", "1");
                MusicPlayManage.playNextMusic(context, 0); //下一首
                return;
            }
            /*mediaPlayer.setDataSource(musicPath);   //设置MediaPlayer数据源
            mediaPlayer.prepare();
            mediaPlayer.start();*/
            //这个参数是变速又变声的，这个参数大于0，否则会报错
            player.setRate(rate);
            player.setTempoChange(tempo);
            new Thread(player).start();
            player.start();
            LogUtils.insertPlayLog(musicInfo, "开始", "0");
            int playMode = MusicPlayManage.getIntShared(Constant.KEY_ACTIVITY_STATUS);//获取播放的范围
            if (!App.getIsHistory() && playMode != Constant.POSITION_HISTORY) {
                App.getDbManager().setMusicPrePlayTime(musicId, String.valueOf(System.currentTimeMillis()));
            }
            //new UpdateUIThread(this, context, threadNumber).start();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("chen", "playMusic Exception" + e);

            Toast.makeText(context, "播放错误", Toast.LENGTH_SHORT).show();

        }

    }


    //取一个（0，100）之间的不一样的随机数
    private void NumberRandom() {
        int count;
        do {
            count = (int) (Math.random() * 100);
        } while (count == threadNumber);
        threadNumber = count;
    }


    private void updateUI() {
        Intent playBarintent = new Intent(PlayBarFragment.ACTION_UPDATE_UI_PlayBar);    //接收广播为MusicUpdateMain
        playBarintent.putExtra(Constant.STATUS, status);
        context.sendBroadcast(playBarintent);

        Intent intent = new Intent(ACTION_UPDATE_UI_ADAPTER);    //接收广播为所有歌曲列表的adapter
        context.sendBroadcast(intent);

    }


    private void initMediaPlayer() {


        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        int current = MusicPlayManage.getIntShared(Constant.KEY_CURRENT);
        Log.d(TAG, "initMediaPlayer musicId = " + musicId);

        // 如果是没取到当前正在播放的音乐ID，则从数据库中获取第一首音乐的播放信息初始化
        if (musicId == -1) {
            return;
        }
        String path = dbManager.getMusicPath(musicId);
        if (path == null) {
            Log.e(TAG, "initMediaPlayer: path == null");
            return;
        }
        if (current == 0) {
            status = Constant.STATUS_STOP; // 设置播放状态为停止
        } else {
            status = Constant.STATUS_PAUSE; // 设置播放状态为暂停
        }
        Log.d(TAG, "initMediaPlayer status = " + status);
        MusicPlayManage.setShared(Constant.KEY_ID, musicId);
        MusicPlayManage.setShared(Constant.KEY_PATH, path);
        updateUI();
    }


}
