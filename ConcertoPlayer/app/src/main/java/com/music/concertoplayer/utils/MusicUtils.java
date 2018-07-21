/**
 * Copyright (lrc_arrow) www.longdw.com
 */
package com.music.concertoplayer.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Files.FileColumns;
import android.text.TextUtils;


import com.github.promeg.pinyinhelper.Pinyin;
import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.entity.FolderInfo;
import com.music.concertoplayer.entity.MusicInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import static com.music.concertoplayer.Constant.FILE_URL;

/**
 * 查询各主页信息，获取封面图片等
 */
public class MusicUtils {

    /*public static final int FILTER_SIZE = 1 * 1024 * 1024;// 1MB
    public static final int FILTER_DURATION = 1 * 60 * 1000;// 1分钟*/
    public static final int FILTER_SIZE = 0;// 1MB
    public static final int FILTER_DURATION = 0;// 1分钟


    public static String[] proj_music = new String[]{
            Media._ID, Media.TITLE,
            Media.DATA, Media.ALBUM_ID,
            Media.ALBUM, Media.ARTIST,
            Media.ARTIST_ID, Media.DURATION, Media.SIZE};
    private static String[] proj_album = new String[]{Albums._ID, Albums.ALBUM_ART,
            Albums.ALBUM, Albums.NUMBER_OF_SONGS, Albums.ARTIST};
    private static String[] proj_artist = new String[]{
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
            MediaStore.Audio.Artists._ID};
    private static String[] proj_folder = new String[]{FileColumns.DATA};


    /**
     * 获取包含音频文件的文件夹信息
     *
     * @param context
     * @return
     */
    /*public static List<FolderInfo> queryFolder(Context context) {
        Uri uri = MediaStore.Files.getContentUri("external");
        ContentResolver cr = context.getContentResolver();
        StringBuilder mSelection = new StringBuilder(FileColumns.MEDIA_TYPE
                + " = " + FileColumns.MEDIA_TYPE_AUDIO + " and " + "("
                + FileColumns.DATA + " like'%.mp3' or " + Media.DATA
                + " like'%.wma')");
        // 查询语句：检索出.mp3为后缀名，时长大于1分钟，文件大小大于1MB的媒体文件
        mSelection.append(" and " + Media.SIZE + " > " + FILTER_SIZE);
        mSelection.append(" and " + Media.DURATION + " > " + FILTER_DURATION);
        mSelection.append(") group by ( " + FileColumns.PARENT);
        List<FolderInfo> list = getFolderList(context,cr.query(uri, proj_folder, mSelection.toString(), null,
                null));
        return list;

    }*/
    public static List<FolderInfo> queryFolder(Context context) {
        List<FolderInfo> list = new ArrayList<>();
        File scanner5Directory = new File(FILE_URL);
        if (scanner5Directory.isDirectory()) {
            for (File file : scanner5Directory.listFiles()) {
                if (file.isDirectory()) {
                    if (file.listFiles().length != 0) {
                        FolderInfo info = new FolderInfo();
                        info.setFolder_count(file.listFiles().length);
                        if (App.getDbManager().isHasFolder(file.getName())) {
                            //为空表明没有添加过 反之添加过就跳过
                            continue;
                        }
                        info.setName(file.getName());
                        info.setPath(file.getAbsolutePath());
                        info.setFolder_sort(Pinyin.toPinyin(info.getName().charAt(0)).substring(0, 1).toUpperCase());
                        Bitmap bitmap = getFolderFirstBitmap(info);
                        info.setBitmap(bitmap);
                        info.setPre_play_id(-1);
                        info.setCan_recycle_able(0);
                        info.setPre_play_progress(0);
                        info.setFolder_select(1);
                        info.setPlaying(false);
                        list.add(info);
                    }
                }

            }
        }
        Collections.sort(list, new FolderComparator());
        //进行排序
        int preFolderCount  = App.getDbManager().getAllFolder().size();
        if (preFolderCount >= Constant.FOLDER_COUNT) {
            ToastUtils.showToast("已超过最大文件夹读取数量！");
            return App.getDbManager().getAllFolder();
        }

        list.subList(0, (list.size() <= (Constant.FOLDER_COUNT - preFolderCount)) ? list.size() : (Constant.FOLDER_COUNT - preFolderCount));

        for (int i = 0; i < list.size(); i++) {
            list.get(i).setFolder_order(i + preFolderCount);
        }
        if (list.size() != 0) {
            for (FolderInfo info : list) {
                App.getDbManager().createFolderName(info);
            }

        }
        if (preFolderCount == 0) {
            //创建我喜欢文件夹
            FolderInfo love = new FolderInfo();
            love.setName(Constant.FOLDER_LOVE);
            love.setFolder_order(list.size());
            love.setBitmap_path("");
            love.setFolder_count(0);
            love.setPath("");
            love.setFolder_sort(Pinyin.toPinyin(love.getName().charAt(0)).substring(0, 1).toUpperCase());
            love.setBitmap(null);
            love.setPre_play_id(-1);
            love.setCan_recycle_able(0);
            love.setPre_play_progress(0);
            love.setFolder_select(1);
            App.getDbManager().createFolderName(love);
        }

        return App.getDbManager().getAllFolder();

    }

