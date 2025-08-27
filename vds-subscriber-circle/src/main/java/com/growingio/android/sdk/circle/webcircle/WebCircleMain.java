package com.growingio.android.sdk.circle.webcircle;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.growingio.android.sdk.autoburry.events.InjectJsEvent;
import com.growingio.android.sdk.autoburry.events.WebCircleHybridReturnEvent;
import com.growingio.android.sdk.base.event.CircleEvent;
import com.growingio.android.sdk.base.event.HttpCallBack;
import com.growingio.android.sdk.base.event.HttpEvent;
import com.growingio.android.sdk.base.event.ViewTreeStatusChangeEvent;
import com.growingio.android.sdk.circle.CircleManager;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.debugger.AbstractSocketAdapter;
import com.growingio.android.sdk.debugger.DebuggerManager;
import com.growingio.android.sdk.debugger.view.WebCircleTipView;
import com.growingio.android.sdk.java_websocket.GioProtocol;
import com.growingio.android.sdk.java_websocket.WebCircleSocketMain;
import com.growingio.android.sdk.utils.ActivityUtil;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.NetworkUtil;
import com.growingio.android.sdk.utils.ObjectUtils;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WeakSet;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.ThreadMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by liangdengke on 2018/9/17.
 */
public class WebCircleMain extends AbstractSocketAdapter{
    private static final String TAG = "GIO.WebCircleMain";

    private String wsUrl = null;
    private String pairKey = null;
    private WeakSet<View> injectJsWebViews = new WeakSet<>();
    private WeakReference<View> currentWebView;

    public WebCircleMain(DebuggerManager manager) {
        super(manager);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRejectJsEvent(InjectJsEvent event){
        injectJsWebViews.add(event.getWebView());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHybridEventReturn(WebCircleHybridReturnEvent event){
        try {
            dealHybridReturnMessage(event.getWebView(), event.getMessage());
        } catch (JSONException e) {
            LogUtil.d(TAG, e.getMessage(), e);
        }
    }

    protected void onServerStarted(String ws){
        wsUrl = ws;
        if (debuggerManager.isLoginDone()){
            afterLoginAndServerStarted();
        }
        ThreadUtils.cancelTaskOnUiThread(timeoutRunnable);
        ThreadUtils.postOnUiThreadDelayed(timeoutRunnable, 10 * 1000L);
    }


    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtil.d(TAG, "waiting for web's connection, but timeout");
            if (mCircleTipView != null){
                mCircleTipView.setError(true);
                mCircleTipView.setContent("电脑端连接超时，请再次扫码圈选");
            }
        }
    };

