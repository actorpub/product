package com.music.concertoplayer.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.github.promeg.pinyinhelper.Pinyin;
import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.entity.FolderInfo;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.entity.User;
import com.music.concertoplayer.net.LogUtils;
import com.orhanobut.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.music.concertoplayer.database.DatabaseHelper.CAN_RECYCLE_ABLE;
import static com.music.concertoplayer.database.DatabaseHelper.FOLDER_IMG;
import static com.music.concertoplayer.database.DatabaseHelper.FOLDER_NAME;
import static com.music.concertoplayer.database.DatabaseHelper.FOLDER_ORDER;
import static com.music.concertoplayer.database.DatabaseHelper.FOLDER_SELECT;
import static com.music.concertoplayer.database.DatabaseHelper.ID_COLUMN;
import static com.music.concertoplayer.database.DatabaseHelper.LOVE_COLUMN;
import static com.music.concertoplayer.database.DatabaseHelper.MUSIC_ID_COLUMN;
import static com.music.concertoplayer.database.DatabaseHelper.MUSIC_TABLE;
import static com.music.concertoplayer.database.DatabaseHelper.NAME_COLUMN;
import static com.music.concertoplayer.database.DatabaseHelper.PATH_COLUMN;
import static com.music.concertoplayer.database.DatabaseHelper.PLAY_COUNT;
import static com.music.concertoplayer.database.DatabaseHelper.PLAY_LIST_TABLE;
import static com.music.concertoplayer.database.DatabaseHelper.PRE_PLAY_COLUMN;
import static com.music.concertoplayer.database.DatabaseHelper.PRE_PLAY_PROGRESS;
import static com.music.concertoplayer.database.DatabaseHelper.PRE_PLAY_TIME;
import static com.music.concertoplayer.database.DatabaseHelper.USER_DEVICEID;
import static com.music.concertoplayer.database.DatabaseHelper.USER_DEVICE_TYPE;
import static com.music.concertoplayer.database.DatabaseHelper.USER_LATITUDE;
import static com.music.concertoplayer.database.DatabaseHelper.USER_LONGITUDE;
import static com.music.concertoplayer.database.DatabaseHelper.USER_MOBILE;
import static com.music.concertoplayer.database.DatabaseHelper.USER_NAME;
import static com.music.concertoplayer.database.DatabaseHelper.USER_OPENID;
import static com.music.concertoplayer.database.DatabaseHelper.USER_TABLE;
import static com.music.concertoplayer.database.DatabaseHelper.USER_UNIONID;


public class DBManager {

    private static final String TAG = DBManager.class.getName();
    private DatabaseHelper helper;
    private SQLiteDatabase db;
    private static DBManager instance = null;


    /* 因为getWritableDatabase内部调用了mContext.openOrCreateDatabase(mName, 0,mFactory);
     * 需要一个context参数 ,所以要确保context已初始化,我们可以把实例化DBManager的步骤放在Activity的onCreate里
     */
    public DBManager(Context context) {
        helper = new DatabaseHelper(context);
        db = helper.getWritableDatabase();
    }

    public static synchronized DBManager getInstance(Context context) {
        if (instance == null) {
            instance = new DBManager(context);
        }
        return instance;
    }


    /**
     * 通过文件夹名字获取相应音乐
     *
     * @param name
     * @return
     */
    public List<MusicInfo> getAllMusicByFoderName(String name) {
        List<MusicInfo> musicInfoList = new ArrayList<>();
        Cursor cursor = null;
        db.beginTransaction();
        try {
            cursor = db.rawQuery("select * from " + MUSIC_TABLE + " where " + FOLDER_NAME + " = ?", new String[]{name});
            //"select * from personwhere name like ?and age=?", new String[]{"%iteedu%", "4"}
            musicInfoList = cursorToMusicList(cursor,true);

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            if (cursor != null) {
                cursor.close();
            }
        }
        return musicInfoList;
    }

    public User getUser() {
        Cursor cursor = db.rawQuery("select * from " + USER_TABLE, null);
        User user = new User();
        while (cursor.moveToNext()) {
            user.setUnionid(cursor.getString(cursor.getColumnIndex(USER_UNIONID)));
            user.setOpenid(cursor.getString(cursor.getColumnIndex(USER_OPENID)));
            user.setNickname(cursor.getString(cursor.getColumnIndex(USER_NAME)));
            user.setMobile(cursor.getString(cursor.getColumnIndex(USER_MOBILE)));
            user.setLongitude(cursor.getString(cursor.getColumnIndex(USER_LONGITUDE)));
            user.setLatitude(cursor.getString(cursor.getColumnIndex(USER_LATITUDE)));
            user.setDeviceid(cursor.getString(cursor.getColumnIndex(USER_DEVICEID)));
            user.setDevice_type(cursor.getString(cursor.getColumnIndex(USER_DEVICE_TYPE)));
        }

        if (cursor != null) {
            cursor.close();
        }

        return user;
    }

