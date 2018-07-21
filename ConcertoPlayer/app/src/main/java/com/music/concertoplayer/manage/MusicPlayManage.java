package com.music.concertoplayer.manage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.database.DBManager;
import com.music.concertoplayer.entity.FolderInfo;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.fragment.PlayBarFragment;
import com.music.concertoplayer.receiver.PlayerManagerReceiver;
import com.music.concertoplayer.service.MusicPlayerService;
import com.music.concertoplayer.utils.RxBus;
import com.music.concertoplayer.utils.ToastUtils;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.music.concertoplayer.Constant.KEY_ORDER_MODE;
import static com.music.concertoplayer.Constant.LOOP_ONE;
import static com.music.concertoplayer.Constant.SKIP_SECOND;

/**
 * Created by lijunyan on 2017/2/8.
 */

public class MusicPlayManage {

    private static final String TAG = MusicPlayManage.class.getName();
    public static String locationCount = "";

    private static final int PLAY_FINISH = 100;
    private static final int FOLDER_FINISH = 101;


    public static void playNextMusic(Context context, int progress) {
        //取消播放历史
        App.setIsHistory(false);
        //获取下一首ID
        DBManager dbManager = App.getDbManager();
        int playMode = MusicPlayManage.getIntShared(Constant.KEY_ACTIVITY_STATUS);//获取播放的范围

        //现在正在播放歌曲
        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        MusicInfo currentMusic = dbManager.getMusicFromAllById(musicId);
        //Logger.d("playMode" + playMode);
        if (currentMusic == null) {
            ToastUtils.showToast("此歌曲出现错误，请播放其他歌曲");
            return;
        }
        int nextMusicId = 0;
        if (playMode == Constant.POSITION_FOLDER_LIST) {
            int folderMode = MusicPlayManage.getIntShared(Constant.KEY_MODE);
            //文件夹列表
            if (folderMode == Constant.SEQUENCE_BY_Z) {
                // 横着放

                FolderInfo folder = dbManager.getFolderNameByName(currentMusic.getFolderName());

                nextMusicId = positionByFolderListBY_Z(folder);
            } else if (folderMode == Constant.SEQUENCE_BY_N) {
                //竖着播
                nextMusicId = positionByFolderListBY_N(currentMusic);
            }
        } else if (playMode == Constant.POSITION_FILE_LIST) {

            FolderInfo folder = dbManager.getFolderNameByName(currentMusic.getFolderName());
            //文件夹下的曲目列表
            nextMusicId = positionByFolder(folder, currentMusic);

        } else {
            //播放历史
            nextMusicId = positionByHistory(currentMusic);
        }


        if (nextMusicId == PLAY_FINISH) {
            MusicPlayManage.playSingleMusic(context, currentMusic);
            //获取播放歌曲路径
            String path = App.getDbManager().getMusicPath(MusicPlayManage.getIntShared(Constant.KEY_ID));
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_COMPLETE);
            intent.putExtra(Constant.KEY_PATH, path);
            context.sendBroadcast(intent);
            ToastUtils.showToast("全部播放完毕");
            return;
        }

        MusicInfo nextMusic = App.getDbManager().getMusicFromAllById(nextMusicId);

        File file = new File(nextMusic.getPath());
        if (!file.exists()) {
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_STOP);
            context.sendBroadcast(intent);
            Toast.makeText(context, "歌曲不存在", Toast.LENGTH_LONG).show();
            return;
        }

        List<MusicInfo> musicInfos = App.getDbManager().getAllMusicByFoderName(nextMusic.getFolderName());
        MusicPlayManage.setShared(Constant.KEY_ID, nextMusicId);
        int location = getMusicLocationInFolder(nextMusicId, musicInfos);
        locationCount = (location + 1) + "/" + musicInfos.size();
        MusicPlayManage.setShared(Constant.KEY_LOCATION_COUNT, locationCount);

        Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
        intent.putExtra(Constant.COMMAND, Constant.COMMAND_PLAY);
        intent.putExtra(Constant.KEY_PATH, nextMusic.getPath());

