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
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        tvTitle.setText(title);
        return rootView;
    }

    private int visibleCount = 1;
    private int unVisibleCount = 1;

    @Override
    protected void onLazyLoad() {
        super.onLazyLoad();
        Log.e("onLazyLoad",""+title+"懒加载");
        tvTitle.setText(title);
    }

    @Override
    protected void onResumeVisible() {
        super.onResumeVisible();
        tv.setText("第"+visibleCount+"次可见");
        visibleCount++;
    }

    @Override
    protected void onPauseInVisible() {
        super.onPauseInVisible();
        tv.setText("第"+unVisibleCount+"次不可见");
        unVisibleCount++;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e("onDestroyView",""+title+"摧毁视图");
        unbinder.unbind();
    }
}
