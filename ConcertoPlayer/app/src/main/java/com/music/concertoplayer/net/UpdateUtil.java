package com.music.concertoplayer.net;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.music.concertoplayer.App;
import com.music.concertoplayer.utils.PhoneUtil;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.music.concertoplayer.Constant.LOGIN_SUCCESS;
import static com.music.concertoplayer.Constant.UPDATE_URL;

/**
 * Created by chen on 2018/4/30.
 */

public class UpdateUtil {
    // 外存sdcard存放路径
    private static final String FILE_PATH = Environment.getExternalStorageDirectory() + "/" + "musicUpdate" + "/";
    // 下载应用存放全路径
    private static final String FILE_NAME = FILE_PATH + "concertoplayer.apk";
    private static ProgressDialog progressDialog;
    //Log日志打印标签
    private static final String TAG = "chen";
    // 准备安装新版本应用标记
    private static final int INSTALL_TOKEN = 1;
    private static String updateUrl;

    /*
    * {
    "currentVer": "100001",
    "deviceId": "test device id 001",
    "deviceType": "test device type 001",
    "openId": "oRTLh0-p6RARuLqaqjfCniJBpWlg",
    "submitTimestamp": "1234567890123",
    "unionId": "oQrGM1gXkX4XsYRPurz3knYvofXA"
}
    * */
    public static void update(final Context context, final Handler mHandler) {
        if (App.getUser() == null) {
            return;
        }
        OkHttpClient client = new OkHttpClient();//创建OkHttpClient对象。
        //请求超时设置
        client = client.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS).build();
        MediaType jsonTpe = MediaType.parse("application/json; charset=utf-8");//数据类型为json格式，

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("submitTimestamp", String.valueOf(System.currentTimeMillis()));
        jsonObject.put("openId", App.getUser().getOpenid());
        jsonObject.put("unionId", App.getUser().getUnionid());
        jsonObject.put("deviceType", PhoneUtil.getDeviceType(context));
        jsonObject.put("deviceId", PhoneUtil.getsetDeviceid(context));
        jsonObject.put("currentVer", String.valueOf(getCurrentVersion(context)));
        //Logger.d( "currentVer" + String.valueOf(getCurrentVersion(context)) );
        RequestBody body = RequestBody.create(jsonTpe, jsonObject.toString());
        Request request = new Request.Builder()
                .url(UPDATE_URL)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.sendEmptyMessage(LOGIN_SUCCESS);
                Logger.d("onFailure" + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {//回调的方法执行在子线程。
                    String json = response.body().string();
                    Logger.d(json);
                    JSONObject jsonObject = JSON.parseObject(json);
                    int getFrom = jsonObject.getInteger("getFrom");
                    int serverVer = jsonObject.getInteger("serverVer");
                    int serverTimestamp = jsonObject.getInteger("serverTimestamp");
                    String deviceId = jsonObject.getString("deviceId");
                    String deviceType = jsonObject.getString("deviceType");
                    String openId = jsonObject.getString("openId");
                    String unionId = jsonObject.getString("unionId");
                    updateUrl = jsonObject.getString("updateUrl");
                    final String updateDesc = jsonObject.getString("updateDesc");

                    if (getFrom == 0) {
                        mHandler.sendEmptyMessage(LOGIN_SUCCESS);
                    } else {
                        //有更新
                        String code = String.valueOf(getFrom);
                        if (code.length() == 2) {
                            final String qiangzhi = code.substring(0, 1);
                            final String from = code.substring(1);
                            //强制更新
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    showNoticeDialog(context, updateDesc, Integer.valueOf(qiangzhi), Integer.valueOf(from),mHandler);
                                }
                            });

                        } else {
                            mHandler.sendEmptyMessage(LOGIN_SUCCESS);
                        }
                    }

                }
            }
        });
    }

    /**
     * 显示提示更新对话框
     */
    private static void showNoticeDialog(final Context context, String update_describe, Integer qiangzhi, Integer from, final Handler mHandler) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        dialog.setTitle("检测到新版本！")
                .setMessage(update_describe)
                .setPositiveButton("下载", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showDownloadDialog(context);
                    }
                });
        if (qiangzhi == 1) {
            dialog.setCancelable(false);
            //设置用户为空
            App.setUser(null);
            /*dialog.setTitle("检测到新版本！").setMessage(update_describe).setNegativeButton("下次再说", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mHandler.sendEmptyMessage(LOGIN_SUCCESS);
                }
            });
            dialog.setCancelable(true);*/
        } else {
            dialog.setTitle("检测到新版本！").setMessage(update_describe).setNegativeButton("下次再说", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mHandler.sendEmptyMessage(LOGIN_SUCCESS);
                }
            });
            dialog.setCancelable(true);
        }
        if (!((Activity) context).isDestroyed() && !((Activity) context).isFinishing()) {
            dialog.create().show();
        }
    }

    /**
     * 显示下载进度对话框
     *
     * @param context
     */
    public static void showDownloadDialog(Context context) {

        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("正在下载...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        new downloadAsyncTask().execute();
    }

    /**
     * 下载新版本应用
     */
    private static class downloadAsyncTask extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            //Log.e(TAG, "执行至--onPreExecute");
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (TextUtils.isEmpty(updateUrl)) {
                return null;
            }
            //Log.e(TAG, "执行至--doInBackground");

            URL url;
            HttpURLConnection connection = null;
            InputStream in = null;
            FileOutputStream out = null;
            try {
                url = new URL(updateUrl);
                connection = (HttpURLConnection) url.openConnection();

                in = connection.getInputStream();
                long fileLength = connection.getContentLength();
                File file_path = new File(FILE_PATH);
                if (!file_path.exists()) {
                    file_path.mkdir();
                }

                out = new FileOutputStream(new File(FILE_NAME));//为指定的文件路径创建文件输出流
                byte[] buffer = new byte[1024 * 1024];
                int len = 0;
                long readLength = 0;

                //Log.e(TAG, "执行至--readLength = 0");

                while ((len = in.read(buffer)) != -1) {

                    out.write(buffer, 0, len);//从buffer的第0位开始读取len长度的字节到输出流
                    readLength += len;

                    int curProgress = (int) (((float) readLength / fileLength) * 100);

                    //Log.e(TAG, "当前下载进度：" + curProgress);

                    publishProgress(curProgress);

                    if (readLength >= fileLength) {

                       // Log.e(TAG, "执行至--readLength >= fileLength");
                        break;
                    }
                }

                out.flush();
                return INSTALL_TOKEN;

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {

            //Log.e(TAG, "异步更新进度接收到的值：" + values[0]);
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Integer integer) {

            progressDialog.dismiss();//关闭进度条
            //安装应用
            installApp();
        }
    }

    /**
     * 安装新版本应用
     */
    private static void installApp() {
       /* File appFile = new File(FILE_NAME);
        if (!appFile.exists()) {
            return;
        }
        // 跳转到新版本应用安装页面
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.setDataAndType(Uri.parse("file://" + appFile.toString()), "application/vnd.android.package-archive");
        App.getContext().startActivity(intent);*/


        File file= new File(
                FILE_NAME);
        if (!file.exists()) {
            return;
        }


        Intent intent = new Intent(Intent.ACTION_VIEW);
        // 由于没有在Activity环境下启动Activity,设置下面的标签
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


        if(Build.VERSION.SDK_INT>=24) { //判读版本是否在7.0以上
            //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
            Uri apkUri =
                    FileProvider.getUriForFile(App.getContext(), "com.music.concertoplayer.fileprovider", file);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        }else{
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
        }

        App.getContext().startActivity(intent);

    }


    /**
     * 获取当前版本号
     *
     * @param context
     */
    private static int getCurrentVersion(Context context) {
        try {

            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);


            return info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();

            return 0;
        }
    }


}
