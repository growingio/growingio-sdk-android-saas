package com.growingio.android.sdk.status;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;

import com.growingio.android.sdk.base.event.InitializeSDKEvent;
import com.growingio.android.sdk.utils.ContextUtil;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.bus.ThreadMode;

/**
 * author CliffLeopard
 * time   2018/7/2:下午2:53
 * email  gaoguanling@growingio.com
 *
 * 此组件作用在于监听Activity的生命周期，View结构变更，网络状变更等信息，将这些变更信息以事件的形式发送出去，供其他组件监听。
 */
public class StatusObservableInitialize {

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = false, priority = 0)
    public static void onSDKInitialize(InitializeSDKEvent event) {

        Application application = event.getApplication();
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleObservable());
        broadcastRegister(application);
        application.registerComponentCallbacks(new LowMemoryObservable());
    }

    private static  void  broadcastRegister(Application application){
        BroadcastObservable observable = new BroadcastObservable();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        ContextUtil.registerReceiver(application, observable, intentFilter);
    }
}
