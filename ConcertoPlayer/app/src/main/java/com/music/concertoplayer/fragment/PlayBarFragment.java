package com.music.concertoplayer.fragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.database.DBManager;
import com.music.concertoplayer.manage.MusicPlayManage;
import com.music.concertoplayer.receiver.PlayerManagerReceiver;
import com.music.concertoplayer.service.MusicPlayerService;
import com.music.concertoplayer.utils.RxBus;
import com.music.concertoplayer.utils.TimeUtil;
import com.music.concertoplayer.view.PlayingPopWindow;
import com.orhanobut.logger.Logger;

import de.hdodenhof.circleimageview.CircleImageView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.music.concertoplayer.receiver.PlayerManagerReceiver.status;

/**
 * Created by lijunyan on 2017/3/12.
 */

public class PlayBarFragment extends Fragment {

    private static final String TAG = "PlayBarFragment";
    public static final String ACTION_UPDATE_UI_PlayBar = "com.lijunyan.blackmusic.fragment.PlayBarFragment:action_update_ui_broad_cast";
   // private LinearLayout playBarLl;
    private ImageView playIv;
    private ImageView iv_show;
    private CircleImageView iv_order;
    private ImageView iv_love;
    private TextView musicNameTv;
    private TextView time;
    private TextView tv_count;
    private TextView tv_folder_name;
    private HomeReceiver mReceiver;
    private DBManager dbManager;
    private View view;
    private LinearLayout home_music_name_ll;
    private Context context;
    public static int current;
    private int duration;
    private PlayingPopWindow playingPopWindow;


    public static synchronized PlayBarFragment newInstance(){
        return new PlayBarFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbManager = DBManager.getInstance(getActivity());
        register();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_playbar,container,false);
        //playBarLl = (LinearLayout) view.findViewById(R.id.home_activity_playbar_ll);
        playIv = (ImageView)view.findViewById(R.id.iv_play);
        iv_order = (CircleImageView)view.findViewById(R.id.iv_order);
        iv_show = (ImageView)view.findViewById(R.id.iv_show);
        iv_love = (ImageView)view.findViewById(R.id.iv_love);
        musicNameTv = (TextView) view.findViewById(R.id.home_music_name_tv);
        tv_count = (TextView) view.findViewById(R.id.tv_count);
        tv_folder_name = (TextView) view.findViewById(R.id.tv_folder_name);
        time = (TextView) view.findViewById(R.id.tv_time);
        home_music_name_ll = (LinearLayout) view.findViewById(R.id.home_music_name_ll);

