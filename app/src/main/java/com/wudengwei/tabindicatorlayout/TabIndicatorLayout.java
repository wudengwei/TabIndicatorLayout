package com.wudengwei.tabindicatorlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.OverScroller;
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
    private int mTabScrollOffset = -1;//滑动过程中，tab与父视图的左边保持的距离(不设置默认为tab居中)
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
    private int mSumTabWidth = 0;//所有tab宽度和
    private int mTotalWidth = 0;//TabIndicatorLayout的可用宽度


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
        mScroller = new OverScroller(context);
        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
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
            mTabScrollOffset = typedArray.getDimensionPixelOffset(R.styleable.TabIndicatorLayout_tabScrollOffset, mTabScrollOffset);
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
        for (int i=0;i<getChildCount();i++) {
            final View view = getChildAt(i);
            mSumTabWidth = mSumTabWidth + getChildAt(i).getMeasuredWidth();
        }
    }

    //惯性滑动
    private OverScroller mScroller;
    private int mLastFlingX = 0;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;//判断滑动的临界值
    private int mMinFlingVelocity;//最小加速度
    private int mMaxFlingVelocity;//最大加速度
    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            final int x = mScroller.getCurrX();
            int dx = x - mLastFlingX;
            mLastFlingX = x;
            constrainScrollBy(dx, 0);
            postInvalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            //手指按下时执行的方法
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                lastTouchX = (int) event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX()-lastTouchX) > mTouchSlop) {
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
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        switch (event.getAction()) {
            //手指按下时执行的方法
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                if (mStartX != lastTouchX) {
                    int dx = mStartX - lastTouchX;
                    //Log.e("event ACTION_MOVE",""+dx+"==="+getScrollX());
                    lastTouchX = mStartX;
                    lastTouchY = mStartY;
                    constrainScrollBy(-dx,0);
                    requestDisallowInterceptTouchEvent(true);
                    mVelocityTracker.addMovement(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                float xVelocity = -mVelocityTracker.getXVelocity();
                if (Math.abs(xVelocity) < mMinFlingVelocity) {
                    xVelocity = 0F;
                } else {
                    xVelocity = Math.max(-mMaxFlingVelocity, Math.min(xVelocity, mMaxFlingVelocity));
                }
                if (xVelocity != 0) {
                    fling((int) xVelocity);
                } else {
                    mScroller.abortAnimation();
                }
                if (mVelocityTracker != null) {
                    mVelocityTracker.clear();
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void fling(int xVelocity) {
        mLastFlingX = 0;
        mScroller.fling(0, 0, xVelocity, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        invalidate();
    }

    private void constrainScrollBy(int dx, int dy) {
        if (dx == 0)
            return;
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();
        //左边界
        if (-scrollX - dx > 0) {
            dx = -scrollX;
        }
        //右边界
        if (mTotalWidth + scrollX + dx > mSumTabWidth) {
            dx = mSumTabWidth - mTotalWidth - scrollX;
        }
        scrollBy(dx, dy);
    }

    //---------------------------ViewPager滑动-----------------------------------
    //记录开始滑动的页面
    private int mScrollPagerIndex;
    //记录开始滑动的页面百分比
    private float mPositionOffset;
    //记录开始滑动的页面
    private int mTempScrollPosition;
    private int lastScrollX = 0;//记录最后滑动位置，放置重复滑动
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
                scroll(position, positionOffset);
                if(mScrollPagerIndex == position){//正在向左滑动,正在进入下一页
//                        Log.e("onPageScrolled","正在向左滑动");
                }else{//正在向右滑动,正在进入上一页
//                        Log.e("正在向右滑动","正在向右滑动");
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
            int newScrollX = (int) (currTab.getLeft() + currTab.getWidth()*0.5 + offset);
            int threshold = (int) (mTotalWidth*0.5);
            if (mTabScrollOffset >= 0) {
                newScrollX = currTab.getLeft() + offset;
                threshold = mTabScrollOffset;
            }
            if (newScrollX != lastScrollX) {
                if (newScrollX >= threshold) {
                    newScrollX -= threshold;
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

    /**
     * 更新所有数据
     */
    private void dataSetChanged() {
        if (this.mAdapter != null && getChildCount() > 0) {
            for (int i=0;i<getChildCount();i++) {
                final int pageIndex = i;
                //赋值position位置的view
                this.mAdapter.onBindView(getChildAt(i),pageIndex,mCurrentTabIndex);
            }
        }
    }

    /**
     * 删除tab
     * @param position 开始位置
     * @param itemCount 删除数量
     */
    private void onItemRangeRemoved(int position, int itemCount) {
        int count = getChildCount();
        int endIndex = position+itemCount-1;
        if (position >= 0 && position <= count-1 && endIndex <= count-1) {
            Log.e("删除","删除位置["+position+","+endIndex+"]");
            if (mCurrentTabIndex >= getChildCount())
                mCurrentTabIndex = getChildCount()-1;
            removeViews(position, itemCount);
            int newCount = getChildCount();
            for (int i=0;i<newCount;i++) {
                View view = getChildAt(i);
                view.setTag(i);
                mAdapter.onBindView(view,i,mCurrentTabIndex);
                view.setOnClickListener(onClickListener);
            }
        } else {
            throw new IndexOutOfBoundsException("子view总数："+count+",删除位置["+position+","+endIndex+"]");
        }
    }

    /**
     * 添加tab
     * @param position 开始位置
     * @param itemCount 添加数量
     */
    private void onItemRangeInserted(final int position, final int itemCount) {
        Log.e("onItemRangeInserted",""+position+" ,"+itemCount);
        int count = mAdapter.getCount();
        for (int i=position;i<count;i++) {
            Log.e("i",""+i);
            if (i >= position && i < position+itemCount) {
                Log.e("添加position",""+position);
                //获得position位置的view
                View view = this.mAdapter.onCreateView(this,i);
                view.setTag(i);
                mAdapter.onBindView(view,i,mCurrentTabIndex);
                view.setOnClickListener(onClickListener);
                addView(view,i);
            } else {
                View view = getChildAt(i);
                view.setTag(i);
                mAdapter.onBindView(view,i,mCurrentTabIndex);
                view.setOnClickListener(onClickListener); 
            }
        }
    }

    private class PagerObserver extends DataSetObserver {
        PagerObserver() {
        }

        public void onChanged() {
            TabIndicatorLayout.this.dataSetChanged();
        }

        public void notifyItemRangeRemoved(int position, int itemCount) {TabIndicatorLayout.this.onItemRangeRemoved(position, itemCount);}

        public void notifyItemInserted(int position, int itemCount) {TabIndicatorLayout.this.onItemRangeInserted(position, itemCount);}

        public void onInvalidated() {
            TabIndicatorLayout.this.dataSetChanged();
        }
    }

    private static class AdapterDataObservable extends DataSetObservable {

        public void notifyItemRangeRemoved(int positionStart, int itemCount) {
            for(int i = this.mObservers.size() - 1; i >= 0; --i) {
                ((PagerObserver)this.mObservers.get(i)).notifyItemRangeRemoved(positionStart, itemCount);
            }
        }

        public void notifyItemInserted(int positionStart, int itemCount) {
            for(int i = this.mObservers.size() - 1; i >= 0; --i) {
                ((PagerObserver)this.mObservers.get(i)).notifyItemInserted(positionStart, itemCount);
            }
        }
    }

    public abstract static class Adapter {
        private final AdapterDataObservable mObservable = new AdapterDataObservable();
        private PagerObserver mViewPagerObserver;

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

        void setObserver(PagerObserver observer) {
            synchronized(this) {
                this.mViewPagerObserver = observer;
            }
            registerDataSetObserver(mViewPagerObserver);
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

        public void notifyItemRemoved(int position) {
            this.mObservable.notifyItemRangeRemoved(position, 1);
        }


        public void notifyItemInserted(int position) {
            this.mObservable.notifyItemInserted(position, 1);
        }

        public void registerDataSetObserver(@NonNull DataSetObserver observer) {
            this.mObservable.registerObserver(observer);
        }

        public void unregisterDataSetObserver(@NonNull DataSetObserver observer) {
            this.mObservable.unregisterObserver(observer);
        }
    }
}