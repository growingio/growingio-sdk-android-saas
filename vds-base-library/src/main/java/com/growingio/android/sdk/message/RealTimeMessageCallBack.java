package com.growingio.android.sdk.message;

import org.json.JSONObject;

/**
 * author CliffLeopard
 * time   2018/4/20:下午3:48
 * email  gaoguanling@growingio.com
 */
public interface RealTimeMessageCallBack {
    void receivedMessage(String type, JSONObject event);
}
