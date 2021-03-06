package com.chenjimou.recyclerviewpullrefreshdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ListViewCompat;

public class MyRefreshLayout extends ViewGroup implements NestedScrollingParent3
{
    private final Context mContext;
    private final NestedScrollingParentHelper mParentHelper;
    private final OverScroller mScroller;
    private final int mTouchSlop;
    private final int mHeadViewHeight;
    // 中等动画持续时间
    private final int mMediumAnimationDuration;

    private View mHeaderView;
    private View mTarget;
    // 执行 fling 回弹动画的 runnable
    private FlingRunnable mFlingRunnable;
    // 执行下拉回弹动画的 runnable
    private ReboundRunnable mReboundRunnable;
    private OnRefreshListener onRefreshListener;
    // mTarget 已经滚动的距离
    private int mTargetScrollY = 0;
    // 是否是向下滚动
    private boolean isUp = false;
    // 是否正在拖动
    private boolean mIsBeingDragged;
    // 是否正处于刷新状态
    private boolean mRefreshing;
    private float mInitialDownY;
    private float mInitialMotionY;
    // 下拉阻尼系数
    private static final float DAMPING_COEFFICIENT = 0.025f;

    public MyRefreshLayout(@NonNull Context context)
    {
        this(context, null);
    }

    public MyRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        createHeaderView();
        mHeadViewHeight = dip2px(56);
        setOverScrollMode(OVER_SCROLL_NEVER);
        mScroller = new OverScroller(mContext);
        mParentHelper = new NestedScrollingParentHelper(this);
        mMediumAnimationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        ensureTarget();

        final int action = ev.getAction();

