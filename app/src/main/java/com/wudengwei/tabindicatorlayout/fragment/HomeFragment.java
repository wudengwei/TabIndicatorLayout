package com.wudengwei.tabindicatorlayout.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wudengwei.tabindicatorlayout.R;
import com.wudengwei.tabindicatorlayout.base.BaseFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by wudengwei
 * on 2018/12/18
 */
public class HomeFragment extends BaseFragment {

    @BindView(R.id.tv)
    TextView tv;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    Unbinder unbinder;

    String title;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title = getArguments().getString("title");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (rootView != null)
            return rootView;
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        tvTitle.setText(title);
        return rootView;
    }

    private int visibleCount = 1;
    private int unVisibleCount = 1;

    @Override
    protected void onVisibleChange(boolean isVisible, boolean firstVisible) {
        super.onVisibleChange(isVisible, firstVisible);
        if (isVisible) {
            if (firstVisible) {
                Log.e("CARD " + title, "第一次可见");
                tv.setText("第一次可见");
            } else {
                Log.e("CARD " + title, isVisible ? "可见" : "不可见");
                tv.setText("第" + visibleCount + "次可见");
            }
            visibleCount++;
        } else {
            Log.e("CARD " + title, isVisible ? "可见" : "不可见");
            tv.setText("第" + unVisibleCount + "不可见");
            unVisibleCount++;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getUserVisibleHint()) {
            Log.e("onResume",title+"可见");
        } else {
            Log.e("onResume",title+"不可见");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getUserVisibleHint()) {
            Log.e("onPause",title+"可见");
        } else {
            Log.e("onPause",title+"不可见");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
