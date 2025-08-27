package com.growingio.android.sdk.collection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.growingio.android.sdk.base.event.BgInitializeSDKEvent;
import com.growingio.android.sdk.base.event.InitializeSDKEvent;
import com.growingio.android.sdk.deeplink.DeeplinkManager;
import com.growingio.android.sdk.ipc.GrowingIOIPC;
import com.growingio.android.sdk.utils.PersistUtil;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;

/**
 * 为了初始化核心的SDK
 * Created by liangdengke on 2018/7/4.
 */
public class CoreInitialize {

    @SuppressLint("StaticFieldLeak")
    private static CoreAppState coreAppState;
    private static GConfig gConfig;
    private static DeviceUUIDFactory deviceUUIDFactory;
    private static MessageProcessor messageProcessor;
    private static DeeplinkManager deeplinkManager;
    private static SessionManager sessionManager;

    private static GrowingIOIPC growingIOIPC;

    @Subscribe(priority = 2000)
    public static void initialize(InitializeSDKEvent event) {
        Configuration configuration = event.getConfiguration();
        Context context = event.getApplication();
        deviceUUIDFactory = new DeviceUUIDFactory(context, configuration);
        gConfig = new GConfig(configuration);
        growingIOIPC = new GrowingIOIPC();
        growingIOIPC.init(event.getApplication(), gConfig);
        coreAppState = new CoreAppState(gConfig, event.getApplication());
        coreAppState.setSPN(TextUtils.isEmpty(configuration.packageName) ? event.getApplication().getPackageName() : configuration.packageName);
        coreAppState.setGrowingIOIPC(growingIOIPC);

        deviceUUIDFactory.setCoreAppState(coreAppState);

        messageProcessor = new MessageProcessor(gConfig, coreAppState);
        coreAppState.setMsgProcessor(messageProcessor);

        sessionManager = new SessionManager(messageProcessor, growingIOIPC, gConfig);
        coreAppState.setSessionManager(sessionManager);
    }

    static boolean checkInitializeSuccessfully() {
        return gConfig != null &&
                growingIOIPC != null &&
                coreAppState != null &&
                deviceUUIDFactory != null &&
                messageProcessor != null &&
                sessionManager != null;
    }

    @Subscribe
    public static void onBgInit(BgInitializeSDKEvent event) {
        deeplinkManager = new DeeplinkManager(gConfig, event.application);
        gConfig.onBgInit(deviceUUIDFactory);
        PersistUtil.init(coreAppState.getGlobalContext());
        EventCenter.getInstance().register(coreAppState);
        EventCenter.getInstance().register(messageProcessor);
        EventCenter.getInstance().register(deeplinkManager);
        EventCenter.getInstance().register(sessionManager);
    }

    @NonNull
    public static GrowingIOIPC growingIOIPC() {
        return growingIOIPC;
    }

    @NonNull
    public static DeeplinkManager deeplinkManager() {
        return deeplinkManager;
    }

    @NonNull
    public static CoreAppState coreAppState() {
        return coreAppState;
    }

    @NonNull
    public static GConfig config() {
        return gConfig;
    }

    @NonNull
    public static DeviceUUIDFactory deviceUUIDFactory() {
        return deviceUUIDFactory;
    }

    @NonNull
    public static MessageProcessor messageProcessor() {
        return messageProcessor;
    }

    @NonNull
    public static SessionManager sessionManager() {
        return sessionManager;
    }
}
