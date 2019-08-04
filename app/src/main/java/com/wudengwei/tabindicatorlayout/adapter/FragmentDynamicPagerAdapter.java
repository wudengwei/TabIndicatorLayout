package com.wudengwei.tabindicatorlayout.adapter;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.wudengwei.tabindicatorlayout.bean.Tab;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C)
 * FileName: FragmentDynamicPagerAdapter
 * Author: wudengwei
 * Date: 2019/8/2 20:04
 * Description: ${DESCRIPTION}
 */
public abstract class FragmentDynamicPagerAdapter<T> extends PagerAdapter {
    private static final String TAG = "FragmentStatePagerAdapt";
    private static final boolean DEBUG = true;

    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;

    private ArrayList<Fragment.SavedState> mSavedState = new ArrayList<Fragment.SavedState>();
    //保存所有fragment和对应位置
    private ArrayList<ItemInfo> mItemInfoList = new ArrayList();
    //当前选中fragment
    private Fragment mCurrentPrimaryItem = null;
    private boolean mNeedProcessCache = false;

    protected List<T> mDataList;

    public FragmentDynamicPagerAdapter(FragmentManager fm, List<T> dataList) {
        mFragmentManager = fm;
        mDataList = dataList;
    }

    /**
     * position位置的fragment=null时，创建并返回fragment
     * @param position
     * @return
     */
    public abstract Fragment getItem(int position);

    public T getItemData(int position) {
        if (position >= mDataList.size()) {
            return null;
        }
        return mDataList == null ? null : mDataList.get(position);
    }

    public boolean dataEquals(Object oldData, Object newData) {
        if (oldData == null || newData == null) {
            return false;
        }
        return oldData == newData;
    }

