package com.music.concertoplayer.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.activity.base.BaseActivity;
import com.music.concertoplayer.entity.Data;
import com.music.concertoplayer.entity.LoginInfo;
import com.music.concertoplayer.entity.User;
import com.music.concertoplayer.entity.WeiXin;
import com.music.concertoplayer.entity.WeiXinInfo;
import com.music.concertoplayer.entity.WeiXinToken;
import com.music.concertoplayer.utils.AssetsUtil;
import com.music.concertoplayer.utils.LocationUtils;
import com.music.concertoplayer.utils.PhoneUtil;
import com.music.concertoplayer.utils.RxBus;
import com.music.concertoplayer.utils.ToastUtils;
import com.music.concertoplayer.net.UpdateUtil;
import com.music.concertoplayer.view.dialog.MyDialogHandler;
import com.orhanobut.logger.Logger;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;

import java.io.File;
import java.io.IOException;
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

import static com.music.concertoplayer.Constant.LOGIN_URL;

/**
 * Created by chen on 2018/4/11.
 */

public class LoginActivity extends BaseActivity {
    private IWXAPI wxAPI;

    @BindView(R.id.tv_weixin_login)
    TextView tv_weixin_login;
    @BindView(R.id.btn_login)
    Button btn_login;
    @BindView(R.id.tv_login_error)
    TextView tv_login_error;

    private RxPermissions mRxPermissions;

    //private long currentTime = 0;


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constant.LOGIN_SUCCESS:
                    uiFlusHandler.sendEmptyMessage(DISMISS_LOADING_DIALOG);
                    App.getDbManager().insertUser(App.getUser());
                    intoMain();

