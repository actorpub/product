package com.music.concertoplayer.net;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.entity.LogInfo;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.utils.PhoneUtil;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by chen on 2018/4/26.
 */

public class LogUtils {
    public static final String mSeparator = "\t\t\t";
    private static final String TAG = "chen";

    /*
    *
    * ①开始/完成
②播放成功【0】或失败【1】
③时间（YYYYMMDDHHmmss）
④播放列表文件夹名
⑤播放曲目名
⑥文件完整路径
⑦文件名（含扩展名）*/
    public static void insertPlayLog(MusicInfo musicInfo, String state, String isSuccess) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        long timeMillis = System.currentTimeMillis();
        String time = simpleDateFormat.format(new Date(timeMillis));
        String folderName = musicInfo.getFolderName();
        String musicName = musicInfo.getName();
        String path = musicInfo.getPath();
        String[] paths = path.split(File.separator);
        String fileName = paths[paths.length - 1];//文件名（含扩展名）
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(
                "01" + mSeparator +
                        state + mSeparator +
                        isSuccess + mSeparator +
                        timeMillis + mSeparator +
                        folderName + mSeparator +
                        musicName + mSeparator +
                        fileName);
        //Logger.d(stringBuffer.toString());
        String logName = time.substring(0, 8)  + App.getUser().getOpenid() + ".log";
        writeLog(logName, stringBuffer.toString());

    }

    /*①读取本地文件+分隔符+文件夹总数+分隔符+文件总数
②播放全部-按钮点击
③播放一个文件夹-按钮点击
④点击文件夹播放
⑤进入文件夹
⑥点击曲目播放
⑦关闭播放控制面板
⑧展开播放控制面板
⑨收起播放控制面板
⑩播放进度-以拖动放手为准
⑪快进
⑫后退
⑬排序
⑭排序返回
⑮喜欢*/
    public static void inserOperationLog(String msg) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        long timeMillis = System.currentTimeMillis();
        String time = simpleDateFormat.format(new Date(timeMillis));

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("00" + mSeparator +
                timeMillis + mSeparator +
                msg
        );
        //Logger.d(stringBuffer.toString());
        String logName = time.substring(0, 8) + App.getUser().getOpenid()  + ".log";
        writeLog(logName, stringBuffer.toString());
    }


    public static void writeLog(String fileName, String msg) {
        try {
            msg = new String(msg.getBytes(), "utf-8");
            fileName = new String(fileName.getBytes(), "utf-8");
            File folderFile = new File(Constant.LOG_FILE_URL);
            if (!folderFile.exists()) {
                folderFile.mkdir();
            }
            File file = new File(Constant.LOG_FILE_URL + fileName);
            if (file.exists()) {
                //如果为追加则在原来的基础上继续写文件
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                raf.seek(file.length());
                raf.write(msg.getBytes());
                raf.write("\n".getBytes());

            } else {
                file.createNewFile();

                //如果为追加则在原来的基础上继续写文件
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                raf.seek(file.length());
                raf.write(msg.getBytes());
                raf.write("\n".getBytes());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void postLog() {
        String submitTimestamp = String.valueOf(System.currentTimeMillis());
        String openId = App.getUser().getOpenid();;//App.getUser().getOpenid();
        String unionId = App.getUser().getUnionid();;//App.getUser().getUnionid();
        String deviceType = PhoneUtil.getDeviceType(App.getContext());
        String deviceId = PhoneUtil.getsetDeviceid(App.getContext());

        File folderFile = new File(Constant.LOG_FILE_URL);
        if (!folderFile.exists()) {
            folderFile.mkdir();
            return;
        }

        try {
            File[] files = folderFile.listFiles();
            for (File file : files) {
                String name = file.getName().substring(0, 8);
                //Logger.d("时间" + name);

                if (!isToday(name)) {
                    Logger.d("开始上传日志");
                    //表明是一天前的
                    HashMap<String, Object> paramsMap = new HashMap<>();
                    paramsMap.put("submitTimestamp", submitTimestamp);
                    paramsMap.put("openId", openId);
                    paramsMap.put("unionId", unionId);
                    paramsMap.put("fileType", "001");
                    paramsMap.put("deviceType", deviceType);
                    paramsMap.put("deviceId", deviceId);
                    paramsMap.put("file", file);
                    upLoadFile(paramsMap,file);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }


    /**
     * 判断给定字符串时间是否为今日
     *
     * @param sdate
     * @return boolean
     */
    public static boolean isToday(String sdate) throws ParseException {
        boolean b = false;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        Date time = simpleDateFormat.parse(sdate);
        Date today = new Date();
        if (time != null) {
            String nowDate = simpleDateFormat.format(today);
            String timeDate = simpleDateFormat.format(time);
            if (nowDate.equals(timeDate)) {
                b = true;
            }
        }
        return b;
    }

    private static final MediaType MEDIA_OBJECT_STREAM = MediaType.parse("application/octet-stream");//mdiatype 这个需要和服务端保持一致 你需要看下你们服务器设置的ContentType 是不是这个，他们设置的是哪个 我们要和他们保持一致

    /**
     *上传文件
     * @param paramsMap 参数
     * @param <T>
     */
    public static <T>void upLoadFile(HashMap<String, Object> paramsMap, final File logFile) {
        try {
            //补全请求地址
            String requestUrl = Constant.LOG_URL;
            MultipartBody.Builder builder = new MultipartBody.Builder();
            //设置类型
            builder.setType(MultipartBody.FORM);
            //追加参数
            for (String key : paramsMap.keySet()) {
                Object object = paramsMap.get(key);
                if (!(object instanceof File)) {
                    builder.addFormDataPart(key, object.toString());
                } else {
                    File file = (File) object;
                    builder.addFormDataPart(key, file.getName(), RequestBody.create(null, file));
                }
            }
            OkHttpClient mOkHttpClient = new OkHttpClient();
            //创建RequestBody
            RequestBody body = builder.build();
            //创建Request
            final Request request = new Request.Builder().url(requestUrl).post(body).addHeader("Content-Type","application/x-www-form-urlencoded").build();
            //单独设置参数 比如读取超时时间
            final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        JSONObject jsonObject = JSON.parseObject(string);
                        int code = jsonObject.getInteger("code");
                        String msg = jsonObject.getString("msg");
                        if (code == 1) {
                            Logger.d("开始删除日志");
                            deleteLogFile(logFile);
                        } else {

                        }
                        Log.e(TAG, "response s ----->" + string);
                    } else {
                        String string = response.toString();
                        Log.e(TAG, "response  f----->" + string);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public static void deleteLogFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }



}
