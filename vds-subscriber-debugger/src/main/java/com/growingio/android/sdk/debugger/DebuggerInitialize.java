package com.growingio.android.sdk.debugger;

import com.growingio.android.sdk.base.event.BgInitializeSDKEvent;
import com.growingio.android.sdk.base.event.InitializeSDKEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;

/**
 * 监听外部来的事件
 * Created by liangdengke on 2018/8/8.
 */
public class DebuggerInitialize {
    private static final String TAG = "GIO.DebuggerIni";

    private static DebuggerManager debuggerManager;

    @Subscribe(priority = 500)
    public static void onSDKInit(BgInitializeSDKEvent event){
        debuggerManager = new DebuggerManager(CoreInitialize.coreAppState());
        EventCenter.getInstance().register(debuggerManager);
        MobileDebuggerMain main = new MobileDebuggerMain(debuggerManager);
        debuggerManager.registerDebuggerEventListener("mobile-debugger-main", main);
        MobileDebuggerNonMain nonMain = new MobileDebuggerNonMain(debuggerManager);
        debuggerManager.registerDebuggerEventListener("mobile-debugger-non-main", nonMain);
    }

    public static DebuggerManager debuggerManager(){
        return debuggerManager;
    }
}
