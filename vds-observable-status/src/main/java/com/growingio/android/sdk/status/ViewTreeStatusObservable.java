package com.growingio.android.sdk.status;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.ViewTreeObserver;

import com.growingio.android.sdk.base.event.ViewTreeDrawEvent;
import com.growingio.android.sdk.base.event.ViewTreeStatusChangeEvent;
import com.growingio.android.sdk.base.event.ViewTreeWindowFocusChangedEvent;
import com.growingio.eventcenter.bus.EventBus;

/**
 * author CliffLeopard
 * time   2018/7/3:下午3:43
 * email  gaoguanling@growingio.com
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class ViewTreeStatusObservable implements ViewTreeObserver.OnGlobalLayoutListener,
        ViewTreeObserver.OnScrollChangedListener,
        ViewTreeObserver.OnGlobalFocusChangeListener,
        ViewTreeObserver.OnDrawListener{

    public static volatile ViewTreeStatusObservable viewTreeStatusObservable;
    public static ViewTreeStatusObservable getInstance(){
        if(viewTreeStatusObservable == null)
        {
            synchronized (ViewTreeStatusObservable.class){
                if(viewTreeStatusObservable == null)
                    viewTreeStatusObservable = new ViewTreeStatusObservable();
            }
        }
        return  viewTreeStatusObservable;
    }

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        EventBus.getDefault().post(new ViewTreeStatusChangeEvent(ViewTreeStatusChangeEvent.StatusType.FocusChanged,oldFocus,newFocus));
    }

    @Override
    public void onGlobalLayout() {
        EventBus.getDefault().post(new ViewTreeStatusChangeEvent(ViewTreeStatusChangeEvent.StatusType.LayoutChanged));
    }

    @Override
    public void onScrollChanged() {
        EventBus.getDefault().post(new ViewTreeStatusChangeEvent(ViewTreeStatusChangeEvent.StatusType.ScrollChanged));
    }

    @Override
    public void onDraw() {
        EventBus.getDefault().post(new ViewTreeDrawEvent());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static class FocusListener implements ViewTreeObserver.OnWindowFocusChangeListener{

        private static FocusListener instance = null;

        public static FocusListener getInstance(){
            if (instance == null){
                instance = new FocusListener();
            }
            return instance;
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            EventBus.getDefault().post(new ViewTreeWindowFocusChangedEvent());
        }
    }
}
