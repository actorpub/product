package com.music.concertoplayer.activity;


import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.activity.base.BaseActivity;
import com.music.concertoplayer.entity.Data;
import com.music.concertoplayer.entity.LoginInfo;
import com.music.concertoplayer.entity.User;
import com.music.concertoplayer.entity.WeiXinInfo;
import com.music.concertoplayer.fragment.HistoryFragment;
import com.music.concertoplayer.fragment.MainFragment;
import com.music.concertoplayer.service.MusicPlayerService;
import com.music.concertoplayer.net.LogUtils;
import com.music.concertoplayer.utils.PhoneUtil;
import com.music.concertoplayer.manage.MusicPlayManage;
import com.music.concertoplayer.utils.RxBus;
import com.music.concertoplayer.utils.ToastUtils;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.music.concertoplayer.Constant.FILE_URL;
import static com.music.concertoplayer.Constant.KEY_MODE;
import static com.music.concertoplayer.Constant.LOGIN_URL;

public class MainActivity extends BaseActivity {

    @BindView(R.id.iv_all)
    ImageView iv_all;
    @BindView(R.id.iv_single)
    ImageView iv_single;
    @BindView(R.id.iv_order)
    ImageView iv_order;

    @BindView(R.id.vp_main)
    ViewPager vp_main;



    private User user;


    private Timer timer;
    private MainFragment mainFragment;


    @Override
    protected void initDate() {
        user = App.getDbManager().getUser();
        App.setUser(user);
        if (!TextUtils.isEmpty(user.getNickname())) {
             initTimerTask();
        }
        //开始上传日志
        LogUtils.postLog();

    }

