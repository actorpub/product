package com.music.concertoplayer.fragment.base;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.music.concertoplayer.R;
import com.music.concertoplayer.activity.MainActivity;
import com.music.concertoplayer.fragment.PlayBarFragment;

import butterknife.ButterKnife;

/**
 * Created by chen on 2017/9/7.
 * Fragment基类
 */

public abstract class BaseFragment extends Fragment {


    protected PlayBarFragment fragment; //底部播放控制栏

    protected Context mContext;

    protected View mRootView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = inflater.inflate(attachLayoutRes(), null);
            ButterKnife.bind(this, mRootView);
            initViews();
            initData();
        } else {
            ViewGroup parent = (ViewGroup) mRootView.getParent();
            if (parent != null) {
                parent.removeView(mRootView);
            }
        }
        return mRootView;
    }

    /*@Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getUserVisibleHint() && mRootView != null ) {
            updateViews();
        }
    }*/

    protected abstract void updateViews();

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser && isVisible() && mRootView != null) {
            updateViews();
        } else {
            super.setUserVisibleHint(isVisibleToUser);
        }
    }

/*

    protected abstract void updateViews();*/





    protected abstract void initData();


    protected abstract void initViews();


    protected abstract int attachLayoutRes();

    /**
     * @param show 显示或关闭底部播放控制栏
     */
    protected void showQuickControl(boolean show) {
        FragmentTransaction ft = ((MainActivity)mContext).getSupportFragmentManager().beginTransaction();
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
