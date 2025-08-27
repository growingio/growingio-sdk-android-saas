package com.growingio.android.sdk.autoburry;

import android.app.Activity;
import android.graphics.Rect;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.base.event.ViewTreeDrawEvent;
import com.growingio.android.sdk.base.event.ViewTreeStatusChangeEvent;
import com.growingio.android.sdk.base.event.ViewTreeWindowFocusChangedEvent;
import com.growingio.android.sdk.collection.AbstractGrowingIO;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.collection.ImpressionMark;
import com.growingio.android.sdk.utils.ActivityUtil;
import com.growingio.android.sdk.utils.JsonUtil;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ObjectUtils;
import com.growingio.android.sdk.utils.SysTrace;
import com.growingio.android.sdk.utils.TimerToggler;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WeakSet;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventBus;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 用于监听并生成imp事件
 * <p>
 * 由OnGlobalLayoutListener触发, EventBus订阅有此类自行订阅与反订阅
 */
public class ImpObserver {
    private static final String TAG = "GIO.Imp";

    private static final String GIO_CONTENT = "gio_v";

    private final CoreAppState coreAppState;

    TimerToggler viewTreeChangeTimerToggler;
    @VisibleForTesting
    WeakHashMap<Activity, ActivityScope> mActivityScopes;

    private Set<String> mTmpGlobalIds;
    private Rect mTmpRect;

    public ImpObserver(CoreAppState coreAppState) {
        this.coreAppState = coreAppState;
    }

    private void init() {
        if (mActivityScopes != null)
            return;
        mActivityScopes = new WeakHashMap<>();
        viewTreeChangeTimerToggler = new TimerToggler.Builder(new Runnable() {
            @Override
            public void run() {
                LogUtil.d(TAG, "stampViewImp after resume or re draw, force check");
                onGlobalLayout(new ViewTreeStatusChangeEvent(ViewTreeStatusChangeEvent.StatusType.LayoutChanged));
            }
        }).delayTime(500).maxDelayTime(5000).firstTimeDelay(true).build();
        EventCenter.getInstance().register(this);
    }

    public void markViewImpression(ImpressionMark mark) {
        View view = mark.getView();
        if (view == null) {
            return;
        }

        Activity activity = ActivityUtil.findActivity(view.getContext());
        if (activity == null) {
            activity = coreAppState.getForegroundActivity();
        }
        if (activity == null) {
            LogUtil.e(TAG, "can't find the activity of view: " + view);
            return;
        }
        LogUtil.d(TAG, "stampViewImp: ", mark.getEventId());
        init();
        ActivityScope scope = mActivityScopes.get(activity);
        if (scope == null) {
            scope = new ActivityScope(activity);
            mActivityScopes.put(activity, scope);
        }

        ImpEvent event = new ImpEvent();
        event.mark = mark;
        event.activity = new WeakReference<>(activity);

        if (mark.getGlobalId() != null) {
            event = moveGlobalId(scope, view, mark, event);
            if (event == null) {
                LogUtil.d(TAG, "stampViewImp, and nothing changed, globalId: ", mark.getGlobalId());
                return;
            }
        } else if (scope.containView(view)) {
            ImpEvent impEvent = scope.getImpEvent(view);
            if (event.equals(impEvent)) {
                LogUtil.d(TAG, "stampViewImp, and nothing changed: ", mark.getEventId());
                impEvent.mark = event.mark;
                return;
            }
            stopStampViewImpInternal(scope, view);
        }
        view.setTag(AbstractGrowingIO.GROWING_IMP_TAG_MARKED, true);
        scope.getFromDelay(mark.getDelayTimeMills()).addView(view, event, scope);
        checkAndSendViewTreeChange(activity);
    }

