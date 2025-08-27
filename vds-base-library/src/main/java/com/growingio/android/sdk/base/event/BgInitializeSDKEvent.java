package com.growingio.android.sdk.base.event;

import android.app.Application;

import com.growingio.android.sdk.collection.Configuration;

/**
 * 能够在子线程初始化SDK的事件
 */
public class BgInitializeSDKEvent {
    public final Application application;
    public final Configuration configuration;

    public BgInitializeSDKEvent(Application application, Configuration configuration) {
        this.application = application;
        this.configuration = configuration;
    }
}
