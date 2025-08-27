package com.growingio.android.sdk.circle;

import com.growingio.android.sdk.base.event.BgInitializeSDKEvent;
import com.growingio.android.sdk.base.event.InitializeSDKEvent;
import com.growingio.android.sdk.circle.webcircle.WebCircleMain;
import com.growingio.android.sdk.circle.webcircle.WebCircleNonMain;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.debugger.DebuggerInitialize;
import com.growingio.android.sdk.debugger.DebuggerManager;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.bus.ThreadMode;

/**
 * author CliffLeopard
 * time   2018/7/3:下午12:10
 * email  gaoguanling@growingio.com
 */
public class CircleSubscriberInitialize {
    @Subscribe(priority = 100)
    public static void onSDKInitialize(BgInitializeSDKEvent event) {
        DebuggerManager manager = DebuggerInitialize.debuggerManager();
        CircleSubscriber subscriber = new CircleSubscriber(manager, true);
        // just for defaultListener and action type
        manager.registerDebuggerEventListener(PendingStatus.APP_CIRCLE, subscriber);
        manager.registerDebuggerEventListener("app-circle-main", subscriber);

        WebCircleMain circleMain = new WebCircleMain(manager);
        manager.registerDebuggerEventListener("web-circle-main", circleMain);

        GConfig config = CoreInitialize.config();
        if (config.isMultiProcessEnabled()){
            CircleSubscriber nonMainSubscriber = new CircleSubscriber(manager, false);
            manager.registerDebuggerEventListener("app-circle-non-main", nonMainSubscriber);

            WebCircleNonMain nonMainWebCircle = new WebCircleNonMain(manager);
            manager.registerDebuggerEventListener("web-circle-non-main", nonMainWebCircle);
        }
    }
}
