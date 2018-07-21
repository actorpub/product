package com.music.concertoplayer;



public class Constant {
	//www.solnk.com
	public static final String LOGIN_URL = "http://www.solnk.com/validUser.php";
	public static final String LOG_URL = "http://www.solnk.com/logUpload/logUpload.php";
	public static final String UPDATE_URL = "http://www.solnk.com/update/ver.php";

	public static final int LOGIN_SUCCESS = 0;
	public static final int LOGIN_OUT_TIME = 10004;
	public static final int LOGIN_REPEATE = 10005;
	public static final int LOGIN_UPDATE = 10006;

	public static final int CHECK_MILLIS = 1000 * 60 * 60 * 3; //1000 * 60 * 60 * 3;//3个小时


	public static final String FILE_URL = "/storage/emulated/0/cmusic/" ;
	public static final String LOG_FILE_URL = "/storage/emulated/0/musicLog/" ;



	//读取文件夹名字
	public static final String FOLDER_NAME = "cmusic";

	//service名
	public static final String SERVICE_NAME = "com.example.vinyl.service.MediaPlayerService";//服务的名称为包名+类名
	//播放状态
	public static final String STATUS = "status";

	//我喜欢文件夹的名字
	public static final String FOLDER_LOVE = "folder_love";

	//页面位置
	/*public static final int SEQUENCE_BY_Z = 0;//横着放
	public static final int SEQUENCE_BY_N = 1;//竖着播*/

	//播放模式
	public static final int LOOP_LIST = 0;//列表循环
	public static final int LOOP_ONE = 1;//单曲循环
	public static final int LOOP_RANDOM = 2;//随机循环
	public static final int LOOP_NONE = 3;//不单曲循环

	//功能位置-同时代表播放范围
	public static final int SEQUENCE_BY_Z = 1;//文件夹列表，播放所有文件夹 横着放
	public static final int SEQUENCE_BY_N = 2;//竖着播

	public static final int POSITION_FOLDER_LIST = 3;//文件夹的曲目列表
	public static final int POSITION_FILE_LIST = 4;//文件夹下的曲目列表
	public static final int POSITION_HISTORY = 5;//播放历史，仅播放选中的曲目

	public static final int STATUS_STOP = 0; //停止状态
	public static final int STATUS_PLAY = 1; //播放状态
	public static final int STATUS_PAUSE = 2; //暂停状态
	public static final int STATUS_RUN = 3;  //   状态

	public static String WECHAT_APPID = "wx2c7efb213488f4a6";
	public static String WECHAT_SECRET = "d9a6b7a193a95cb8a2acc3de129b124f";


	public static final String BUGLY_LOG_APPID = "3cf763da6d";


	public static final int FOLDER_COUNT = 30;  //   最多读取的文件夹数目




	public static final int SKIP_SECOND = 15*1000;  //   快进多少



	public static final String PLAY_CURRENT = "play_current";  //   播放的现在
	public static final String PLAY_TOTAL = "play_total";  //   总的播放







	public static final String COMMAND = "cmd";

	public static final int COMMAND_INIT = 1; //初始化命令
	public static final int COMMAND_PLAY = 2; //播放命令
	public static final int COMMAND_PAUSE = 3; //暂停命令
	public static final int COMMAND_STOP = 4; //停止命令
	public static final int COMMAND_PROGRESS = 5; //改变进度命令
	public static final int COMMAND_RELEASE = 6; //退出程序时释放
	public static final int COMMAND_COMPLETE = 7; //播放完成
	public static final int COMMAND_SPEEED = 8; //播放倍速



	public static final String PLAYMODE_SEQUENCE_TEXT = "顺序播放";
	public static final String PLAYMODE_RANDOM_TEXT = "随机播放";
	public static final String PLAYMODE_SINGLE_REPEAT_TEXT = "单曲循环";


	//Activity label

	public static final String LABEL = "label";
	public static final String LABEL_MYLOVE = "我喜爱";
	public static final String LABEL_LAST = "最近播放";
	public static final String LABEL_LOCAL = "本地音乐";

	public static final int ACTIVITY_LOCAL = 20; //我喜爱
	public static final int ACTIVITY_RECENTPLAY = 21;//最近播放
	public static final int ACTIVITY_MYLOVE = 22; //我喜爱
	public static final int ACTIVITY_MYLIST = 24;//我的歌单





	//SharedPreferences key 常量
	public static final String KEY_ID = "id";
	public static final String KEY_LOCATION_COUNT = "location_count";
	public static final String KEY_PATH = "path";

	public static final String KEY_MODE = "mode";
	public static final String KEY_ORDER_MODE = "order_mode";
	public static final String KEY_ACTIVITY_STATUS = "activity_status";

	public static final String KEY_LIST = "list";
	public static final String KEY_LIST_ID = "list_id";
	public static final String KEY_CURRENT = "current";
	public static final String KEY_DURATION = "duration";
	public static final String KEY_ORDER = "order";//第几个
	public static final String KEY_SPEED = "speed";//倍速
	public static final String KEY_PITCHSEMI = "pitchSemi";//音调
	public static final String KEY_SPEED_RANGE = "speed_range";//倍速的使用范围  如果是适用于单个文件夹 就直接保存文件夹名字，如果适用于所有音乐就保存speedToAllSong

	//SharedPreferences value 常量 匹配 KEY_LIST
	public static final int LIST_SINGLE = 101;	//单曲列表
//	public static final int LIST_SINGLE = 101;	//歌手列表
//	public static final int LIST_SINGLE = 101;	//专辑列表
//	public static final int LIST_SINGLE = 101;	//最近播放列表
//	public static final int LIST_SINGLE = 101;	//我喜爱列表


	//歌曲列表常量
	public static final int LIST_ALLMUSIC = -1;
	public static final int LIST_MYLOVE = 10000;
	public static final int LIST_LASTPLAY = 10001;
	public static final int LIST_DOWNLOAD = 10002;
	public static final int LIST_MYPLAY = 10003; //我的歌单列表
	public static final int LIST_PLAYLIST = 10004;	//歌单音乐列表

	public static final int LIST_SINGER = 10005;	//歌手
	public static final int LIST_ALBUM = 10006;	    //专辑
	public static final int LIST_FOLDER = 10007;	//文件夹


	//ReceiverForMain.action
	public static final String UPDATE_MAIN_ACTIVITY ="MainActivityToReceiver.action";
	//MediaPlayerManager.action
	public static final String MP_FILTER = "com.example.vinyl.start_mediaplayer";
	//WidgetUtil.action
	public static final String UPDATE_WIDGET = "android.intent.ACTION_WIDGET";
	//UpdateWidget.action
	public static final String WIDGET_STATUS = "android.appwidget.action.WIDGET_STATUS";
	public static final String WIDGET_SEEK = "android.appwidget.action.WIDGET_SEEK";
	//
	public static final String MUSIC_CONTROL = "kugoumusic.ACTION_CONTROL";
	public static final String UPDATE_STATUS = "kugoumusic.ACTION_STATUS";

	//widget播放控制
	public static final String WIDGET_PLAY="android.appwidget.WIDGET_PLAY";
	public static final String WIDGET_NEXT="android.appwidget.WIDGET_NEXT";
	public static final String WIDGET_PREVIOUS="android.appwidget.WIDGET_PREVIOUS";


	//主题
	public static final String THEME="theme";
}