    /**
     * @param data
     * @return <0  表示该数据已经不存在了，就是说被删除了....
     */
    public int getDataPosition(Object data) {
        int index = -1;
        for (int i = 0;i < mDataList.size();i++){
            T newData = mDataList.get(i);
            if (dataEquals(newData,data)){
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * container通过该方法获得对象
     *
     * @param container
     * @param position
     * @return
     */
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        //先从mItemInfoList中寻找position位置的itemInfo
        //如果itemInfo.position == position，直接返回itemInfo
        //如果itemInfo.position ！= position，则表示itemInfo位置变化,改变位置
        if (mItemInfoList.size() > 0 && mItemInfoList.size() > position) {
            ItemInfo itemInfo = mItemInfoList.get(position);
            if (itemInfo != null) {
                if (itemInfo.position == position) {
                    return itemInfo;
                } else {
                    changedCache();
                }
            }
        }
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        //position的fragment不存在，就创建
        Fragment fragment = getItem(position);
        if (DEBUG) Log.e(TAG, "Adding item #" + position + ": f=" + fragment);
        if (mSavedState.size() > position) {
            Fragment.SavedState fss = mSavedState.get(position);
            if (fss != null) {
                fragment.setInitialSavedState(fss);
            }
        }
        //如果position大于mItemInfoList.size()，以null扩充
        while (mItemInfoList.size() <= position) {
            mItemInfoList.add(null);
        }
        //设置fragment不可见
        fragment.setMenuVisibility(false);
        fragment.setUserVisibleHint(false);
        //创建储存fragment和位置的对象
        ItemInfo newItemInfo = new ItemInfo(fragment, getItemData(position), position);
        mItemInfoList.set(position, newItemInfo);
        mCurTransaction.add(container.getId(), fragment);

        return newItemInfo;
    }

    /**
     * 删除position位置页面
     * mItemInfoList设置position为null
     * mCurTransaction删除position位置的fragment
     *
     * @param container
     * @param position
     * @param object {@link ViewPager.ItemInfo}
     */
    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        ItemInfo itemInfo = (ItemInfo) object;
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        if (DEBUG) Log.e(TAG, "Removing item #" + position + ": f=" + object
                + " v=" + itemInfo.fragment);
        while (mSavedState.size() <= position) {
            mSavedState.add(null);
        }
        mSavedState.set(position, itemInfo.fragment.isAdded()
                ? mFragmentManager.saveFragmentInstanceState(itemInfo.fragment) : null);
        mItemInfoList.set(position, null);

        mCurTransaction.remove(itemInfo.fragment);
    }

    /**
     * 设置选中fragment
     * @param container
     * @param position
     * @param object
     */
    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        ItemInfo itemInfo = (ItemInfo) object;
        Fragment fragment = itemInfo.fragment;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    /**
     * ViewPager获得object所在位置，用于是否更新
     * -1表示不更新
     * -2表示删除
     * @param object
     * @return
     */
    @Override
    public int getItemPosition(@NonNull Object object) {
        mNeedProcessCache = true;
        ItemInfo itemInfo = (ItemInfo) object;
        int oldPosition = mItemInfoList.indexOf(itemInfo);
        if (oldPosition >= 0) {
            Object oldData = itemInfo.data;
            Object newData = getItemData(oldPosition);
            if (dataEquals(oldData, newData)) {
                return POSITION_UNCHANGED;
            } else {
                ItemInfo oldItemInfo = mItemInfoList.get(oldPosition);
                int oldDataNewPosition = getDataPosition(oldData);
                if (oldDataNewPosition < 0) {
                    oldDataNewPosition = POSITION_NONE;
                }
                //把新的位置赋值到缓存的itemInfo中，以便调整时使用
                if (oldItemInfo != null) {
                    oldItemInfo.position = oldDataNewPosition;
                }
                changedCache();
                return oldDataNewPosition;
            }
        }
        return POSITION_UNCHANGED;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        //通知ViewPager更新完成后对缓存的ItemInfo List进行调整
        changedCache();
    }

    /**
     * 修改mItemInfoList
     * 保证itemInfo.position位置对于的是position位置的itemInfo
     */
    private void changedCache() {
        if (!mNeedProcessCache)
            return;
        mNeedProcessCache = false;
        ArrayList<ItemInfo> tempList = new ArrayList<>();
        //先存入空数据
        for (int i = 0; i < mItemInfoList.size(); i++) {
            tempList.add(null);
        }
        //保证itemInfo.position位置对于的是position位置的itemInfo
        for (ItemInfo itemInfo : mItemInfoList) {
            if (itemInfo != null) {
                if (itemInfo.position >= 0) {
                    //如果itemInfo位置超过tempItemInfoList大小，自动填充
                    while (tempList.size() <= itemInfo.position) {
                        tempList.add(null);
                    }
                    tempList.set(itemInfo.position,itemInfo);
                } else {//删除itemInfo
                    Fragment fragment = itemInfo.fragment;
                    if (mCurTransaction == null) {
                        mCurTransaction = mFragmentManager.beginTransaction();
                    }
                    mCurTransaction.remove(fragment);
                }
            }
        }
        mItemInfoList.clear();
        mItemInfoList.addAll(tempList);
    }

    /**
     * 储存fragment和位置的对象
     */
    static class ItemInfo {
        Fragment fragment;
        Object data;
        int position;

        public ItemInfo(Fragment fragment, int position) {
            this.fragment = fragment;
            this.data = null;
            this.position = position;
        }

        public ItemInfo(Fragment fragment, Object data, int position) {
            this.fragment = fragment;
            this.data = data;
            this.position = position;
        }
    }

    @Override
    public void startUpdate(ViewGroup container) {
        if (container.getId() == View.NO_ID) {
            throw new IllegalStateException("ViewPager with adapter " + this
                    + " requires a view id");
        }
    }

    @Override
    public void finishUpdate(@NonNull ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitNowAllowingStateLoss();
            mCurTransaction = null;
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        Fragment fragment = ((ItemInfo) object).fragment;
        return fragment.getView() == view;
    }

    @Override
    public Parcelable saveState() {
        Bundle state = null;
        if (mSavedState.size() > 0) {
            state = new Bundle();
            Fragment.SavedState[] fss = new Fragment.SavedState[mSavedState.size()];
            mSavedState.toArray(fss);
            state.putParcelableArray("states", fss);
        }
        for (int i = 0; i < mItemInfoList.size(); i++) {

            ItemInfo info = mItemInfoList.get(i);
            if (info == null) {
                continue;
            } else {
                Fragment f = mItemInfoList.get(i).fragment;
                if (f != null && f.isAdded()) {
                    if (state == null) {
                        state = new Bundle();
                    }
                    String key = "f" + i;
                    mFragmentManager.putFragment(state, key, f);
                }
            }
        }

        return state;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {

        if (state != null) {
            Bundle bundle = (Bundle) state;
            bundle.setClassLoader(loader);
            Parcelable[] fss = bundle.getParcelableArray("states");
            mSavedState.clear();
            mItemInfoList.clear();
            if (fss != null) {
                for (int i = 0; i < fss.length; i++) {
                    mSavedState.add((Fragment.SavedState) fss[i]);
                }
            }
            Iterable<String> keys = bundle.keySet();
            for (String key : keys) {
                if (key.startsWith("f")) {
                    int index = Integer.parseInt(key.substring(1));
                    Fragment f = mFragmentManager.getFragment(bundle, key);

                    if (f != null) {
                        while (mItemInfoList.size() <= index) {
                            mItemInfoList.add(null);
                        }
                        f.setMenuVisibility(false);
                        ItemInfo iiNew = new ItemInfo(f, getItemData(index), index);
                        mItemInfoList.set(index, iiNew);
                    } else {
                        Log.w(TAG, "Bad fragment at key " + key);
                    }
                }
            }
        }
    }
}
