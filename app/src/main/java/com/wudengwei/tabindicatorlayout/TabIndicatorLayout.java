package com.wudengwei.tabindicatorlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Scroller;

/**
 * Copyright (C)
 * FileName: IndicatorLayout
 * Author: wudengwei
 * Date: 2019/7/10 16:15
 * Description: ${DESCRIPTION}
 */
public class TabIndicatorLayout extends LinearLayout {
    private int mVisibleTabNum = 5;//tabFixedWidth=true情况下,父view可以显示的tab数量（每个tab的宽度相等）
    private boolean mTabFixedWidth = true;//tab的宽度是否固定（默认是，配合mVisibleTabNum）
    private String mTabWidthRatio;//tabFixedWidth=true情况下,屏幕宽度按比重分给tab(专门针对，tab数量固定，但每个tab宽度相差较大)
    private float mTabWidthRatioSum;
    private float[] mTabWidthRatioArray;

    private float[] radiusArray = new float[8];//two radius values [X, Y]. The corners are ordered top-left, top-right, bottom-right, bottom-left
    private Paint mIndicatorPaint;
    private Path mIndicatorPath;
    private RectF mIndicatorRect;
    private float mIndicatorRadius = 0;//指示器圆角半径
    private float mIndicatorHeight = 6;//指示器固定高度
    private float mIndicatorWidth = -1;//指示器固定宽度
    private float mIndicatorWidthPercent = -1;//指示器宽度占tab宽度的百分比(优先度大于mIndicatorWidth)
    private int mIndicatorColor;

    private int mCurrentTabIndex = 0;//当前页面
    private int scrollOffset = 0;//
    private int lastScrollX = 0;
    private int mSumTabWidth = 0;
    private int mTotalWidth = 0;//TabIndicatorLayout的可用宽度
    private int mPointTabSelected = 0;//选择tab的中心点


    private ViewPager mViewPager;//关联的ViewPager
    private TabIndicatorLayout.Adapter mAdapter;
    private TabIndicatorLayout.PagerObserver mObserver;

