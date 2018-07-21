package com.music.concertoplayer.utils;

import android.text.TextUtils;
import android.text.format.DateUtils;

import com.music.concertoplayer.entity.Data;
import com.orhanobut.logger.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by chen on 2018/3/29.
 */

public class TimeUtil {
    private static long MINUTE_IN_MICMILLIS = 1000000 * 60;
    private static long SECOND_IN_MICMILLIS = 1000000 ;
    public static String formatTime(long time) {
        return formatTime("mm:ss", time);
    }

    private static SimpleDateFormat mmdd = new SimpleDateFormat("MM-dd");

    public static String formatTime(String pattern, long milli) {
        int m = (int) (milli / DateUtils.MINUTE_IN_MILLIS);
        int s = (int) ((milli / DateUtils.SECOND_IN_MILLIS) % 60);
        String mm = String.format(Locale.getDefault(), "%02d", m);
        String ss = String.format(Locale.getDefault(), "%02d", s);
        return pattern.replace("mm", mm).replace("ss", ss);
    }

    public static String parseHistoryTime(String time) {
        if (TextUtils.isEmpty(time)) {
            return "";
        }
        Date date = new Date(Long.valueOf(time));
        Date current = new Date(System.currentTimeMillis());
        if (date.getYear() == current.getYear() && date.getMonth() == current.getMonth() && date.getDay() == current.getDay()) {
            return "今天 " + date.getHours() + ":" + date.getMinutes();//如果是今天就显示小时：分钟
        } else {
           // Logger.d(mmdd.format(date));
            return mmdd.format(date);//如果不是今天就显示月：日
        }
    }
}
