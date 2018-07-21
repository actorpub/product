package com.music.concertoplayer.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by chen on 2018/4/13.
 */

public class PhoneUtil {
    public static String getPhoneNum(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            String te1 = tm.getLine1Number();//获取本机号码
            if (TextUtils.isEmpty(te1)) {
                te1 = "";
            }
            //Log.d("chen", "getPhoneNum" + te1);
            return te1;
        } else {
            return "";
        }

    }

    public static String getsetDeviceid(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context,new String[]{Manifest.permission.READ_PHONE_STATE},1);
            return null;
        }
        String imei = "";
        try {
            final TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if(manager.getDeviceId() == null || manager.getDeviceId().equals("")) {
                if (Build.VERSION.SDK_INT >= 23) {
                    imei = manager.getDeviceId(0);
                }
            }else{
                imei = manager.getDeviceId();
            }
        }catch (Exception e)
        {
            return "";
        }
        return imei;


    }
    public static String getDeviceType(Context context) {
        String xinghao = android.os.Build.MODEL;
        String band = android.os.Build.BRAND;
        return band + "-" + xinghao;

    }
}
