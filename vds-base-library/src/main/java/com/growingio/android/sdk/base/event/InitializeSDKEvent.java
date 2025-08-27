package com.growingio.android.sdk.base.event;

import android.app.Application;

import com.growingio.android.sdk.collection.Configuration;

/**
 * author CliffLeopard
 * time   2018/6/28:下午12:32
 * email  gaoguanling@growingio.com
 *
 * SDK 被初始化时，释放事件
 */
public class InitializeSDKEvent {
    private Application application;
    private final Configuration configuration;

    public InitializeSDKEvent(Application application, Configuration configuration){
        this.application = application;
        this.configuration = configuration;
    }

    public Application getApplication() {
        return application;
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
