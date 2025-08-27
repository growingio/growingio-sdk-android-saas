package com.growingio.android.sdk.debugger;

import android.content.Context;
import android.net.Uri;

import com.growingio.android.sdk.base.event.SocketEvent;
import com.growingio.android.sdk.base.event.SocketStatusEvent;
import com.growingio.android.sdk.base.event.net.NetWorkChangedEvent;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.DeviceUUIDFactory;
import com.growingio.android.sdk.debugger.event.ExitAndKillAppEvent;
import com.growingio.android.sdk.debugger.view.CircleTipView;
import com.growingio.android.sdk.interfaces.SocketInterface;
import com.growingio.android.sdk.ipc.GrowingIOIPC;
import com.growingio.android.sdk.java_websocket.GioProtocol;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.NetworkUtil;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventBus;
import com.growingio.eventcenter.bus.ThreadMode;

import org.json.JSONObject;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 适用于WebSocket相关联的业务处理
 * - Web圈选
 * - MobileDebugger
 * Created by liangdengke on 2018/9/13.
 */
public class AbstractSocketAdapter implements DebuggerEventListener {
    private static final String TAG = "GIO.AbstractSocketAdapter";

    protected CircleTipView mCircleTipView;
    protected CoreAppState coreAppState;
    protected DeviceUUIDFactory deviceUUIDFactory;
    protected DebuggerManager debuggerManager;
    protected SocketInterface socketInterface;
    protected GrowingIOIPC growingIOIPC;
    private DebuggerEventListener circleManager;

    private volatile boolean isConnected = false;
    private Queue<String> collectionMessage = new ConcurrentLinkedQueue<>();

    public AbstractSocketAdapter(DebuggerManager manager) {
        debuggerManager = manager;
        inject();
    }

    void inject() {
        coreAppState = CoreInitialize.coreAppState();
        deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
        growingIOIPC = CoreInitialize.growingIOIPC();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onSocketEvent(SocketEvent socketEvent) {
        if (socketInterface == null) {
            LogUtil.d(TAG, "onSocketEvent, but not ready, return");
            return;
        }
        if (socketEvent.type == SocketEvent.EVENT_TYPE.SEND_DEBUGGER) {
            sendMessage(GioProtocol.sendDebuggerStr(socketEvent.debuggerJson));
        } else if (socketEvent.type == SocketEvent.EVENT_TYPE.SEND) {
            sendMessage(socketEvent.message);
        } else if (socketEvent.type == SocketEvent.EVENT_TYPE.SCREEN_UPDATE) {
            ThreadUtils.cancelTaskOnUiThread(screenUpdateRunnable);
            ThreadUtils.postOnUiThreadDelayed(screenUpdateRunnable, 1000L);
        }
    }

    private Runnable screenUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            sendMessage(GioProtocol.sendScreenUpdate());
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNetChanged(NetWorkChangedEvent event) {
        if (!event.isConnected()) {
            LogUtil.e(TAG, "network disconnected");
            onSocketErrorCallback();
        } else if (NetworkUtil.getWifiIp(coreAppState.getGlobalContext()) == null && !(this instanceof MobileDebuggerMain)) {
            LogUtil.e(TAG, "not wifi connected");
            onSocketErrorCallback();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketStatusEvent(SocketStatusEvent socketStatusEvent) {
        switch (socketStatusEvent.event_type) {
            case EDITOR_READY:
                onConnected();
                break;
            case EDITOR_QUIT:
                debuggerManager.exit();
                break;
            case REMOTE_CLOSE:
            case ERROR:
                onSocketErrorCallback();
                break;
            case SERVER_STARTED:
                onServerStarted();
                break;
            case CLIENT_QUIT:
                EventCenter.getInstance().post(new ExitAndKillAppEvent());
                break;
            case HYBRID_MESSAGE:
                onHybridMessageFromWeb((JSONObject) socketStatusEvent.obj);
                break;
        }
    }

    protected void onHybridMessageFromWeb(JSONObject jsonObject) {
        LogUtil.d(TAG, "onHybridMessageFromWeb: ", jsonObject);
    }

    private void onServerStarted() {
        int port = getServerPort();
        String wifiIp = NetworkUtil.getWifiIp(coreAppState.getGlobalContext());
        String wsUrl = "ws://" + wifiIp + ":" + port;
        LogUtil.d(TAG, "server started, and wsUrl: " + wsUrl);
        CoreInitialize.growingIOIPC().setWsServerUrl("ws://127.0.0.1:" + port);
        onServerStarted(wsUrl);
    }

    // 当手机自有的WebSocketServer启动时
    protected void onServerStarted(String wsUrl) {

    }

    // 多进程时， 返回手机自己启动的WebSocketServer的ws地址
    protected int getServerPort() {
        return socketInterface.getPort();
    }

    private void onSocketDisconnectCallback() {
        debuggerManager.exit();
    }

    private void onSocketErrorCallback() {
        LogUtil.e(TAG, "设备已断开连接，something wrong,重扫");
        if (mCircleTipView != null) {
            mCircleTipView.setError(true);
        }
    }

    @Override
    public void onFirstLaunch(Uri validData) {
        EventCenter.getInstance().register(this);
        circleManager = debuggerManager.getDebuggerEventListenerByType(PendingStatus.APP_CIRCLE);
        if (circleManager != null) {
            EventCenter.getInstance().register(circleManager);
        }
        addTipView(coreAppState.getGlobalContext());
        if (coreAppState.getResumedActivity() != null) {
            mCircleTipView.show();
        }
        // 错开一个时间差
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                onPluginReady();
            }
        });
    }

