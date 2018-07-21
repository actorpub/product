package com.music.concertoplayer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = DatabaseHelper.class.getName();
	//数据库名
	private static final String DATABASE_NAME = "MusicDatabase.db";

	//音乐表
	public static final String MUSIC_TABLE = "music_table";
	//用户
	public static final String USER_TABLE = "user_table";
	//音乐  文件夹 字符
	public static final String ID_COLUMN = "id";
	public static final String MUSIC_ID_COLUMN = "music_id";
	public static final String NAME_COLUMN = "name";
	public static final String FOLDER_IMG = "img";
	public static final String FOLDER_NAME = "folder_name";
	public static final String PLAY_COUNT = "count";
	public static final String PRE_PLAY_TIME = "pre_play_time"; //上次播放时间
	public static final String SINGER_COLUMN = "singer";
	public static final String DURATION_COLUMN = "duration";
	public static final String ALBUM_COLUMN = "album";
	public static final String PATH_COLUMN = "path";
	public static final String PARENT_PATH_COLUMN = "parent_path";
	public static final String FIRST_LETTER_COLUMN = "first_letter";
	public static final String LOVE_COLUMN = "love";
	public static final String PRE_PLAY_COLUMN = "pre_play";
	public static final String PRE_PLAY_PROGRESS = "pre_play_progress";
	public static final String FOLDER_ORDER= "folder_order";
	public static final String FOLDER_SELECT= "folder_select";  //是否选中 选中为 1  没有选中为0
	public static final String CAN_RECYCLE_ABLE = "can_recycle_able";


	//用户 字符
	public static final String USER_NAME = "user_name";
	public static final String USER_UNIONID = "user_unionid";
	public static final String USER_OPENID = "user_openid";
	public static final String USER_MOBILE = "user_mobile";
	public static final String USER_LATITUDE = "user_latitude";
	public static final String USER_LONGITUDE = "user_longitude";
	public static final String USER_DEVICE_TYPE = "user_device_type";
	public static final String USER_DEVICEID = "user_deviceid";
	public static final String USER_EXPIRY = "user_expiry";


	//最近播放表
	public static final String HISTORY_PLAY_TABLE = "history_play_table";

	//单个歌单表
	public static final String PLAY_LIST_TABLE = "play_list_table";
	//包含所有音乐的歌单表
	public static final String PLAY_ALL_LIST_TABLE = "play_all_list_table";
	//歌单歌曲表
	public static final String PLAY_LISY_MUSIC_TABLE = "play_list_music_table";


	//数据库版本号
	private static final int VERSION = 2;

	//音乐表建表语句
	private String createMusicTable = "create table if not exists " + MUSIC_TABLE+ "("
			+ ID_COLUMN +" integer PRIMARY KEY ,"
			+ NAME_COLUMN +" text,"
			+ SINGER_COLUMN +" text,"
			+ ALBUM_COLUMN + " text,"
			+ DURATION_COLUMN + " long,"
			+ PATH_COLUMN + " text,"
			+ PARENT_PATH_COLUMN + " text,"
			+ LOVE_COLUMN + " integer,"
			+ FIRST_LETTER_COLUMN + " text,"
			+ FOLDER_NAME + " text ,"
			+ PLAY_COUNT + " text ,"
			+ PRE_PLAY_TIME + " text );";

	//创建播放历史表
	private String createLastPlayTable = "create table if not exists " + HISTORY_PLAY_TABLE +" ("
			+ ID_COLUMN +" integer,"
			+ "FOREIGN KEY(id) REFERENCES "+ MUSIC_TABLE + " (id) ON DELETE CASCADE);";


	//创建歌单表 文件夹
	private String createPlaylistTable = "create table if not exists " + PLAY_LIST_TABLE + " ("
			+ ID_COLUMN +" integer PRIMARY KEY autoincrement,"
			+ FOLDER_IMG + " text,"
			+ PATH_COLUMN + " text,"
			+ NAME_COLUMN + " text,"
			+ PRE_PLAY_COLUMN + " integer ,"
			+ CAN_RECYCLE_ABLE + " integer ,"
			+ PRE_PLAY_PROGRESS + " integer ,"
			+ FOLDER_ORDER + " integer ,"
			+ FOLDER_SELECT + " integer );";


	//创建用户表
	private String createUserTable = "create table if not exists " + USER_TABLE+ "("
			+ ID_COLUMN +" integer PRIMARY KEY ,"
			+ USER_NAME +" text,"
			+ USER_UNIONID +" text,"
			+ USER_OPENID + " text,"
			+ USER_MOBILE + " text,"
			+ USER_LATITUDE + " text,"
			+ USER_LONGITUDE + " text,"
			+ USER_DEVICE_TYPE + " text,"
			+ USER_DEVICEID + " text,"
			+ USER_EXPIRY + " text );";




	//所有歌的歌单表
	private String createAllPlaylistTable = "create table if not exists " + PLAY_ALL_LIST_TABLE + " ("
			+ ID_COLUMN +" integer PRIMARY KEY autoincrement,"
			+ MUSIC_ID_COLUMN + " integer);";






	//创建歌单歌曲表
	private String createListinfoTable = "create table if not exists " + PLAY_LISY_MUSIC_TABLE +" ("
			+ ID_COLUMN + " integer,"
			+ MUSIC_ID_COLUMN + " integer,"
			+ "FOREIGN KEY(id) REFERENCES " + PLAY_LIST_TABLE + "(id) ON DELETE CASCADE,"
			+ "FOREIGN KEY(music_id) REFERENCES "+ MUSIC_TABLE + " (id) ON DELETE CASCADE) ;";




	public DatabaseHelper(Context context) {
		// 数据库实际被创建是在getWritableDatabase()或getReadableDatabase()方法调用时
		super(context, DATABASE_NAME, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate");
		db.execSQL(createMusicTable);			//创建音乐表
		db.execSQL(createUserTable);			//创建用户
		db.execSQL(createLastPlayTable);		//创建播放历史表
		db.execSQL(createPlaylistTable);		//创建歌单表
		db.execSQL(createAllPlaylistTable);		//创建所有音乐歌单表
		db.execSQL(createListinfoTable);		//创建歌单歌曲表

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.e(TAG, "onUpgrade: oldVersion ="+oldVersion );
		Log.e(TAG, "onUpgrade: newVersion ="+newVersion );
		if (oldVersion < VERSION){
			db.execSQL("drop table if exists "+MUSIC_TABLE);
			db.execSQL("drop table if exists "+createUserTable);
			db.execSQL("drop table if exists "+ HISTORY_PLAY_TABLE);
			db.execSQL("drop table if exists "+ PLAY_LIST_TABLE);
			db.execSQL("drop table if exists "+ PLAY_ALL_LIST_TABLE);
			db.execSQL("drop table if exists "+ PLAY_LISY_MUSIC_TABLE);
			onCreate(db);
		}
	}


}