    private ImpEvent moveGlobalId(ActivityScope scope, View view, @NonNull ImpressionMark mark, @NonNull ImpEvent impEvent) {
        if (!scope.globalIdToImpEvent.containsKey(mark.getGlobalId())) {
            // globalId对应的元素未被记录
            scope.globalIdToImpEvent.put(mark.getGlobalId(), impEvent);
            return impEvent;
        } else {
            ImpEvent globalImpEvent = scope.globalIdToImpEvent.get(mark.getGlobalId());
            View oldView = globalImpEvent.mark.getView();
            ImpEvent currentViewImpEvent = scope.getImpEvent(view);
            if (oldView != view) {
                // globalId对应不同的View
                if (currentViewImpEvent != null && currentViewImpEvent != globalImpEvent) {
                    scope.nextPassInvisible.add(currentViewImpEvent);
                }
                stopStampViewImpInternal(scope, view);
                if (oldView == null) {
                    //TODO: 应该放在可见性判断那里进行检测 oldView被GC回收, 认为上次不可见
                    globalImpEvent.lastVisible = false;
                }
            } else if (impEvent.equals(currentViewImpEvent)) {
                // globalId前后内容一致， 且 view相同
                return null;
            }
            globalImpEvent.mark = mark;
            globalImpEvent.activity = impEvent.activity;
            return globalImpEvent;
        }
    }

    private void checkAndSendViewTreeChange(Activity activity) {
        Activity resumeActivity = coreAppState.getResumedActivity();
        if (resumeActivity != null && activity == resumeActivity) {
            viewTreeChangeTimerToggler.toggle();
        }
    }

    private ImpEvent stopStampViewImpInternal(ActivityScope scope, View view) {
        view.setTag(AbstractGrowingIO.GROWING_IMP_TAG_MARKED, false);
        TogglerWithViews togglerWithViews = scope.viewToTogglerWithViews.get(view);
        if (togglerWithViews != null) {
            ImpEvent result = togglerWithViews.getViewImpEvent(view);
            togglerWithViews.removeView(view, scope);
            return result;
        }
        return null;
    }


    public void stopStampViewImp(View view) {
        ActivityScope scope = findActivityScopeByView(view);
        if (scope == null) {
            return;
        }
        stopStampViewImpInternal(scope, view);
    }

    private void removeGlobalId(ActivityScope scope, ImpEvent impEvent) {
        if (impEvent != null && impEvent.mark.getGlobalId() != null) {
            scope.globalIdToImpEvent.remove(impEvent.mark.getGlobalId());
        }
    }

    // only used by stopStampViewImp
    private ActivityScope findActivityScopeByView(View view) {
        if (mActivityScopes == null || view == null)
            return null;
        Activity activity = ActivityUtil.findActivity(view.getContext());
        if (activity != null) {
            return mActivityScopes.get(activity);
        }
        for (ActivityScope scope : mActivityScopes.values()) {
            if (scope.viewToTogglerWithViews.containsKey(view)) {
                return scope;
            }
        }
        return null;
    }

    @Subscribe
    public void onGlobalWindowFocusChanged(ViewTreeWindowFocusChangedEvent event) {
        onGlobalLayout(null);
    }

    @Subscribe
    public void onGlobalLayout(ViewTreeStatusChangeEvent event) {
        Activity current = coreAppState.getResumedActivity();
        if (current != null) {
            layoutActivity(current);
        }
    }

    @Subscribe
    public void onGlobalDraw(ViewTreeDrawEvent event) {
        if (viewTreeChangeTimerToggler != null) {
            viewTreeChangeTimerToggler.toggle();
        }
    }

    private void layoutActivity(Activity current) {
        if (mActivityScopes == null)
            return;
        ActivityScope scope = mActivityScopes.get(current);
        if (scope == null) {
            return;
        }
        for (int i = scope.togglerWithViewsList.size() - 1; i >= 0; i--) {
            if (i >= scope.togglerWithViewsList.size()) {
                continue;
            }
            TogglerWithViews toggler = scope.togglerWithViewsList.get(i);
            toggler.toggle();
        }
    }

    @Subscribe
    public void onActivityLifecycle(ActivityLifecycleEvent event) {
        if (event.event_type == ActivityLifecycleEvent.EVENT_TYPE.ON_DESTROYED) {
            mActivityScopes.remove(event.getActivity());
            if (mActivityScopes.isEmpty()) {
                EventBus.getDefault().unregister(this);
                viewTreeChangeTimerToggler.reset();
                mActivityScopes = null;
            }
        } else if (event.event_type == ActivityLifecycleEvent.EVENT_TYPE.ON_RESUMED) {
            ActivityScope scope = mActivityScopes.get(event.getActivity());
            if (scope != null) {
                for (View view : scope.viewToTogglerWithViews.keySet()) {
                    ImpEvent impEvent = scope.getImpEvent(view);
                    if (impEvent != null) {
                        impEvent.lastVisible = false;
                    }
                }
                layoutActivity(event.getActivity());
            }
        }
    }

