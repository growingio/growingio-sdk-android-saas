package com.growingio.android.sdk.circle.webcircle;

import android.content.Context;
import android.net.Uri;

import com.growingio.android.sdk.base.event.CircleEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.debugger.DebuggerManager;
import com.growingio.android.sdk.debugger.view.WebCircleTipView;
import com.growingio.android.sdk.java_websocket.GioNonMainProcessSocketClient;
import com.growingio.android.sdk.java_websocket.GioProtocol;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.eventcenter.EventCenter;

/**
 * 非主进程的Web圈选Socket收发
 * Created by liangdengke on 2018/9/19.
 */
public class WebCircleNonMain extends WebCircleMain{
    private static final String TAG = "GIO.WebCircleNonMain";

    public WebCircleNonMain(DebuggerManager manager) {
        super(manager);
    }

    @Override
    public void addTipView(Context applicationContext) {
        mCircleTipView = new WebCircleTipView(applicationContext);
    }

    @Override
    protected void onServerStarted(String ws) {
        // ignore
    }

    @Override
    protected void onWebCircleFirstLaunch(Uri validData) {
        EventCenter.getInstance().post(new CircleEvent("defaultListener"));
    }

    @Override
    protected void onWebCirclePluginReady() {
        String wsUrl = CoreInitialize.growingIOIPC().getWsServerUrl();
        LogUtil.d(TAG, "onPluginReady, and wsUrl is ", wsUrl);
        try {
            onConnecting();
            socketInterface = new GioNonMainProcessSocketClient(wsUrl);
            socketInterface.setGioProtocol(new GioProtocol());
            socketInterface.start();
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage(), e);
            onLoadFailed();
        }
    }
}
