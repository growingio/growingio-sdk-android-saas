package com.growingio.android.sdk.collection;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.webkit.WebView;

import com.growingio.android.sdk.deeplink.DeeplinkCallback;
import com.growingio.android.sdk.hybrid.WebViewJavascriptBridge;

import org.json.JSONObject;

/**
 * Created by liangdengke on 2018/7/14.
 */
public class GrowingIO extends AbstractGrowingIO<GrowingIO> {

    GrowingIO(Configuration configuration) {
        super(configuration);
    }

    GrowingIO() {
        super();
    }

    public static GrowingIO startWithConfiguration(Application application, Configuration configuration) {
        // just for kotlin
        return AbstractGrowingIO.startWithConfiguration(application, configuration);
    }

    /**
     * 仅埋点SDK提供 bridgeForWebView 接口
     * @param webView
     */
    public void bridgeForWebView(WebView webView) {
        WebViewJavascriptBridge.bridgeForWebView(webView);
    }

    public void bridgeForX5WebView(com.tencent.smtt.sdk.WebView x5WebView) {
        WebViewJavascriptBridge.bridgeForWebView(x5WebView);
    }

    public void bridgeForUcWebView(com.uc.webview.export.WebView ucWebView) {
        WebViewJavascriptBridge.bridgeForWebView(ucWebView);
    }

    static class EmptyGrowingIO extends GrowingIO {
        // 不要直接编写这个类， 利用工具

        @Override
        public void disableDataCollect() {
            // empty
        }

        @Override
        public void enableDataCollect() {
            // empty
        }

        @Override
        public GrowingIO setThrottle(boolean throttle) {
            return this;
        }

        @Override
        public GrowingIO disable() {
            return this;
        }

        @Override
        public GrowingIO resume() {
            return this;
        }

        @Override
        public GrowingIO stop() {
            return this;
        }

        @Override
        public String getDeviceId() {
            return null;
        }

        @Override
        public String getVisitUserId() {
            return null;
        }

        @Override
        public String getSessionId() {
            return null;
        }

        @Override
        public GrowingIO setTestHandler(Handler handler) {
            return this;
        }

        @Override
        public GrowingIO setGeoLocation(double latitude, double longitude) {
            return this;
        }

        @Override
        public GrowingIO clearGeoLocation() {
            return this;
        }

        @Override
        public GrowingIO setUserId(String userId) {
            return this;
        }

        @Override
        public GrowingIO clearUserId() {
            return this;
        }

        @Override
        public String getUserId() {
            return null;
        }

        @Override
        public GrowingIO setPeopleVariable(JSONObject variables) {
            return this;
        }

        @Override
        public GrowingIO setPeopleVariable(String key, Number value) {
            return this;
        }

        @Override
        public GrowingIO setPeopleVariable(String key, String value) {
            return this;
        }

        @Override
        public GrowingIO setPeopleVariable(String key, boolean value) {
            return this;
        }

        @Override
        public GrowingIO setEvar(JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO setEvar(String key, Number value) {
            return this;
        }

        @Override
        public GrowingIO setEvar(String key, String value) {
            return this;
        }

        @Override
        public GrowingIO setEvar(String key, boolean value) {
            return this;
        }

        @Override
        public GrowingIO setVisitor(JSONObject visitorVariable) {
            return this;
        }

        @Override
        public GrowingIO setAppVariable(JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO setAppVariable(String key, Number value) {
            return this;
        }

        @Override
        public GrowingIO setAppVariable(String key, String value) {
            return this;
        }

        @Override
        public GrowingIO setAppVariable(String key, boolean value) {
            return this;
        }

        @Override
        public GrowingIO setChannel(String channel) {
            return this;
        }

        @Override
        public GrowingIO disableImpression() {
            return this;
        }

        @Override
        public GrowingIO setImp(boolean enable) {
            return this;
        }

        @Override
        public GrowingIO track(String eventName) {
            return this;
        }

        @Override
        public GrowingIO track(String eventName, Number number) {
            return this;
        }

        @Override
        public GrowingIO track(String eventName, JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO track(String eventName, Number number, JSONObject variable) {
            return this;
        }

        @Override
        public void trackPage(String pagename, String lastpage, long ptm) {
            // empty
        }

        @Override
        public void saveVisit(String pagename) {
            // empty
        }

        @Override
        public void trackPage(String pageName) {
            // empty
        }

        @Override
        public GrowingIO setPageVariable(String pageName, JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO onNewIntent(Activity activity, Intent intent) {
            return this;
        }

        @Override
        public GrowingIO setImeiEnable(boolean imeiEnable) {
            return this;
        }

        @Override
        public GrowingIO setGoogleAdIdEnable(boolean googleAdIdEnable) {
            return this;
        }

        @Override
        public GrowingIO setAndroidIdEnable(boolean androidIdEnable) {
            return this;
        }

        @Override
        public GrowingIO setOAIDEnable(boolean oaidEnable) {
            return this;
        }

        @Override
        public boolean doDeeplinkByUrl(String url, DeeplinkCallback callback) {
            return false;
        }

        public void bridgeForWebView(WebView webView) {
            // empty
        }

        public void bridgeForX5WebView(com.tencent.smtt.sdk.WebView x5WebView) {
            // empty
        }

        public void bridgeForUcWebView(com.uc.webview.export.WebView ucWebView) {
            // empty
        }
    }
}
