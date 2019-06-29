package com.litesnap.open.drift.behavior;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.litesnap.open.drift.R;
import com.litesnap.open.drift.behavior.source.ViewOffsetBehavior;
import com.litesnap.open.drift.view.MyCoordinatorLayout;

import java.util.ArrayList;
import java.util.List;

public class DriftBehavior extends ViewOffsetBehavior<View> {
    public static final String TAG = "DriftBehavior";

    private final List<ObjectAnimator> mAnimationList;
    private VelocityTracker mVelocityTracker;

    private View mDuoyView;
    private View mDriftContentView;

    private View mDriftView;
    private boolean mIsAnimation;
    private float mLastY;

    private boolean mIsTouchDriftDuoy;
    private boolean mIsTouchDriftBody;

    private int mMaxTop;
    private int mMinTop = 0;
    private int mOffsetBottom;

    private CoordinatorLayout mParent;


    public DriftBehavior() {
        this(null, null);
    }

    public DriftBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAnimationList = new ArrayList<>();
    }

    @Override
    public boolean onMeasureChild(@NonNull CoordinatorLayout parent, @NonNull View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        mParent = parent;
        mDriftView = child;
        mDuoyView = parent.findViewById(R.id.duoy);
        mDriftContentView = parent.findViewById(R.id.drift_content);

        mOffsetBottom = mDuoyView.getMeasuredHeight();

        setCallback((MyCoordinatorLayout) parent);

        int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredHeight() - mMinTop, View.MeasureSpec.EXACTLY);
        parent.onMeasureChild(mDriftView, parentWidthMeasureSpec, widthUsed, heightSpec, heightUsed);
        return true;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        super.onLayoutChild(parent, child, layoutDirection);
        Rect rect = new Rect();
        mDriftView.getHitRect(rect);

        int height = mDriftView.getMeasuredHeight();
        mMaxTop = parent.getMeasuredHeight() - mOffsetBottom;

        rect.top = mMaxTop;
        rect.bottom = rect.top + height;
        mDriftView.layout(rect.left, rect.top, rect.right, rect.bottom);
        return true;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return true;
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {

        Rect parentRect = new Rect();

        int[] point = new int[2];

        parent.getGlobalVisibleRect(parentRect);
        mDriftView.getLocationInWindow(point);

        if (dy > 0){
            int bottom = point[1] + mDriftView.getHeight();
            if (mIsTouchDriftBody && bottom > parentRect.bottom){
                int detialY = bottom - dy;
                detialY = detialY <= parentRect.bottom ? bottom - parentRect.bottom : dy;
                mDriftView.offsetTopAndBottom(- detialY);
                consumed[1] = dy;
            }
        }

        if (mIsTouchDriftDuoy || mIsAnimation){
            consumed[1] = dy;
        }
        super.onNestedPreScroll(parent, child, target, dx, dy, consumed, type);
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {

        if (dyUnconsumed < 0){
            int top = mDriftView.getTop();
            if (mIsTouchDriftBody && top < mMaxTop){
                int detialY = top - dyUnconsumed;
                detialY = detialY >= mParent.getBottom() ? - (mParent.getBottom() - top) : dyUnconsumed;
                mDriftView.offsetTopAndBottom(- detialY);
            }
        }
        super.onNestedScroll(parent, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
    }

    private void setCallback(final MyCoordinatorLayout parent){
        parent.setCallback(new MyCoordinatorLayout.OnDispatchTouchEventCallback() {
            @Override
            public void onCallback(MotionEvent ev) {
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                }

                mVelocityTracker.addMovement(ev);

                switch (ev.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        mLastY = ev.getY();
                        mIsAnimation = false;

                        mIsTouchDriftDuoy = isTouchInGlobaRect(mDuoyView, ev);

                        if (!mIsTouchDriftDuoy){
                            mIsTouchDriftBody = isTouchInGlobaRect(mDriftContentView, ev);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dy = (int) (mLastY - ev.getY());
                        mLastY = ev.getY();

                        Rect parentRect = new Rect();
                        parent.getGlobalVisibleRect(parentRect);
                        int[] point = new int[2];
                        mDriftView.getLocationInWindow(point);

                        if (mIsTouchDriftDuoy){
                            int bottom = point[1] + mDriftView.getHeight();
                            int top = point[1] + mOffsetBottom;
                            int downBottom = parentRect.bottom;
                            if (dy > 0 && bottom > parentRect.bottom){
                                int direct = bottom - dy;
                                int offsetY = direct < parentRect.bottom ? bottom - parentRect.bottom : dy;
                                mDriftView.offsetTopAndBottom(- offsetY);
                            }else if (dy < 0 && top < downBottom){
                                //int direct = top - dy;
                                //int offsetY = direct >= downBottom ? downBottom - top : dy;
                                //Log.i(TAG, "onCallback: "+top+"  "+direct+"  "+offsetY+"  "+downBottom);
                                mDriftView.offsetTopAndBottom(- dy);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mVelocityTracker.computeCurrentVelocity(1000);
                        float velocityY = mVelocityTracker.getYVelocity();
                        Rect rect = new Rect();
                        mDriftView.getLocalVisibleRect(rect);

                        if (rect.height() != mDriftView.getHeight() && (mIsTouchDriftBody || mIsTouchDriftDuoy)){
                            mIsAnimation = true;
                            if (Math.abs(velocityY) > mParent.getHeight()){
                                if (velocityY > 0){
                                    startDownAnimation();
                                }else {
                                    startUpAnimation();
                                }
                            }else {
                                actionAnimation();
                            }
                        }
                        mIsTouchDriftDuoy = false;
                        mIsTouchDriftBody = false;
                        break;
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull CoordinatorLayout parent, @NonNull View child, @NonNull MotionEvent ev) {
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                Rect parentRect = new Rect();

                int[] point = new int[2];

                mDriftView.getLocationInWindow(point);

                parent.getGlobalVisibleRect(parentRect);
                int bottom = point[1] + mDriftView.getHeight();
                if (bottom <= parentRect.bottom && !mIsTouchDriftDuoy && !mIsTouchDriftBody){
                    startDownAnimation();
                    return false;
                }
                break;
        }
        return super.onInterceptTouchEvent(parent, child, ev);
    }

    public boolean isTouchInGlobaRect(View view, MotionEvent event){
        Rect rect = new Rect();
        int[] point = new int[2];
        view.getGlobalVisibleRect(rect);
        view.getLocationOnScreen(point);
        rect.top = point[1];
        rect.bottom = rect.top + view.getHeight();

        int touchX = (int) event.getRawX();
        int touchY = (int) event.getRawY();
        if (touchX >= rect.left&& touchX <= rect.right && touchY >= rect.top && touchY <= rect.bottom){
            return true;
        }else {
            return false;
        }
    }

    private void startDownAnimation(){
        for (ObjectAnimator animator : mAnimationList){
            animator.cancel();
            animator.end();
        }
        mAnimationList.clear();

        int end = mMaxTop - mDriftView.getTop();
        ObjectAnimator animator = ObjectAnimator.ofInt(new Spac(), "DownSpac", 0, end);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimationList.add(animator);
        animator.start();
    }

    private void startUpAnimation(){
        for (ObjectAnimator animator : mAnimationList) {
            animator.cancel();
            animator.end();
        }
        mAnimationList.clear();
        Rect rect = new Rect();
        mDriftView.getLocalVisibleRect(rect);

        int end = mDriftView.getHeight() - rect.height();
        ObjectAnimator animator = ObjectAnimator.ofInt(new Spac(), "UpSpac", 0, -end);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        mAnimationList.add(animator);
        animator.start();
    }

    private void actionAnimation(){
        Rect rect = new Rect();
        mDriftView.getLocalVisibleRect(rect);
        int maxHeight = rect.height();
        int limit = mDriftView.getHeight() / 2;
        if (rect.height() < mDriftView.getHeight()){
            if (maxHeight < limit){
                startDownAnimation();
            }else {
                startUpAnimation();
            }
        }
    }

    private class Spac{
        private int num;

        private void setDownSpac(int x){
            int offset = x - num;
            mDriftView.offsetTopAndBottom(offset);
            num = x;
        }

        private void setUpSpac(int x){
            int offset = x - num;
            mDriftView.offsetTopAndBottom(offset);
            num = x;
        }
    }
}
