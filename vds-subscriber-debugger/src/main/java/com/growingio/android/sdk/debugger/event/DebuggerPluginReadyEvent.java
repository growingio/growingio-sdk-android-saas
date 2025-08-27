package com.growingio.android.sdk.debugger.event;

/**
 * 用于PluginManager插件加载完成后的消息
 * Created by liangdengke on 2018/8/8.
 */
public class DebuggerPluginReadyEvent {
    public final boolean isFromNet;

    public DebuggerPluginReadyEvent(boolean isFromNet) {
        this.isFromNet = isFromNet;
    }
}
