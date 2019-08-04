package com.wudengwei.tabindicatorlayout.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.wudengwei.tabindicatorlayout.SimpleFragmet;
import com.wudengwei.tabindicatorlayout.bean.Tab;

import java.util.List;

/**
 * Copyright (C)
 * FileName: EditViewPagerAdapter
 * Author: wudengwei
 * Date: 2019/8/4 22:04
 * Description: ${DESCRIPTION}
 */
public class EditViewPagerAdapter extends FragmentDynamicPagerAdapter<Tab> {

    public EditViewPagerAdapter(FragmentManager manager, List<Tab> tabList) {
        super(manager,tabList);
    }

    @Override
    public int getCount() {
        int count = mDataList == null ? 0 : mDataList.size();
        return count;
    }

    @Override
    public Fragment getItem(int position) {
        Tab subject = mDataList.get(position);
        Fragment fragment = SimpleFragmet.newInstance(subject.getTitle());
        return fragment;
    }
}