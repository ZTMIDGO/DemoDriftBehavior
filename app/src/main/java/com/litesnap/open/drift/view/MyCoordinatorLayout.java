package com.litesnap.open.drift.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyCoordinatorLayout extends CoordinatorLayout {
    private OnDispatchTouchEventCallback callback;

    public MyCoordinatorLayout(@NonNull Context context) {
        this(context, null);
    }

    public MyCoordinatorLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (callback != null){
            callback.onCallback(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setCallback(OnDispatchTouchEventCallback callback) {
        this.callback = callback;
    }

    public interface OnDispatchTouchEventCallback{
        void onCallback(MotionEvent event);
    }
}
