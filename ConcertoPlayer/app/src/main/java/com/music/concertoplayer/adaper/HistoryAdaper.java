package com.music.concertoplayer.adaper;



import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.entity.FolderInfo;
import com.music.concertoplayer.entity.MusicInfo;
import com.music.concertoplayer.utils.TimeUtil;
import com.orhanobut.logger.Logger;

import java.util.List;

/**
 * Created by chen on 2018/3/28.
 */

public class HistoryAdaper extends BaseQuickAdapter<MusicInfo, BaseViewHolder> {
    private Context context;

    public HistoryAdaper(Context context, int layoutResId, @Nullable List<MusicInfo> data) {
        super(layoutResId, data);
        this.context = context;
    }

    @Override
    protected void convert(BaseViewHolder holder, final MusicInfo musicInfo) {

        holder.setText(R.id.tv_music_name, musicInfo.getName())
                .setText(R.id.tv_time, TimeUtil.parseHistoryTime(musicInfo.getTime()))
                .setText(R.id.tv_play_count, musicInfo.getCount());
        if (musicInfo.getFolderName().equalsIgnoreCase(Constant.FOLDER_LOVE)) {
            holder.setText(R.id.tv_folder_name, "喜欢");
        } else {
            holder.setText(R.id.tv_folder_name, musicInfo.getFolderName());
        }

        holder.addOnClickListener(R.id.ll_history_contain);

        if (musicInfo.getPlayColor()== 1) {
            holder.setVisible(R.id.iv_current_show, true);
        } else {
            holder.setVisible(R.id.iv_current_show, false);
        }
        ImageView iv_love = holder.itemView.findViewById(R.id.iv_love);
        if (musicInfo.getLove() == 1) {
            iv_love.setImageLevel(1);
        } else {
            iv_love.setImageLevel(0);

        }

    }
}
