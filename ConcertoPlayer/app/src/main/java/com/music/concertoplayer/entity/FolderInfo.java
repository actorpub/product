package com.music.concertoplayer.entity;

import android.graphics.Bitmap;

/**
 * Created by chen on 2018/3/28.
 */

public class FolderInfo {
    private Bitmap bitmap;
    private String name;
    private String path;
    private String folder_sort;
    private String bitmap_path;
    private int folder_count;
    private int pre_play_id;
    private int can_recycle_able;
    private int pre_play_progress;
    private int folder_order;
    private int folder_select;
    private boolean isPlaying;

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public int getFolder_select() {
        return folder_select;
    }

    public void setFolder_select(int folder_select) {
        this.folder_select = folder_select;
    }

    public int getFolder_order() {
        return folder_order;
    }

    public void setFolder_order(int folder_order) {
        this.folder_order = folder_order;
    }

    public String getBitmap_path() {
        return bitmap_path;
    }

    public void setBitmap_path(String bitmap_path) {
        this.bitmap_path = bitmap_path;
    }

    public int getPre_play_progress() {
        return pre_play_progress;
    }

    public void setPre_play_progress(int pre_play_progress) {
        this.pre_play_progress = pre_play_progress;
    }

    public int getCan_recycle_able() {
        return can_recycle_able;
    }

    public void setCan_recycle_able(int can_recycle_able) {
        this.can_recycle_able = can_recycle_able;
    }

    public int getPre_play_id() {
        return pre_play_id;
    }

    public void setPre_play_id(int pre_play_id) {
        this.pre_play_id = pre_play_id;
    }

    public int getFolder_count() {
        return folder_count;
    }

    public void setFolder_count(int folder_count) {
        this.folder_count = folder_count;
    }

    public String getFolder_sort() {
        return folder_sort;
    }

    public void setFolder_sort(String folder_sort) {
        this.folder_sort = folder_sort;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
