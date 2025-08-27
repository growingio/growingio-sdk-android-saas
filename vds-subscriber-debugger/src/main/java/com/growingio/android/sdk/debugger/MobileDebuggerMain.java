package com.growingio.android.sdk.debugger;

import android.content.Context;
import android.net.Uri;

import com.growingio.android.sdk.base.event.CircleEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.debugger.view.DebuggerCircleTipView;
import com.growingio.android.sdk.java_websocket.DebuggerSocketMain;
import com.growingio.android.sdk.java_websocket.GioProtocol;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.eventcenter.EventCenter;

/**
 * 唤起Mobile Debugger主进程处理器
 * data check也放在这里面了
 * Created by liangdengke on 2018/9/13.
 */
public class MobileDebuggerMain extends AbstractSocketAdapter{
    private static final String TAG = "GIO.MobileDebuggerMain";

    private String wsUrl;

    public MobileDebuggerMain(DebuggerManager manager) {
        super(manager);
    }

    @Override
    protected void onFirstMessage() {
        super.onFirstMessage();
        if (socketInterface != null){
            socketInterface.sendMessage(GioProtocol.sendDebuggerInit());
        }
    }

    @Override
    public void onFirstLaunch(Uri validData) {
        super.onFirstLaunch(validData);
        String ai = coreAppState.getProjectId();
        if (PendingStatus.isDataCheckEnable()){
            String roomNum = validData.getQueryParameter("dataCheckRoomNumber");
            String wsHostWithSchema = validData.getQueryParameter("wsHost");
            this.wsUrl = NetworkConfig.getInstance().getWsDataCheckUrl(wsHostWithSchema, ai, roomNum);
        }else{
            String circleRoomNumber = validData.getQueryParameter("circleRoomNumber");
            String wsUrlFormat = NetworkConfig.getInstance().getWSEndPointFormatter();
            this.wsUrl = String.format(wsUrlFormat, ai, circleRoomNumber);
        }
        mCircleTipView.setContent("正在准备MobileDebugger(初始化)....");
        EventCenter.getInstance().post(new CircleEvent("defaultListener"));
    }

    @Override
    public void onPluginReady() {
        super.onPluginReady();
        onConnecting();
        try {
            socketInterface = new DebuggerSocketMain(wsUrl, CoreInitialize.config().isMultiProcessEnabled());
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
        mCircleTipView = new DebuggerCircleTipView(applicationContext);
    }
}