//如果播放单曲循环  就不保存的
        if (currentMusic.getFolderName().equalsIgnoreCase(Constant.FOLDER_LOVE) && currentMusic.getLove() == 0) {
            App.getDbManager().deleteLoveMusic();

        } else {
            //如果是要删除的喜欢曲目就不保存
            App.getDbManager().updateFolderPrePlayId(currentMusic.getId(), progress);
        }

        context.sendBroadcast(intent);

    }


    // 横着放

    public static int positionByFolderListBY_Z(FolderInfo folderInfo) {
        FolderInfo nextFolder = getSelectNextFolder(folderInfo, App.getDbManager().getSelectFolder());
        //Logger.d(nextFolder.getName());
        if (nextFolder.getPre_play_id() == -1) {
            if (App.getDbManager().getAllMusicByFoderName(nextFolder.getName()).size() == 0) {
                return positionByFolderListBY_Z(nextFolder);
            } else {
                return App.getDbManager().getAllMusicByFoderName(nextFolder.getName()).get(0).getId();
            }
        } else {
            MusicInfo nextMusic = App.getDbManager().getMusicFromAllById(nextFolder.getPre_play_id());
            return positionByFolderBY_Z(nextFolder, nextMusic);
        }


    }

    public static int positionByFolderBY_Z(FolderInfo folderInfo, MusicInfo currentMusic) {
        if (folderInfo.getPre_play_progress() == 0) {
            List<MusicInfo> allMusic = App.getDbManager().getAllMusicByFoderName(folderInfo.getName());

            int location = getMusicLocationInFolder(currentMusic.getId(), allMusic);
            if (location == -1) {
                ToastUtils.showToast("文件夹读取错误");
                return -1;
            }
            //表明上次是播放完的 就播放下一首
            //如果是最后一个音乐的话就改写成第一个音乐
            if (location == (allMusic.size() - 1)) {
                int orderMode = MusicPlayManage.getIntShared(KEY_ORDER_MODE);//获取播放的顺序
                if (orderMode == Constant.LOOP_ONE) {
                    //单曲播放
                    return currentMusic.getId();
                } else {
                    return allMusic.get(0).getId();

                }
            } else {
                return allMusic.get(location + 1).getId();
            }
        } else {
            //上次是点出去了，么有播放完，就重新播放这首
            return currentMusic.getId();
        }

    }


    private static int positionByFolderListBY_N(MusicInfo currentMusic) {
        FolderInfo folderInfo = App.getDbManager().getFolderNameByName(currentMusic.getFolderName());
        List<MusicInfo> allMusic = App.getDbManager().getAllMusicByFoderName(folderInfo.getName());

        int location = getMusicLocationInFolder(currentMusic.getId(), allMusic);
        if (location == -1) {
            ToastUtils.showToast("文件夹读取错误");
            return -1;
        }

        //表明上次是播放完的 就播放下一首
        int orderMode = MusicPlayManage.getIntShared(KEY_ORDER_MODE);//获取播放的顺序
        if (orderMode == LOOP_ONE) {
            //单曲播放
            return currentMusic.getId();
        } else {

            if (location == (allMusic.size() - 1)) {
                //如果是最后一个音乐的话就改写成下一个文件夹第一个音乐
                List<FolderInfo> allFolder = App.getDbManager().getSelectFolder();
                FolderInfo nextFolder = getSelectNextFolder(folderInfo, allFolder);
                if (nextFolder.getPre_play_progress() == 0) {
                    //下一个文件夹的播放第一个
                    return App.getDbManager().getAllMusicByFoderName(nextFolder.getName()).get(0).getId();
                } else {
                    //上次是点出去了，么有播放完，就重新播放这首
                    MusicInfo nextFolderMusicPre = App.getDbManager().getMusicFromAllById(nextFolder.getPre_play_id());
                    return nextFolderMusicPre.getId();
                }
            } else {
                //播放这个文件夹的下一首
                return allMusic.get(location + 1).getId();
            }

        }


    }

    private static int positionByHistory(MusicInfo currentMusic) {
        int orderMode = MusicPlayManage.getIntShared(KEY_ORDER_MODE);//获取播放的顺序
        if (orderMode == LOOP_ONE) {
            //单曲播放
            return currentMusic.getId();
        } else {
            return PLAY_FINISH;

        }
    }

    public static int positionByFolder(FolderInfo folderInfo, MusicInfo currentMusic) {
        //if (folderInfo.getPre_play_progress() == 0) {

        int orderMode = MusicPlayManage.getIntShared(KEY_ORDER_MODE);//获取播放的顺序
        if (orderMode == LOOP_ONE) {
            return currentMusic.getId();
        }

        List<MusicInfo> allMusic = App.getDbManager().getAllMusicByFoderName(folderInfo.getName());

        int location = getMusicLocationInFolder(currentMusic.getId(), allMusic);
        if (location == -1) {
            ToastUtils.showToast("文件夹读取错误");
            return -1;
        }
        //表明上次是播放完的 就播放下一首
        //如果是最后一个音乐的话就改写成第一个音乐

        if (location == (allMusic.size() - 1)) {
            if (orderMode == Constant.LOOP_LIST) {
                return allMusic.get(0).getId();
            } else {
                //播放完了
                return PLAY_FINISH;

            }
        } else {

            return allMusic.get(location + 1).getId();
        }


    }


    public static void playSingleMusic(Context context, MusicInfo musicInfo) {
        if (musicInfo == null) {
            ToastUtils.showToast("播放音乐为空");
            return;
        }
        //取消播放历史
        App.setIsHistory(false);
        DBManager dbManager = App.getDbManager();
        File file = new File(musicInfo.getPath());
        if (!file.exists()) {
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_STOP);
            context.sendBroadcast(intent);
            Toast.makeText(context, "歌曲不存在", Toast.LENGTH_LONG).show();
            return;
        }
        FolderInfo folderInfo = App.getDbManager().getFolderNameByName(musicInfo.getFolderName());
        List<MusicInfo> musicInfos = App.getDbManager().getAllMusicByFoderName(musicInfo.getFolderName());
        int location = getMusicLocationInFolder(musicInfo.getId(), musicInfos);
        locationCount = (location + 1) + "/" + musicInfos.size();

        MusicPlayManage.setShared(Constant.KEY_LOCATION_COUNT, locationCount);

        Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
        intent.putExtra(Constant.COMMAND, Constant.COMMAND_PLAY);
        intent.putExtra(Constant.KEY_PATH, musicInfo.getPath());
        if (folderInfo.getPre_play_id() == musicInfo.getId()) {
            intent.putExtra(Constant.KEY_CURRENT, folderInfo.getPre_play_progress());
        }
        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        int current = PlayBarFragment.current;
        if (current == -1 || current == 0) {
            if (musicId != -1) {
                dbManager.updateFolderPrePlayId(musicId, 0);
            }
        } else {
            dbManager.updateFolderPrePlayId(musicId, current);
        }
        MusicPlayManage.setShared(Constant.KEY_ID, musicInfo.getId());
        context.sendBroadcast(intent);
    }


    public static void playPreMusic(Context context) {
        //设置当前是播放历史
        App.setIsHistory(true);

        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        MusicInfo currentMusic = App.getDbManager().getMusicFromAllById(musicId);
        List<MusicInfo> historyMusic = App.getDbManager().getHistory();
        int location = getMusicLocationInFolder(currentMusic.getId(), historyMusic);
        Logger.d("location" + location);

        if (location == historyMusic.size() - 1) {
            ToastUtils.showToast("没有上一首了");
            return;
        }
        //获取的是历史播放，顺序是先播放的在上面，所以要+  而不是 -
        MusicInfo preMusic = historyMusic.get(location + 1);

        File file = new File(preMusic.getPath());
        if (!file.exists()) {
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_STOP);
            context.sendBroadcast(intent);
            Toast.makeText(context, "歌曲不存在", Toast.LENGTH_LONG).show();
            return;
        }

        List<MusicInfo> musicInfos = App.getDbManager().getAllMusicByFoderName(preMusic.getFolderName());
        MusicPlayManage.setShared(Constant.KEY_ID, preMusic.getId());
        int folderLocation = getMusicLocationInFolder(preMusic.getId(), musicInfos);
        locationCount = (folderLocation + 1) + "/" + musicInfos.size();
        MusicPlayManage.setShared(Constant.KEY_LOCATION_COUNT, locationCount);

        Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
        intent.putExtra(Constant.COMMAND, Constant.COMMAND_PLAY);
        intent.putExtra(Constant.KEY_PATH, preMusic.getPath());