    private void saveImpEvent(ImpEvent impEvent) {
        JSONObject variable = impEvent.mark.getVariable();
        if (impEvent.mark.isCollectContent()) {
            String content = Util.getViewContent(impEvent.mark.getView(), null);
            if (!TextUtils.isEmpty(content)) {
                if (variable == null) {
                    variable = new JSONObject();
                }
                if (!variable.has(GIO_CONTENT)) {
                    try {
                        variable.put(GIO_CONTENT, content);
                    } catch (JSONException e) {
                        LogUtil.e(TAG, e.getMessage(), e);
                    }
                }
            }
        }
        if (variable != null) {
            if (impEvent.mark.getNum() == null) {
                GrowingIO.getInstance().track(impEvent.mark.getEventId(), variable);
            } else {
                GrowingIO.getInstance().track(impEvent.mark.getEventId(), impEvent.mark.getNum(), variable);
            }
        } else {
            if (impEvent.mark.getNum() == null) {
                GrowingIO.getInstance().track(impEvent.mark.getEventId());
            } else {
                GrowingIO.getInstance().track(impEvent.mark.getEventId(), impEvent.mark.getNum());
            }
        }
    }

    void checkImp(TogglerWithViews togglerWithViews, WeakHashMap<View, ImpEvent> impViews) {
        Activity current = coreAppState.getResumedActivity();
        if (current == null || mActivityScopes == null || !mActivityScopes.containsKey(current)) {
            return;
        }
        ActivityScope scope = mActivityScopes.get(current);
        if (scope == null) return;
        if (impViews == null || impViews.isEmpty()) {
            scope.togglerWithViewsList.remove(togglerWithViews);
            return;
        }
        // LogUtil.d(TAG, "checkImp");
        List<View> mTmpViewCache = new ArrayList<>();
        try {
            removeOutDateGlobalId(scope);
            viewTreeChangeTimerToggler.reset();

            // when api < 24, 即23(6.0)及以下版本可能出现该问题， 发生gc时key被回收遍历操作，get的poll操作导致modCount改变
            // 如果gc正好出现在循环内，则会出现该异常
            for (View view : impViews.keySet()) {
                ImpEvent event = impViews.get(view);
                if (event == null)
                    // impossible
                    continue;
                boolean lastVisible = event.lastVisible;
                boolean currentVisible = checkViewVisibility(event.mark);
                if (event.mark.getView() != view) {
                    // current impEvent's view not current view, may be globalId changed
                    mTmpViewCache.add(view);
                    LogUtil.e(TAG, "event's view is not same with current view, maybe globalId changed");
                    continue;
                }
                if (currentVisible && !lastVisible) {
                    saveImpEvent(event);
                }
                event.lastVisible = currentVisible;
            }
            for (ImpEvent impEvent : scope.nextPassInvisible) {
                if (!scope.containView(impEvent.mark.getView())) {
                    impEvent.lastVisible = false;
                }
            }
        } catch (ConcurrentModificationException ignore) {
        }
        scope.nextPassInvisible.clear();
        for (View view : mTmpViewCache) {
            stopStampViewImpInternal(scope, view);
        }
    }

    boolean checkViewVisibility(ImpressionMark mark) {
        View view = mark.getView();
        if (ViewHelper.viewVisibilityInParents(view)) {
            if (mark.getVisibleScale() == 0) {
                // 任意像素可见均被认为有效曝光
                return true;
            }

            if (mTmpRect == null) {
                mTmpRect = new Rect();
            }
            view.getLocalVisibleRect(mTmpRect);
            return mTmpRect.right * mTmpRect.bottom >= view.getMeasuredHeight() * view.getMeasuredWidth() * mark.getVisibleScale();
        }
        return false;
    }

