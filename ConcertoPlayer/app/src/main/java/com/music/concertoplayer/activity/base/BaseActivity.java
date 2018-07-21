package com.music.concertoplayer.activity.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.music.concertoplayer.R;
import com.music.concertoplayer.fragment.PlayBarFragment;
import com.music.concertoplayer.utils.Util;
import com.music.concertoplayer.view.dialog.MyDialogHandler;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by chen on 2018/3/28.
 */

public  class BaseActivity extends AppCompatActivity {
    protected PlayBarFragment fragment; //底部播放控制栏
    private String TAG = "BaseActivity";
    protected MyDialogHandler uiFlusHandler;
    protected final int SHOW_LOADING_DIALOG = 0x0102;
    protected final int DISMISS_LOADING_DIALOG = 0x0103;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.setStatusBarTranslucent(getWindow());
        setContentView(getContentSrc());
        ButterKnife.bind(this);
        initViews();
        initDate();
    }

    protected   void initDate() {

    }
    protected   void initViews() {

    }

    protected  int getContentSrc() {
        return 0;
    }

    /**
     * @param show 显示或关闭底部播放控制栏
     */
    protected void showQuickControl(boolean show) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (show) {
            if (fragment == null) {
                fragment = new PlayBarFragment();
                ft.add(R.id.bottom_container, fragment).commitAllowingStateLoss();
            } else {
                ft.show(fragment).commitAllowingStateLoss();
            }
        } else {
            if (fragment != null)
                ft.hide(fragment).commitAllowingStateLoss();
        }
    }

}
