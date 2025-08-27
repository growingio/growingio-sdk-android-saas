package com.growingio.android.sdk.base.event.message;

/**
 * author CliffLeopard
 * time   2018/7/2:下午7:28
 * email  gaoguanling@growingio.com
 */
public class RemoveEvent {
    public Runnable runnable;

    public RemoveEvent(Runnable runnable) {
        this.runnable = runnable;
    }
}
