package com.growingio.android.sdk.circle;

import android.app.Activity;
import android.net.Uri;

import com.growingio.android.sdk.base.event.CircleEvent;
import com.growingio.android.sdk.base.event.CircleGotWebSnapshotNodeEvent;
import com.growingio.android.sdk.base.event.ViewTreeStatusChangeEvent;
import com.growingio.android.sdk.base.event.ViewTreeWindowFocusChangedEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.debugger.DebuggerEventListener;
import com.growingio.android.sdk.debugger.DebuggerManager;
import com.growingio.android.sdk.models.VPAEvent;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventBus;
import com.growingio.eventcenter.bus.ThreadMode;

/**
 * author CliffLeopard
 * time   2018/7/5:下午3:21
 * email  gaoguanling@growingio.com
 */
public class CircleSubscriber implements DebuggerEventListener{

    private final String TAG = "GIO.CircleSubscriber";
    private DebuggerManager debuggerManager;
    private boolean isMainProcess;

    public CircleSubscriber(DebuggerManager debuggerManager, boolean isMainProcess){
        this.debuggerManager = debuggerManager;
        this.isMainProcess = isMainProcess;
    }

    @Override
    public void onFirstLaunch(Uri validData) {
        EventCenter.getInstance().register(this);
        String source = validData == null ? null : validData.getQueryParameter("source");
        if (PendingStatus.HEAT_MAP_CIRCLE.equals(source)){
//            PendingStatus.mCircleType = PendingStatus.APP_CIRCLE;
        }
        if (isMainProcess){
            debuggerManager.login();
        }else{
            onLoginSuccess();
        }
    }

    @Override
    public void onLoginSuccess() {
        LogUtil.d(TAG, "onLoginSuccess");
        CircleManager.getInstance().launchAppCircle();
    }

    @Override
    public void onPageResume() {
        Activity activity = CoreInitialize.coreAppState().getForegroundActivity();
        if (activity != null){
            CircleManager.getInstance().onResumed(activity);
        }
    }

    @Override
    public void onPagePause() {
        CircleManager.getInstance().removeFloatViews();
    }

    @Override
    public void onExit() {
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onViewTreeChange(ViewTreeStatusChangeEvent viewTreeStatusChangeEvent) {
        CircleManager.getInstance().refreshWebCircleTasks();
    }

    @Subscribe
    public void onGlobalWindowFocusChanged(ViewTreeWindowFocusChangedEvent event) {
        LogUtil.d(TAG, "onGlobalWindowFocusChanged: refreshWebCircleTasks");
        CircleManager.getInstance().refreshWebCircleTasks();
    }

    @Subscribe(threadMode=ThreadMode.MAIN)
    public void onVPAEvent(VPAEvent vpaEvent){
        if (vpaEvent != null && "page".equals(vpaEvent.getType())){
            CircleManager manager = CircleManager.getInstance();
            manager.refreshSnapshotWithType("page", null, vpaEvent);
        }
    }

    @Subscribe
    public void onCircleEvent(CircleEvent event) {
        LogUtil.d(TAG, "onCircleEvent: ", event.type);
        switch (event.type) {
            case "defaultListener":
                CircleManager.getInstance().defaultListener();
                break;
            case "updateTagsIfNeeded":
                CircleManager.getInstance().updateTagsIfNeeded();
                break;
            default:
                LogUtil.d(TAG, "UNKnow event type: ", event.type);
        }
    }

    @Subscribe
    public void onGotSnapShotEvent(CircleGotWebSnapshotNodeEvent event){
        CircleManager.getInstance().gotWebSnapshotNodes(event.getNodes(), event.getHost(), event.getPath());
    }
}
