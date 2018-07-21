package com.music.concertoplayer.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.activity.FolderActivity;
import com.music.concertoplayer.activity.MainActivity;
import com.music.concertoplayer.adaper.MainAdaper;
import com.music.concertoplayer.database.DBManager;
import com.music.concertoplayer.entity.FolderInfo;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.fragment.base.BaseFragment;
import com.music.concertoplayer.utils.DentityUtil;
import com.music.concertoplayer.net.LogUtils;
import com.music.concertoplayer.utils.MusicUtils;
import com.music.concertoplayer.manage.MusicPlayManage;
import com.music.concertoplayer.utils.RxBus;
import com.music.concertoplayer.utils.ToastUtils;
import com.music.concertoplayer.view.headview.ClassicsHeader;
import com.orhanobut.logger.Logger;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.music.concertoplayer.Constant.FILE_URL;
import static com.music.concertoplayer.utils.MusicUtils.FILTER_DURATION;
import static com.music.concertoplayer.utils.MusicUtils.FILTER_SIZE;
import static com.music.concertoplayer.utils.MusicUtils.proj_music;

/**
 * Created by chen on 2018/4/25.
 */

public class MainFragment extends BaseFragment {
    @BindView(R.id.bottom_container)
    FrameLayout bottom_container;
    @BindView(R.id.refreshLayout)
    RefreshLayout refreshLayout;
    @BindView(R.id.rv_main)
    RecyclerView recyclerView;
    private AlertDialog dialogSearch;
    private List<FolderInfo> folders = new ArrayList<>();
    private MainAdaper adaper;
    private DBManager dbManager;
    private TextView tv_title;
    private TextView tv_file;
    private TextView tv_music;
    private TextView tv_searching;


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 2) {
                dialogSearch.dismiss();
                //startPlay();
            } else {
                Bundle bundle = msg.getData();
                tv_searching.setText(bundle.getString("3"));
            }

            super.handleMessage(msg);

        }
    };



    @Override
    protected void initData() {
        dbManager = DBManager.getInstance(mContext);
        setUpData();
        showQuickControl(true);
        if (MusicPlayManage.getIntShared(Constant.KEY_ID) != -1) {
            bottom_container.setVisibility(View.VISIBLE);
            App.setIsShowPop(true);
        } else {
            bottom_container.setVisibility(View.VISIBLE);
            App.setIsShowPop(true);
        }

    }

    @Override
    protected void initViews() {
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, 3));
        adaper = new MainAdaper(mContext, R.layout.adapter_main_item, folders);
        adaper.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                FolderInfo folderEntity = folders.get(position);
                switch (view.getId()) {
                    case R.id.iv_img:
                        int preMusicId = dbManager.getFolderPrePlayIdByName(folderEntity.getName());
                        MusicPlayManage.setShared(Constant.FOLDER_NAME, folderEntity.getName());
                        List<MusicInfo> folderMusic = dbManager.getAllMusicByFoderName(folderEntity.getName());
                        if (preMusicId != -1) {
                            MusicPlayManage.playSingleMusic(mContext, dbManager.getMusicFromAllById(preMusicId));
                        } else {
                            if (folderMusic.size() != 0) {
                                MusicPlayManage.playSingleMusic(mContext, dbManager.getMusicFromAllById(folderMusic.get(0).getId()));
                            } else {
                                ToastUtils.showToast("文件夹音乐为空");
                                return;
                            }
                        }
                        RxBus.getDefault().post("open_bottom_bar");
                        LogUtils.inserOperationLog("点击文件夹播放");
                        break;
                    case R.id.tv_name:
                        Intent intent = new Intent(mContext, FolderActivity.class);
                        intent.putExtra("FolderInfo", folderEntity.getName());
                        startActivity(intent);
                        LogUtils.inserOperationLog("进入文件夹");
                        break;
                }

            }
        });
        recyclerView.setAdapter(adaper);//设置Item增加、移除动画
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        ClassicsHeader header = new ClassicsHeader(mContext);
        refreshLayout.setRefreshHeader(header);
        header.setOnSearchClickListener(new ClassicsHeader.OnSearchClickListener() {
            @Override
            public void onClick() {
                ToastUtils.showToast("开始搜索");
                File file = new File(FILE_URL);
                if (file.exists()) {
                    queryFolder();
                } else {
                    showSearchDialog(null, 0);
                }
            }
        });

        //0则不执行拖动或者滑动
        ItemTouchHelper.Callback mCallback = new ItemTouchHelper.Callback() {
            /**
             * 是否处理滑动事件 以及拖拽和滑动的方向 如果是列表类型的RecyclerView的只存在UP和DOWN，
             * 如果是网格类RecyclerView则还应该多有LEFT和RIGHT
             * @param recyclerView
             * @param viewHolder
             * @return
             */
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                    final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;//代表支持哪个方向的拖拽
                    final int swipeFlags = 0;
                    //final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;//代表处理滑动删除,将执行onSwiped()方法   代表支持哪个方向的滑动删除
                    return makeMovementFlags(dragFlags, swipeFlags);
                } else {
                    final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                    final int swipeFlags = 0;//为0 代表不处理滑动删除
                    //final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;//代表处理滑动删除,将执行onSwiped()方法
                    return makeMovementFlags(dragFlags, swipeFlags);
                }
            }

            /**
             * @param recyclerView
             * @param viewHolder 拖动的ViewHolder
             * @param target 目标位置的ViewHolder
             * @return
             */
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();//得到拖动ViewHolder的position
                int toPosition = target.getAdapterPosition();//得到目标ViewHolder的position
                Log.d("chen", "fromPosition" + fromPosition + "toPosition" + toPosition);
                if (fromPosition < toPosition) {
                    //分别把中间所有的item的位置重新交换
                    for (int i = fromPosition; i < toPosition; i++) {
                        int order  = folders.get(i).getFolder_order();
                        folders.get(i).setFolder_order(folders.get(i + 1).getFolder_order());
                        folders.get(i + 1).setFolder_order(order);
                        dbManager.updateFolderOrder(folders.get(i).getName(),folders.get(i).getFolder_order());
                        dbManager.updateFolderOrder(folders.get(i + 1).getName(),folders.get(i + 1).getFolder_order());
                        Collections.swap(folders, i, i + 1);

                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        int order  = folders.get(i).getFolder_order();
                        folders.get(i).setFolder_order(folders.get(i - 1).getFolder_order());
                        folders.get(i - 1).setFolder_order(order);
                        dbManager.updateFolderOrder(folders.get(i).getName(),folders.get(i).getFolder_order());
                        dbManager.updateFolderOrder(folders.get(i - 1).getName(),folders.get(i - 1).getFolder_order());
                        Collections.swap(folders, i, i - 1);
                    }
                }

                adaper.notifyItemMoved(fromPosition, toPosition);
                //返回true表示执行拖动
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }


            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                //当选中Item时候会调用该方法，重写此方法可以实现选中时候的一些动画逻辑
                if(actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    //滑动时改变Item的透明度
                    //final float alpha = 1 - Math.abs(dX) / (float)viewHolder.itemView.getWidth();
                    final float alpha = 0.8f;
                    viewHolder.itemView.setAlpha(alpha);
                    //Log.d("chen", "onSelectedChanged");
                }
                LogUtils.inserOperationLog("排序");
            }
            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                //当动画已经结束的时候调用该方法，重写此方法可以实现恢复Item的初始状态
                final float alpha = 1.0f;
                viewHolder.itemView.setAlpha(alpha);
                //Log.d("chen", "clearView");
                LogUtils.inserOperationLog("排序返回");
            }


        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);


        RxBus.getDefault().toObservable(String.class)
                //在io线程进行订阅，可以执行一些耗时操作
                .subscribeOn(Schedulers.io())
                //在主线程进行观察，可做UI更新操作
                .observeOn(AndroidSchedulers.mainThread())
                //观察的对象
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String msg) {
                        if (msg.equalsIgnoreCase("刷新")) {
                            reflashMain();

                        } else if (msg.equalsIgnoreCase("lose_bottom_bar")) {
                            bottom_container.setVisibility(View.GONE);
                            App.setIsShowPop(false);
                            LogUtils.inserOperationLog("关闭播放控制面板");

                        } else if (msg.equalsIgnoreCase("open_bottom_bar")) {
                            bottom_container.setVisibility(View.VISIBLE);
                            App.setIsShowPop(true);
                        } else if (msg.equalsIgnoreCase("喜爱")) {
                            setUpData();
                        }

                    }
                });

    }

    private void reflashMain() {
        int current_id  = MusicPlayManage.getIntShared(Constant.KEY_ID);
        if (current_id == -1) {
            return;
        }
        MusicInfo musicInfo = dbManager.getMusicFromAllById(current_id);
        for (FolderInfo folder : folders) {
            if (folder.getName().equalsIgnoreCase(musicInfo.getFolderName())) {
                folder.setPlaying(true);
            } else {
                folder.setPlaying(false);
            }
        }
        adaper.notifyDataSetChanged();
    }

    private void startPlay() {
        int current_id  = MusicPlayManage.getIntShared(Constant.KEY_ID);
        if (current_id == -1) {
            MusicPlayManage.playSingleMusic(mContext,App.getDbManager().getMusicFromAllById(1));
        } else {
            MusicPlayManage.playSingleMusic(mContext,App.getDbManager().getMusicFromAllById(current_id));
        }
    }

    private void setUpData() {
        if (dbManager.getMusicCount(Constant.LIST_MYPLAY) == 0) {
            File file = new File(FILE_URL);
            if (file.exists()) {
                queryFolder();
            } else {
                showSearchDialog(null, 0);
            }

        } else {
            folders.clear();
            folders.addAll(dbManager.getAllFolder());
            //Logger.d("开始读取数据库");
            //startPlay();
            reflashMain();

        }

    }






    private void showSearchDialog(final List<FolderInfo> folderList, int text) {
        LayoutInflater inflaterDl = LayoutInflater.from(mContext);
        RelativeLayout layout = (RelativeLayout) inflaterDl.inflate(R.layout.dialog_search_music, null);
        //对话框
        dialogSearch = new AlertDialog.Builder(mContext).create();
        if (!((MainActivity) mContext).isDestroyed() && !((MainActivity) mContext).isFinishing()) {
            dialogSearch.show();
        }
        Window window = dialogSearch.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.width = DentityUtil.dip2px(mContext, 300);//宽高可设置具体大小
        lp.height = DentityUtil.dip2px(mContext, 400);
        dialogSearch.getWindow().setAttributes(lp);
        dialogSearch.getWindow().setContentView(layout);
        dialogSearch.setCanceledOnTouchOutside(true);
        dialogSearch.setCancelable(true);

        tv_title = layout.findViewById(R.id.tv_title);
        tv_file = layout.findViewById(R.id.tv_file);
        tv_music = layout.findViewById(R.id.tv_music);
        tv_searching = layout.findViewById(R.id.tv_searching);
        LinearLayout ll_nothing_contain = layout.findViewById(R.id.ll_nothing_contain);
        LinearLayout ll_search_contain = layout.findViewById(R.id.ll_search_contain);
        if ((folderList == null || folderList.size() == 0) && text == 1) {
            text = 0;
        }
        if (text == 0) {
            ll_nothing_contain.setVisibility(View.VISIBLE);
            ll_search_contain.setVisibility(View.GONE);
            try {
                File file = new File(FILE_URL);
                file.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ll_nothing_contain.setVisibility(View.GONE);
            ll_search_contain.setVisibility(View.VISIBLE);
            int folderSize = folderList.size();
            int musicSize = 0;
            for (FolderInfo info : folderList) {
                musicSize = musicSize + info.getFolder_count();
            }

            tv_file.setText("找到了" + folderSize + "个文件夹.....");
            tv_music.setText("共包含" + musicSize + "个曲目.....");


        }

    }

    @SuppressLint("StaticFieldLeak")
    private void queryFolder() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(final Void... unused) {
                final List<FolderInfo> folderList = MusicUtils.queryFolder(mContext);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showSearchDialog(folderList, 1);
                    }
                });
                int musicSize = 0;

                //获取媒体库中所有的音乐文件
                ContentResolver cr = mContext.getContentResolver();
                StringBuilder select = new StringBuilder(" 1=1 and title != ''");
                // 查询语句：检索出.mp3为后缀名，时长大于0分钟，文件大小大于0MB的媒体文件
                select.append(" and " + MediaStore.Audio.Media.SIZE + " > " + FILTER_SIZE);
                select.append(" and " + MediaStore.Audio.Media.DURATION + " > " + FILTER_DURATION);
                ArrayList<MusicInfo> list = MusicUtils.getMusicListCursor(cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj_music,
                        select.toString(), null,
                        null));
                //Logger.d("list.size()   " + list.toString());


                for (int i = 0; i < folderList.size(); i++) {
                    if (folderList.get(i).getName().equalsIgnoreCase(Constant.FOLDER_LOVE)) {
                        //如果是喜欢文件夹的直接跳过
                        continue;
                    }
                    String folderPath = folderList.get(i).getPath();


                    ArrayList<MusicInfo> finalList = new ArrayList<>();



                    for (int j = 0; j < list.size(); j++) {
                        MusicInfo music = list.get(j);
                        if (music.getPath().substring(0, music.getPath().lastIndexOf(File.separator)).equals(folderPath)) {
                            String[] path = folderPath.split(File.separator);
                            music.setFolderName(path[path.length - 1]);
                            music.setCount("0");
                            Message message = mHandler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putString("3", "正在读取第" + (i + 1) + "/" + folderList.size() + "个文件夹中的第" + (j + 1) + "/" + list.size() + "个文件.....");
                            message.setData(bundle);
                            mHandler.sendMessage(message);
                            finalList.add(music);
                        }
                    }
                    if (finalList.size() != 0) {
                        App.getDbManager().insertMusicListToMusicTable(finalList);
                        folderList.get(i).setFolder_count(App.getDbManager().getAllMusicByFoderName(folderList.get(i).getName()).size());
                    }
                    musicSize = musicSize + App.getDbManager().getAllMusicByFoderName(folderList.get(i).getName()).size();

                }
                folders.clear();
                folders.addAll(folderList);
                //MusicUtils.createAllPlayOrder();
                String queryLog = "读取本地文件" + LogUtils.mSeparator + folders.size() + LogUtils.mSeparator + musicSize;
                LogUtils.inserOperationLog(queryLog);

                return null;
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                adaper.notifyDataSetChanged();
                if (folders.size() != 0) {
                    mHandler.sendEmptyMessage(2);
                }
                ToastUtils.showToast("搜索完成");
            }
        }.execute();
    }


    @Override
    protected int attachLayoutRes() {
        return R.layout.fragment_main;
    }
    @Override
    protected void updateViews() {
        MusicPlayManage.setShared(Constant.KEY_ACTIVITY_STATUS, Constant.POSITION_FOLDER_LIST);
    }

    @Override
    public void onResume() {
        super.onResume();
        MusicPlayManage.setShared(Constant.KEY_ACTIVITY_STATUS, Constant.POSITION_FOLDER_LIST);
    }
}
