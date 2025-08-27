package com.growingio.android.sdk.status;

import android.content.ComponentCallbacks;
import android.content.res.Configuration;

import com.growingio.android.sdk.base.event.OnCloseBufferEvent;
import com.growingio.eventcenter.EventCenter;

/**
 * Created by liangdengke on 2018/12/18.
 */
public class LowMemoryObservable implements ComponentCallbacks {
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore
    }

    @Override
    public void onLowMemory() {
        EventCenter.getInstance().post(new OnCloseBufferEvent());
    }
}
