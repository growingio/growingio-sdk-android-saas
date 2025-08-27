package com.growingio.android.sdk.hybrid;

import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

class WebViewJavascriptBridgeConfiguration {
    private static final String TAG = "WebViewJavascriptBridgeConfiguration";

    private final String mProjectId;
    private final String mAppId;
    private final String mNativeSdkVersion;
    private final int mNativeSdkVersionCode;

    WebViewJavascriptBridgeConfiguration(String projectId, String appId, String nativeSdkVersion, int nativeSdkVersionCode) {
        mProjectId = projectId;
        mAppId = appId;
        mNativeSdkVersion = nativeSdkVersion;
        mNativeSdkVersionCode = nativeSdkVersionCode;
    }

    String toJsonString() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("projectId", mProjectId);
            jsonObject.put("appId", mAppId);
            jsonObject.put("nativeSdkVersion", mNativeSdkVersion);
            jsonObject.put("nativeSdkVersionCode", mNativeSdkVersionCode);
            return jsonObject.toString();
        } catch (JSONException e) {
            LogUtil.e(TAG, e.getMessage(), e);
        }
        return "";
    }
}
