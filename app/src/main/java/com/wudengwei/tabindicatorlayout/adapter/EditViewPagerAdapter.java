package com.wudengwei.tabindicatorlayout.adapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.wudengwei.tabindicatorlayout.SimpleFragmet;
import com.wudengwei.tabindicatorlayout.bean.Tab;
import com.wudengwei.tabindicatorlayout.fragment.HomeFragment;

import java.util.List;

/**
 * Copyright (C)
 * FileName: EditViewPagerAdapter
 * Author: wudengwei
 * Date: 2019/8/4 22:04
 * Description: ${DESCRIPTION}
 */
//public class EditViewPagerAdapter extends FragmentDynamicPagerAdapter<Tab> {
public class EditViewPagerAdapter extends FragmentStatePagerAdapter {
    List<Tab> mDataList;

    public EditViewPagerAdapter(FragmentManager manager, List<Tab> tabList) {
        super(manager);
        mDataList = tabList;
    }

//    public EditViewPagerAdapter(FragmentManager manager, List<Tab> tabList) {
//        super(manager, tabList);
//    }

    @Override
    public int getCount() {
        int count = mDataList == null ? 0 : mDataList.size();
        return count;
    }

    @Override
    public Fragment getItem(int position) {
        Tab tab = mDataList.get(position);
        Fragment fragment = new HomeFragment();
        Bundle b = new Bundle();
        b.putString("title", tab.getTitle());
        fragment.setArguments(b);
        return fragment;
    }
}