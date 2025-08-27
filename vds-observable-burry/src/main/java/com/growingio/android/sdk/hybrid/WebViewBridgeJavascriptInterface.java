package com.growingio.android.sdk.hybrid;

import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.CustomEvent;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.collection.MessageProcessor;
import com.growingio.android.sdk.models.ConversionEvent;
import com.growingio.android.sdk.models.PageVariableEvent;
import com.growingio.android.sdk.models.PeopleEvent;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.WebEvent;
import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONObject;

class WebViewBridgeJavascriptInterface {
    private static final String TAG = "GIO.WebViewBridge";

    static final String JAVASCRIPT_INTERFACE_NAME = "GrowingWebViewJavascriptBridge";

    private final WebViewJavascriptBridgeConfiguration mConfiguration;
    private MessageProcessor mMsgProcessor;
    private ViewNode mViewNode;

    WebViewBridgeJavascriptInterface(WebViewJavascriptBridgeConfiguration configuration, ViewNode viewNode) {
        mConfiguration = configuration;
        mMsgProcessor = CoreInitialize.messageProcessor();
        mViewNode = viewNode;
    }

    @com.uc.webview.export.JavascriptInterface
    @JavascriptInterface
    public String getConfiguration() {
        return mConfiguration.toJsonString();
    }

    @com.uc.webview.export.JavascriptInterface
    @JavascriptInterface
    public void dispatchEvent(String event) {
        LogUtil.printJson(TAG, "dispatchEvent: ", event);
        if (TextUtils.isEmpty(event)) {
            return;
        }
        handleUploadData(event);
    }

    @com.uc.webview.export.JavascriptInterface
    @JavascriptInterface
    public void setNativeUserId(String userId) {
        LogUtil.d(TAG, "setNativeUserId: " + userId);
        GrowingIO.getInstance().setUserId(userId);
    }

    @com.uc.webview.export.JavascriptInterface
    @JavascriptInterface
    public void clearNativeUserId() {
        LogUtil.d(TAG, "clearNativeUserId: ");
        GrowingIO.getInstance().clearUserId();
    }

    /**
     * 保持和无埋点逻辑一致，使用GIOFakePage作为PageName的prefix
     * 目前gio_hybrid_track.js 仅支持evar、ppl、vstr、page、cstm事件的发送(最新版本不支持pvar)
     *
     * @param event
     */
    void handleUploadData(String event) {
        try {
            JSONObject object = new JSONObject(event);
            String type = object.getString("t");
            // ppl 与 evar 不做PageName的拼接，历史逻辑
            if (type.equals("cstm")) {
                mMsgProcessor.saveCustomEvent(new CustomEvent(object, null));
            } else if (type.equals("ppl")) {
                mMsgProcessor.persistEvent(new PeopleEvent(object));
            } else if (type.equals("evar")) {
                mMsgProcessor.persistEvent(new ConversionEvent(object));
            } else if (type.equals("pvar")) {
                mMsgProcessor.persistEvent(new PageVariableEvent(object, null));
            } else {
                final WebEvent web = new WebEvent(event, mViewNode, MessageProcessor.FAKE_PAGE_NAME);
                mMsgProcessor.persistEvent(web);
            }
        } catch (Throwable e) {
            LogUtil.e(TAG, "dispatchEvent异常", e);
        }
    }
}
