package com.music.concertoplayer.view;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.database.DBManager;
import com.music.concertoplayer.fragment.PlayBarFragment;
import com.music.concertoplayer.manage.MusicPlayManage;
import com.music.concertoplayer.receiver.PlayerManagerReceiver;
import com.music.concertoplayer.service.MusicPlayerService;
import com.music.concertoplayer.net.LogUtils;
import com.music.concertoplayer.utils.RxBus;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.music.concertoplayer.Constant.STATUS_RUN;
import static com.music.concertoplayer.receiver.PlayerManagerReceiver.status;
import static com.music.concertoplayer.utils.TimeUtil.formatTime;

/**
 * Created by lijunyan on 2017/4/5.
 */

public class PlayingPopWindow extends PopupWindow implements View.OnClickListener {

    private static final String TAG = PlayingPopWindow.class.getName();
    private View view;
    private Activity activity;
    private ImageView iv_down, iv_order, iv_pre_skip, iv_pre, iv_play, iv_next, iv_next_skip, iv_love;
    private TextView tv_name, tv_folder_name, tv_current_time, tv_total_time;
    private SeekBar seekBar;
    private DBManager dbManager;
    private int current;
    private int duration;
    private int mProgress;
    private PlayReceiver mReceiver;
    private boolean mReceiverTag = false;//广播接受者标识

    public PlayingPopWindow(Activity activity) {
        super(activity);
        this.activity = activity;
        dbManager = DBManager.getInstance(activity);
        initView();
        register();
        LogUtils.inserOperationLog("展开播放控制面板");
    }

    private void initView() {
        this.view = LayoutInflater.from(activity).inflate(R.layout.playbar_show_window, null);
        this.setContentView(this.view);
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size);
        int height = (int) (size.y * 0.35);
        this.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        this.setHeight(height);

        this.setFocusable(true);
        this.setOutsideTouchable(true);

        // 设置弹出窗体的背景
        this.setBackgroundDrawable(activity.getResources().getDrawable(R.color.colorWhite));
        // 设置弹出窗体显示时的动画，从底部向上弹出
        this.setAnimationStyle(R.style.pop_window_animation);

