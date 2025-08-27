package com.growingio.android.sdk.status;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

import com.growingio.android.sdk.base.event.ScreenStatusEvent;
import com.growingio.android.sdk.base.event.net.NetWorkChangedEvent;
import com.growingio.eventcenter.bus.EventBus;

/**
 * author CliffLeopard
 * time   2018/7/2:下午2:57
 * email  gaoguanling@growingio.com
 */
public class BroadcastObservable extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = false;
        String action = intent.getAction();
        if (Intent.ACTION_SCREEN_ON.equals(action)) { // 开屏
            EventBus.getDefault().post(new ScreenStatusEvent(ScreenStatusEvent.ScreenStatusType.SCREEN_ON));
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) { // 锁屏
            EventBus.getDefault().post(new ScreenStatusEvent(ScreenStatusEvent.ScreenStatusType.SCREEN_OFF));
        } else if (Intent.ACTION_USER_PRESENT.equals(action)) { //解锁
            EventBus.getDefault().post(new ScreenStatusEvent(ScreenStatusEvent.ScreenStatusType.SCREEN_PRESENT));
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            try {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                    ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    NetworkInfo dataNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                    if (!wifiNetworkInfo.isConnected() || dataNetworkInfo.isConnected()) {
                        isConnected = true;
                    }
                } else {
                    ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    Network[] networks = connMgr.getAllNetworks();
                    for (int i = 0; i < networks.length; i++) {
                        NetworkInfo networkInfo = connMgr.getNetworkInfo(networks[i]);
                        if (networkInfo.isConnected()) {
                            isConnected = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                isConnected = true;
            }
            EventBus.getDefault().post(new NetWorkChangedEvent(isConnected));
        }
    }
}