    private void initTimerTask() {
        //登录进来后就进行一次请求
        WeiXinInfo weiXinInfo = new WeiXinInfo();
        weiXinInfo.setOpenid(user.getOpenid());
        weiXinInfo.setNickname(user.getNickname());
        weiXinInfo.setUnionid(user.getUnionid());
        postLogin(weiXinInfo, "0");
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
               // Logger.d("3个小时了 我要发起请求了");
                WeiXinInfo weiXinInfo = new WeiXinInfo();
                weiXinInfo.setOpenid(user.getOpenid());
                weiXinInfo.setNickname(user.getNickname());
                weiXinInfo.setUnionid(user.getUnionid());
                postLogin(weiXinInfo, "0");

            }
        };
        //time为Date类型：在指定时间执行一次。
        timer.schedule(task, 0,Constant.CHECK_MILLIS);
    }





    @Override
    protected void initViews() {
        mainFragment = new MainFragment();
        vp_main.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {

                if (position == 0) {
                    return new HistoryFragment();
                } else {
                    return mainFragment;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        });
        vp_main.setOffscreenPageLimit(0);
        vp_main.setCurrentItem(1);

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
                            reflashModeUI();

                        }

                    }
                });
        reflashModeUI();
        //开启播放服务
        Intent startIntent = new Intent(MainActivity.this,MusicPlayerService.class);
        startService(startIntent);
        scanFileAsync(this, FILE_URL);

    }


    @Override
    protected int getContentSrc() {
        return R.layout.activity_main;
    }




    @OnClick({R.id.iv_order, R.id.iv_all, R.id.iv_single})
    void onOrderClick(View view) {
        switch (view.getId()) {
            case R.id.iv_all:
                iv_all.setImageLevel(1);
                iv_single.setImageLevel(0);
                // 0 是全部  1是单个文件夹
                //MusicPlayManage.setShared(Constant.KEY_MODE, 0);
                MusicPlayManage.setShared(KEY_MODE, Constant.SEQUENCE_BY_Z);
                /*MusicPlayManage.playAllMusic(this);*/
                RxBus.getDefault().post("open_bottom_bar");
                LogUtils.inserOperationLog("横着播放");
                break;
            case R.id.iv_single:
                iv_all.setImageLevel(0);
                iv_single.setImageLevel(1);
                // 0 是全部  1是单个文件夹
                //MusicPlayManage.setShared(Constant.KEY_MODE, 1);
                MusicPlayManage.setShared(KEY_MODE, Constant.SEQUENCE_BY_N);
                LogUtils.inserOperationLog("竖着播放");
                break;
            case R.id.iv_order:
                int mode = MusicPlayManage.getIntShared(Constant.KEY_ORDER_MODE);
                if (mode == 1) {
                    mode = 3;
                } else if (mode == 3) {
                    mode = 0;
                } else {
                    mode = 1;
                }
                iv_order.setImageLevel(mode);
                MusicPlayManage.setShared(Constant.KEY_ORDER_MODE, mode);
                RxBus.getDefault().post("刷新");
                break;

            default:
                break;
        }
    }

    public void reflashModeUI() {
        if (MusicPlayManage.getIntShared(Constant.KEY_MODE) == -1) {
            iv_all.setImageLevel(1);
            iv_single.setImageLevel(0);
            // 0 是全部  1是单个文件夹
            MusicPlayManage.setShared(Constant.KEY_MODE, Constant.SEQUENCE_BY_Z);
        } else {
            if (MusicPlayManage.getIntShared(Constant.KEY_MODE) == Constant.SEQUENCE_BY_Z) {
                iv_all.setImageLevel(1);
                iv_single.setImageLevel(0);
            } else {
                iv_all.setImageLevel(0);
                iv_single.setImageLevel(1);
            }
        }


        if (MusicPlayManage.getIntShared(Constant.KEY_ORDER_MODE) == -1) {
            MusicPlayManage.setShared(Constant.KEY_ORDER_MODE, Constant.LOOP_LIST);
            iv_order.setImageLevel(0);
        } else {
            //初始循环
            int playMode = MusicPlayManage.getIntShared(Constant.KEY_ORDER_MODE);
            if (playMode == -1) {
                playMode = 0;
            }
            iv_order.setImageLevel(playMode);


        }
    }


    @Override
    public void onBackPressed() {
        exitApp();
    }

    /**
     * 退出程序
     */
    private long exitTime = 0;

    private void exitApp() {
        // 判断2次点击事件时间
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (mediaScannerConnection != null) {
            mediaScannerConnection.disconnect();
        }
        stopService();
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }

    public void postLogin(WeiXinInfo obj, final String from) {
        OkHttpClient client = new OkHttpClient();//创建OkHttpClient对象。
        //请求超时设置
        client.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS).build();
        MediaType jsonTpe = MediaType.parse("application/json; charset=utf-8");//数据类型为json格式，
        final Data data = new Data();
        data.setFrom(from);
        data.setDevice_type(PhoneUtil.getDeviceType(MainActivity.this));
        data.setDeviceid(PhoneUtil.getsetDeviceid(MainActivity.this));
        data.setLatitude(App.getLat());
        data.setLongitude(App.getLng());
        data.setMobile(PhoneUtil.getPhoneNum(MainActivity.this));
        data.setNickname(obj.getNickname());
        data.setOpenid(obj.getOpenid());
        data.setUnionid(obj.getUnionid());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("{" +
                " \"data\" : ");
        stringBuffer.append(com.alibaba.fastjson.JSON.toJSONString(data));
        stringBuffer.append("}");

        //Logger.d("开始检测" + stringBuffer.toString());
        RequestBody body = RequestBody.create(jsonTpe, stringBuffer.toString());
        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {//回调的方法执行在子线程。
                    String json = response.body().string();
                    //Logger.d(json);
                    LoginInfo loginInfo = (LoginInfo) JSON.parseObject(json, LoginInfo.class);
                    /* 正确返回{"result":0,"expiry":"20180404"} ；
                    过期返回{"result":10004,"expiry":"20180404"} ；
                    重复登录{"result":10005} ；
                    */
                    switch (loginInfo.getResult()) {
                        case Constant.LOGIN_SUCCESS:
                            User user = new User();
                            user.setDevice_type(data.getDevice_type());
                            user.setDeviceid(data.getDeviceid());
                            user.setExpiry(loginInfo.getExpiry());
                            user.setLatitude(data.getLatitude());
                            user.setLongitude(data.getLongitude());
                            user.setMobile(data.getMobile());
                            user.setNickname(data.getNickname());
                            user.setUnionid(data.getUnionid());
                            user.setOpenid(data.getOpenid());
                            App.getDbManager().updateUser(user);
                            break;
                        case Constant.LOGIN_OUT_TIME:
                        case Constant.LOGIN_REPEATE:
                            App.getDbManager().delteUser();
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ToastUtils.showToast("登录超时 请重新登录");
                                    stopService();
                                    if (timer != null) {
                                        timer.cancel();
                                    }
                                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                    finish();
                                }
                            });
                            break;

                    }

                }
            }
        });
    }
    private MediaScannerConnection mediaScannerConnection;
    public void scanFileAsync(Context ctx, final String filePath) {

        MediaScannerConnection.MediaScannerConnectionClient client = new MediaScannerConnection.MediaScannerConnectionClient() {

            @Override
            public void onScanCompleted(String path, Uri uri) {  //当client和MediaScaner扫描完成后  进行关闭我们的连接
                // TODO Auto-generated method stub
                mediaScannerConnection.disconnect();

            }

            @Override
            public void onMediaScannerConnected() {   //当client和MediaScanner完成链接后，就开始进行扫描。
                // TODO Auto-generated method stub
                mediaScannerConnection.scanFile(filePath, null);
            }
        };
        mediaScannerConnection = new MediaScannerConnection(this, client);
        mediaScannerConnection.connect();


    }


    private   void stopService() {
        // 程序终止的时候执行
        Intent intentBroadcast = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
        intentBroadcast.putExtra(Constant.COMMAND, Constant.COMMAND_RELEASE);
        sendBroadcast(intentBroadcast);
        Intent stopIntent = new Intent(this,MusicPlayerService.class);
        stopService(stopIntent);
    }
}