        if (!isEnabled() || canTargetScrollUp() || mRefreshing)
        {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished())
                {
                    mScroller.abortAnimation();
                }
                if (mFlingRunnable != null)
                {
                    cancelFlingAnimation();
                }
                if (mReboundRunnable != null)
                {
                    cancelReboundAnimation();
                }
                // 使视图回滚到正常位置
                scrollBy(0, mHeaderView.getHeight() + mHeaderView.getTop());
                mIsBeingDragged = false;
                mInitialDownY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float y = ev.getY();
                isDragging(y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                break;
        }
        return mIsBeingDragged;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(
                MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                        MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                        MeasureSpec.EXACTLY));
        mHeaderView.measure(
                MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                        MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mHeadViewHeight, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final int targetLeft = getPaddingLeft();
        final int targetTop = getPaddingTop();
        final int targetWidth = width - getPaddingLeft() - getPaddingRight();
        final int targetHeight = height - getPaddingTop() - getPaddingBottom();
        mTarget.layout(targetLeft, targetTop, targetLeft + targetWidth, targetTop + targetHeight);
        mHeaderView.layout(targetLeft, -mHeaderView.getMeasuredHeight(), targetLeft + targetWidth, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        final int action = ev.getAction();

        if (!isEnabled() || canTargetScrollUp() || mRefreshing)
        {
            return false;
        }

        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
            {
                final float y = ev.getY();
                isDragging(y);
                if (mIsBeingDragged)
                {
                    final float dy = (y - mInitialMotionY) * DAMPING_COEFFICIENT;
                    if (Math.abs(getScrollY()) <= mHeadViewHeight + 200)
                    {
                        if (Math.abs(dy) > 0)
                        {
                            scrollBy(0, (int)-dy);
                        }
                        else
                        {
                            return false;
                        }
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged)
                {
                    mIsBeingDragged = false;
                    if (Math.abs(getScrollY()) > mHeaderView.getHeight())
                    {
                        mScroller.startScroll(0, getScrollY(), 0,
                                Math.abs(getScrollY()) - mHeaderView.getHeight(), mMediumAnimationDuration);
                        mRefreshing = true;
                    }
                    else
                    {
                        mScroller.startScroll(0, getScrollY(), 0,
                                Math.abs(getScrollY()), mMediumAnimationDuration);
                    }
                    mReboundRunnable = new ReboundRunnable();
                    postOnAnimation(mReboundRunnable);
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
                return false;
        }
        return true;
    }

    public void createHeaderView()
    {
        mHeaderView = new MyHeader(mContext);
        // 修复 refreshView 中 LayoutParams 丢失问题
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(mHeaderView, layoutParams);
    }

    private void dispatchFlingAnimation(int velocityY)
    {
        if (mHeaderView != null && isUp)
        {
            if (!mScroller.isFinished())
            {
                mScroller.abortAnimation();
            }
            mScroller.fling(0, mTargetScrollY,
                    0, velocityY,
                    0, 0,
                    0, Integer.MAX_VALUE,
                    0, mHeaderView.getHeight());
            // 执行新的 fling 回弹动画
            mFlingRunnable = new FlingRunnable();
            postOnAnimation(mFlingRunnable);
        }
    }

    private void dispatchReboundAnimation()
    {

    }

    private void cancelFlingAnimation()
    {
        removeCallbacks(mFlingRunnable);
        mFlingRunnable = null;
    }

    private void cancelReboundAnimation()
    {
        removeCallbacks(mReboundRunnable);
        mReboundRunnable = null;
    }

    private void isDragging(float y)
    {
        final float dy = y - mInitialDownY;
        if (dy > mTouchSlop && !mIsBeingDragged)
        {
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;
        }
    }

    private boolean canTargetScrollUp()
    {
        if (mTarget instanceof ListView)
        {
            return ListViewCompat.canScrollList((ListView) mTarget, -1);
        }
        return mTarget.canScrollVertically(-1);
    }

    private void ensureTarget()
    {
        if (mTarget == null)
        {
            for (int i = 0; i < getChildCount(); i++)
            {
                View child = getChildAt(i);
                if (!child.equals(mHeaderView))
                {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    public int dip2px(float dipValue)
    {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type)
    {
        return isEnabled() && (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type)
    {
        mParentHelper.onNestedScrollAccepted(child, target, axes, type);
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type)
    {
        mParentHelper.onStopNestedScroll(target, type);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, int type)
    {

    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type)
    {
        isUp = dy < 0;
        // todo mScrollY的计算还存在问题，需要修改
        if ((dy < 0 && mTarget.canScrollVertically(-1)) || (dy > 0 && mTarget.canScrollVertically(1)))
        {
            mTargetScrollY += dy;
        }
        if (dy > 0 && mFlingRunnable == null && getScrollY() < 0)
        {
            mRefreshing = false;
            int move = Math.min(dy, Math.abs(getScrollY()));
            scrollBy(0, move);
            consumed[1] = move;
        }
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY)
    {
        dispatchFlingAnimation((int)velocityY);
        return false;
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed, int type, @NonNull int[] consumed)
    {

    }

    class FlingRunnable implements Runnable
    {
        @Override
        public void run()
        {
            if (mScroller.computeScrollOffset())
            {
                if (!canTargetScrollUp())
                {
                    scrollTo(0, mScroller.getCurrY());
                }
                postOnAnimation(this);
            }
            else
            {
                mFlingRunnable = null;
            }
        }
    }

    class ReboundRunnable implements Runnable
    {
        @Override
        public void run()
        {
            if (mScroller.computeScrollOffset())
            {
                scrollTo(0, mScroller.getCurrY());
                postOnAnimation(this);
            }
            else
            {
                if (mRefreshing && onRefreshListener != null)
                {
                    onRefreshListener.onRefresh();
                }
                mReboundRunnable = null;
            }
        }
    }

    public interface OnRefreshListener
    {
        void onRefresh();
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener)
    {
        this.onRefreshListener = onRefreshListener;
    }

    public void finishRefresh()
    {
        if (mRefreshing)
        {
            scrollBy(0, -getScrollY());
            mRefreshing = false;
        }
//        else
//        {
//            // 使视图回滚到正常位置
//            scrollBy(0, mHeaderView.getHeight() + mHeaderView.getTop());
//        }
    }
}
