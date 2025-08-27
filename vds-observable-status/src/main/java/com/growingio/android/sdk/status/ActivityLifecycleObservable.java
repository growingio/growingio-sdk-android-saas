package com.growingio.android.sdk.status;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.View;

import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.collection.AbstractGrowingIO;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.utils.SysTrace;
import com.growingio.eventcenter.EventCenter;

/**
 * author CliffLeopard
 * time   2018/7/2:下午2:21
 * email  gaoguanling@growingio.com
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ActivityLifecycleObservable implements Application.ActivityLifecycleCallbacks {


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        SysTrace.beginSection("gio.ActivityOnCreate");
        EventCenter.getInstance().post( ActivityLifecycleEvent.createOnCreatedEvent(activity,savedInstanceState));
        SysTrace.endSection();
    }

    @Override
    public void onActivityStarted(Activity activity) {
        SysTrace.beginSection("gio.onActivityStart");
        EventCenter.getInstance().post(ActivityLifecycleEvent.createOnStartedEvent(activity));
        SysTrace.endSection();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        SysTrace.beginSection("gio.onActivityResumed");
        monitorViewTreeChange(activity.getWindow().getDecorView());
        EventCenter.getInstance().post(ActivityLifecycleEvent.createOnResumedEvent(activity));
        SysTrace.endSection();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onActivityPaused(Activity activity) {
        SysTrace.beginSection("gio.onActivityPaused");
        unRegisterViewTreeChange(activity.getWindow().getDecorView());
        EventCenter.getInstance().post(ActivityLifecycleEvent.createOnPausedEvent(activity));
        SysTrace.endSection();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        EventCenter.getInstance().post(ActivityLifecycleEvent.createOnStoppedEvent(activity));
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        EventCenter.getInstance().post(ActivityLifecycleEvent.createOnSaveInstanceStateEvent(activity, outState));
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        EventCenter.getInstance().post(ActivityLifecycleEvent.createOnDestroyedEvent(activity));
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void unRegisterViewTreeChange(View root){
        if (root.getTag(AbstractGrowingIO.GROWING_MONITORING_VIEWTREE_KEY) != null){
            ViewTreeStatusObservable observable = ViewTreeStatusObservable.getInstance();
            root.getViewTreeObserver().removeOnGlobalLayoutListener(observable);
            root.getViewTreeObserver().removeOnGlobalFocusChangeListener(observable);
            root.getViewTreeObserver().removeOnScrollChangedListener(observable);
            root.getViewTreeObserver().removeOnDrawListener(observable);
            root.setTag(AbstractGrowingIO.GROWING_MONITORING_VIEWTREE_KEY, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                root.getViewTreeObserver().removeOnWindowFocusChangeListener(ViewTreeStatusObservable.FocusListener.getInstance());
            }
        }
    }

    private void monitorViewTreeChange(View root) {
        if (root.getTag(AbstractGrowingIO.GROWING_MONITORING_VIEWTREE_KEY) == null) {
            root.getViewTreeObserver().addOnGlobalLayoutListener(ViewTreeStatusObservable.getInstance());
            root.getViewTreeObserver().addOnScrollChangedListener(ViewTreeStatusObservable.getInstance());
            root.getViewTreeObserver().addOnGlobalFocusChangeListener(ViewTreeStatusObservable.getInstance());
            root.getViewTreeObserver().addOnDrawListener(ViewTreeStatusObservable.getInstance());
            root.setTag(AbstractGrowingIO.GROWING_MONITORING_VIEWTREE_KEY, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                root.getViewTreeObserver().addOnWindowFocusChangeListener(ViewTreeStatusObservable.FocusListener.getInstance());
            }
        }
    }
}