    public static ArrayList<MusicInfo> queryMusic(Context context, String folderPath) {
        ContentResolver cr = context.getContentResolver();
        StringBuilder select = new StringBuilder(" 1=1 and title != ''");
        // 查询语句：检索出.mp3为后缀名，时长大于1分钟，文件大小大于1MB的媒体文件
        select.append(" and " + Media.SIZE + " > " + FILTER_SIZE);
        select.append(" and " + Media.DURATION + " > " + FILTER_DURATION);

        ArrayList<MusicInfo> list1 = new ArrayList<>();
        ArrayList<MusicInfo> list = getMusicListCursor(cr.query(Media.EXTERNAL_CONTENT_URI, proj_music,
                select.toString(), null,
                null));
        for (MusicInfo music : list) {
            if (music.getPath().substring(0, music.getPath().lastIndexOf(File.separator)).equals(folderPath)) {
                //storage/emulated/0/ConcertoPlayerData/john king 2
                String[] path = folderPath.split(File.separator);
                music.setFolderName(path[path.length -1]);
                music.setCount("0");
                list1.add(music);
            }
        }
        App.getDbManager().insertMusicListToMusicTable(list1);
        return list1;


    }



    public static ArrayList<MusicInfo> getMusicListCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        ArrayList<MusicInfo> musicList = new ArrayList<>();
        while (cursor.moveToNext()) {
            MusicInfo music = new MusicInfo();
            music.setId(cursor.getInt(cursor
                    .getColumnIndex(Media._ID)));
            music.setAlbum(cursor.getString(cursor
                    .getColumnIndex(Albums.ALBUM)));
            //music.albumData = getAlbumArtUri(music.albumId) + "";
            music.setDuration(String.valueOf(cursor.getInt(cursor
                    .getColumnIndex(Media.DURATION))));
            music.setName(cursor.getString(cursor
                    .getColumnIndex(Media.TITLE)));
            music.setSinger(cursor.getString(cursor
                    .getColumnIndex(Media.ARTIST)));
            // music.artistId = cursor.getLong(cursor.getColumnIndex(Media.ARTIST_ID));
            String filePath = cursor.getString(cursor
                    .getColumnIndex(Media.DATA));
            if (App.getDbManager().isContainByMusicPath(filePath)) {
                //如果这个音乐文件已经有了就跳过
                continue;
            }
            music.setPath(filePath);
            //music.folder = filePath.substring(0, filePath.lastIndexOf(File.separator));
            //music.size = cursor.getInt(cursor.getColumnIndex(Media.SIZE));
            //music.islocal = true;
            music.setFirstLetter(Pinyin.toPinyin(music.getName().charAt(0)).substring(0, 1).toUpperCase());
            music.setCount("0");
            music.setTime("-1");
            musicList.add(music);
        }
        cursor.close();
        return musicList;
    }