    private void afterLoginAndServerStarted(){
        LogUtil.d(TAG, "after login and server started");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("wsUrl", wsUrl);
            jsonObject.put("pairKey", pairKey);
        } catch (JSONException e) {
            LogUtil.d(TAG, e);
        }
        HttpEvent event = HttpEvent.createCircleHttpEvent(NetworkConfig.getInstance().getMobileLinkUrl(), jsonObject, false);
        event.setCallBack(new HttpCallBack() {
            @Override
            public void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
                try {
                    String result = null;
                    if (responseCode == HttpURLConnection.HTTP_OK){
                        result = new String(data);
                        JSONObject resultJson = new JSONObject(result);
                        String status = resultJson.getString("status");
                        if ("ok".equals(status)){
                            LogUtil.d(TAG, "post wsUrl to server success");
                            return;
                        }
                    }
                    LogUtil.e(TAG, "post wsUrl to server, status code not ok: " + responseCode + ", and result: " + result);
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            debuggerManager.exit();
                            Toast.makeText(coreAppState.getGlobalContext(), "打开WebCircle失败， 请重新扫码连接", Toast.LENGTH_LONG).show();
                        }
                    });
                }catch (Exception e){
                    LogUtil.d(TAG, e);
                }
            }
        });
        EventCenter.getInstance().post(event);
    }

    @Override
    protected void onHybridMessageFromWeb(JSONObject jsonObject) {
        super.onHybridMessageFromWeb(jsonObject);
        try {
            dealHybridMessageInCatch(jsonObject);
        } catch (JSONException e) {
            LogUtil.d(TAG, e.getMessage(), e);
        }
    }

    void dealHybridReturnMessage(View webView, JSONObject message) throws JSONException{
        if (webView == null){
            LogUtil.d(TAG, "dealHybridReturnMessage and found webView null, return");
            return;
        }
        if (!checkWebViewInActivity(coreAppState.getForegroundActivity(), webView)){
            LogUtil.d(TAG, "deal with hybrid return message, but activity is not same as current activity, just return");
            return;
        }
        // TODO: 2018/11/27 ssKey
        int[] location = new int[2];
        message.put("msgId", "hybridEvent");
        webView.getLocationOnScreen(location);
        double scaleFactor = ScreenshotHelper.getScaledFactor();
        transformCoordinates(message, (int) (-location[0] * scaleFactor), (int) (-location[1] * scaleFactor), 1/scaleFactor);
        sendMessage(message.toString());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void dealHybridMessageInCatch(JSONObject jsonObject) throws JSONException{
        if (jsonObject.has("x")){
            int x = jsonObject.getInt("x");
            int y = jsonObject.getInt("y");
            String ssKey = jsonObject.getString("sk");
            if (!ObjectUtils.equals(ssKey, CircleManager.getInstance().getCurrentSnapShotKey())){
                LogUtil.d(TAG, "dealHybridMessageInCatch, and sskey not same: ", ssKey, CircleManager.getInstance().getCurrentSnapShotKey());
                return;
            }
            double scaledFactor = ScreenshotHelper.getScaledFactor();
            x = (int) (x/scaledFactor);
            y = (int) (y /scaledFactor);
            TargetWebView targetWebView = lookupTargetWebView(x, y);
            if (targetWebView == null){
                LogUtil.d(TAG, "dealHybridMessageWithCatch, and not found valid webView: ", x,", ", y);
                return;
            }
            transformCoordinates(jsonObject, targetWebView.screenX, targetWebView.screenY, scaledFactor);
            Util.callJavaScript(targetWebView.webView, "_vds_hybrid.handleWebEvent", jsonObject);
        }else{
            Activity current = coreAppState.getForegroundActivity();
            if (current == null){
                LogUtil.d(TAG, "not found activity, return");
                return;
            }
            for (View webView: injectJsWebViews){
                if (checkWebViewInActivity(current, webView)
                        && ViewHelper.viewVisibilityInParents(webView)){
                    Util.callJavaScript(webView, "_vds_hybrid.handleWebEvent", jsonObject);
                }
            }
        }
    }

    static void transformCoordinates(JSONObject jsonObject, int deltaX, int deltaY, double scaledFactor) throws JSONException{
        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()){
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof Number){
                float intValue = ((Number)value).floatValue();
                if ("x".equals(key)){
                    jsonObject.put("x", (int)(intValue/scaledFactor) - deltaX);
                }else if ("y".equals(key)){
                    jsonObject.put("y", (int)(intValue/scaledFactor) - deltaY);
                }else if ("ex".equals(key)){
                    jsonObject.put("ex", (int)(intValue/scaledFactor) - deltaX);
                }else if ("ey".equals(key)){
                    jsonObject.put("ey", (int)(intValue/scaledFactor) - deltaY);
                }else if ("ew".equals(key)){
                    jsonObject.put("ew", (int)(intValue/scaledFactor));
                }else if ("eh".equals(key)){
                    jsonObject.put("eh", (int)(intValue/scaledFactor));
                }
            }else if (value instanceof JSONObject){
                transformCoordinates((JSONObject) value, deltaX, deltaY, scaledFactor);
            }else if (value instanceof JSONArray){
                transformCoordinatesJsonArray((JSONArray) value, deltaX, deltaY, scaledFactor);
            }
        }
    }

    static void transformCoordinatesJsonArray(JSONArray jsonArray, int deltaX, int deltaY, double scaledFactor) throws JSONException{
        for (int i = 0; i < jsonArray.length(); i++){
            Object object = jsonArray.get(i);
            if (object instanceof JSONObject){
                transformCoordinates((JSONObject) object, deltaX, deltaY, scaledFactor);
            }else if (object instanceof JSONArray){
                transformCoordinatesJsonArray((JSONArray) object, deltaX, deltaY, scaledFactor);
            }
        }
    }

    @VisibleForTesting
    TargetWebView lookupTargetWebView(int screenX, int screenY){
        Activity currentActivity = coreAppState.getForegroundActivity();
        if (currentActivity == null){
            LogUtil.d(TAG, "currentActivity is null, and return");
            return null;
        }

        int[] location = new int[2];
        Rect rect = new Rect();

        if (currentWebView != null && currentWebView.get() != null){
            View webView = currentWebView.get();
            if (checkWebViewInActivity(currentActivity, webView)){
                webView.getLocationOnScreen(location);
                rect.set(0, 0, webView.getWidth(), webView.getHeight());
                rect.offset(location[0], location[1]);
                if (rect.contains(screenX, screenY)){
                    LogUtil.d(TAG, "lookupTargetWebView, found valid webView(From cache): ", webView);
                    return new TargetWebView(webView, location[0], location[1]);
                }
            }
        }

        for (View webView : injectJsWebViews){
            if (!checkWebViewInActivity(currentActivity, webView))
                continue;
            webView.getLocationOnScreen(location);
            rect.set(0, 0, webView.getWidth(), webView.getHeight());
            rect.offset(location[0], location[1]);
            if (rect.contains(screenX, screenY)){
                LogUtil.d(TAG, "lookupTargetWebView, found valid webView: ", webView);
                currentWebView = new WeakReference<>(webView);
                return new TargetWebView(webView, location[0], location[1]);
            }
        }
        return null;
    }

    private boolean checkWebViewInActivity(Activity activity, View webView){
        Activity webViewActivity = ActivityUtil.findActivity(webView.getContext());
        return webViewActivity == null || activity == webViewActivity;
    }

    @Override
    public void addTipView(Context applicationContext) {
        mCircleTipView = new WebCircleTipView(applicationContext);
    }

    @Override
    public void onFirstLaunch(Uri validData) {
        super.onFirstLaunch(validData);
        onWebCircleFirstLaunch(validData);
    }

    protected void onWebCircleFirstLaunch(Uri validData){
        pairKey = validData.getQueryParameter("pairKey");
        if (NetworkUtil.getWifiIp(coreAppState.getGlobalContext()) == null){
            Log.e(TAG, "cannot find wifi ip, and exit");
            Toast.makeText(coreAppState.getGlobalContext(), "没有检测到wifi网络， 请确保网络连接后从扫码唤起", Toast.LENGTH_LONG).show();
            debuggerManager.exit();
        }else{
            mCircleTipView.setContent("正在准备Web圈选(初始化)....");
            EventCenter.getInstance().post(new CircleEvent("defaultListener"));
            debuggerManager.login();
        }
    }

    @Override
    public void onLoginSuccess() {
        if (wsUrl != null){
            afterLoginAndServerStarted();
        }
    }

    @Override
    public void onExit() {
        super.onExit();
        wsUrl = null;
        currentWebView = null;
        injectJsWebViews.clear();
    }

    @Override
    protected void onConnected() {
        super.onConnected();
        ThreadUtils.cancelTaskOnUiThread(timeoutRunnable);
        if (mCircleTipView != null){
            mCircleTipView.doing();
        }
        // web端反馈有时没有收到SCREEN截图， 这里强制补充一次
        EventCenter.getInstance().post(new ViewTreeStatusChangeEvent(ViewTreeStatusChangeEvent.StatusType.ScrollChanged));
    }

    @Override
    public void onPluginReady() {
        super.onPluginReady();
        onWebCirclePluginReady();
    }

    protected void onWebCirclePluginReady(){
        startServer();
    }


    private void startServer() {
        try {
            socketInterface = new WebCircleSocketMain();
            socketInterface.setGioProtocol(new GioProtocol());
            socketInterface.start();
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage(), e);
            onConnectFailed();
        }
    }


    private static class TargetWebView{
        View webView;
        int screenX;
        int screenY;

        public TargetWebView(View webView, int screenX, int screenY) {
            this.webView = webView;
            this.screenX = screenX;
            this.screenY = screenY;
        }
    }
}