    public void insertUser(User user) {
        ContentValues values = new ContentValues();
        values.put(USER_DEVICE_TYPE, user.getDevice_type());
        values.put(USER_DEVICEID, user.getDeviceid());
        values.put(USER_LATITUDE, user.getLatitude());
        values.put(USER_LONGITUDE, user.getLongitude());
        values.put(USER_MOBILE, user.getMobile());
        values.put(USER_NAME, user.getNickname());
        values.put(USER_OPENID, user.getOpenid());
        values.put(USER_UNIONID, user.getUnionid());

        db.insert(USER_TABLE, null, values);
    }

    public void updateUser(User user) {
        ContentValues values = new ContentValues();
        values.put(USER_DEVICE_TYPE, user.getDevice_type());
        values.put(USER_DEVICEID, user.getDeviceid());
        values.put(USER_LATITUDE, user.getLatitude());
        values.put(USER_LONGITUDE, user.getLongitude());
        values.put(USER_MOBILE, user.getMobile());
        values.put(USER_NAME, user.getNickname());
        values.put(USER_OPENID, user.getOpenid());
        values.put(USER_UNIONID, user.getUnionid());

        db.update(USER_TABLE, values, ID_COLUMN + "=?", new String[]{"1"});
    }


    public void delteUser() {
        db.delete(USER_TABLE, null, null);
    }


    /*public List<MusicInfo> getAllMusic() {
        Log.d(TAG, "getAllMusic: ");
        List<MusicInfo> musicInfoList = new ArrayList<>();
        Cursor cursor = null;
        db.beginTransaction();
        try {
            cursor = db.rawQuery("select * from " + MUSIC_TABLE, null);
            //cursor = db.query(MUSIC_TABLE, null, null, null, null, null, null);
            musicInfoList = cursorToMusicList(cursor);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            if (cursor != null) {
                cursor.close();
            }
        }
        return musicInfoList;
    }*/

    public MusicInfo getMusicFromAllById(int id) {
        List<MusicInfo> musicInfoList = null;
        MusicInfo musicInfo = null;
        Cursor cursor = null;
        db.beginTransaction();
        try {
            cursor = db.rawQuery("select * from " + MUSIC_TABLE + " where " + ID_COLUMN + " = ?", new String[]{id + ""});
            musicInfoList = cursorToMusicList(cursor,false);
            if (musicInfoList.size() == 0) {
                return musicInfo;
            }

            musicInfo = musicInfoList.get(0);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            if (cursor != null) {
                cursor.close();
            }
        }
        return musicInfo;
    }




