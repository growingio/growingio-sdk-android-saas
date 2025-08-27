package com.growingio.android.sdk.api;

import android.os.Build;

import com.growingio.android.sdk.base.event.HttpCallBack;
import com.growingio.android.sdk.base.event.HttpEvent;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.DeviceUUIDFactory;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.deeplink.DeeplinkInfo;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.NetworkUtil;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.eventcenter.EventCenter;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * Created by lishaojie on 16/8/4.
 */

public class DynamicLinkTrackTask {

    private static DeeplinkInfo applinkInfo = null;

    private static DeeplinkInfo getApplinkInfo() {
        return applinkInfo;
    }

    public static void setApplinkInfo(DeeplinkInfo applinkInfo) {
        DynamicLinkTrackTask.applinkInfo = applinkInfo;
    }

    public static void run() {
        try {
            CoreAppState state = CoreInitialize.coreAppState();
            DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
            String url = NetworkConfig.getInstance().trackHost() +
                    "/" + state.getProjectId() +
                    "/android/devices?u=" + NetworkUtil.encode(deviceUUIDFactory.getDeviceId()) +
                    "&dm=" + NetworkUtil.encode(Build.BRAND + " " + Build.MODEL) +
                    "&osv=" + NetworkUtil.encode("Android " + Build.VERSION.RELEASE) +
                    "&d=" + NetworkUtil.encode(state.getSPN()) +
                    "&t=activate" +
                    "&tm=" + NetworkUtil.encode(String.valueOf(System.currentTimeMillis())) +
                    "&imei=" + NetworkUtil.encode(deviceUUIDFactory.getIMEI()) +
                    "&adrid=" + NetworkUtil.encode(deviceUUIDFactory.getAndroidId());

            String googleId = deviceUUIDFactory.getGoogleAdId();
            if (googleId != null) {
                url += "&gaid=" + NetworkUtil.encode(googleId);
            }
            String oaid = deviceUUIDFactory.getOaid();
            if (oaid != null){
                url += "&oaid=" + NetworkUtil.encode(oaid);
            }

            if (getApplinkInfo() != null) {
                url += "&link_id=" + NetworkUtil.encode(applinkInfo.linkID);
                url += "&click_id=" + NetworkUtil.encode(applinkInfo.clickID);
                url += "&tm_click=" + NetworkUtil.encode(applinkInfo.clickTM);
                url += "&cl=defer";
            }

            LogUtil.d("T_SEND", "发送事件：" + url);
            final HttpEvent httpEvent = new HttpEvent();
            httpEvent.setUrl(url);
            httpEvent.setCallBack(new HttpCallBack() {
                @Override
                public void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        LogUtil.d("T_SEND", "得到反馈");

                    } else {
                        ThreadUtils.postOnUiThreadDelayed(new Runnable() {
                            @Override
                            public void run() {
                                EventCenter.getInstance().post(httpEvent);
                            }
                        }, 10000);
                    }
                }
            });
            EventCenter.getInstance().post(httpEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
