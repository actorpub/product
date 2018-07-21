package com.music.concertoplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.activity.base.BaseActivity;
import com.music.concertoplayer.adaper.MusicAdapter;
import com.music.concertoplayer.database.DBManager;
import com.music.concertoplayer.entity.FolderInfo;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.manage.MusicPlayManage;
import com.music.concertoplayer.utils.DentityUtil;
import com.music.concertoplayer.utils.RxBus;
import com.music.concertoplayer.utils.ToastUtils;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.music.concertoplayer.fragment.PlayBarFragment.ACTION_UPDATE_UI_PlayBar;

/**
 * Created by chen on 2018/3/28.
 */

public class FolderActivity extends BaseActivity {

    @BindView(R.id.rv_main)
    RecyclerView recyclerView;
    @BindView(R.id.iv_back)
    ImageView iv_back;
    @BindView(R.id.tv_folder_name)
    TextView tv_folder_name;
    @BindView(R.id.tv_folder_count)
    TextView tv_folder_count;
    @BindView(R.id.bottom_container)
    FrameLayout bottom_container;
    @BindView(R.id.rl_speed)
    RelativeLayout rl_speed;
    @BindView(R.id.tv_speed)
    TextView tv_speed;
    @BindView(R.id.cb_select)
    CheckBox cb_select;

    private List<MusicInfo> musicInfoList = new ArrayList<>();
    private FolderInfo folderInfo;
    private MusicAdapter adapter;
    private String folderName;
    private DBManager dbManager;
    private FolderReceiver mReceiver;

    private String pitchSemi = "";


    @Override
    protected void initDate() {
        musicInfoList.clear();
        List<MusicInfo> newMusicInfo = dbManager.getAllMusicByFoderName(folderName);
        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        if (musicId != -1) {
            for (MusicInfo musicInfo : newMusicInfo) {

                //上次播放
                if (folderInfo.getPre_play_id() != -1) {
                    if (musicInfo.getId() == folderInfo.getPre_play_id()) {
                        musicInfo.setPlayColor(2);
                    }
                }
                //正在播放
                if (musicInfo.getId() == musicId) {
                    musicInfo.setPlayColor(1);
                }

            }
        }
        musicInfoList.addAll(newMusicInfo);
        adapter.notifyDataSetChanged();
        showQuickControl(true);
        if (MusicPlayManage.getIntShared(Constant.KEY_ID )!= -1) {
            if (App.getIsShowPop()) {
                bottom_container.setVisibility(View.VISIBLE);
                App.setIsShowPop(true);
            } else {
                bottom_container.setVisibility(View.GONE);
                App.setIsShowPop(false);
            }

        } else {
            bottom_container.setVisibility(View.GONE);
            App.setIsShowPop(false);
        }
    }