    private void removeOutDateGlobalId(ActivityScope scope) {
        if (mTmpGlobalIds == null) {
            mTmpGlobalIds = new HashSet<>();
        }
        mTmpGlobalIds.clear();
        for (String globalId : scope.globalIdToImpEvent.keySet()) {
            ImpEvent impEvent = scope.globalIdToImpEvent.get(globalId);
            View currentView = impEvent.mark.getView();
            if (currentView == null) {
                mTmpGlobalIds.add(globalId);
            } else {
                ImpEvent viewTargetImpEvent = scope.getImpEvent(currentView);
                if (viewTargetImpEvent != impEvent) {
                    mTmpGlobalIds.add(globalId);
                }
            }
        }
        if (mTmpGlobalIds.size() != 0) {
            for (String globalKey : mTmpGlobalIds) {
                scope.globalIdToImpEvent.remove(globalKey);
            }
        }
    }

    static class TogglerWithViews implements Runnable {
        TimerToggler timerToggler;
        WeakHashMap<View, ImpEvent> impViews;
        long delayTime;
        ImpObserver impObserver;

        public TogglerWithViews(long delayTime) {
            impViews = new WeakHashMap<>();
            timerToggler = new TimerToggler.Builder(this)
                    .maxDelayTime(2000)
                    .delayTime(delayTime)
                    .build();
            this.delayTime = delayTime;
            impObserver = AutoBuryObservableInitialize.impObserver();
        }

        public void addView(View view, ImpEvent impEvent, ActivityScope scope) {
            impViews.put(view, impEvent);
            scope.viewToTogglerWithViews.put(view, this);
        }

        public void removeView(View view, ActivityScope scope) {
            if (impViews != null) {
                impViews.remove(view);
            }
            scope.viewToTogglerWithViews.remove(view);
        }

        public ImpEvent getViewImpEvent(View view) {
            return impViews == null ? null : impViews.get(view);
        }

        public void toggle() {
            timerToggler.toggle();
        }

        @Override
        public void run() {
            try {
                SysTrace.beginSection("gio.imp");
                impObserver.checkImp(this, impViews);
            } finally {
                SysTrace.endSection();
            }
        }
    }

    static class ActivityScope {
        final WeakReference<Activity> activity;
        final List<TogglerWithViews> togglerWithViewsList;
        final HashMap<String, ImpEvent> globalIdToImpEvent;
        final WeakHashMap<View, TogglerWithViews> viewToTogglerWithViews;
        final WeakSet<ImpEvent> nextPassInvisible = new WeakSet<>();

        public ActivityScope(Activity activity) {
            this.activity = new WeakReference<>(activity);
            togglerWithViewsList = new ArrayList<>();
            viewToTogglerWithViews = new WeakHashMap<>();
            globalIdToImpEvent = new HashMap<>();
        }

        public boolean containView(View view) {
            return viewToTogglerWithViews.containsKey(view);
        }

        public ImpEvent getImpEvent(View view) {
            TogglerWithViews togglerWithViews = viewToTogglerWithViews.get(view);
            if (togglerWithViews != null) {
                return togglerWithViews.getViewImpEvent(view);
            }
            return null;
        }

        public TogglerWithViews getFromDelay(long delayTime) {
            for (TogglerWithViews togglerWithViews : togglerWithViewsList) {
                if (togglerWithViews.delayTime == delayTime) {
                    return togglerWithViews;
                }
            }
            TogglerWithViews withViews = new TogglerWithViews(delayTime);
            togglerWithViewsList.add(withViews);
            return withViews;
        }
    }

    static class ImpEvent {
        ImpressionMark mark;
        boolean lastVisible;
        WeakReference<Activity> activity;


        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ImpEvent))
                return false;
            ImpEvent other = (ImpEvent) obj;
            if (!ObjectUtils.equals(mark.getEventId(), other.mark.getEventId())
                    || !ObjectUtils.equals(mark.getGlobalId(), other.mark.getGlobalId())
                    || !(ObjectUtils.equals(mark.getNum(), other.mark.getNum()))
                    || mark.getDelayTimeMills() != other.mark.getDelayTimeMills()
                    || !JsonUtil.equal(mark.getVariable(), other.mark.getVariable())) {
                return false;
            }

            return true;
        }
    }
}
