package com.music.concertoplayer.adaper;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.database.DBManager;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.net.LogUtils;
import com.music.concertoplayer.manage.MusicPlayManage;
import com.music.concertoplayer.utils.RxBus;
import com.music.concertoplayer.utils.TimeUtil;

import java.util.List;

import static com.music.concertoplayer.Constant.KEY_ACTIVITY_STATUS;
import static com.music.concertoplayer.Constant.KEY_MODE;

/**
 * Created by chen on 2018/3/29.
 */

public class MusicAdapter extends  RecyclerView.Adapter<MusicAdapter.ViewHolder>  {
    private Context context;
    private List<MusicInfo> musicInfoList;
    private DBManager dbManager;
    public MusicAdapter(Context context, List<MusicInfo> folders) {
        this.context = context;
        this.musicInfoList = folders;
        dbManager = App.getDbManager();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MusicAdapter.ViewHolder holder = new MusicAdapter.ViewHolder(LayoutInflater.from(
                context).inflate(R.layout.adapter_music_item, parent,
                false));
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final MusicInfo info = musicInfoList.get(position);
        holder.tv_music_name.setText(info.getName());
        holder.tv_music_time.setText(TimeUtil.formatTime(Long.parseLong(info.getDuration())));
        holder.tv_music_frequency.setText(info.getCount());
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MusicPlayManage.setShared(Constant.FOLDER_NAME, info.getFolderName());
                //dbManager.updateSingleFolderRecycleAble( info.getFolderName(),0);
                MusicPlayManage.playSingleMusic(context,info);
                RxBus.getDefault().post("open_bottom_bar");
                LogUtils.inserOperationLog("点击曲目播放");
                MusicPlayManage.setShared(KEY_ACTIVITY_STATUS, Constant.POSITION_FILE_LIST);

            }
        });
        if (info.getPlayColor() == 1) {
            holder.iv_history_show.setBackgroundColor(context.getResources().getColor(R.color.colorPlaying));
            holder.iv_history_show.setVisibility(View.VISIBLE);
        } else if (info.getPlayColor() == 2) {
            holder.iv_history_show.setBackgroundColor(context.getResources().getColor(R.color.colorHistory));
            holder.iv_history_show.setVisibility(View.VISIBLE);
        } else {
            holder.iv_history_show.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return musicInfoList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iv_history_show;
        TextView tv_music_name;
        TextView tv_music_time;
        TextView tv_music_frequency;
        View view;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            iv_history_show = view.findViewById(R.id.iv_history_show);
            tv_music_name = view.findViewById(R.id.tv_music_name);
            tv_music_time = view.findViewById(R.id.tv_music_time);
            tv_music_frequency = view.findViewById(R.id.tv_music_frequency);
        }
    }
}