                    break;
                case Constant.LOGIN_UPDATE:
                    uiFlusHandler.sendEmptyMessage(DISMISS_LOADING_DIALOG);
                    UpdateUtil.update(LoginActivity.this,mHandler);
                    //login();
                    break;
                case Constant.LOGIN_REPEATE:
                    uiFlusHandler.sendEmptyMessage(DISMISS_LOADING_DIALOG);
                    ToastUtils.showToast("请勿重复登录");
                    tv_login_error.setVisibility(View.VISIBLE);
                    tv_weixin_login.setText("使用其他微信登录");
                    break;
                case Constant.LOGIN_OUT_TIME:
                    uiFlusHandler.sendEmptyMessage(DISMISS_LOADING_DIALOG);
                    ToastUtils.showToast("登录超时");
                    tv_login_error.setVisibility(View.VISIBLE);
                    tv_weixin_login.setText("使用其他微信登录");
                    break;

            }
            super.handleMessage(msg);
        }
    };


    private void intoMain() {
        LoginActivity.this.startActivity(new Intent(LoginActivity.this, MainActivity.class));
        ToastUtils.showToast("登录成功");
        finish();
    }

    @Override
    protected void initDate() {
        mRxPermissions = new RxPermissions(LoginActivity.this);
        mRxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                 Manifest.permission.READ_PHONE_STATE)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            if (TextUtils.isEmpty(App.getDbManager().getUser().getNickname())) {
                                Logger.d("请登录");
                                initLogin();

                            } else {
                                Logger.d("已登录 无需再次授权");
                                LoginActivity.this.startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();

                            }
                        } else {
                            ToastUtils.showToast("请开启权限");
                            finish();
                        }
                    }
                });
        uiFlusHandler = new MyDialogHandler(this, "正在登录...");

    }





    @Override
    protected void onResume() {
        /*//检查更新 用于用户下载apk后没有安装而是再次进入到界面
        if (App.getUser() != null) {
            UpdateUtil.update(LoginActivity.this,mHandler);
        }*/
        super.onResume();

    }

    @OnClick({R.id.tv_weixin_login,R.id.btn_login})
    public void onClick(View view) {
        uiFlusHandler.sendEmptyMessage(SHOW_LOADING_DIALOG);
            login();
        /*WeiXinInfo obj = new WeiXinInfo();
        obj.setUnionid("0123456789");
        obj.setOpenid("9876543210");
        obj.setNickname("test");
        postLogin(obj, "1");*/
    }

    private void initLogin() {
        wxAPI = App.getWxAPI();
        RxBus.getDefault().toObservable(WeiXin.class)
                //在io线程进行订阅，可以执行一些耗时操作
                .subscribeOn(Schedulers.io())
                //在主线程进行观察，可做UI更新操作
                .observeOn(AndroidSchedulers.mainThread())
                //观察的对象
                .subscribe(new Action1<WeiXin>() {
                    @Override
                    public void call(WeiXin weiXin) {
                        getAccessToken(weiXin.getCode());

                    }
                });

    }


    @Override
    protected void initViews() {
    }



    @Override
    protected int getContentSrc() {
        return R.layout.activity_login;
    }


    /**
     * 微信登陆(三个步骤)
     * 1.微信授权登陆
     * 2.根据授权登陆code 获取该用户token
     * 3.根据token获取用户资料
     */
    public void login() {
        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        //req.state = String.valueOf(System.currentTimeMillis());
        req.state = "diandi_wx_login";
        wxAPI.sendReq(req);
    }

    public void getAccessToken(String code) {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
                "appid=" + Constant.WECHAT_APPID + "&secret=" + Constant.WECHAT_SECRET +
                "&code=" + code + "&grant_type=authorization_code";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.sendEmptyMessage(Constant.LOGIN_OUT_TIME);
                ToastUtils.showToast("登录失败！");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {//回调的方法执行在子线程。
                    String json = response.body().string();
                    Logger.d(json);
                    WeiXinToken obj = (WeiXinToken) JSON.parseObject(json, WeiXinToken.class);
                    if (obj.getErrcode() == 0) {//请求成功
                        Logger.d(obj);
                        getWeiXinUserInfo(obj);
                    } else {//请求失败
                        ToastUtils.showToast(obj.getErrmsg());
                    }
                } else {
                    mHandler.sendEmptyMessage(Constant.LOGIN_OUT_TIME);
                    ToastUtils.showToast("登录失败！");
                }
            }
        });

    }

    public void getWeiXinUserInfo(final WeiXinToken weiXinToken) {
        String url = "https://api.weixin.qq.com/sns/userinfo?access_token=" +
                weiXinToken.getAccess_token() + "&openid=" + weiXinToken.getOpenid();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.sendEmptyMessage(Constant.LOGIN_OUT_TIME);
                ToastUtils.showToast("登录失败！");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {//回调的方法执行在子线程。
                    String json = response.body().string();
                    WeiXinInfo obj = (WeiXinInfo) JSON.parseObject(json, WeiXinInfo.class);
                    obj.setUnionid(weiXinToken.getUnionid());
                    Logger.d(obj);
                    postLogin(obj, "1");
                }
            }
        });

    }

    public void postLogin(WeiXinInfo obj, final String from) {
        OkHttpClient client = new OkHttpClient();//创建OkHttpClient对象。
        //请求超时设置
        client = client.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS).build();
        MediaType jsonTpe = MediaType.parse("application/json; charset=utf-8");//数据类型为json格式，
        final Data data = new Data();
        data.setFrom(from);
        data.setDevice_type(PhoneUtil.getDeviceType(LoginActivity.this));
        data.setDeviceid(PhoneUtil.getsetDeviceid(LoginActivity.this));
        data.setLatitude(App.getLat());
        data.setLongitude(App.getLng());
        data.setMobile(PhoneUtil.getPhoneNum(LoginActivity.this));
        data.setNickname(obj.getNickname());
        data.setOpenid(obj.getOpenid());
        data.setUnionid(obj.getUnionid());
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("{" +
                " \"data\" : ");
        stringBuffer.append(com.alibaba.fastjson.JSON.toJSONString(data));
        stringBuffer.append("}");

        //Logger.d(stringBuffer.toString());
        RequestBody body = RequestBody.create(jsonTpe, stringBuffer.toString());
        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.d("onFailure" + e);
                mHandler.sendEmptyMessage(Constant.LOGIN_OUT_TIME);
                ToastUtils.showToast("登录失败！");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {//回调的方法执行在子线程。
                    String json = response.body().string();
                    Logger.d(json);
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

                            /*if (from.equalsIgnoreCase("1")) {
                                App.getDbManager().insertUser(user);
                            } else {
                                App.getDbManager().updateUser(user);
                            }*/
                            App.setUser(user);

                            mHandler.sendEmptyMessage(Constant.LOGIN_UPDATE);
                            break;
                        case Constant.LOGIN_OUT_TIME:
                            mHandler.sendEmptyMessage(Constant.LOGIN_OUT_TIME);
                            break;
                        case Constant.LOGIN_REPEATE:
                            mHandler.sendEmptyMessage(Constant.LOGIN_REPEATE);
                            break;

                    }

                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocationUtils.getInstance(this).removeLocationUpdatesListener();
    }




}
