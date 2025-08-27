package com.growingio.android.sdk.base.event.net;

/**
 * author CliffLeopard
 * time   2018/7/2:下午3:00
 * email  gaoguanling@growingio.com
 */
public class NetWorkChangedEvent {

    private boolean isConnected;

    public NetWorkChangedEvent(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
