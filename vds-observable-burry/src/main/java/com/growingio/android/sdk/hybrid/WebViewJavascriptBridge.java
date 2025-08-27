package com.growingio.android.sdk.hybrid;

import android.annotation.SuppressLint;
import android.webkit.WebView;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.utils.ViewHelper;

public class WebViewJavascriptBridge {
    private static WebViewJavascriptBridgeConfiguration getJavascriptBridgeConfiguration() {
        String projectId = CoreInitialize.coreAppState().getProjectId();
        String appId = CoreInitialize.config().getsGrowingScheme();
        String nativeSdkVersion = GrowingIO.getVersion();
        int nativeSdkVersionCode = 0;

        return new WebViewJavascriptBridgeConfiguration(projectId, appId, nativeSdkVersion, nativeSdkVersionCode);
    }


    @SuppressLint("AddJavascriptInterface")
    public static void bridgeForWebView(WebView webView) {
        webView.addJavascriptInterface(new WebViewBridgeJavascriptInterface(getJavascriptBridgeConfiguration(), ViewHelper.getViewNode(webView, null)),
                WebViewBridgeJavascriptInterface.JAVASCRIPT_INTERFACE_NAME);
    }

    public static void bridgeForWebView(com.tencent.smtt.sdk.WebView x5WebView) {
        x5WebView.addJavascriptInterface(new WebViewBridgeJavascriptInterface(getJavascriptBridgeConfiguration(), ViewHelper.getViewNode(x5WebView, null)),
                WebViewBridgeJavascriptInterface.JAVASCRIPT_INTERFACE_NAME);
    }

    public static void bridgeForWebView(com.uc.webview.export.WebView ucWebView) {
        ucWebView.addJavascriptInterface(new WebViewBridgeJavascriptInterface(getJavascriptBridgeConfiguration(), ViewHelper.getViewNode(ucWebView, null)),
                WebViewBridgeJavascriptInterface.JAVASCRIPT_INTERFACE_NAME);
    }
}
