package com.growingio.android.sdk.burry;

import android.support.annotation.NonNull;

import com.growingio.android.sdk.base.event.InitializeSDKEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.bus.EventBus;
import com.growingio.eventcenter.bus.ThreadMode;

/**
 * author CliffLeopard
 * time   2018/7/3:下午2:14
 * email  gaoguanling@growingio.com
 */
public class BuryObservableInitialize {
    private static BuryMessageProcessor buryMessageProcessor;

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = false, priority = 0)
    public static void onSDKInitialize(InitializeSDKEvent event) {
        buryMessageProcessor = new BuryMessageProcessor(CoreInitialize.messageProcessor());
    }

    @NonNull
    public static BuryMessageProcessor buryMessageProcessor(){
        return buryMessageProcessor;
    }
}