    @Override
    public void onLoginSuccess() {

    }

    @Override
    public void onPageResume() {
        if (mCircleTipView != null) {
            mCircleTipView.show();
        }
    }

    @Override
    public void onPagePause() {
        if (mCircleTipView != null) {
            mCircleTipView.remove();
        }
    }

    @Override
    public void onExit() {
        EventBus.getDefault().unregister(this);
//        Toast.makeText(coreAppState.getGlobalContext(), "断开连接...", Toast.LENGTH_SHORT).show();
        if (mCircleTipView != null) {
            if (mCircleTipView.isShown()) {
                mCircleTipView.remove();
            }
            mCircleTipView = null;
        }
        if (socketInterface != null) {
            socketInterface.stopAsync();
            socketInterface = null;
        }
        if (circleManager != null) {
            EventBus.getDefault().unregister(circleManager);
            circleManager = null;
        }
    }

    public final void sendMessage(String message) {
        if (isConnected) {
            sendMessageInternal(message);
        } else {
            LogUtil.d(TAG, "not connected, and collection: ", message);
            collectionMessage.add(message);
        }
    }

    private void sendMessageInternal(String content) {
        synchronized (collectionMessage) {
            sendMessageLock(content);
        }
    }

    protected void sendMessageLock(String content) {
        if (socketInterface != null && socketInterface.isReady())
            socketInterface.sendMessage(content);
    }

    // UIThread
    public void onPluginReady() {
        if (mCircleTipView != null)
            mCircleTipView.setContent("正在建立连接....");
        LogUtil.d(TAG, "onPluginReady");
    }

    protected void onConnecting() {
        // ignore
    }

    protected void onConnectFailed() {
        LogUtil.d(TAG, "onConnectFailed");
        if (mCircleTipView != null) {
            mCircleTipView.setError(true);
            mCircleTipView.setContent("ERROR: 建立连接失败");
        }
    }

    protected void onConnected() {
        LogUtil.d(TAG, "onConnected");
        if (mCircleTipView != null) {
            mCircleTipView.setError(false);
        }
        new Thread() {
            @Override
            public void run() {
                synchronized (collectionMessage) {
                    isConnected = true;
                    onFirstMessage();
                    for (String message : collectionMessage) {
                        sendMessageInternal(message);
                    }
                    collectionMessage.clear();
                }
            }
        }.start();
    }

    protected void onFirstMessage() {
        LogUtil.d(TAG, "onFirstMessage");
    }

    protected void onLoadFailed() {
        LogUtil.d(TAG, "onLoadFailed");
        onConnectFailed();
    }

    public void addTipView(Context applicationContext) {
        // 不要删， 这是给Android 4.4准备的
    }
}