//如果播放单曲循环  就不保存的
        if (currentMusic.getFolderName().equalsIgnoreCase(Constant.FOLDER_LOVE) && currentMusic.getLove() == 0) {
            App.getDbManager().deleteLoveMusic();
        } else {
            //如果是要删除的喜欢曲目就不保存
            App.getDbManager().updateFolderPrePlayId(currentMusic.getId(), 0);
        }
        context.sendBroadcast(intent);
    }









    public static int getMusicLocationInFolder(int musicId, List<MusicInfo> nextAllMusic) {
        if (musicId == -1) {
            //表明当前文件第一次播放  默认播放第一个音乐
            return -1;
        }
        for (int i = 0; i < nextAllMusic.size(); i++) {
            if (nextAllMusic.get(i).getId() == musicId) {
                return i;
            }
        }
        return -1;
    }

    public static FolderInfo getSelectNextFolder(FolderInfo folder, List<FolderInfo> allFolder) {
        for (int i = 0; i < allFolder.size(); i++) {
            if (allFolder.get(i).getName().equalsIgnoreCase(folder.getName())) {
                if (i == allFolder.size() - 1) {
                    return allFolder.get(0);
                } else {
                    return allFolder.get(i + 1);
                }

            }
        }
        if (folder.getName().equalsIgnoreCase(Constant.FOLDER_LOVE)) {
            return allFolder.get(0);
        }
        return allFolder.get(0);
    }


    public static MusicInfo getCurrentMusic() {
        int musicId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        return App.getDbManager().getMusicFromAllById(musicId);
    }






    public static void skipNext(Context context, int current) {
        Logger.d("skipNext");
        //发送播放请求
        Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
        intent.putExtra("cmd", Constant.COMMAND_PROGRESS);
        intent.putExtra("current", current + SKIP_SECOND);
        context.sendBroadcast(intent);
    }

    public static void skipPre(Context context, int current) {
        //发送播放请求
        Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
        intent.putExtra("cmd", Constant.COMMAND_PROGRESS);
        intent.putExtra("current", current - SKIP_SECOND);
        context.sendBroadcast(intent);
    }

    public static void skipTo(Context context, int current) {
        //发送播放请求
        Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
        intent.putExtra("cmd", Constant.COMMAND_PROGRESS);
        intent.putExtra("current", current);
        context.sendBroadcast(intent);
    }


    public static void pause(Context context) {
        Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
        intent.putExtra(Constant.COMMAND, Constant.COMMAND_PAUSE);
        context.sendBroadcast(intent);
    }


    public static void speedTo(Context context) {

        if (PlayerManagerReceiver.status == Constant.STATUS_STOP
                || PlayerManagerReceiver.status == Constant.STATUS_PAUSE) {
            return;
        }
        //发送播放请求
        Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
        intent.putExtra("cmd", Constant.COMMAND_PLAY);
        context.sendBroadcast(intent);
    }


    // 设置sharedPreferences
    public static void setShared(String key, int value) {
        SharedPreferences pref = App.getContext().getSharedPreferences("music", App.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static void setShared(String key, String value) {
        SharedPreferences pref = App.getContext().getSharedPreferences("music", App.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
        if (Constant.KEY_LOCATION_COUNT.equalsIgnoreCase(key)) {
            RxBus.getDefault().post("刷新");
        }
    }

    // 获取sharedPreferences
    public static int getIntShared(String key) {
        SharedPreferences pref = App.getContext().getSharedPreferences("music", App.getContext().MODE_PRIVATE);
        int value;
        if (key.equals(Constant.KEY_CURRENT)) {
            value = pref.getInt(key, 0);
        } else {
            value = pref.getInt(key, -1);
        }
        return value;
    }

    public static String getStringShared(String key) {
        SharedPreferences pref = App.getContext().getSharedPreferences("music", App.getContext().MODE_PRIVATE);
        String value;
        value = pref.getString(key, "");
        return value;
    }



    //得到主题
    public static int getTheme(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constant.THEME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt("theme_select", 0);
    }

    //得到上一次选择的主题，用于取消夜间模式时恢复用
    public static int getPreTheme(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constant.THEME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt("pre_theme_select", 0);
    }

    //设置夜间模式
    public static void setNightMode(Context context, boolean mode) {
        if (mode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constant.THEME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("night", mode).commit();
    }

    //得到是否夜间模式
    public static boolean getNightMode(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constant.THEME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("night", false);
    }


    // 设置必用图片 sharedPreferences
    public static void setBingShared(String value) {
        SharedPreferences pref = App.getContext().getSharedPreferences("bing_pic", App.getContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("pic", value);
        editor.commit();
    }

    // 获取必用图片 sharedPreferences
    public static String getBingShared() {
        SharedPreferences pref = App.getContext().getSharedPreferences("bing_pic", App.getContext().MODE_PRIVATE);
        String value = pref.getString("pic", null);
        return value;
    }


}