        setMusicName();
        initPlayIv();
        //setFragmentBb();
        initPlayMode();
        initLove();
        setMusicBitmap();
        playIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
                if (musicId == -1 || musicId == 0) {
                    Intent intent = new Intent(Constant.MP_FILTER);
                    intent.putExtra(Constant.COMMAND, Constant.COMMAND_STOP);
                    getActivity().sendBroadcast(intent);
                    Toast.makeText(getActivity(), "歌曲不存在", Toast.LENGTH_SHORT).show();
                    return;
                }
                //如果当前媒体在播放音乐状态，则图片显示暂停图片，按下播放键，则发送暂停媒体命令，图片显示播放图片。以此类推。
                if (status == Constant.STATUS_PAUSE) {
                    Logger.d("status == Constant.STATUS_PAUSE");
                    Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
                    intent.putExtra(Constant.COMMAND,Constant.COMMAND_PLAY);
                    getActivity().sendBroadcast(intent);
                }else if (status == Constant.STATUS_PLAY) {
                    Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
                    intent.putExtra(Constant.COMMAND, Constant.COMMAND_PAUSE);
                    getActivity().sendBroadcast(intent);
                }else {
                    Logger.d("status == Constant.STATUS_STOP");
                    //为停止状态时发送播放命令，并发送将要播放歌曲的路径
                    String path = dbManager.getMusicPath(musicId);
                    Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
                    intent.putExtra(Constant.COMMAND, Constant.COMMAND_PLAY);
                    intent.putExtra(Constant.KEY_PATH, path);
                    if (PlayerManagerReceiver.status == Constant.STATUS_STOP) {
                        intent.putExtra(Constant.KEY_CURRENT, current);
                    }
                    Log.i(TAG, "onClick: path = "+path);
                    getActivity().sendBroadcast(intent);
                }

            }
        });


        iv_show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopFormBottom();
            }
        });
        home_music_name_ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopFormBottom();
            }
        });
        iv_love.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int musicId2 = MusicPlayManage.getIntShared(Constant.KEY_ID);
                if (App.getDbManager().isLoveByMusicId(String.valueOf(musicId2))) {
                    iv_love.setImageLevel(0);
                    App.getDbManager().setMyLove(musicId2,0);
                } else {
                    iv_love.setImageLevel(1);
                    App.getDbManager().setMyLove(musicId2,1);
                }
                RxBus.getDefault().post("喜爱");
            }
        });
        iv_order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RxBus.getDefault().post("lose_bottom_bar");
                /*if (status == Constant.STATUS_STOP || status == Constant.STATUS_PAUSE) {
                    RxBus.getDefault().post("lose_bottom_bar");
                    return;
                }

                int mode = MusicPlayManage.getIntShared(Constant.KEY_ORDER_MODE);
                if (mode == 1) {
                    mode = 3;
                } else if (mode == 3) {
                    mode = 0;
                } else {
                    mode = 1;
                }
                iv_order.setImageLevel(mode);
                MusicPlayManage.setShared(Constant.KEY_ORDER_MODE,mode);
                RxBus.getDefault().post("刷新");*/
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
                            setMusicBitmap();
                        } else if (msg.equalsIgnoreCase("喜爱")) {
                            initLove();
                        }

                    }
                });

        return view;
    }
    public void initLove() {
        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        if (App.getDbManager().isLoveByMusicId(String.valueOf(musicId))) {
            iv_love.setImageLevel(1);
        } else {
            iv_love.setImageLevel(0);
        }
    }

    private void initPlayMode() {
        /*if (status == Constant.STATUS_STOP || status == Constant.STATUS_PAUSE) {
            iv_order.setImageLevel(4);
            return;
        }
        int playMode = MusicPlayManage.getIntShared(Constant.KEY_ORDER_MODE);
        if (playMode == -1) {
            playMode = 0;
        }
        iv_order.setImageLevel(playMode);*/


    }

    public void showPopFormBottom() {
         playingPopWindow = new PlayingPopWindow(getActivity());
//      设置Popupwindow显示位置（从底部弹出）
        playingPopWindow.showAtLocation(view, Gravity.BOTTOM| Gravity.CENTER_HORIZONTAL, 0, 0);
        WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
        //当弹出Popupwindow时，背景变半透明
        params.alpha=0.7f;
        getActivity().getWindow().setAttributes(params);

        //设置Popupwindow关闭监听，当Popupwindow关闭，背景恢复1f
        playingPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
                params.alpha=1f;
                getActivity().getWindow().setAttributes(params);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MusicPlayManage.setShared(Constant.PLAY_TOTAL,duration);
        MusicPlayManage.setShared(Constant.PLAY_CURRENT,current);
        unRegister();
        if (playingPopWindow != null ) {
            playingPopWindow.dismiss();

        }
    }

    public void setFragmentBb(){
        //获取播放控制栏颜色
        int defaultColor = 0xFFFFFF;
        int[] attrsArray = {R.attr.play_bar_color };
        TypedArray typedArray = context.obtainStyledAttributes(attrsArray);
        int color = typedArray.getColor(0, defaultColor);
        typedArray.recycle();
        //playBarLl.setBackgroundColor(color);
    }

    private void register() {
        mReceiver = new HomeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UPDATE_UI_PlayBar);
        App.getContext().registerReceiver(mReceiver, intentFilter);
    }

    private void unRegister() {
        if (mReceiver != null) {
            App.getContext().unregisterReceiver(mReceiver);
        }
    }

    private void setMusicName(){
        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        if (musicId == -1){
            musicNameTv.setText("听音乐");
            time.setText("好音质");
            tv_folder_name.setText("");
            tv_count.setText("");
        }else{
            musicNameTv.setText(dbManager.getMusicInfo(musicId).get(1));
            if (dbManager.getMusicInfo(musicId).get(9).equalsIgnoreCase(Constant.FOLDER_LOVE)) {
                tv_folder_name.setText("喜欢");
            } else {
                tv_folder_name.setText(dbManager.getMusicInfo(musicId).get(9));
            }
            tv_count.setText( MusicPlayManage.getStringShared(Constant.KEY_LOCATION_COUNT));
            current = MusicPlayManage.getIntShared(Constant.PLAY_CURRENT);
            duration = MusicPlayManage.getIntShared(Constant.PLAY_TOTAL);
            if (current != -1 && duration != -1) {
                time.setText(TimeUtil.formatTime(duration - current));
            }
        }

    }
    private void setMusicBitmap() {
        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        if (musicId == -1){
            /*iv_order.setBackground(getResources().getDrawable(R.drawable.default_artist_img));*/
        }else{
            iv_order.setImageBitmap(dbManager.getFolderNameByName(dbManager.getMusicInfo(musicId).get(9)).getBitmap());
            //iv_order.setImageBitmap(dbManager.getFolderNameByName(dbManager.getMusicInfo(musicId).get(9)).getBitmap());
        }
    }

    private void initPlayIv(){
        int status = PlayerManagerReceiver.status;
        switch (status) {
            case Constant.STATUS_STOP:
                playIv.setSelected(false);
                break;
            case Constant.STATUS_PLAY:
                playIv.setSelected(true);
                break;
            case Constant.STATUS_PAUSE:
                playIv.setSelected(false);
                break;
            case Constant.STATUS_RUN:
                playIv.setSelected(true);
                break;
        }
    }


    class HomeReceiver extends BroadcastReceiver {

        int status;

        @Override
        public void onReceive(Context context, Intent intent) {
            setMusicName();
            initLove();
            initPlayIv();
            status = intent.getIntExtra(Constant.STATUS,0);
            current = intent.getIntExtra(Constant.KEY_CURRENT,0);
            duration = intent.getIntExtra(Constant.KEY_DURATION,-100);
            if (duration == -100) {
                //Log.d(TAG, "onReceive: "+ PlayerManagerReceiver.status);
                if (PlayerManagerReceiver.status == Constant.STATUS_STOP) {
                    //一开始点进去的时候
                    current = MusicPlayManage.getIntShared(Constant.PLAY_CURRENT);
                    duration = MusicPlayManage.getIntShared(Constant.PLAY_TOTAL);
                    if (current != -1 && duration != -1) {
                        time.setText(TimeUtil.formatTime(duration - current));
                    }
                }
                return;
            }
            time.setText(TimeUtil.formatTime(duration - current));
            switch (status){
                case Constant.STATUS_STOP:
                    playIv.setSelected(false);
                    MusicPlayManage.setShared(Constant.PLAY_TOTAL,duration);
                    MusicPlayManage.setShared(Constant.PLAY_CURRENT,current);
                    //iv_order.setImageLevel(4);
                    break;
                case Constant.STATUS_PLAY:

                    playIv.setSelected(true);
                    MusicPlayManage.setShared(Constant.PLAY_TOTAL,duration);
                    MusicPlayManage.setShared(Constant.PLAY_CURRENT,current);
                    break;
                case Constant.STATUS_PAUSE:
                    playIv.setSelected(false);
                    MusicPlayManage.setShared(Constant.PLAY_TOTAL,duration);
                    MusicPlayManage.setShared(Constant.PLAY_CURRENT,current);
                    //iv_order.setImageLevel(4);

                    break;
                case Constant.STATUS_RUN:
                    playIv.setSelected(true);
                    break;
                default:
                    break;
            }

        }
    }
}