        // 添加OnTouchListener监听判断获取触屏位置，如果在选择框外面则销毁弹出框
        this.view.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                int height = view.getTop();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (y < height) {
                        dismiss();
                    }
                }
                return true;
            }
        });

        iv_down = view.findViewById(R.id.iv_down);
        tv_name = view.findViewById(R.id.tv_name);
        tv_folder_name = view.findViewById(R.id.tv_folder_name);
        iv_order = view.findViewById(R.id.iv_order);
        tv_current_time = view.findViewById(R.id.tv_current_time);
        tv_total_time = view.findViewById(R.id.tv_total_time);
        iv_pre_skip = view.findViewById(R.id.iv_pre_skip);
        iv_pre = view.findViewById(R.id.iv_pre);
        iv_play = view.findViewById(R.id.iv_play);
        iv_next = view.findViewById(R.id.iv_next);
        iv_next_skip = view.findViewById(R.id.iv_next_skip);
        iv_love = view.findViewById(R.id.iv_love);
        seekBar = view.findViewById(R.id.activity_play_seekbar);
        iv_pre_skip.setOnClickListener(this);
        iv_pre.setOnClickListener(this);
        iv_play.setOnClickListener(this);
        iv_next.setOnClickListener(this);
        iv_next_skip.setOnClickListener(this);
        iv_order.setOnClickListener(this);
        iv_love.setOnClickListener(this);
        initPlayMode();
        setMusicName();
        initSeekBar();

        iv_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        tv_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                seekBar_touch = true;	//可以拖动标志
                int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
                if (musicId == -1) {
                    Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
                    intent.putExtra("cmd", Constant.COMMAND_STOP);
                    activity.sendBroadcast(intent);
                    Toast.makeText(activity, "歌曲不存在", Toast.LENGTH_LONG).show();
                    return;
                }

                //发送播放请求
                Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
                intent.putExtra("cmd", Constant.COMMAND_PROGRESS);
                intent.putExtra("current", mProgress);
                activity.sendBroadcast(intent);
                LogUtils.inserOperationLog("播放进度");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mProgress = progress;
                initTime();
            }
        });

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
                            initPlayMode();
                            if (PlayerManagerReceiver.status == Constant.STATUS_PLAY) {
                                iv_play.setSelected(true);
                            } else {
                                iv_play.setSelected(false);
                            }
                        }

                    }
                });


    }

    private void initSeekBar() {
        //一开始点进去的时候
        current = MusicPlayManage.getIntShared(Constant.PLAY_CURRENT);
        duration = MusicPlayManage.getIntShared(Constant.PLAY_TOTAL);
        if (current != -1 && duration != -1) {
            seekBar.setMax(duration);
            seekBar.setProgress(current);
            tv_current_time.setText(formatTime(current));
            tv_total_time.setText(formatTime(duration));
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.iv_order:
                switchPlayMode();
                break;
            case R.id.iv_play:
                play();
                break;
            case R.id.iv_next:
                MusicPlayManage.playNextMusic(activity, 0);
                break;
            case R.id.iv_pre:
                MusicPlayManage.playPreMusic(activity);
                break;
            case R.id.iv_next_skip:
                MusicPlayManage.skipNext(activity, current);
                LogUtils.inserOperationLog("快进");
                break;
            case R.id.iv_pre_skip:
                MusicPlayManage.skipPre(activity, current);
                LogUtils.inserOperationLog("后退");
                break;
            case R.id.iv_love:
                int musicId2 = MusicPlayManage.getIntShared(Constant.KEY_ID);
                if (App.getDbManager().isLoveByMusicId(String.valueOf(musicId2))) {
                    iv_love.setImageLevel(0);
                    App.getDbManager().setMyLove(musicId2,0);
                } else {
                    iv_love.setImageLevel(1);
                    App.getDbManager().setMyLove(musicId2,1);
                }
                RxBus.getDefault().post("喜爱");
                break;
        }
    }

    private void play() {
        int musicId;
        musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        if (musicId == -1 || musicId == 0) {
            musicId = dbManager.getFirstId(Constant.LIST_ALLMUSIC);
            Intent intent = new Intent(Constant.MP_FILTER);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_STOP);
            activity.sendBroadcast(intent);
            Toast.makeText(activity, "歌曲不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        //如果当前媒体在播放音乐状态，则图片显示暂停图片，按下播放键，则发送暂停媒体命令，图片显示播放图片。以此类推。
        if (status == Constant.STATUS_PAUSE) {
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_PLAY);
            activity.sendBroadcast(intent);
        } else if (status == Constant.STATUS_PLAY) {
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_PAUSE);
            activity.sendBroadcast(intent);
        } else {
            //为停止状态时发送播放命令，并发送将要播放歌曲的路径
            String path = dbManager.getMusicPath(musicId);
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_PLAY);
            intent.putExtra(Constant.KEY_PATH, path);
            if (status == Constant.STATUS_STOP) {
                intent.putExtra(Constant.KEY_CURRENT, current);
            }

            Log.i(TAG, "onClick: path = " + path);
            activity.sendBroadcast(intent);
        }
    }


    private void switchPlayMode() {
        int mode = MusicPlayManage.getIntShared(Constant.KEY_ORDER_MODE);
        if (mode == Constant.LOOP_LIST) {
            mode = Constant.LOOP_NONE;
        } else if (mode == Constant.LOOP_NONE) {
            mode = Constant.LOOP_ONE;
        } else {
            mode = Constant.LOOP_LIST;
        }
        MusicPlayManage.setShared(Constant.KEY_ORDER_MODE, mode);
        initPlayMode();
        RxBus.getDefault().post("刷新");
    }

    private void initPlayMode() {
        int playMode = MusicPlayManage.getIntShared(Constant.KEY_ORDER_MODE);
        if (playMode == -1) {
            playMode = 0;
        }
        iv_order.setImageLevel(playMode);

    }


    class PlayReceiver extends BroadcastReceiver {

        int status;

        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(TAG, "onReceive: ");
            status = intent.getIntExtra(Constant.STATUS, 0);
            current = intent.getIntExtra(Constant.KEY_CURRENT, 0);
            duration = intent.getIntExtra(Constant.KEY_DURATION, -100);
            //Log.d("chen", status + "onReceive: " +current +"---" + duration);
            setMusicName();
            if (duration == -100) {
                if (status == Constant.STATUS_STOP) {
                    //一开始点进去的时候
                    current = MusicPlayManage.getIntShared(Constant.PLAY_CURRENT);
                    duration = MusicPlayManage.getIntShared(Constant.PLAY_TOTAL);
                    if (current != -1 && duration != -1) {
                        seekBar.setMax(duration);
                        seekBar.setProgress(current);
                        tv_current_time.setText(formatTime(current));
                        tv_total_time.setText(formatTime(duration));
                    }
                }
                return;
            }
            tv_current_time.setText(formatTime(current));
            tv_total_time.setText(formatTime(duration));
            switch (status) {
                case Constant.STATUS_STOP:
                    iv_play.setSelected(false);
                    break;
                case Constant.STATUS_PLAY:
                    iv_play.setSelected(true);
                    seekBar.setMax(duration);
                    seekBar.setProgress(current);
                    break;
                case Constant.STATUS_PAUSE:
                    iv_play.setSelected(false);
                    break;
                case STATUS_RUN:
                    iv_play.setSelected(true);
                    seekBar.setMax(duration);
                    seekBar.setProgress(current);
                    break;
                default:
                    break;
            }

        }
    }


    private void setMusicName() {
        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        if (musicId == -1) {
            tv_name.setText("听听音乐");
            tv_folder_name.setText("");
        } else {
            tv_name.setText(dbManager.getMusicInfo(musicId).get(1));
            if (dbManager.getMusicInfo(musicId).get(9).equalsIgnoreCase(Constant.FOLDER_LOVE)) {
                tv_folder_name.setText("喜欢" + "   " + MusicPlayManage.getStringShared(Constant.KEY_LOCATION_COUNT));
            } else {

                tv_folder_name.setText(dbManager.getMusicInfo(musicId).get(9) + "   " + MusicPlayManage.getStringShared(Constant.KEY_LOCATION_COUNT));
            }

        }
        if (App.getDbManager().isLoveByMusicId(String.valueOf(musicId))) {
            iv_love.setImageLevel(1);
        } else {
            iv_love.setImageLevel(0);
        }
    }


    private void initTime() {
        tv_current_time.setText(formatTime(current));
        tv_total_time.setText(formatTime(duration));
//        if (progress - mLastProgress >= 1000) {
//            tvCurrentTime.setText(formatTime(progress));
//            mLastProgress = progress;
//        }
    }


    @Override
    public void dismiss() {
        unRegister();
        LogUtils.inserOperationLog("收起播放控制面板");
        super.dismiss();
    }

    private void register() {
        if (!mReceiverTag) {
            mReceiver = new PlayReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(PlayBarFragment.ACTION_UPDATE_UI_PlayBar);
            App.getContext().registerReceiver(mReceiver, intentFilter);
            mReceiverTag = true;    //标识值 赋值为 true 表示广播已被注册
        }

    }

    private void unRegister() {
        if (mReceiver != null && mReceiverTag) {
            mReceiverTag = false;   //Tag值 赋值为false 表示该广播已被注销
            App.getContext().unregisterReceiver(mReceiver);
        }
    }


}
