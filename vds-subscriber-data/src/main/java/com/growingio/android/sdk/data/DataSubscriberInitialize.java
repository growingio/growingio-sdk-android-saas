package com.growingio.android.sdk.data;

import android.support.annotation.NonNull;

import com.growingio.android.sdk.base.event.BgInitializeSDKEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.data.db.MessageUploader;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;

/**
 * author CliffLeopard
 * time   2018/7/2:下午3:39
 * email  gaoguanling@growingio.com
 */
public class DataSubscriberInitialize {
    private volatile static MessageUploader messageUploader;
    private volatile static DataSubscriber dataSubscriber;

    @Subscribe
    public static void onSDKInitialize(BgInitializeSDKEvent event) {
        messageUploader = new MessageUploader(event.application);
        messageUploader.afterInit();
        dataSubscriber = new DataSubscriber(event.application, CoreInitialize.config(),
                CoreInitialize.coreAppState(), CoreInitialize.deviceUUIDFactory(), messageUploader);
        EventCenter.getInstance().register(dataSubscriber);
    }

    @NonNull
    public static MessageUploader messageUploader(){
        return messageUploader;
    }
}