    public List<FolderInfo> getAllFolder() {
        List<FolderInfo> playListInfos = new ArrayList<>();
        Cursor cursor = db.rawQuery("select * from " + PLAY_LIST_TABLE, null);

        try {
            while (cursor.moveToNext()) {
                FolderInfo playListInfo = new FolderInfo();
                playListInfo.setName(cursor.getString(cursor.getColumnIndex(NAME_COLUMN)));
                playListInfo.setPath(cursor.getString(cursor.getColumnIndex(PATH_COLUMN)));
                String bitmapPath = cursor.getString(cursor.getColumnIndex(FOLDER_IMG));
                File file = new File(bitmapPath);
                if (file.exists()) {
                    Bitmap bmpout = BitmapFactory.decodeFile(bitmapPath);
                    playListInfo.setBitmap(bmpout);
                } else {
                    if (playListInfo.getName().equalsIgnoreCase(Constant.FOLDER_LOVE)) {
                        Bitmap bmpout = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.default_artist_img);
                        playListInfo.setBitmap(bmpout);
                    } else {
                        Bitmap bmpout = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.default_artist_img);
                        playListInfo.setBitmap(bmpout);
                    }
                }
                playListInfo.setPre_play_id(cursor.getInt(cursor.getColumnIndex(PRE_PLAY_COLUMN)));
                playListInfo.setCan_recycle_able(cursor.getInt(cursor.getColumnIndex(CAN_RECYCLE_ABLE)));
                playListInfo.setPre_play_progress(cursor.getInt(cursor.getColumnIndex(PRE_PLAY_PROGRESS)));
                playListInfo.setFolder_order(cursor.getInt(cursor.getColumnIndex(FOLDER_ORDER)));
                playListInfo.setFolder_select(cursor.getInt(cursor.getColumnIndex(FOLDER_SELECT)));
                playListInfo.setPlaying(false);
                //cursorCount = db.query(MUSIC_TABLE, null, FOLDER_NAME + " = ?", new String[]{"" + playListInfo.getName()}, null, null, null);
                //如果我的喜欢的文件夹的数量为0  就不显示
                playListInfo.setFolder_count(getAllMusicByFoderName(playListInfo.getName()).size());
                if (playListInfo.getName().equalsIgnoreCase(Constant.FOLDER_LOVE) && playListInfo.getFolder_count() == 0) {
                    continue;
                }
                playListInfos.add(playListInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        if (cursor != null) {
            cursor.close();
        }


        Collections.sort(playListInfos, new Comparator<FolderInfo>() {
            @Override
            public int compare(FolderInfo folderInfo, FolderInfo t1) {
                return folderInfo.getFolder_order() - t1.getFolder_order();
            }
        });
        return playListInfos;
    }

    public List<FolderInfo> getSelectFolder() {
        List<FolderInfo> playListInfos = new ArrayList<>();
        Cursor cursor = db.rawQuery("select * from " + PLAY_LIST_TABLE + " where " + FOLDER_SELECT + " = ? ", new String[]{"1"});

        try {
            while (cursor.moveToNext()) {
                FolderInfo playListInfo = new FolderInfo();
                playListInfo.setName(cursor.getString(cursor.getColumnIndex(NAME_COLUMN)));
                playListInfo.setPath(cursor.getString(cursor.getColumnIndex(PATH_COLUMN)));
                String bitmapPath = cursor.getString(cursor.getColumnIndex(FOLDER_IMG));
                File file = new File(bitmapPath);
                if (file.exists()) {
                    Bitmap bmpout = BitmapFactory.decodeFile(bitmapPath);
                    playListInfo.setBitmap(bmpout);
                } else {
                    if (playListInfo.getName().equalsIgnoreCase(Constant.FOLDER_LOVE)) {
                        Bitmap bmpout = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.default_artist_img);
                        playListInfo.setBitmap(bmpout);
                    } else {
                        Bitmap bmpout = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.default_artist_img);
                        playListInfo.setBitmap(bmpout);
                    }
                }
                playListInfo.setPre_play_id(cursor.getInt(cursor.getColumnIndex(PRE_PLAY_COLUMN)));
                playListInfo.setCan_recycle_able(cursor.getInt(cursor.getColumnIndex(CAN_RECYCLE_ABLE)));
                playListInfo.setPre_play_progress(cursor.getInt(cursor.getColumnIndex(PRE_PLAY_PROGRESS)));
                playListInfo.setFolder_order(cursor.getInt(cursor.getColumnIndex(FOLDER_ORDER)));
                playListInfo.setFolder_select(cursor.getInt(cursor.getColumnIndex(FOLDER_SELECT)));
                //cursorCount = db.query(MUSIC_TABLE, null, FOLDER_NAME + " = ?", new String[]{"" + playListInfo.getName()}, null, null, null);
                //如果我的喜欢的文件夹的数量为0  就不显示
                playListInfo.setFolder_count(getAllMusicByFoderName(playListInfo.getName()).size());
                if (playListInfo.getName().equalsIgnoreCase(Constant.FOLDER_LOVE) && playListInfo.getFolder_count() == 0) {
                    continue;
                }
                playListInfos.add(playListInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        if (cursor != null) {
            cursor.close();
        }


        Collections.sort(playListInfos, new Comparator<FolderInfo>() {
            @Override
            public int compare(FolderInfo folderInfo, FolderInfo t1) {
                return folderInfo.getFolder_order() - t1.getFolder_order();
            }
        });
        return playListInfos;
    }

    public boolean isHasFolder(String folderName) {
        Cursor cursorCount = db.rawQuery("select * from " + PLAY_LIST_TABLE + " where " + NAME_COLUMN + " = ? ", new String[]{folderName});
        int count = cursorCount.getCount();
        if (cursorCount != null) {
            cursorCount.close();
        }
        if (count == 0) {
            return false;
        } else {
            return true;
        }
    }

    public FolderInfo getFolderNameByName(String name) {
        //"select * from personwhere name like ? and age=?", new String[]{"%iteedu%", "4"}
        Cursor cursor = db.rawQuery("select * from " + PLAY_LIST_TABLE + " where " + NAME_COLUMN + " = ? ", new String[]{name});
        Cursor cursorCount = null;
        FolderInfo playListInfo = new FolderInfo();
        while (cursor.moveToNext()) {
            playListInfo.setName(cursor.getString(cursor.getColumnIndex(NAME_COLUMN)));
            playListInfo.setPath(cursor.getString(cursor.getColumnIndex(PATH_COLUMN)));
            String bitmapPath = cursor.getString(cursor.getColumnIndex(FOLDER_IMG));

            if (!TextUtils.isEmpty(bitmapPath) ) {
                File file = new File(bitmapPath);
                if (file.exists()) {
                    try {
                        Bitmap bmpout = BitmapFactory.decodeFile(bitmapPath);
                        playListInfo.setBitmap(bmpout);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Bitmap bmpout = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.default_artist_img);
                        playListInfo.setBitmap(bmpout);
                    }
                } else {
                    Bitmap bmpout = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.default_artist_img);
                    playListInfo.setBitmap(bmpout);
                }

            } else {
                Bitmap bmpout = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.default_artist_img);
                playListInfo.setBitmap(bmpout);
            }
            cursorCount = db.rawQuery("select * from " + MUSIC_TABLE + " where " + FOLDER_NAME + " = ? ", new String[]{"" + playListInfo.getName()});
            //cursorCount = db.query(MUSIC_TABLE, null, FOLDER_NAME + " = ?", new String[]{"" + playListInfo.getName()}, null, null, null);
            playListInfo.setFolder_count(cursorCount.getCount());
            playListInfo.setPre_play_id(cursor.getInt(cursor.getColumnIndex(PRE_PLAY_COLUMN)));
            playListInfo.setCan_recycle_able(cursor.getInt(cursor.getColumnIndex(CAN_RECYCLE_ABLE)));
            playListInfo.setPre_play_progress(cursor.getInt(cursor.getColumnIndex(PRE_PLAY_PROGRESS)));
            playListInfo.setFolder_order(cursor.getInt(cursor.getColumnIndex(FOLDER_ORDER)));
            playListInfo.setFolder_select(cursor.getInt(cursor.getColumnIndex(FOLDER_SELECT)));
        }

