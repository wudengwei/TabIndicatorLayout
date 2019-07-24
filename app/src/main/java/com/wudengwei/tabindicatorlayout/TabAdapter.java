package com.wudengwei.tabindicatorlayout;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C)
 * FileName: TabAdapter
 * Author: wudengwei
 * Date: 2019/7/10 20:51
 * Description: ${DESCRIPTION}
 */
public abstract class TabAdapter<T> extends TabIndicatorLayout.Adapter {

    protected static final String TAG = TabAdapter.class.getSimpleName();
    protected Context mContext;
    protected int mLayoutResId;
    protected List<T> mData;

    public TabAdapter(@LayoutRes int layoutResId, @Nullable List<T> data) {
        this.mData = data == null ? new ArrayList<T>() : data;
        if (layoutResId != 0) {
            this.mLayoutResId = layoutResId;
        }
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull ViewGroup viewGroup, int position) {
        this.mContext = viewGroup.getContext();
        View v = LayoutInflater.from(mContext).inflate(mLayoutResId, viewGroup,false);
        return v;
    }

    @Override
    public void onBindView(@NonNull View view, int position, int currentTabPosition) {
        convert(view, position, currentTabPosition);
    }

    protected abstract void convert(@NonNull View view, int poistion, int currentTabPosition);


}