/*

    public static List<FolderInfo> getFolderList(Context context, Cursor cursor) {
        List<FolderInfo> list = new ArrayList<>();
        Logger.d("cursor.getCount()" +cursor.getCount());
        while (cursor.moveToNext()) {
            FolderInfo info = new FolderInfo();
            String filePath = cursor.getString(cursor
                    .getColumnIndex(FileColumns.DATA));
            info.setFolder_count(cursor.getCount());
            info.setPath(filePath.substring(0, filePath.lastIndexOf(File.separator)));
            info.setName(info.getPath().substring(info.getPath()
                    .lastIndexOf(File.separator) + 1));
            info.setFolder_sort(Pinyin.toPinyin(info.getName().charAt(0)).substring(0, 1).toUpperCase());
            String[] name = filePath.split(File.separator);
            if (name[Integer.valueOf(name.length - 3)].equalsIgnoreCase(Constant.FOLDER_NAME) && info.getFolder_count() != 0) {
                Bitmap bitmap = getFolderFirstBitmap(info);
                if (bitmap != null) {
                    info.setBitmap(bitmap);
                }
                Log.d("chen", "getFolderList  " + info.getName());
                if (list.size() < FOLDER_COUNT) {
                    boolean isHas = false;
                    for (FolderInfo hasInfo : list) {
                        if (hasInfo.getName().equalsIgnoreCase(info.getName())) {
                            Log.d("chen", "getFolderList 相同 ");
                            isHas = true;
                        }
                    }
                    if (!isHas) {
                        list.add(info);
                    }

                }
            }
        }

        Collections.sort(list, new FolderComparator());
        for (FolderInfo info : list) {
            App.getDbManager().createFolderName(info);
        }
        cursor.close();
        return list;
    }
*/

    private static Bitmap getFolderFirstBitmap(FolderInfo info) {
        Bitmap bitmap = null;
        File  scanner5Directory = new File(info.getPath());
        if (scanner5Directory.isDirectory()) {
            for (File file : scanner5Directory.listFiles()) {
                String path = file.getAbsolutePath();
                if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")) {
                    info.setBitmap_path(path);
                    bitmap = BitmapFactory.decodeFile(path);
                    break;

                }
            }
        }
        return bitmap;
    }

   /* public static void createAllPlayOrder() {
        App.getDbManager().deletePlayAllList();
        List<FolderInfo> folderInfoList = App.getDbManager().getAllFolder();
        List<List<MusicInfo>> allMusicInfo = new ArrayList<>();
        List<MusicInfo> playAllMusicList = new ArrayList<>();
        int maxMusic = 0;
        for (FolderInfo folderInfo : folderInfoList) {
            List<MusicInfo> musicInfoList = App.getDbManager().getAllMusicByFoderName(folderInfo.getName());
            if (musicInfoList.size() > maxMusic) {
                maxMusic = musicInfoList.size();
            }
            allMusicInfo.add(musicInfoList);
        }
        for (int i = 0; i < maxMusic; i++) {
            for (int j = 0; j < folderInfoList.size(); j++) {
                if (allMusicInfo.get(j).size() - 1 < i) {
                    continue;
                } else {
                    playAllMusicList.add(allMusicInfo.get(j).get(i));
                }
            }
        }
        if (playAllMusicList.size() != 0) {
            App.getDbManager().inserMusicInfoToAllPlayList(playAllMusicList);
        }

    }*/


    /**
     * 获取歌手信息
     *
     * @param context
     * @return
     */
    /*public static List<ArtistInfo> queryArtist(Context context) {

        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        ContentResolver cr = context.getContentResolver();
        StringBuilder where = new StringBuilder(MediaStore.Audio.Artists._ID
                + " in (select distinct " + Media.ARTIST_ID
                + " from audio_meta where (1=1 )");
        where.append(" and " + Media.SIZE + " > " + FILTER_SIZE);
        where.append(" and " + Media.DURATION + " > " + FILTER_DURATION);

        where.append(")");

        List<ArtistInfo> list = getArtistList(cr.query(uri, proj_artist,
                where.toString(), null, PreferencesUtility.getInstance(context).getArtistSortOrder()));

        return list;

    }

    */

    /**
     * 获取专辑信息
     *
     * @param
     * @param context
     * @return
     *//*
    public static List<AlbumInfo> queryAlbums(Context context) {

        ContentResolver cr = context.getContentResolver();
        StringBuilder where = new StringBuilder(Albums._ID
                + " in (select distinct " + Media.ALBUM_ID
                + " from audio_meta where (1=1)");
        where.append(" and " + Media.SIZE + " > " + FILTER_SIZE);
        where.append(" and " + Media.DURATION + " > " + FILTER_DURATION);

        where.append(" )");

        // Media.ALBUM_KEY 按专辑名称排序
        List<AlbumInfo> list = getAlbumList(cr.query(Albums.EXTERNAL_CONTENT_URI, proj_album,
                where.toString(), null, PreferencesUtility.getInstance(context).getAlbumSortOrder()));
        return list;

    }



    public static List<MusicInfo> queryMusic(Context context, int from) {
        return queryMusic(context, null, from);
    }


    public static ArrayList<MusicInfo> queryMusic(Context context, String id, int from) {

        Uri uri = Media.EXTERNAL_CONTENT_URI;
        ContentResolver cr = context.getContentResolver();

        StringBuilder select = new StringBuilder(" 1=1 and title != ''");
        // 查询语句：检索出.mp3为后缀名，时长大于1分钟，文件大小大于1MB的媒体文件
        select.append(" and " + Media.SIZE + " > " + FILTER_SIZE);
        select.append(" and " + Media.DURATION + " > " + FILTER_DURATION);

        String selectionStatement = "is_music=1 AND title != ''";
        final String songSortOrder = PreferencesUtility.getInstance(context).getSongSortOrder();


        switch (from) {
            case START_FROM_LOCAL:
                ArrayList<MusicInfo> list3 = getMusicListCursor(cr.query(uri, proj_music,
                        select.toString(), null,
                        songSortOrder));
                return list3;
            case START_FROM_ARTIST:
                select.append(" and " + Media.ARTIST_ID + " = " + id);
                return getMusicListCursor(cr.query(uri, proj_music, select.toString(), null,
                        PreferencesUtility.getInstance(context).getArtistSongSortOrder()));
            case START_FROM_ALBUM:
                select.append(" and " + Media.ALBUM_ID + " = " + id);
                return getMusicListCursor(cr.query(uri, proj_music,
                        select.toString(), null,
                        PreferencesUtility.getInstance(context).getAlbumSongSortOrder()));
            case START_FROM_FOLDER:
                ArrayList<MusicInfo> list1 = new ArrayList<>();
                ArrayList<MusicInfo> list = getMusicListCursor(cr.query(Media.EXTERNAL_CONTENT_URI, proj_music,
                        select.toString(), null,
                        null));
                for (MusicInfo music : list) {
                    if (music.data.substring(0, music.data.lastIndexOf(File.separator)).equals(id)) {
                        list1.add(music);
                    }
                }
                return list1;
            default:
                return null;
        }

    }


    public static ArrayList<MusicInfo> getMusicLists(Context context, long[] id) {
        final StringBuilder selection = new StringBuilder();
        selection.append(MediaStore.Audio.Media._ID + " IN (");
        for (int i = 0; i < id.length; i++) {
            selection.append(id[i]);
            if (i < id.length - 1) {
                selection.append(",");
            }
        }
        selection.append(")");

        //sqlite 不支持decode

//        final StringBuilder order = new StringBuilder();
//        order.append("DECODE(" +MediaStore.Audio.Media._ID +",");
//        for (int i = 0; i < id.length; i++) {
//            order.append(id[i]);
//            order.append(",");
//            order.append(i);
//            if (i < id.length - 1) {
//                order.append(",");
//            }
//        }
//        order.append(")");

        Cursor cursor = (context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI, proj_music,
                selection.toString(),
                null, null));
        if (cursor == null) {
            return null;
        }
        ArrayList<MusicInfo> musicList = new ArrayList<>();
        musicList.ensureCapacity(id.length);
        for (int i = 0; i < id.length; i++) {
            musicList.add(null);
        }

        while (cursor.moveToNext()) {
            MusicInfo music = new MusicInfo();
            music.songId = cursor.getInt(cursor
                    .getColumnIndex(Media._ID));
            music.albumId = cursor.getInt(cursor
                    .getColumnIndex(Media.ALBUM_ID));
            music.albumName = cursor.getString(cursor
                    .getColumnIndex(Albums.ALBUM));
            music.albumData = getAlbumArtUri(music.albumId) + "";
            music.musicName = cursor.getString(cursor
                    .getColumnIndex(Media.TITLE));
            music.artist = cursor.getString(cursor
                    .getColumnIndex(Media.ARTIST));
            music.artistId = cursor.getLong(cursor.getColumnIndex(Media.ARTIST_ID));
            music.islocal = true;
            for (int i = 0; i < id.length; i++) {
                if (id[i] == music.songId) {
                    musicList.set(i, music);
                }
            }
        }
        cursor.close();
        return musicList;
    }


    public static ArrayList<MusicInfo> getMusicListCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        ArrayList<MusicInfo> musicList = new ArrayList<>();
        while (cursor.moveToNext()) {
            MusicInfo music = new MusicInfo();
            music.songId = cursor.getInt(cursor
                    .getColumnIndex(Media._ID));
            music.albumId = cursor.getInt(cursor
                    .getColumnIndex(Media.ALBUM_ID));
            music.albumName = cursor.getString(cursor
                    .getColumnIndex(Albums.ALBUM));
            music.albumData = getAlbumArtUri(music.albumId) + "";
            music.duration = cursor.getInt(cursor
                    .getColumnIndex(Media.DURATION));
            music.musicName = cursor.getString(cursor
                    .getColumnIndex(Media.TITLE));
            music.artist = cursor.getString(cursor
                    .getColumnIndex(Media.ARTIST));
            music.artistId = cursor.getLong(cursor.getColumnIndex(Media.ARTIST_ID));
            String filePath = cursor.getString(cursor
                    .getColumnIndex(Media.DATA));
            music.data = filePath;
            music.folder = filePath.substring(0, filePath.lastIndexOf(File.separator));
            music.size = cursor.getInt(cursor
                    .getColumnIndex(Media.SIZE));
            music.islocal = true;
            music.sort = Pinyin.toPinyin(music.musicName.charAt(0)).substring(0, 1).toUpperCase();
            musicList.add(music);
        }
        cursor.close();
        return musicList;
    }

    public static List<AlbumInfo> getAlbumList(Cursor cursor) {
        List<AlbumInfo> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            AlbumInfo info = new AlbumInfo();
            info.album_name = cursor.getString(cursor
                    .getColumnIndex(Albums.ALBUM));
            info.album_id = cursor.getInt(cursor.getColumnIndex(Albums._ID));
            info.number_of_songs = cursor.getInt(cursor
                    .getColumnIndex(Albums.NUMBER_OF_SONGS));
            info.album_art = getAlbumArtUri(info.album_id) + "";
            info.album_artist = cursor.getString(cursor.getColumnIndex(Albums.ARTIST));
            info.album_sort = Pinyin.toPinyin(info.album_name.charAt(0)).substring(0, 1).toUpperCase();
            list.add(info);
        }
        cursor.close();
        return list;
    }

    public static List<ArtistInfo> getArtistList(Cursor cursor) {
        List<ArtistInfo> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            ArtistInfo info = new ArtistInfo();
            info.artist_name = cursor.getString(cursor
                    .getColumnIndex(MediaStore.Audio.Artists.ARTIST));
            info.number_of_tracks = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
            info.artist_id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
            info.artist_sort = Pinyin.toPinyin(info.artist_name.charAt(0)).substring(0, 1).toUpperCase();
            list.add(info);
        }
        cursor.close();
        return list;
    }*/


   /* public static String makeTimeString(long milliSecs) {
        StringBuffer sb = new StringBuffer();
        long m = milliSecs / (60 * 1000);
        sb.append(m < 10 ? "0" + m : m);
        sb.append(":");
        long s = (milliSecs % (60 * 1000)) / 1000;
        sb.append(s < 10 ? "0" + s : s);
        return sb.toString();
    }


    public static Uri getAlbumArtUri(long albumId) {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
    }

    public static Uri getAlbumUri(Context context, long musicId) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(Media.EXTERNAL_CONTENT_URI, proj_music, "_id =" + String.valueOf(musicId), null, null);
        long id = -3;
        if (cursor == null) {
            return null;
        }
        if (cursor.moveToFirst()) {
            id = cursor.getInt(cursor.getColumnIndex(Media.ALBUM_ID));
        }

        cursor.close();
        return getAlbumArtUri(id);
    }

    public static String getAlbumdata(Context context, long musicid) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(Media.EXTERNAL_CONTENT_URI, proj_music, "_id = " + String.valueOf(musicid), null, null);
        if (cursor == null) {
            return null;
        }
        long albumId = -1;
        if (cursor.moveToNext()) {
            albumId = cursor.getLong(cursor.getColumnIndex(Media.ALBUM_ID));
        }

        if (albumId != -1) {
            cursor = cr.query(Albums.EXTERNAL_CONTENT_URI, proj_album, Albums._ID + " = " + String.valueOf(albumId), null, null);
        }
        if (cursor == null) {
            return null;
        }
        String data = "";
        if (cursor.moveToNext()) {
            data = cursor.getString(cursor.getColumnIndex(Albums.ALBUM_ART));
        }
        cursor.close();

        return data;
    }


    public static ArtistInfo getArtistinfo(Context context, long id) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, proj_artist, "_id =" + String.valueOf(id), null, null);
        if (cursor == null) {
            return null;
        }
        ArtistInfo artistInfo = new ArtistInfo();
        while (cursor.moveToNext()) {
            artistInfo.artist_name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
            artistInfo.number_of_tracks = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
        }
        cursor.close();
        return artistInfo;
    }


    public static AlbumInfo getAlbumInfo(Context context, long albumId) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(Albums.EXTERNAL_CONTENT_URI, proj_album, "_id =" + String.valueOf(albumId), null, null);
        if (cursor == null) {
            return null;
        }
        AlbumInfo albumInfo = new AlbumInfo();
        while (cursor.moveToNext()) {
            albumInfo.album_name = cursor.getString(cursor.getColumnIndex(Albums.ALBUM));
            albumInfo.album_art = cursor.getString(cursor.getColumnIndex(Albums.ALBUM_ART));
        }
        cursor.close();
        return albumInfo;

    }


    public static MusicInfo getMusicInfo(Context context, long id) {
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(Media.EXTERNAL_CONTENT_URI, proj_music, "_id = " + String.valueOf(id), null, null);
        if (cursor == null) {
            return null;
        }
        MusicInfo music = new MusicInfo();
        while (cursor.moveToNext()) {
            music.songId = cursor.getInt(cursor
                    .getColumnIndex(Media._ID));
            music.albumId = cursor.getInt(cursor
                    .getColumnIndex(Media.ALBUM_ID));
            music.albumName = cursor.getString(cursor
                    .getColumnIndex(Albums.ALBUM));
            music.albumData = getAlbumArtUri(music.albumId) + "";
            music.duration = cursor.getInt(cursor
                    .getColumnIndex(Media.DURATION));
            music.musicName = cursor.getString(cursor
                    .getColumnIndex(Media.TITLE));
            music.size = cursor.getInt(cursor.getColumnIndex(Media.SIZE));
            music.artist = cursor.getString(cursor
                    .getColumnIndex(Media.ARTIST));
            music.artistId = cursor.getLong(cursor.getColumnIndex(Media.ARTIST_ID));
            String filePath = cursor.getString(cursor
                    .getColumnIndex(Media.DATA));
            music.data = filePath;
            String folderPath = filePath.substring(0,
                    filePath.lastIndexOf(File.separator));
            music.folder = folderPath;
            music.sort = Pinyin.toPinyin(music.musicName.charAt(0)).substring(0, 1).toUpperCase();
        }
        cursor.close();
        return music;
    }


    public static String makeShortTimeString(final Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs %= 3600;
        mins = secs / 60;
        secs %= 60;

        final String durationFormat = context.getResources().getString(hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }*/


}