    @Override
    protected void initViews() {
        folderName = getIntent().getStringExtra("FolderInfo");
        dbManager = App.getDbManager();
        folderInfo = dbManager.getFolderNameByName(folderName);
        if (folderInfo.getName().equalsIgnoreCase("folder_love")) {
            tv_folder_name.setText("喜欢");
        } else {
            tv_folder_name.setText(folderInfo.getName());

        }
        tv_folder_count.setText(folderInfo.getFolder_count() + "");
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        adapter = new MusicAdapter(this, musicInfoList);
        //recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        initTitle();
        initSpeed();
        register();
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
                            initTitle();
                            updateAdapterData();

                        } else if (msg.equalsIgnoreCase("增加播放次数")){
                            initDate();

                        } else if (msg.equalsIgnoreCase("lose_bottom_bar")) {
                            bottom_container.setVisibility(View.GONE);
                            App.setIsShowPop(false);

                        } else if (msg.equalsIgnoreCase("open_bottom_bar")){
                            bottom_container.setVisibility(View.VISIBLE);
                            App.setIsShowPop(true);
                        }

                    }
                });
        if (folderInfo.getFolder_select() == 0) {
            cb_select.setChecked(false);
        } else {
            cb_select.setChecked(true);

        }
        cb_select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    dbManager.updateFolderSelect(folderName, 1);
                } else {
                    dbManager.updateFolderSelect(folderName, 0);
                }
            }
        });

    }

    private void initSpeed() {
        String speed = MusicPlayManage.getStringShared(Constant.KEY_SPEED);
        pitchSemi = MusicPlayManage.getStringShared(Constant.KEY_PITCHSEMI);
        if (TextUtils.isEmpty(speed)) {
            tv_speed.setText("1.0 x");
        } else {
            String speedRange = MusicPlayManage.getStringShared(Constant.KEY_SPEED_RANGE);
            if (!TextUtils.isEmpty(speedRange)) {
                if (speedRange.equalsIgnoreCase("speedToAllSong")) {
                    //当前播放速度适用于所有音乐
                    tv_speed.setText(speed + " x");
                } else {
                    //单个文件夹
                    if (folderName.equalsIgnoreCase(speedRange)) {
                        tv_speed.setText(speed + " x");
                    } else {
                        tv_speed.setText("1.0 x");
                    }

                }

            } else {
                tv_speed.setText("1.0 x");
            }

        }
        if (TextUtils.isEmpty(pitchSemi)) {
            pitchSemi = "1";
        }
    }

    private void initTitle() {
        if (MusicPlayManage.getStringShared(Constant.FOLDER_NAME).equalsIgnoreCase(folderInfo.getName())) {
            //Logger.d("initTitle" + MusicPlayManage.getStringShared(Constant.KEY_LOCATION_COUNT));

                tv_folder_count.setText(MusicPlayManage.getStringShared(Constant.KEY_LOCATION_COUNT));

        } else {
            tv_folder_count.setText("");

        }

    }

    @OnClick(R.id.rl_speed)
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_speed:
                showSpeedDialog();
                break;
        }


    }


    public void showSpeedDialog() {
        LayoutInflater inflaterDl = LayoutInflater.from(this);
        LinearLayout layout = (LinearLayout) inflaterDl.inflate(R.layout.dialog_speed, null);
        //对话框
        final AlertDialog dialogSpeed = new AlertDialog.Builder(this,R.style.MyDialog).create();
        dialogSpeed.show();
        Window window = dialogSpeed.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        Display display = this.getWindowManager().getDefaultDisplay();
        lp.gravity = Gravity.TOP;
        lp.width = LinearLayout.LayoutParams.MATCH_PARENT;//宽高可设置具体大小
        lp.height = DentityUtil.dip2px(this, 350);
        lp.y = DentityUtil.dip2px(this, 40);
        dialogSpeed.getWindow().getDecorView().setPadding(0, 0, 0, 0);
        dialogSpeed.getWindow().setAttributes(lp);
        dialogSpeed.getWindow().setContentView(layout);
        dialogSpeed.setCanceledOnTouchOutside(true);
        dialogSpeed.setCancelable(true);

        ImageView iv_close = layout.findViewById(R.id.iv_close);
        final Button btn_low = layout.findViewById(R.id.btn_speed_low);
        final Button btn_speed = layout.findViewById(R.id.btn_speed);
        final Button btn_speed_high = layout.findViewById(R.id.btn_speed_high);
        final Button btn_speed_highest = layout.findViewById(R.id.btn_speed_highest);

        final Button btn_add = layout.findViewById(R.id.btn_add);
        final Button btn_cover = layout.findViewById(R.id.btn_cover);
        final Button btn_subtract = layout.findViewById(R.id.btn_subtract);

        Button btn_save = layout.findViewById(R.id.btn_save);
        final RadioGroup rg_speed = layout.findViewById(R.id.rg_speed);
        final RadioButton rb_folder = layout.findViewById(R.id.rb_folder);
        final RadioButton rb_all_song = layout.findViewById(R.id.rb_all_song);

        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Integer.valueOf(pitchSemi) >= 10) {
                    ToastUtils.showToast("音调不能大于10");
                    return;
                }

                pitchSemi = String.valueOf(Integer.valueOf(pitchSemi) + 1);
                ToastUtils.showToast("现在音调为" + pitchSemi );
            }
        });
        btn_cover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pitchSemi = "1";
                ToastUtils.showToast("现在音调为" + pitchSemi );
            }
        });
        btn_subtract.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Integer.valueOf(pitchSemi) <= -10) {
                    ToastUtils.showToast("音调不能低于-10");
                    return;
                }
                pitchSemi = String.valueOf(Integer.valueOf(pitchSemi) - 1);
                ToastUtils.showToast("现在音调为" + pitchSemi );
            }
        });


        String range = MusicPlayManage.getStringShared(Constant.KEY_SPEED_RANGE);
        if (TextUtils.isEmpty(range)) {
            btn_speed.setEnabled(false);
        } else {
            String speed = MusicPlayManage.getStringShared(Constant.KEY_SPEED);
            if (speed.equalsIgnoreCase("0.5")) {
                btn_low.setEnabled(false);
            } else if (speed.equalsIgnoreCase("1.5")) {
                btn_speed_high.setEnabled(false);
            }else if (speed.equalsIgnoreCase("2.0")) {
                btn_speed_highest.setEnabled(false);
            }else {
                btn_speed.setEnabled(false);
            }
            if (range.equalsIgnoreCase("speedToAllSong")) {
                rb_folder.setChecked(false);
                rb_all_song.setChecked(true);
            } else {
                rb_all_song.setChecked(false);
                rb_folder.setChecked(true);
            }
        }

        btn_low.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_low.setEnabled(false);
                btn_speed.setEnabled(true);
                btn_speed_high.setEnabled(true);
                btn_speed_highest.setEnabled(true);

            }
        });
        btn_speed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_speed.setEnabled(false);
                btn_low.setEnabled(true);
                btn_speed_high.setEnabled(true);
                btn_speed_highest.setEnabled(true);

            }
        });
        btn_speed_high.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_speed_high.setEnabled(false);
                btn_speed.setEnabled(true);
                btn_low.setEnabled(true);
                btn_speed_highest.setEnabled(true);

            }
        });
        btn_speed_highest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btn_speed_highest.setEnabled(false);
                btn_speed.setEnabled(true);
                btn_speed_high.setEnabled(true);
                btn_low.setEnabled(true);

            }
        });

        iv_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogSpeed.dismiss();
            }
        });


        rb_folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rg_speed.clearCheck();
                rb_folder.setChecked(true);
            }
        });

        rb_all_song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rg_speed.clearCheck();
                rb_all_song.setChecked(true);
            }
        });
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String speed = "";
                if (!btn_low.isEnabled()) {
                    speed = "0.5";
                } else if (!btn_speed.isEnabled()) {
                    speed = "1.0";
                }else if (!btn_speed_high.isEnabled()) {
                    speed = "1.5";
                } else if (!btn_speed_highest.isEnabled()) {
                    speed = "2.0";
                }
                Logger.d("speed" + speed);

                if (TextUtils.isEmpty(speed)) {
                    ToastUtils.showToast("请选择倍速！");
                    return;
                }

                String speedRange = "";
                if (rb_all_song.isChecked()) {
                    speedRange = "speedToAllSong";
                } else if (rb_folder.isChecked()) {
                    speedRange = folderName;
                }
                if (TextUtils.isEmpty(speedRange)) {
                    ToastUtils.showToast("请选择适用范围！");
                    return;
                }
                //Logger.d("KEY_SPEED" + speed);
                MusicPlayManage.setShared(Constant.KEY_SPEED, speed);
                MusicPlayManage.setShared(Constant.KEY_PITCHSEMI, pitchSemi);
                MusicPlayManage.setShared(Constant.KEY_SPEED_RANGE, speedRange);
                MusicPlayManage.speedTo(FolderActivity.this);
                initSpeed();
                dialogSpeed.dismiss();
            }
        });

    }
    @Override
    protected int getContentSrc() {
        return R.layout.activity_folder;
    }


    class FolderReceiver extends BroadcastReceiver {

        int status;

        @Override
        public void onReceive(Context context, Intent intent) {
            status = intent.getIntExtra(Constant.STATUS, 0);
            switch (status) {
                case Constant.STATUS_STOP:

                    break;
                case Constant.STATUS_PLAY:

                    break;
                case Constant.STATUS_PAUSE:

                    break;
                case Constant.STATUS_RUN:

                    break;
                default:
                    break;
            }

        }
    }

    private void updateAdapterData() {
        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        int prePlayId = dbManager.getFolderPrePlayIdByName(folderName);
        //Logger.d("PlayerManagerReceiver.status" + PlayerManagerReceiver.status);
        if (musicId != -1) {
            for (MusicInfo musicInfo : musicInfoList) {
                musicInfo.setPlayColor(0);

                //上次播放
                if (prePlayId != -1) {
                    if (musicInfo.getId() == prePlayId) {
                        musicInfo.setPlayColor(2);
                    }
                }
                //正在播放
                if (musicInfo.getId() == musicId) {
                    musicInfo.setPlayColor(1);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegister();
    }

    private void register() {
        mReceiver = new FolderReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UPDATE_UI_PlayBar);
        registerReceiver(mReceiver, intentFilter);
    }

    private void unRegister() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }
}
