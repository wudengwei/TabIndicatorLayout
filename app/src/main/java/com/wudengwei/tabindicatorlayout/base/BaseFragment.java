package com.wudengwei.tabindicatorlayout.base;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

/**
 * Created by wudengwei
 * on 2018/12/17
 */
public class BaseFragment extends Fragment {

    /*onCreateView返回的view*/
    protected View rootView;
    /*是否第一次可见*/
    private boolean isFirstVisible;
    /*是否可见*/
    private boolean isVisible;
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (rootView == null)
            return;
        /*可见回调方法*/
        if (isVisible != isVisibleToUser)
            onVisibleChange(isVisibleToUser,isFirstVisible);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFirstVisible = true;
        isVisible = false;
        rootView = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        rootView = rootView==null?view:rootView;
        super.onViewCreated(view, savedInstanceState);
        if (getUserVisibleHint()) {
            /*可见,是否第一次*/
            onVisibleChange(true,isFirstVisible);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isFirstVisible = true;
        isVisible = false;
        rootView = null;
    }

    /*是否可见回调方法*/
    protected void onVisibleChange(boolean isVisible,boolean firstVisible) {
        this.isVisible = isVisible;
        if (firstVisible)
            isFirstVisible = false;
    }
}
