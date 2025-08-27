package com.growingio.android.sdk.debugger;

import android.content.Context;
import android.net.Uri;

import com.growingio.android.sdk.base.event.CircleEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.debugger.view.DebuggerCircleTipView;
import com.growingio.android.sdk.java_websocket.GioNonMainProcessSocketClient;
import com.growingio.android.sdk.java_websocket.GioProtocol;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.eventcenter.EventCenter;

/**
 * Created by liangdengke on 2018/9/19.
 */
public class MobileDebuggerNonMain extends AbstractSocketAdapter{
    private static final String TAG = "GIO.MobileDebuggerNonMain";

    public MobileDebuggerNonMain(DebuggerManager manager) {
        super(manager);
    }

    @Override
    public void onFirstLaunch(Uri validData) {
        super.onFirstLaunch(validData);
        EventCenter.getInstance().post(new CircleEvent("defaultListener"));
    }

    @Override
    public void onPluginReady() {
        super.onPluginReady();
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

    @Override
    protected void onConnected() {
        super.onConnected();
        mCircleTipView.doing();
    }

    @Override
    public void addTipView(Context applicationContext) {
        LogUtil.d(TAG, "addTipView");
        mCircleTipView = new DebuggerCircleTipView(applicationContext);
    }
}