    private OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            int pageIndex = (int) view.getTag();
            if (pageIndex == mCurrentTabIndex)
                return;
            mCurrentTabIndex = pageIndex;
            if (mViewPager != null) {
                mViewPager.setCurrentItem(pageIndex);
            }
            scroll(pageIndex,0);
            dataSetChanged();
            if (mAdapter != null && mAdapter.mOnTabClickListener != null) {
                mAdapter.mOnTabClickListener.onClick(view, pageIndex);
            }
        }
    };

    public TabIndicatorLayout(Context context) {
        this(context, null);
    }

    public TabIndicatorLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabIndicatorLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new Scroller(context);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TabIndicatorLayout);
        if (typedArray != null) {
            mVisibleTabNum = typedArray.getInt(R.styleable.TabIndicatorLayout_tabVisibleNum, mVisibleTabNum);
            mTabFixedWidth = typedArray.getBoolean(R.styleable.TabIndicatorLayout_tabFixedWidth, mTabFixedWidth);
            mIndicatorRadius = typedArray.getDimension(R.styleable.TabIndicatorLayout_indicatorRadius, mIndicatorRadius);
            mIndicatorWidth = typedArray.getDimension(R.styleable.TabIndicatorLayout_indicatorWidth, mIndicatorWidth);
            mIndicatorHeight = typedArray.getDimension(R.styleable.TabIndicatorLayout_indicatorHeight, mIndicatorHeight);
            mIndicatorWidthPercent = typedArray.getFloat(R.styleable.TabIndicatorLayout_indicatorWidthPercent, mIndicatorWidthPercent);
            mIndicatorColor = typedArray.getColor(R.styleable.TabIndicatorLayout_indicatorColor, Color.parseColor("#FE9926"));
            mTabWidthRatio = typedArray.getString(R.styleable.TabIndicatorLayout_tabWidthRatio);
            typedArray.recycle();
            if (mIndicatorWidthPercent != -1 && mIndicatorWidthPercent < 0 || mIndicatorWidthPercent > 1) {
                throw new RuntimeException("app:indicatorWidthPercent取值范围[0,1]");
            }
            if (mTabWidthRatio != null) {
                if (mVisibleTabNum > 1 && !mTabWidthRatio.contains(":")) {
                    throw new RuntimeException("app:tabWidthRatio设置必须以':'分开");
                }
                if (mTabWidthRatio.split(":").length != mVisibleTabNum) {
                    throw new RuntimeException("app:tabWidthRatio设置要与app:visibleTabNum对应");
                }
                String[] arrStr = mTabWidthRatio.split(":");
                mTabWidthRatioArray = new float[arrStr.length];
                mTabWidthRatioSum = 0;
                for (int i=0;i<arrStr.length;i++) {
                    float value = Float.valueOf(arrStr[i]);
                    mTabWidthRatioSum = mTabWidthRatioSum + value;
                    mTabWidthRatioArray[i] = value;
                }
            }
        }
        radiusArray[0] = mIndicatorRadius;
        radiusArray[1] = mIndicatorRadius;
        radiusArray[2] = mIndicatorRadius;
        radiusArray[3] = mIndicatorRadius;
        radiusArray[4] = mIndicatorRadius;
        radiusArray[5] = mIndicatorRadius;
        radiusArray[6] = mIndicatorRadius;
        radiusArray[7] = mIndicatorRadius;

        mIndicatorPaint = new Paint();
        mIndicatorPaint.setColor(mIndicatorColor);
        mIndicatorPaint.setAntiAlias(true);
        mIndicatorPaint.setStyle(Paint.Style.FILL);

        mIndicatorPath = new Path();
        mIndicatorRect = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //测量子view
        measureAdapterItems(widthMeasureSpec,heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    /**
     * 绘制子View
     *
     * @param canvas
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        drawTriangle(canvas);
        super.dispatchDraw(canvas);
    }

    /**
     * 绘制指示器
     */
    private void drawTriangle(Canvas canvas) {
        mIndicatorPath.reset();
        View currentTab = getChildAt(mTempScrollPosition);
        float lineLeft = getTabLeft(currentTab);
        float lineRight = getTabRight(currentTab);
        if (mPositionOffset > 0f && mTempScrollPosition < getChildCount() - 1) {
            View nextTab = getChildAt(mTempScrollPosition + 1);
            float nextTabLeft = getTabLeft(nextTab);
            float nextTabRight = getTabRight(nextTab);
            //普通效果
            lineLeft = lineLeft + mPositionOffset * (nextTabLeft - lineLeft);
            lineRight = lineRight + mPositionOffset * (nextTabRight - lineRight);
        }
        mIndicatorRect.set(lineLeft,currentTab.getHeight()-mIndicatorHeight,lineRight,currentTab.getHeight());
        mIndicatorPath.addRoundRect(mIndicatorRect, radiusArray, Path.Direction.CW);
        canvas.drawPath(mIndicatorPath, mIndicatorPaint);
    }

    private float getTabLeft(View tab) {
        if (mIndicatorWidthPercent >= 0) {
            //让指示器居中的两边空格
            final float space = tab.getWidth()*(1-mIndicatorWidthPercent)*0.5f;
            return tab.getLeft() + space;
        } else {
            if (mIndicatorWidth >= 0 && mIndicatorWidth <= tab.getWidth()) {
                //让指示器居中的两边空格
                final float space = (tab.getWidth()-mIndicatorWidth)*0.5f;
                return tab.getLeft() + space;
            } else {
                return tab.getLeft();
            }
        }
    }

    private float getTabRight(View tab) {
        if (mIndicatorWidthPercent >= 0) {
            //让指示器居中的两边空格
            final float space = tab.getWidth()*(1-mIndicatorWidthPercent)*0.5f;
            return tab.getRight() - space;
        } else {
            if (mIndicatorWidth >= 0 && mIndicatorWidth <= tab.getWidth()) {
                //让指示器居中的两边空格
                final float space = (tab.getWidth()-mIndicatorWidth)*0.5f;
                return tab.getRight() - space;
            } else {
                return tab.getRight();
            }
        }
    }

    /**
     * 添加mAdapter中所有的view
     */
    private void addAdapterItems() {
        if (this.mAdapter == null)return;
        int itemCount = this.mAdapter.getCount();
        for (int i=0;i<itemCount;i++) {
            final int pageIndex = i;
            //获得position位置的view
            View view = this.mAdapter.onCreateView(this,pageIndex);
            view.setTag(pageIndex);
            mAdapter.onBindView(view,pageIndex,mCurrentTabIndex);
            view.setOnClickListener(onClickListener);
            addView(view);
        }
    }

    private void measureAdapterItems(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        //子view的宽高
        int childWidthSize = 0, childWidthMode = 0, childHeightSize = 0, childHeightMode = 0;
        for (int i=0;i<getChildCount();i++) {
            final int pageIndex = i;
            View view = getChildAt(pageIndex);
            /*测量孩子的宽高*/
            if (mTabFixedWidth) {
                if (mTabWidthRatioSum > 0) {
                    childWidthSize = (int) (widthSize*mTabWidthRatioArray[i]/mTabWidthRatioSum);
                } else {
                    childWidthSize = widthSize/mVisibleTabNum;
                }
            } else {
                childWidthSize = widthSize;
            }
            measureChild(view,MeasureSpec.makeMeasureSpec(childWidthSize,widthMode),heightMeasureSpec);
        }
        //获取测量后所有tab的宽度和
        mSumTabWidth = 0;
        mTotalWidth = getMeasuredWidth();
        mPointTabSelected = mTotalWidth/2;
        for (int i=0;i<getChildCount();i++) {
            final View view = getChildAt(i);
            mSumTabWidth = mSumTabWidth + getChildAt(i).getMeasuredWidth();
        }
    }

    //惯性滑动
    private Scroller mScroller;

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            // 产生了动画效果，根据当前值 每次滚动一点
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            //手指按下时执行的方法
            case MotionEvent.ACTION_DOWN:
                lastTouchX = (int) event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX()-lastTouchX) > 10) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    int lastTouchX=-1;
    int lastTouchY=-1;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int mStartX = (int) event.getX();
        int mStartY = (int) event.getY();
        switch (event.getAction()) {
            //手指按下时执行的方法
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                if (mStartX != lastTouchX) {
                    int offsetX = mStartX - lastTouchX;
                    Log.e("event ACTION_MOVE",""+offsetX+"==="+getScrollX());
                    scrollBy(-offsetX,0);
//                    if (offsetX >= 0) {//从左往右滑
//                        if (getScrollX() - offsetX <= 0) {
//                            scrollBy(-offsetX,0);
//                        } else {
//                            scrollBy(0,0);
//                        }
//                    } else {//从右往左滑
//                        if (getScrollX() + mTotalWidth <= mSumTabWidth) {
//                            scrollBy(-offsetX,0);
//                        } else {
//                            scrollBy(0,0);
//                        }
//                    }
                    lastTouchX = mStartX;
                    lastTouchY = mStartY;
                    requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_UP:
                //回弹效果
                if (getScrollX() <= 0) {
                    mScroller.startScroll(getScrollX(),0,-getScrollX(),0);
                    invalidate();
                }
                if (getScrollX() + mTotalWidth >= mSumTabWidth) {
                    mScroller.startScroll(getScrollX(),0,mSumTabWidth - getScrollX() - mTotalWidth,0);
                    invalidate();
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    //---------------------------ViewPager滑动-----------------------------------
    //记录上次滑动位置，防止重复滑动
    private int lastPositionOffsetPixels = 0;
    //记录开始滑动的页面
    private int mScrollPagerIndex;
    //记录开始滑动的页面百分比
    private float mPositionOffset;
    //记录开始滑动的页面
    private int mTempScrollPosition;
    /**
     * 绑定ViewPager
     *
     * @param viewPager
     */
    public void setViewPager(ViewPager viewPager) {
        this.mViewPager = viewPager;
        this.mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            //position 当前所在页面
            //positionOffset 当前所在页面偏移百分比
            //positionOffsetPixels 当前所在页面偏移量
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                mPositionOffset = positionOffset;
                mTempScrollPosition = position;
                if (lastPositionOffsetPixels != positionOffsetPixels) {
                    if(mScrollPagerIndex == position){//正在向左滑动,正在进入下一页
//                        Log.e("onPageScrolled","正在向左滑动");
                    }else{//正在向右滑动,正在进入上一页
//                        Log.e("正在向右滑动","正在向右滑动");
                    }
                    scroll(position, positionOffset);
                    lastPositionOffsetPixels = positionOffsetPixels;
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position != mCurrentTabIndex) {
                    mCurrentTabIndex = position;
                    dataSetChanged();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == 1) {//仅滑动时记录当前页面
                    mScrollPagerIndex = mViewPager.getCurrentItem();
                }
            }
        });
    }

    /**
     * 监听ViewPager滑动,联动Indicator
     *
     * @param position 滑动的页面
     * @param positionOffset 滑动的距离
     */
    protected void scroll(int position, float positionOffset) {
        if (mAdapter != null && mAdapter.getCount() > 0) {
            //监听页面滑动
            if (position < getChildCount()-1) {
                View currentTab;
                final View nextTab;
                int leftToRight = mScrollPagerIndex==position?1:-1;
                if (leftToRight >= 0) {
                    currentTab = getChildAt(position);
                    nextTab = getChildAt(position+1);
                    mAdapter.onEnterOrLeave(currentTab,nextTab,position,position+1,1-positionOffset,positionOffset);
                } else {
                    currentTab = getChildAt(position+1);
                    nextTab = getChildAt(position);
                    mAdapter.onEnterOrLeave(currentTab,nextTab,position+1,position,positionOffset,1-positionOffset);
                }
            }
            //tab滑动
            final View currTab = getChildAt(position);
            int offset = (int) (positionOffset * currTab.getWidth());
            int newScrollX = currTab.getLeft() + currTab.getWidth()/2 + offset;
            if (position > 0 || offset > 0) {
                newScrollX -= scrollOffset;
            }
            if (newScrollX != lastScrollX) {
                if (newScrollX >= mPointTabSelected) {
                    newScrollX -= mPointTabSelected;
                    lastScrollX = newScrollX;
                    if (newScrollX + mTotalWidth <= mSumTabWidth) {
                        scrollTo(newScrollX, 0);
                    }
                }
                invalidate();
            }
        }
    }

    protected void setAdapter(@Nullable Adapter adapter) {
        if (this.mAdapter != null)return;
        this.mAdapter = adapter;
        if (this.mObserver == null) {
            this.mObserver = new TabIndicatorLayout.PagerObserver();
        }
        this.mAdapter.setObserver(this.mObserver);
        removeAllViews();
        //添加mAdapter中所有的view
        addAdapterItems();
    }

    private void dataSetChanged() {
        if (this.mAdapter != null && getChildCount() > 0) {
            for (int i=0;i<getChildCount();i++) {
                final int pageIndex = i;
                //赋值position位置的view
                this.mAdapter.onBindView(getChildAt(i),pageIndex,mCurrentTabIndex);
            }
        }
    }

    private class PagerObserver extends DataSetObserver {
        PagerObserver() {
        }

        public void onChanged() {
            TabIndicatorLayout.this.dataSetChanged();
        }

        public void onInvalidated() {
            TabIndicatorLayout.this.dataSetChanged();
        }
    }

    public abstract static class Adapter {
        private final DataSetObservable mObservable = new DataSetObservable();
        private DataSetObserver mViewPagerObserver;

        //------------------------添加点击事件------------------------------

        private OnTabClickListener mOnTabClickListener;

        public void setOnTabClickListener(OnTabClickListener mOnTabClickListener) {
            this.mOnTabClickListener = mOnTabClickListener;
        }

        public interface OnTabClickListener {
            void onClick(View v, int position);
        }

        public Adapter() {
        }

        void setObserver(DataSetObserver observer) {
            synchronized(this) {
                this.mViewPagerObserver = observer;
            }
        }

        public abstract int getCount();

        @NonNull
        public abstract View onCreateView(@NonNull ViewGroup viewGroup, int position);

        public abstract void onBindView(@NonNull View view, int position, int currentTabPosition);

        public void onEnterOrLeave(@NonNull View formView, @NonNull View toView, int formPosition, int toPosition, float formPercent, float toPercent) {}

        public void notifyDataSetChanged() {
            synchronized(this) {
                if (this.mViewPagerObserver != null) {
                    this.mViewPagerObserver.onChanged();
                }
            }

            this.mObservable.notifyChanged();
        }

        public void registerDataSetObserver(@NonNull DataSetObserver observer) {
            this.mObservable.registerObserver(observer);
        }

        public void unregisterDataSetObserver(@NonNull DataSetObserver observer) {
            this.mObservable.unregisterObserver(observer);
        }
    }
}
