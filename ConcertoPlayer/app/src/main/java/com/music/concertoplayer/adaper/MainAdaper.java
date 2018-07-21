package com.music.concertoplayer.adaper;



import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.R;
import com.music.concertoplayer.entity.FolderInfo;

import java.util.List;

/**
 * Created by chen on 2018/3/28.
 */

public class MainAdaper extends BaseQuickAdapter<FolderInfo, BaseViewHolder> {
    private Context context;

    public MainAdaper(Context context,int layoutResId, @Nullable List<FolderInfo> data) {
        super(layoutResId, data);
        this.context = context;
    }

    @Override
    protected void convert(BaseViewHolder holder, final FolderInfo folderEntity) {
        if (folderEntity.getBitmap() != null) {
            holder.setImageBitmap(R.id.iv_img, folderEntity.getBitmap());
        }
        if (folderEntity.getName().equalsIgnoreCase(Constant.FOLDER_LOVE)) {
            holder.setText(R.id.tv_name, "喜欢 " + folderEntity.getFolder_count());

        } else {
            holder.setText(R.id.tv_name,folderEntity.getName());
        }
        holder.addOnClickListener(R.id.iv_img)
                .addOnClickListener(R.id.tv_name);
        if ( folderEntity.isPlaying()) {
            holder.setVisible(R.id.iv_select, true);
            holder.setTextColor(R.id.tv_name, mContext.getColor(R.color.colorPrimary));
        } else {
            holder.setVisible(R.id.iv_select, false);
            holder.setTextColor(R.id.tv_name, Color.BLACK);
            //holder.setBackgroundColor(R.id.rl_select, Color.WHITE);
            //holder.setTextColor(R.id.tv_name, Color.BLACK);
            //holder.setBackgroundColor(R.id.rl_select, Color.TRANSPARENT);

        }

    }
}
