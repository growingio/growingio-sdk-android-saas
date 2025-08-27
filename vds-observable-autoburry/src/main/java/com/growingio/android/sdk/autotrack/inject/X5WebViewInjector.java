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


import com.growingio.android.sdk.autoburry.VdsAgent;
import com.tencent.smtt.sdk.WebView;

import java.util.Map;

public class X5WebViewInjector {
    private static final String TAG = "X5WebViewInjector";

    private X5WebViewInjector() {
    }

    public static void x5WebViewLoadUrl(WebView webView, String url) {
        VdsAgent.loadUrl(webView, url);
    }

    public static void x5WebViewLoadUrl(WebView webView, String url, Map<String, String> additionalHttpHeaders) {
        VdsAgent.loadUrl(webView, url, additionalHttpHeaders);
    }

    public static void x5WebViewLoadData(WebView webView, String data, String mimeType, String encoding) {
        VdsAgent.loadData(webView, data, mimeType, encoding);
    }

    public static void x5WebViewLoadDataWithBaseURL(WebView webView, String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        VdsAgent.loadDataWithBaseURL(webView, baseUrl,data, mimeType, encoding,historyUrl);
    }

    public static void x5WebViewPostUrl(WebView webView, String url, byte[] postData) {
    }
}
