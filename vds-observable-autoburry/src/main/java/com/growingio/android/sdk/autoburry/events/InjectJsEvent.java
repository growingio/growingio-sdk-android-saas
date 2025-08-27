package com.growingio.android.sdk.autoburry.events;

import android.view.View;

/**
 * Created by liangdengke on 2018/11/27.
 */
public class InjectJsEvent {

    private final View webView;

    public InjectJsEvent(View webView) {
        this.webView = webView;
    }

    public View getWebView() {
        return webView;
    }
}
