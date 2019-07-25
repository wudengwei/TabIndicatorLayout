package com.wudengwei.tabindicatorlayout;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.wudengwei.tabindicatorlayout.bean.Tab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ViewPager mViewPager;

    private TabIndicatorLayout tabIndicatorLayout;
    private List<Tab> tabList;
    private TabAdapter tabAdapter;

//    private List<String> mTitles = Arrays.asList("新闻", "音乐节", "游戏宽度","动漫", "汽车", "打开市场","美食","情感","歌曲");
    private List<String> mTitles = Arrays.asList("新闻", "游戏宽度", "动漫","游戏宽度动漫");
    private List<Fragment> mFragments = new ArrayList<Fragment>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("onCreate","onCreate start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewPager = (ViewPager) findViewById(R.id.vp_main_content);
        tabIndicatorLayout = findViewById(R.id.til_main_tab);
        //创建Fragment
        for (String title : mTitles) {
            SimpleFragmet simpleFragmet = SimpleFragmet.newInstance(title);
            mFragments.add(simpleFragmet);
        }

        //设置适配器
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }

            @Override
            public int getCount() {
                return mFragments.size();
            }
        });
        Log.e("onCreate","onCreate end");

        tabList = new ArrayList<>();
        for (int i=0;i<mTitles.size();i++) {
            tabList.add(new Tab(mTitles.get(i),"1"));
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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
