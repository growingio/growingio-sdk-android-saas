package com.growingio.android.sdk.autoburry.events;

import android.view.View;

import org.json.JSONObject;

/**
 * Web圈选中， Hybrid的返回消息
 * Created by liangdengke on 2018/11/27.
 */
public class WebCircleHybridReturnEvent {

    private final View webView;
    private final JSONObject message;

    public WebCircleHybridReturnEvent(View webView, JSONObject message) {
        this.webView = webView;
        this.message = message;
    }

    public View getWebView() {
        return webView;
    }

    public JSONObject getMessage() {
        return message;
    }
}