        if (cursor != null) {
            cursor.close();
        }
        if (cursorCount != null) {
            cursorCount.close();
        }
        return playListInfo;
    }

    public boolean isContainByMusicPath(String path) {
        Cursor cursorCount = null;
        cursorCount = db.rawQuery("select * from " + MUSIC_TABLE + " where " + PATH_COLUMN + " = ? ", new String[]{path});
        int count = cursorCount.getCount();
        if (cursorCount != null) {
            cursorCount.close();
        }
        if (count == 0) {
            return false;
        } else {
            return true;
        }

    }

    public void createFolderName(List<FolderInfo> list) {
        for (FolderInfo info : list) {
            createFolderName(info);
        }
    }

    public void createFolderName(FolderInfo playListInfo) {
        ContentValues values = new ContentValues();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (playListInfo.getBitmap_path() != null) {
            values.put(FOLDER_IMG, playListInfo.getBitmap_path());
        } else {
            values.put(FOLDER_IMG, "");
        }
        values.put(PATH_COLUMN, playListInfo.getPath());
        values.put(NAME_COLUMN, playListInfo.getName());
        values.put(PRE_PLAY_COLUMN, playListInfo.getPre_play_id());
        values.put(CAN_RECYCLE_ABLE, playListInfo.getCan_recycle_able());
        values.put(PRE_PLAY_PROGRESS, playListInfo.getPre_play_progress());
        values.put(FOLDER_ORDER, playListInfo.getFolder_order());
        values.put(FOLDER_SELECT, playListInfo.getFolder_select());
        db.insert(PLAY_LIST_TABLE, null, values);
    }

    public void updateFolderPlayProgress(String folderName, int progress) {
        ContentValues cv = new ContentValues();
        cv.put(PRE_PLAY_PROGRESS, progress);
        db.update(PLAY_LIST_TABLE, cv, NAME_COLUMN + "=?", new String[]{folderName});
    }

    public void updateSingleFolderRecycleAble(String folderName, int recycleAble) {
        ContentValues cv = new ContentValues();
        cv.put(CAN_RECYCLE_ABLE, recycleAble);
        db.update(PLAY_LIST_TABLE, cv, NAME_COLUMN + "=?", new String[]{folderName});
    }

    public boolean isRecycleAbleByFolderName(String folderName) {
        Cursor cursor = db.rawQuery("select * from " + PLAY_LIST_TABLE + " where " + NAME_COLUMN + " = ?", new String[]{folderName});
        //Cursor cursor = db.query(PLAY_LIST_TABLE, null, NAME_COLUMN + " = ?", new String[]{folderName}, null, null, null);
        cursor.moveToNext();
        int recycleAble = cursor.getInt(cursor.getColumnIndex(CAN_RECYCLE_ABLE));
        if (cursor != null) {
            cursor.close();
        }
        if (recycleAble == 0) {
            return false;
        } else {
            return true;
        }
    }


    public void renewFolderCanRecycleAble() {
        ContentValues cv = new ContentValues();
        cv.put(CAN_RECYCLE_ABLE, 1);
        db.update(PLAY_LIST_TABLE, cv, null, null);
    }


    public int getFolderPrePlayIdByName(String folderName) {
        //Cursor cursor = db.rawQuery("select * from personwhere name like ?and age=?", new String[]{"%iteedu%", "4"});
        Cursor cursor = db.rawQuery("select * from " + PLAY_LIST_TABLE + " where " + NAME_COLUMN + " = ? ", new String[]{folderName});
        //Cursor cursor = db.query(PLAY_LIST_TABLE, null, NAME_COLUMN + " = ?", new String[]{folderName}, null, null, null);

        int prePlayId = -1;
        while (cursor.moveToNext()) {

            prePlayId = cursor.getInt(cursor.getColumnIndex(PRE_PLAY_COLUMN));
        }
        if (cursor != null) {
            cursor.close();
        }
        return prePlayId;
    }


    public ArrayList<Integer> getMusicIdListByPlaylist(int playlistId) {
        Cursor cursor = null;
        db.beginTransaction();
        ArrayList<Integer> list = new ArrayList<Integer>();
        try {
            String sql = "select * from " + DatabaseHelper.PLAY_LISY_MUSIC_TABLE + " where " + ID_COLUMN + " = ? ";
            cursor = db.rawQuery(sql, new String[]{"" + playlistId});
            while (cursor.moveToNext()) {
                int musicId = cursor.getInt(cursor.getColumnIndex(MUSIC_ID_COLUMN));
                list.add(musicId);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            if (cursor != null)
                cursor.close();
        }
        return list;
    }

    /*public List<MusicInfo> getMusicListByPlaylist(int playlistId) {
        List<MusicInfo> musicInfoList = new ArrayList<>();
        Cursor cursor = null;
        int id;
        db.beginTransaction();
        try {
            String sql = "select * from " + DatabaseHelper.PLAY_LISY_MUSIC_TABLE + " where " + ID_COLUMN + " = ? ORDER BY " + ID_COLUMN;
            cursor = db.rawQuery(sql, new String[]{"" + playlistId});
            while (cursor.moveToNext()) {
                MusicInfo musicInfo = new MusicInfo();
                id = cursor.getInt(cursor.getColumnIndex(MUSIC_ID_COLUMN));
                musicInfo = getMusicFromAllById(id);
                musicInfoList.add(musicInfo);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            if (cursor != null)
                cursor.close();
        }
        return musicInfoList;
    }*/


    public void insertMusicListToMusicTable(List<MusicInfo> musicInfoList) {
        for (MusicInfo musicInfo : musicInfoList) {
            insertMusicInfoToMusicTable(musicInfo);
        }
    }


    //添加歌曲到音乐表
    public void insertMusicInfoToMusicTable(MusicInfo musicInfo) {
        ContentValues values;
        try {
            values = musicInfoToContentValues(musicInfo);
            db.insert(MUSIC_TABLE, null, values);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void updateFolderOrder(String folderName, int order) {
        Log.d("chen", folderName + " -->" + order);
        ContentValues cv = new ContentValues();
        cv.put(FOLDER_ORDER, order);
        db.update(PLAY_LIST_TABLE, cv, NAME_COLUMN + "=?", new String[]{folderName});
    }

    public void updateFolderSelect(String folderName, int select) {
        Log.d("chen", folderName + " -->" + select);
        ContentValues cv = new ContentValues();
        cv.put(FOLDER_SELECT, select);
        db.update(PLAY_LIST_TABLE, cv, NAME_COLUMN + "=?", new String[]{folderName});
    }

    public void updateFolderPrePlayId(int musicId, int progress) {
        String folderName = getMusicFromAllById(musicId).getFolderName();
        ContentValues cv = new ContentValues();
        cv.put(PRE_PLAY_COLUMN, musicId);
        cv.put(PRE_PLAY_PROGRESS, progress);
        db.update(PLAY_LIST_TABLE, cv, NAME_COLUMN + "=?", new String[]{folderName});
    }

    //废弃
    public List<Integer> getPlayAllList() {
        List<Integer> musicInfoList = new ArrayList<>();
        Cursor cursor = db.rawQuery("select * from " + PLAY_LIST_TABLE, null);
        //Cursor cursor = db.query(DatabaseHelper.PLAY_ALL_LIST_TABLE, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            musicInfoList.add(cursor.getInt(cursor.getColumnIndex(MUSIC_ID_COLUMN)));
        }
        if (cursor != null) {
            cursor.close();
        }
        return musicInfoList;
    }

    public void deletePlayAllList() {
        db.delete(DatabaseHelper.PLAY_ALL_LIST_TABLE, null, null);
    }

    public void inserMusicInfoToAllPlayList(List<MusicInfo> list) {
        for (MusicInfo info : list) {
            inserMusicInfoToAllPlayList(info);
        }

    }

    public void inserMusicInfoToAllPlayList(MusicInfo info) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.MUSIC_ID_COLUMN, info.getId());
        db.insert(DatabaseHelper.PLAY_ALL_LIST_TABLE, null, values);
    }

    public void updateAllMusic(List<MusicInfo> musicInfoList) {
        db.beginTransaction();
        try {
            deleteAllTable();
            insertMusicListToMusicTable(musicInfoList);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteAllMusic() {
        db.execSQL("delete from " + MUSIC_TABLE);
    }

    public void deletePlayList() {
        db.delete(PLAY_LIST_TABLE, null, null);
    }


    //删除数据库中所有的表
    public void deleteAllTable() {
        db.execSQL("PRAGMA foreign_keys=ON");
        db.delete(MUSIC_TABLE, null, null);
        db.delete(DatabaseHelper.HISTORY_PLAY_TABLE, null, null);
        db.delete(PLAY_LIST_TABLE, null, null);
        db.delete(DatabaseHelper.PLAY_LISY_MUSIC_TABLE, null, null);
    }

    //删除指定音乐
    public void deleteMusic(int id) {
        db.execSQL("PRAGMA foreign_keys=ON");
        db.delete(MUSIC_TABLE, ID_COLUMN + " = ? ", new String[]{"" + id});
        db.delete(DatabaseHelper.HISTORY_PLAY_TABLE, ID_COLUMN + " = ? ", new String[]{"" + id});
    }

    public void deletePlaylist(int id) {
        db.delete(PLAY_LIST_TABLE, ID_COLUMN + " = ? ", new String[]{"" + id});
    }


    // 获取歌曲路径
    public String getMusicPath(int id) {
        Log.d(TAG, "getMusicPath id = " + id);
        if (id == -1) {
            return null;
        }
        String path = null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("select * from " + MUSIC_TABLE + " where " + ID_COLUMN + " = ?", new String[]{"" + id});
            //cursor = db.query(MUSIC_TABLE, null, ID_COLUMN + " = ?", new String[]{"" + id}, null, null, null);
            Log.i(TAG, "getCount: " + cursor.getCount());
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(PATH_COLUMN));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    //获取音乐表中的第一首音乐的ID
    public int getFirstId(int listNumber) {
        Cursor cursor = null;
        int id = -1;
        try {
            switch (listNumber) {
                case Constant.LIST_ALLMUSIC:
                    cursor = db.rawQuery("select min(id) from " + MUSIC_TABLE, null);
                    break;
                default:
                    Log.i(TAG, "getFirstId: default");
                    break;
            }
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0);
                Log.d(TAG, "getFirstId min id = " + id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return id;
    }


    public void addPlayCount(int musicId) {
        MusicInfo musicInfo = getMusicFromAllById(musicId);
        int count = Integer.parseInt(musicInfo.getCount()) + 1;
        ContentValues cv = new ContentValues();
        cv.put(PLAY_COUNT, String.valueOf(count));
        db.update(MUSIC_TABLE, cv, ID_COLUMN + "=?", new String[]{String.valueOf(musicId)});
        if (musicInfo.getLove() == 1 || musicInfo.getFolderName().equalsIgnoreCase(Constant.FOLDER_LOVE)) {
            if (musicInfo.getFolderName().equalsIgnoreCase(Constant.FOLDER_LOVE)) {
                ContentValues cv2 = new ContentValues();
                cv2.put(PLAY_COUNT, String.valueOf(count));
                db.update(MUSIC_TABLE, cv2, ID_COLUMN + "=?", new String[]{String.valueOf(musicInfo.getAlbum())});
            } else {
                List<MusicInfo> loves = getAllMusicByFoderName(Constant.FOLDER_LOVE);
                for (MusicInfo info : loves) {
                    if (musicInfo.getPath().equalsIgnoreCase(info.getPath())) {
                        ContentValues cv2 = new ContentValues();
                        cv2.put(PLAY_COUNT, String.valueOf(count));
                        db.update(MUSIC_TABLE, cv2, ID_COLUMN + "=?", new String[]{String.valueOf(info.getId())});
                    }
                }
            }

        }

    }

    // 获取下一首歌曲(id)
    public int getNextMusic(List<Integer> musicList, int id, int playMode) {
        if (id == -1) {
            return -1;
        }
        //找到当前id在列表的第几个位置（i+1）
        int index = musicList.indexOf(id);
        if (index == -1) {
            return -1;
        }
        switch (playMode) {
            case Constant.LOOP_LIST:
                // 如果当前是最后一首
                if ((index + 1) == musicList.size()) {
                    id = musicList.get(0);
                } else {
                    ++index;
                    id = musicList.get(index);
                }
                break;
            case Constant.LOOP_ONE:
                break;
            case Constant.LOOP_RANDOM:
                id = getRandomMusic(musicList, id);
                break;
            case Constant.LOOP_NONE:
                // 如果当前是最后一首
                if ((index + 1) == musicList.size()) {
                    id = -2;
                } else {
                    ++index;
                    id = musicList.get(index);
                }
                break;
        }
        return id;
    }

    // 获取上一首歌曲(id)
    public int getPreMusic(List<Integer> musicList, int id, int playMode) {
        if (id == -1) {
            return -1;
        }
        //找到当前id在列表的第几个位置（i+1）
        int index = musicList.indexOf(id);
        if (index == -1) {
            return -1;
        }
        // 如果当前是第一首则返回最后一首
        switch (playMode) {
            case Constant.LOOP_LIST:
                if (index == 0) {
                    id = musicList.get(musicList.size() - 1);
                } else {
                    --index;
                    id = musicList.get(index);
                }
                break;
            case Constant.LOOP_ONE:
                break;
            case Constant.LOOP_RANDOM:
                id = getRandomMusic(musicList, id);
                break;
            case Constant.LOOP_NONE:
                if (index == 0) {
                    id = musicList.get(musicList.size() - 1);
                } else {
                    --index;
                    id = musicList.get(index);
                }
                break;
        }
        return id;
    }


    // 获取歌曲详细信息
    public ArrayList<String> getMusicInfo(int id) {
        if (id == -1) {
            return null;
        }
        Cursor cursor = null;
        ArrayList<String> musicInfo = new ArrayList<String>();
        cursor = db.rawQuery("select * from " + MUSIC_TABLE + " where " + ID_COLUMN + " = ?", new String[]{"" + id});
        //cursor = db.query(MUSIC_TABLE, null, ID_COLUMN + " = ?", new String[]{"" + id}, null, null, null);
        if (cursor.moveToFirst()) {
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                musicInfo.add(i, cursor.getString(i));
            }
        } else {
            musicInfo.add("0");
            musicInfo.add("听听音乐");
            musicInfo.add("好音质");
            musicInfo.add("0");
            musicInfo.add("0");
            musicInfo.add("0");
            musicInfo.add("0");
            musicInfo.add("0");
            musicInfo.add("0");
            musicInfo.add("0");
            musicInfo.add("0");
            musicInfo.add("-1");
        }
        if (cursor != null) {
            cursor.close();
        }
        return musicInfo;
    }

    //获取随机歌曲
    public int getRandomMusic(List<Integer> list, int id) {
        int musicId;
        if (id == -1) {
            return -1;
        }
        if (list.isEmpty()) {
            return -1;
        }
        if (list.size() == 1) {
            return id;
        }
        do {
            int count = (int) (Math.random() * list.size());
            musicId = list.get(count);
        } while (musicId == id);

        return musicId;

    }


    //把MusicInfo对象转为ContentValues对象
    public ContentValues musicInfoToContentValues(MusicInfo musicInfo) {
        ContentValues values = new ContentValues();
        try {
//            values.put(DatabaseHelper.ID_COLUMN, musicInfo.getId());
            values.put(NAME_COLUMN, musicInfo.getName());
            values.put(DatabaseHelper.SINGER_COLUMN, musicInfo.getSinger());
            values.put(DatabaseHelper.ALBUM_COLUMN, musicInfo.getAlbum());
            values.put(DatabaseHelper.DURATION_COLUMN, musicInfo.getDuration());
            values.put(PATH_COLUMN, musicInfo.getPath());
            values.put(DatabaseHelper.PARENT_PATH_COLUMN, musicInfo.getParentPath());
            values.put(LOVE_COLUMN, musicInfo.getLove());
            values.put(DatabaseHelper.FIRST_LETTER_COLUMN, "" + Pinyin.toPinyin(musicInfo.getName().charAt(0)).substring(0, 1).toUpperCase());
            values.put(DatabaseHelper.FOLDER_NAME, musicInfo.getFolderName().trim());
            values.put(PLAY_COUNT, musicInfo.getCount());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return values;
    }

    //把Cursor对象转为List<MusicInfo>对象
    public List<MusicInfo> cursorToMusicList(Cursor cursor,boolean isAll) {
        List<MusicInfo> list = null;
        try {
            if (cursor != null) {
                list = new ArrayList<>();
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndex(ID_COLUMN));
                    String name = cursor.getString(cursor.getColumnIndex(NAME_COLUMN));

                    String singer = cursor.getString(cursor.getColumnIndex(DatabaseHelper.SINGER_COLUMN));
                    String album = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ALBUM_COLUMN));
                    String duration = cursor.getString(cursor.getColumnIndex(DatabaseHelper.DURATION_COLUMN));
                    String path = cursor.getString(cursor.getColumnIndex(PATH_COLUMN));
                    String parentPath = cursor.getString(cursor.getColumnIndex(DatabaseHelper.PARENT_PATH_COLUMN));
                    int love = cursor.getInt(cursor.getColumnIndex(LOVE_COLUMN));
                    String firstLetter = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FIRST_LETTER_COLUMN));
                    String folderName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FOLDER_NAME));
                    String count = cursor.getString(cursor.getColumnIndex(PLAY_COUNT));
                    String time = cursor.getString(cursor.getColumnIndex(PRE_PLAY_TIME));
                    if (isAll && folderName.equalsIgnoreCase(Constant.FOLDER_LOVE) && love == 0) {
                        continue;
                    }
                    MusicInfo musicInfo = new MusicInfo();
                    musicInfo.setId(id);
                    musicInfo.setName(name);
                    musicInfo.setSinger(singer);
                    musicInfo.setAlbum(album);
                    musicInfo.setPath(path);
                    musicInfo.setParentPath(parentPath);
                    musicInfo.setLove(love);
                    musicInfo.setDuration(duration);
                    musicInfo.setFirstLetter(firstLetter);
                    musicInfo.setFolderName(folderName);
                    musicInfo.setCount(count);
                    musicInfo.setTime(time);

                    list.add(musicInfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


    //保留最近的100首
    public void setHistoryPlay(int id) {
        if (id == -1 || id == 0) {
            return;
        }
        ContentValues values = new ContentValues();
        ArrayList<Integer> lastList = new ArrayList<Integer>();
        Cursor cursor = null;
        lastList.add(id);
        db.beginTransaction();
        try {
            cursor = db.rawQuery("select id from " + DatabaseHelper.HISTORY_PLAY_TABLE, null);
            while (cursor.moveToNext()) {
                if (cursor.getInt(0) != id) {
                    lastList.add(cursor.getInt(0));
                }
            }
            db.delete(DatabaseHelper.HISTORY_PLAY_TABLE, null, null);
            if (lastList.size() < 100) {
                for (int i = 0; i < lastList.size(); i++) {
                    values.put(ID_COLUMN, lastList.get(i));
                    db.insert(DatabaseHelper.HISTORY_PLAY_TABLE, null, values);
                }
            } else {
                for (int i = 0; i < 100; i++) {
                    values.put(ID_COLUMN, lastList.get(i));
                    db.insert(DatabaseHelper.HISTORY_PLAY_TABLE, null, values);
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<MusicInfo> getHistory() {
        List<MusicInfo> musicInfoList = new ArrayList<>();
        Cursor cursor = db.rawQuery("select id from " + DatabaseHelper.HISTORY_PLAY_TABLE, null);
        while (cursor.moveToNext()) {
            MusicInfo musicInfo = getHistoryById(cursor.getInt(0));
            if (musicInfo != null) {
                musicInfoList.add(musicInfo);
            }

        }
        if (cursor != null) {
            cursor.close();
        }
        return musicInfoList;
    }

    private MusicInfo getHistoryById(int id) {
        List<MusicInfo> musicInfoList = null;
        MusicInfo musicInfo = null;
        Cursor cursor = null;
        db.beginTransaction();
        try {
            cursor = db.rawQuery("select * from " + MUSIC_TABLE + " where " + ID_COLUMN + " = ?", new String[]{id + ""});
            musicInfoList = cursorToMusicList(cursor,true);
            if (musicInfoList.size() == 0) {
                return musicInfo;
            }
            musicInfo = musicInfoList.get(0);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            if (cursor != null) {
                cursor.close();
            }
        }
        return musicInfo;
    }

    // 获取音乐表歌曲数量
    public int getMusicCount(int table) {
        int musicCount = 0;
        Cursor cursor = null;
        switch (table) {
            case Constant.LIST_ALLMUSIC:
                cursor = db.query(MUSIC_TABLE, null, null, null, null, null, null);
                break;
            case Constant.LIST_LASTPLAY:
                cursor = db.query(DatabaseHelper.HISTORY_PLAY_TABLE, null, null, null, null, null, null);
                break;
            case Constant.LIST_MYLOVE:
                cursor = db.query(MUSIC_TABLE, null, LOVE_COLUMN + " = ? ", new String[]{"" + 1}, null, null, null);
                break;
            case Constant.LIST_MYPLAY:
                cursor = db.query(PLAY_LIST_TABLE, null, null, null, null, null, null);
                break;
        }
        if (cursor.moveToFirst()) {
            musicCount = cursor.getCount();
        }
        if (cursor != null) {
            cursor.close();
        }
        return musicCount;
    }


    public void setMyLove(int id, int isLove) {
        LogUtils.inserOperationLog("喜欢");
        MusicInfo musicInfo = getMusicFromAllById(id);
        if (musicInfo.getFolderName().equalsIgnoreCase(Constant.FOLDER_LOVE)) {
            ContentValues values = new ContentValues();
            values.put(LOVE_COLUMN, isLove);
            db.update(MUSIC_TABLE, values, ID_COLUMN + " = ? ", new String[]{"" + id});//更新喜欢文件夹里 的音乐
            db.update(MUSIC_TABLE, values, ID_COLUMN + " = ? ", new String[]{"" + musicInfo.getAlbum()});//更新实际文件夹里 的音乐

        } else {
            ContentValues values = new ContentValues();
            values.put(LOVE_COLUMN, isLove);
            db.update(MUSIC_TABLE, values, ID_COLUMN + " = ? ", new String[]{"" + id});

            if (isLove == 1) {
                MusicInfo loveMusic = new MusicInfo();
                loveMusic.setName(musicInfo.getName());
                loveMusic.setSinger(musicInfo.getSinger());
                loveMusic.setAlbum(id + ""); //保存一下相关真正的音乐类id
                loveMusic.setPath(musicInfo.getPath());
                loveMusic.setParentPath(musicInfo.getParentPath());
                loveMusic.setLove(1);
                loveMusic.setDuration(musicInfo.getDuration());
                loveMusic.setFirstLetter(musicInfo.getFirstLetter());
                loveMusic.setFolderName(Constant.FOLDER_LOVE);
                loveMusic.setCount(musicInfo.getCount());
                loveMusic.setTime("-1");
                insertMusicInfoToMusicTable(loveMusic);
            } else {
                List<MusicInfo> loves = getAllMusicByFoderName(Constant.FOLDER_LOVE);
                for (MusicInfo info : loves) {
                    if (info.getPath().equalsIgnoreCase(musicInfo.getPath())) {
                        ContentValues values2 = new ContentValues();
                        values2.put(LOVE_COLUMN, 0);
                        db.update(MUSIC_TABLE, values, ID_COLUMN + " = ? ", new String[]{"" + info.getId()});
                        break;
                    }
                }

            }
        }

    }

    /*public  List<MusicInfo> getAllMusic() {
        List<MusicInfo> musicInfoList = new ArrayList<>();
        Cursor cursor = null;
        db.beginTransaction();
        try {
            cursor = db.rawQuery("select * from " + MUSIC_TABLE , null);
            musicInfoList = cursorToMusicList(cursor);

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            if (cursor != null) {
                cursor.close();
            }
        }
        return musicInfoList;


    }*/

    public void deleteLoveMusic() {

        List<MusicInfo> loves = getAllMusicByFoderName(Constant.FOLDER_LOVE);
        for (MusicInfo info : loves) {
            if (info.getLove() == 0) {
                deleteMusic(info.getId());
                break;
            }
        }
    }

    public boolean isLoveByMusicId(String id) {
        Cursor cursor = null;
        cursor = db.rawQuery("select * from " + MUSIC_TABLE + " where " + ID_COLUMN + " = ? ", new String[]{id});
        int love = 0;
        while (cursor.moveToNext()) {

            love = cursor.getInt(cursor.getColumnIndex(LOVE_COLUMN));
        }
        if (cursor != null) {
            cursor.close();
        }
        if (love == 0) {
            return false;
        } else {
            return true;
        }
    }

    //设置上次播放的手机时间
    public void setMusicPrePlayTime(int id, String time) {
        ContentValues cv = new ContentValues();
        cv.put(PRE_PLAY_TIME, time);
        db.update(MUSIC_TABLE, cv, ID_COLUMN + "=?", new String[]{String.valueOf(id)});
        setHistoryPlay(id);
    }

}
