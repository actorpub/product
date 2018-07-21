package com.music.concertoplayer.fragment;


import android.content.Intent;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.Toast;


import com.chad.library.adapter.base.BaseQuickAdapter;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.adaper.HistoryAdaper;
import com.music.concertoplayer.database.DBManager;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.fragment.base.BaseFragment;
import com.music.concertoplayer.receiver.PlayerManagerReceiver;
import com.music.concertoplayer.service.MusicPlayerService;
import com.music.concertoplayer.manage.MusicPlayManage;
import com.music.concertoplayer.utils.RxBus;
import com.music.concertoplayer.view.PlayingPopWindow;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.music.concertoplayer.Constant.KEY_ACTIVITY_STATUS;
import static com.music.concertoplayer.Constant.KEY_MODE;
import static com.music.concertoplayer.receiver.PlayerManagerReceiver.status;


/**
 * Created by chen on 2018/4/25.
 */

public class HistoryFragment extends BaseFragment {
    @BindView(R.id.rv_history)
    RecyclerView recyclerView;
    private DBManager dbManager;
    private List<MusicInfo> musicList = new ArrayList<>();
    private HistoryAdaper adaper;
    private PlayingPopWindow playingPopWindow;

    @Override
    protected void updateViews() {
        RxBus.getDefault().post("刷新");
        MusicPlayManage.setShared(KEY_ACTIVITY_STATUS, Constant.POSITION_HISTORY);
    }

    @Override
    protected void initData() {
        dbManager = DBManager.getInstance(mContext);
        showQuickControl(false);
    }

    private void setupData() {
        musicList.clear();
        List<MusicInfo> hMusic = dbManager.getHistory();
        int currentPlayId = MusicPlayManage.getIntShared(Constant.KEY_ID);
        for (MusicInfo musicInfo : hMusic) {
            if (musicInfo.getId() == currentPlayId) {
                musicInfo.setPlayColor(1);
            } else {
                musicInfo.setPlayColor(0);

            }
        }
        musicList.addAll(hMusic);
        adaper.notifyDataSetChanged();
    }

    @Override
    protected void initViews() {
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        adaper = new HistoryAdaper(mContext, R.layout.adapter_history_item, musicList);
        adaper.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                switch (view.getId()) {
                    case R.id.ll_history_contain:
                        MusicInfo musicInfo = musicList.get(position);
                        //如果是点击第一个就特殊处理
                        int musicId = musicInfo.getId();
                        if (musicId == -1 || musicId == 0) {
                            Intent intent = new Intent(Constant.MP_FILTER);
                            intent.putExtra(Constant.COMMAND, Constant.COMMAND_STOP);
                            getActivity().sendBroadcast(intent);
                            Toast.makeText(getActivity(), "歌曲不存在", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        MusicPlayManage.playSingleMusic(mContext, musicInfo);
                        setupData();
                        showPopFormBottom();
                        break;
                }

            }
        });

        recyclerView.setAdapter(adaper);//设置Item增加、移除动画
        recyclerView.setItemAnimator(new DefaultItemAnimator());
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
                            setupData();
                        }

                    }
                });
        if (status == Constant.STATUS_PLAY) {
            showPopFormBottom();
        }
    }

    public void showPopFormBottom() {
        if (playingPopWindow == null) {
            playingPopWindow = new PlayingPopWindow(getActivity());
//      设置Popupwindow显示位置（从底部弹出）
            playingPopWindow.showAtLocation(mRootView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
            WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
            //当弹出Popupwindow时，背景变半透明
            params.alpha = 0.7f;
            getActivity().getWindow().setAttributes(params);

            //设置Popupwindow关闭监听，当Popupwindow关闭，背景恢复1f
            playingPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
                    params.alpha = 1f;
                    getActivity().getWindow().setAttributes(params);
                }
            });
        } else {
            if (!playingPopWindow.isShowing()) {
                playingPopWindow = new PlayingPopWindow(getActivity());
//      设置Popupwindow显示位置（从底部弹出）
                playingPopWindow.showAtLocation(mRootView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
                //当弹出Popupwindow时，背景变半透明
                params.alpha = 0.7f;
                getActivity().getWindow().setAttributes(params);

                //设置Popupwindow关闭监听，当Popupwindow关闭，背景恢复1f
                playingPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
                        params.alpha = 1f;
                        getActivity().getWindow().setAttributes(params);
                    }
                });
            }
        }

    }


    @Override
    protected int attachLayoutRes() {
        return R.layout.fragment_history;
    }
}
