/*
 * Copyright (C) 2023 Beijing Yishu Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.growingio.android.sdk.autotrack.inject;

import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.growingio.android.sdk.autoburry.VdsAgent;

public class WebChromeClientInjector {
    private static final String TAG = "WebChromeClientInjector";

    private WebChromeClientInjector() {
    }

    @Deprecated
    public static void onProgressChangedStart(WebView webView, int progress) {
        VdsAgent.onProgressChangedStart(webView, progress);
    }

    public static void onProgressChangedStart(WebChromeClient client, WebView webView, int progress) {
        VdsAgent.onProgressChangedStart(webView, progress);
    }

    @Deprecated
    public static void onProgressChangedEnd(WebView webView, int progress) {
        VdsAgent.onProgressChangedEnd(webView, progress);
    }

    public static void onProgressChangedEnd(WebChromeClient client, WebView webView, int progress) {
        VdsAgent.onProgressChangedEnd(webView, progress);
    }

    public static void setWebChromeClient(WebView webView, WebChromeClient webChromeClient) {
        VdsAgent.setWebChromeClient(webView, webChromeClient);
    }

    @Deprecated
    public static void onX5ProgressChangedStart(com.tencent.smtt.sdk.WebView webView, int progress) {
        VdsAgent.onProgressChangedStart(webView, progress);
    }

    public static void onX5ProgressChangedStart(com.tencent.smtt.sdk.WebChromeClient client, com.tencent.smtt.sdk.WebView webView, int progress) {
        VdsAgent.onProgressChangedStart(webView, progress);
    }

    @Deprecated
    public static void onX5ProgressChangedEnd(com.tencent.smtt.sdk.WebView webView, int progress) {
        VdsAgent.onProgressChangedEnd(webView, progress);
    }

    public static void onX5ProgressChangedEnd(com.tencent.smtt.sdk.WebChromeClient client, com.tencent.smtt.sdk.WebView webView, int progress) {
        VdsAgent.onProgressChangedEnd(webView, progress);
    }

    public static void setX5WebChromeClient(com.tencent.smtt.sdk.WebView webView, com.tencent.smtt.sdk.WebChromeClient webChromeClient) {
        VdsAgent.setWebChromeClient(webView, webChromeClient);
    }

    @Deprecated
    public static void onUcProgressChangedStart(com.uc.webview.export.WebView webView, int progress) {
        VdsAgent.onProgressChangedStart(webView, progress);
    }

    public static void onUcProgressChangedStart(com.uc.webview.export.WebChromeClient client, com.uc.webview.export.WebView webView, int progress) {
        VdsAgent.onProgressChangedStart(webView, progress);
    }

    @Deprecated
    public static void onUcProgressChangedEnd(com.uc.webview.export.WebView webView, int progress) {
        VdsAgent.onProgressChangedEnd(webView, progress);
    }

    public static void onUcProgressChangedEnd(com.uc.webview.export.WebChromeClient client, com.uc.webview.export.WebView webView, int progress) {
        VdsAgent.onProgressChangedEnd(webView, progress);
    }

    public static void setUcWebChromeClient(com.uc.webview.export.WebView webView, com.uc.webview.export.WebChromeClient webChromeClient) {
        VdsAgent.setWebChromeClient(webView, webChromeClient);
    }
}
