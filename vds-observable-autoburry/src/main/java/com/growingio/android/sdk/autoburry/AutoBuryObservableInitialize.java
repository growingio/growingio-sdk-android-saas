package com.growingio.android.sdk.autoburry;

import android.support.annotation.NonNull;

import com.growingio.android.sdk.autoburry.page.PageObserver;
import com.growingio.android.sdk.base.event.BgInitializeSDKEvent;
import com.growingio.android.sdk.base.event.InitializeSDKEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;

/**
 * 另外单例在业务中是需要的， 但是不希望在代码编写的时候直接调用单例， 因为这样很难测试解耦 (主要是很难mock)
 * 将所有的单例集中到此类中， 作为一个工厂类。
 * author CliffLeopard
 * time   2018/7/3:下午2:07
 * email  gaoguanling@growingio.com
 */
public class AutoBuryObservableInitialize {

    private volatile static AutoBuryAppState autoBuryAppState;
    private volatile static AutoBuryMessageProcessor autoBuryMessageProcessor;
    private volatile static NotificationProcessor notificationProcessor;
    private volatile static ImpObserver impObserver;

    @Subscribe(priority = 1000)
    public static void onSDKInitialize(InitializeSDKEvent event) {
        GConfig config = CoreInitialize.config();
        autoBuryAppState = new AutoBuryAppState(CoreInitialize.coreAppState(), config);
        autoBuryMessageProcessor = new AutoBuryMessageProcessor(config, CoreInitialize.coreAppState(),
                autoBuryAppState, CoreInitialize.messageProcessor());
        autoBuryAppState.autoBuryMessageProcessor = autoBuryMessageProcessor;
        notificationProcessor = new NotificationProcessor(event.getApplication(), CoreInitialize.coreAppState());
        if (!event.getConfiguration().isEnableNotificationTrack()){
            notificationProcessor.enable = false;
        }
        impObserver = new ImpObserver(CoreInitialize.coreAppState());
    }

    @Subscribe
    public static void onBgInitialize(BgInitializeSDKEvent event){
        PageObserver pageObserver = new PageObserver(CoreInitialize.coreAppState(), autoBuryAppState);
        EventCenter.getInstance().register(autoBuryMessageProcessor);
        EventCenter.getInstance().register(autoBuryAppState);
        EventCenter.getInstance().register(pageObserver);
        if (notificationProcessor.isEnable()){
            EventCenter.getInstance().register(notificationProcessor);
        }
    }

    @NonNull
    public static AutoBuryAppState autoBuryAppState(){
        return autoBuryAppState;
    }

    public static ImpObserver impObserver(){
        return impObserver;
    }

    @NonNull
    public static NotificationProcessor notificationProcessor(){
        return notificationProcessor;
    }

    @NonNull
    public static AutoBuryMessageProcessor autoBuryMessageProcessor(){
        return autoBuryMessageProcessor;
    }
}
