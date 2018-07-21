package com.music.concertoplayer;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.music.concertoplayer.activity.LoginActivity;
import com.music.concertoplayer.database.DBManager;
import com.music.concertoplayer.entity.User;
import com.music.concertoplayer.service.MusicPlayerService;
import com.music.concertoplayer.utils.AssetsUtil;
import com.music.concertoplayer.utils.LocationUtils;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;


/**
 * Created by chen on 2018/3/28.
 */

public class App extends Application {
    private static Context context;
    private static DBManager dbManager;
    private static Boolean isShowPop = true;
    private static Boolean isHistory = false; //是否点击上一首
    private static IWXAPI wxAPI;

    private static String lat = "";
    private static String lng = "";
    private static User user;


    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        dbManager = DBManager.getInstance(this);
        Logger.addLogAdapter(new AndroidLogAdapter());
        initBugly();
        initLocation();
        registToWX();

    }



    private void registToWX() {
        //AppConst.WEIXIN.APP_ID是指你应用在微信开放平台上的AppID，记得替换。
        wxAPI = WXAPIFactory.createWXAPI(this,Constant.WECHAT_APPID,true);
        wxAPI.registerApp(Constant.WECHAT_APPID);
    }


    private void initLocation() {
        Location location = LocationUtils.getInstance(context).showLocation();
        if (location != null) {
            lat = String.valueOf(location.getLatitude());
            lng = String.valueOf(location.getLongitude());
            String address = "纬度：" + location.getLatitude() + "经度：" + location.getLongitude();
            //Logger.d(address);

        }
    }


    private void initBugly() {
        CrashReport.initCrashReport(getApplicationContext(), Constant.BUGLY_LOG_APPID, false);
    }
    public static Context getContext() {
        return context;
    }

    public static DBManager getDbManager() {
        return dbManager;
    }
    @Override
    public void onTerminate() {

        super.onTerminate();
    }



    public static Boolean getIsShowPop() {
        return isShowPop;
    }

    public static void setIsShowPop(Boolean isShowPop) {
        App.isShowPop = isShowPop;
    }

    public static IWXAPI getWxAPI() {
        return wxAPI;
    }

    public static void setWxAPI(IWXAPI wxAPI) {
        App.wxAPI = wxAPI;
    }

    public static String getLat() {
        return lat;
    }

    public static void setLat(String lat) {
        App.lat = lat;
    }

    public static String getLng() {
        return lng;
    }

    public static void setLng(String lng) {
        App.lng = lng;
    }

    public static User getUser() {
        return user;
    }

    public static void setUser(User user) {
        App.user = user;
    }

    public static Boolean getIsHistory() {
        return isHistory;
    }

    public static void setIsHistory(Boolean isHistory) {
        App.isHistory = isHistory;
    }
}
