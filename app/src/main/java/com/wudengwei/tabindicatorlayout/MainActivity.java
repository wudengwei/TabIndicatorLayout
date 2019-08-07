package com.wudengwei.tabindicatorlayout;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.wudengwei.tabindicatorlayout.adapter.EditViewPagerAdapter;
import com.wudengwei.tabindicatorlayout.bean.Tab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ViewPager mViewPager;

    private TabIndicatorLayout tabIndicatorLayout;
    private List<Tab> tabList;
    private List<Tab> viewPagerList;
    private TabAdapter tabAdapter;

    private ArrayList<String> mTitles = new ArrayList<>();
//    private List<String> mTitles = Arrays.asList("新闻", "游戏宽度", "动漫","游戏宽度动漫");
    private List<Fragment> mFragments = new ArrayList<Fragment>();

    EditViewPagerAdapter editViewPagerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initClick();

        mViewPager = (ViewPager) findViewById(R.id.vp_main_content);
        tabIndicatorLayout = findViewById(R.id.til_main_tab);

        mTitles.addAll(Arrays.asList("新闻", "音乐节", "游戏宽度","动漫", "汽车", "打开市场","美食","情感","歌曲"));
        //创建Fragment
        for (String title : mTitles) {
            SimpleFragmet simpleFragmet = SimpleFragmet.newInstance(title);
            mFragments.add(simpleFragmet);
        }

        tabList = new ArrayList<>();
        viewPagerList = new ArrayList<>();
        for (int i=0;i<mTitles.size();i++) {
            tabList.add(new Tab(mTitles.get(i),"1"));
            viewPagerList.add(new Tab(mTitles.get(i),"1"));
        }
        tabIndicatorLayout.setAdapter(tabAdapter = new TabAdapter<Tab>(R.layout.tab_item,tabList) {
            @Override
            protected void convert(@NonNull View view, int poistion, int currentTabPosition) {
                TextView title = view.findViewById(R.id.tv_title);
                title.setText(tabList.get(poistion).getTitle());
                TextView num = view.findViewById(R.id.tv_num);
                num.setText(tabList.get(poistion).getNum());
                if (currentTabPosition == poistion) {//选择tab
                    title.setTextColor(Color.RED);
                    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                } else {
                    title.setTextColor(Color.WHITE);
                    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                }
            }

            @Override
            public void onEnterOrLeave(@NonNull View formView, @NonNull View toView, int formPosition, int toPosition, float formPercent, float toPercent) {
                super.onEnterOrLeave(formView, toView, formPosition, toPosition, formPercent, toPercent);
                TextView formTitle = formView.findViewById(R.id.tv_title);
                TextView toTitle = toView.findViewById(R.id.tv_title);
//                formTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20-6*toPercent);
//                toTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14+6*toPercent);
//                formTitle.setScaleX((float) (1.5-0.5*toPercent));
//                formTitle.setScaleX((float) (1.5-0.5*toPercent));
//                toTitle.setScaleX((float) (1+0.5*toPercent));
//                toTitle.setScaleX((float) (1+0.5*toPercent));
            }
        });
        tabAdapter.setOnTabClickListener(new TabIndicatorLayout.Adapter.OnTabClickListener() {
            @Override
            public void onClick(View v, int position) {
//                tabList.get(position).setTitle("打开市场打开市场");
//                tabAdapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this,""+position,Toast.LENGTH_SHORT).show();
            }
        });
        tabIndicatorLayout.setViewPager(mViewPager);

        //设置适配器
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.setAdapter(editViewPagerAdapter = new EditViewPagerAdapter(getSupportFragmentManager(),viewPagerList));
//        editViewPagerAdapter.setData(viewPagerList);
//        editViewPagerAdapter.notifyDataSetChanged();

    }

    private void initClick() {
        findViewById(R.id.tv1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,TabWidthRatioActivity.class));
            }
        });
        findViewById(R.id.tv_change).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collections.swap(tabList,1,2);
                tabAdapter.notifyDataSetChanged();
                Collections.swap(viewPagerList,1,2);
                editViewPagerAdapter.notifyDataSetChanged();
            }
        });
        findViewById(R.id.tv_remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tabList.remove(2);
                tabAdapter.notifyItemRemoved(2);
                viewPagerList.remove(2);
                editViewPagerAdapter.notifyDataSetChanged();
            }
        });
        findViewById(R.id.tv_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tabList.add(2,new Tab("测试","9"));
                Log.e("mTitles",""+tabAdapter.getCount());
                tabAdapter.notifyItemInserted(2);
                viewPagerList.add(2,new Tab("测试","9"));
                editViewPagerAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
