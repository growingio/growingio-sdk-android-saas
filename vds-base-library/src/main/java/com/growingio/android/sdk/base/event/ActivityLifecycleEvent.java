package com.growingio.android.sdk.base.event;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.lang.ref.WeakReference;

/**
 * author CliffLeopard
 * time   2018/7/2:下午2:35
 * email  gaoguanling@growingio.com
 */
public class ActivityLifecycleEvent {

    private WeakReference<Activity> activityWeakReference;
    private WeakReference<Bundle> bundleWeakReference;
    private WeakReference<Intent> intentWeakReference;
    public EVENT_TYPE event_type;


    public static ActivityLifecycleEvent createOnCreatedEvent(Activity activity, Bundle outState) {
        return new ActivityLifecycleEvent(activity, EVENT_TYPE.ON_CREATED, outState);
    }

    public static ActivityLifecycleEvent createOnStartedEvent(Activity activity) {
        return new ActivityLifecycleEvent(activity, EVENT_TYPE.ON_STARTED);
    }

    public static ActivityLifecycleEvent createOnResumedEvent(Activity activity) {
        return new ActivityLifecycleEvent(activity, EVENT_TYPE.ON_RESUMED);
    }

    public static ActivityLifecycleEvent createOnNewIntentEvent(Activity activity, Intent intent) {
        return new ActivityLifecycleEvent(activity, EVENT_TYPE.ON_NEW_INTENT, intent);
    }


    public static ActivityLifecycleEvent createOnPausedEvent(Activity activity) {
        return new ActivityLifecycleEvent(activity, EVENT_TYPE.ON_PAUSED);
    }


    public static ActivityLifecycleEvent createOnSaveInstanceStateEvent(Activity activity, Bundle outState) {
        return new ActivityLifecycleEvent(activity, EVENT_TYPE.ON_SAVE_INSTANCE_STATE, outState);
    }


    public static ActivityLifecycleEvent createOnStoppedEvent(Activity activity) {
        return new ActivityLifecycleEvent(activity, EVENT_TYPE.ON_STOPPED);
    }

    public static ActivityLifecycleEvent createOnDestroyedEvent(Activity activity) {
        return new ActivityLifecycleEvent(activity, EVENT_TYPE.ON_DESTROYED);
    }

    public Activity getActivity() {
        if (activityWeakReference != null)
            return activityWeakReference.get();
        else
            return null;
    }

    public Bundle getBundle() {
        if (bundleWeakReference != null)
            return bundleWeakReference.get();
        else
            return null;
    }

    public Intent getIntent() {
        if (intentWeakReference != null)
            if (intentWeakReference.get() != null) {
                return intentWeakReference.get();
            }
        Activity activity = getActivity();
        if (activity != null){
            return activity.getIntent();
        }
        return null;
    }

    private ActivityLifecycleEvent(Activity activity, EVENT_TYPE event_type) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.event_type = event_type;
    }

    private ActivityLifecycleEvent(Activity activity, EVENT_TYPE event_type, Bundle outState) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.bundleWeakReference = new WeakReference<>(outState);
        this.event_type = event_type;
    }

    private ActivityLifecycleEvent(Activity activity, EVENT_TYPE event_type, Intent intent) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.intentWeakReference = new WeakReference<>(intent);
        this.event_type = event_type;
    }

    public enum EVENT_TYPE {
        ON_CREATED,
        ON_STARTED,
        ON_RESUMED,
        ON_NEW_INTENT,
        ON_PAUSED,
        ON_SAVE_INSTANCE_STATE,
        ON_STOPPED,
        ON_DESTROYED
    }
}
