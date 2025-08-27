package com.growingio.android.sdk.heatmap;

import com.growingio.android.sdk.base.event.BgInitializeSDKEvent;
import com.growingio.android.sdk.base.event.InitializeSDKEvent;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.ThreadMode;

/**
 * author CliffLeopard
 * time   2018/7/3:下午12:12
 * email  gaoguanling@growingio.com
 */
public class HeatMapSubscriberInitialize {

    @Subscribe
    public static void onSDKInitialize(BgInitializeSDKEvent event) {
        HeatMapSubscriber subscriber = new HeatMapSubscriber();
        EventCenter.getInstance().register(subscriber);
    }
}
