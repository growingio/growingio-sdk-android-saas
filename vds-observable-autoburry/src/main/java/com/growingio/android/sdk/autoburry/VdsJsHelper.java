package com.growingio.android.sdk.autoburry;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.growingio.android.sdk.autoburry.events.InjectJsEvent;
import com.growingio.android.sdk.autoburry.events.WebCircleHybridReturnEvent;
import com.growingio.android.sdk.base.event.CircleGotWebSnapshotNodeEvent;
import com.growingio.android.sdk.base.event.ViewTreeStatusChangeEvent;
import com.growingio.android.sdk.collection.AbstractGrowingIO;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.CustomEvent;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.collection.MessageProcessor;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.ipc.GrowingIOIPC;
import com.growingio.android.sdk.models.ConversionEvent;
import com.growingio.android.sdk.models.PageEvent;
import com.growingio.android.sdk.models.PageVariableEvent;
import com.growingio.android.sdk.models.PatternServer;
import com.growingio.android.sdk.models.PeopleEvent;
import com.growingio.android.sdk.models.Screenshot;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.WebEvent;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.ActivityUtil;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.LinkedString;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ReflectUtil;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WebViewUtil;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventBus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lishaojie on 16/3/9.
 */
public class VdsJsHelper extends WebChromeClient implements Runnable {
    private static final String TAG = "GIO.VdsJsHelper";

    private static final int HOOK_DELAY = 1000;
    private static final int HOOK_CIRCLE_DELY = 500;
    private static final int MIN_PROGRESS_FOR_HOOK = 60;

    private static final String JS_INTERFACE_NAME = "_vds_bridge";
    private String mPageName;
    private final WeakReference<View> mWebView;
    private Object mX5ChromeClient;
    private Object mUcChromeClient;
    private ViewNode mViewNode;
    private boolean mReturnedData = false;

    private CoreAppState mCoreAppState;
    private AutoBuryAppState mAutoBuryAppState;
    private MessageProcessor mMsgProcessor;
    private GrowingIOIPC growingIOIPC;
    private GConfig mConfig;
    private int lastHostAndPortHash = -1;

    public VdsJsHelper(View webView) {
        mCoreAppState = CoreInitialize.coreAppState();
        mAutoBuryAppState = AutoBuryObservableInitialize.autoBuryAppState();
        mMsgProcessor = CoreInitialize.messageProcessor();
        growingIOIPC = CoreInitialize.growingIOIPC();
        mConfig = CoreInitialize.config();
        mWebView = new WeakReference<>(webView);
        wrapWebChromeClient(webView);
    }

    public boolean isReturnedData() {
        return mReturnedData;
    }

    // TODO: 强制刷新此WebView的imp事件, 在imp重做后删除
    public void impressAllElements() {
        View view = mWebView.get();
        LogUtil.d(TAG, "impressAllElements: ", view);
        if (view != null && mReturnedData) {
            Util.callJavaScript(view, "_vds_hybrid.impressAllElements", true);
        }
    }

    // 强制刷新当前WebView的Node信息
    public void updateViewNodeForce() {
        View webView = mWebView.get();
        if (webView == null) return;
        Activity activity = ActivityUtil.findActivity(webView.getContext()); // Activity可能为null
        mPageName = mAutoBuryAppState.getPageName(activity == null ? mCoreAppState.getForegroundActivity() : activity);
        try {
            mViewNode = ViewHelper.getViewNode(webView, null);
        } catch (Exception e) {
            LogUtil.e(TAG, "mViewNode update failed");
        }
        checkClient();
    }

    @Override
    public void run() {
        final View view = mWebView.get();
        if (view != null) {
            if (ActivityUtil.isDestroy(view.getContext())) return;
            if (mViewNode == null) {
                updateViewNodeForce();
                if (mViewNode == null)
                    return;
            }
            LogUtil.d(TAG, "inject js into WebView");
            loadUrlWithCatch(view,
                    getVdsHybridConfig(),
                    getInitPatternServer(),
                    getVdsHybridSrc(view.getContext()));
            final String circleSrcJsContent;
            if (PendingStatus.isAppCircleEnabled()) {
                circleSrcJsContent = getCirclePluginSrc(view.getContext());
            } else if (PendingStatus.isWebCircleEnabled()) {
                circleSrcJsContent = getWebCirclePluginSrc();
            } else {
                circleSrcJsContent = null;
            }
            if (circleSrcJsContent != null) {
                ThreadUtils.postOnUiThreadDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!ActivityUtil.isDestroy(view.getContext())) {
                            loadUrlWithCheck(view, circleSrcJsContent);
                        }
                    }
                }, HOOK_CIRCLE_DELY);
            }
            EventCenter.getInstance().post(new InjectJsEvent(view));
        } else {
            LogUtil.d(TAG, "null WebView, hook cancelled");
        }
    }

    private String getInitPatternServer() {
        PatternServer patternServer = new PatternServer(mCoreAppState.getProjectId(), mCoreAppState.getSPN(), mAutoBuryAppState.getPageName(), growingIOIPC.getToken());
        if (mViewNode.mParentXPath != null) {
            patternServer.setXpath(mViewNode.mParentXPath.toStringValue());
        }
        String patternServerJs = "window._vds_hybrid_native_info = " + patternServer.toJson().toString() + ";";
        return String.format("javascript:(function(){try{%s}catch(e){}})()", patternServerJs);
    }

    private String getVdsHybridConfig() {
        String hybridJSConfig = String.format("window._vds_hybrid_config = {\"enableHT\":%s,\"disableImp\":%s, \"protocolVersion\":1}", mConfig.isHashTagEnable(), !mConfig.shouldSendImp());
        return String.format("javascript:(function(){try{%s}catch(e){}})()", hybridJSConfig);
    }

    private String getVdsHybridSrc(Context context) {
        String hybridJSSrc = NetworkConfig.getInstance().getJS_HYBRID_URL();
//        return String.format("javascript:(function(){try{var p=document.createElement('script');p.src='%s';document.head.appendChild(p);}catch(e){}})()", hybridJSSrc);
        return injectScriptFile("_gio_hybird_js", hybridJSSrc);
    }

    private String getCirclePluginSrc(Context context) {
        String circlePluginSrc = NetworkConfig.getInstance().getJS_CIRCLE_URL();
//        return String.format("javascript:(function(){try{var p=document.createElement('script');p.src='%s';document.head.appendChild(p);}catch(e){}})()", circlePluginSrc);
        return injectScriptFile("_gio_circle_js", circlePluginSrc);
    }

    private String getWebCirclePluginSrc() {
        String circlePluginSrc = NetworkConfig.getInstance().getJSWebCircleUrl();
//        return String.format("javascript:(function(){try{var p=document.createElement('script');p.src='%s';document.head.appendChild(p);}catch(e){}})()", circlePluginSrc);
        return injectScriptFile("_gio_web_circle_js", circlePluginSrc);
    }

    private boolean isPageEvent(String event) {
        try {
            JSONObject jsonObject = new JSONObject(event);
            return PageEvent.TYPE_NAME.equals(jsonObject.get("t"));
        } catch (JSONException e) {
            return false;
        }
    }

    public String injectScriptFile(String id, String scriptSrc) {
        String js =
                "javascript:(function(){try{" +
                        "var jsNode = document.getElementById('%s');\n" +
                        "if (jsNode==null) {\n" +
                        "    var p = document.createElement('script');\n" +
                        "    p.src = '%s';\n" +
                        "    p.id = '%s';\n" +
                        "    document.head.appendChild(p);\n" +
                        "}" +
                        "}catch(e){}})()";
        return String.format(js, id, scriptSrc, id);
    }

    private class VdsBridge {

        @com.uc.webview.export.JavascriptInterface
        @JavascriptInterface
        public void webCircleHybridEvent(String message) {
            try {
                LogUtil.d(TAG, "receive webCircleHybridEvent message: ", message);
                JSONObject hybridEvent = new JSONObject(message);
                if (mViewNode.mHasListParent) {
                    int et = hybridEvent.getInt("et");
                    if (et == 2) {
                        JSONArray e = hybridEvent.getJSONArray("e");
                        for (int i = 0; i < e.length(); i++) {
                            JSONObject el = e.getJSONObject(i);
                            if (el.has("idx")) {
                                String circleXpath = el.getString("xpath");
                                String hybridXpath = circleXpath.substring(circleXpath.indexOf(":"));
                                String xpath = mViewNode.mOriginalParentXpath + hybridXpath;
                                el.put("xpath", xpath);

                                JSONArray patterns = hybridEvent.getJSONArray("patterns");
                                for (int j = 0; j < patterns.length(); j++) {
                                    if (patterns.getString(j).equals(circleXpath)) {
                                        patterns.put(j, xpath);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                EventBus.getDefault().post(new WebCircleHybridReturnEvent(mWebView.get(), hybridEvent));
            } catch (Throwable e) {
                LogUtil.e(TAG, "webCircleHybridEvent: " + e.getMessage(), e);
            }
        }

        @com.uc.webview.export.JavascriptInterface
        @JavascriptInterface
        public void onDOMChanged() {
            EventCenter.getInstance().post(new ViewTreeStatusChangeEvent(ViewTreeStatusChangeEvent.StatusType.ScrollChanged));
        }

        @com.uc.webview.export.JavascriptInterface
        @JavascriptInterface
        public void saveEvent(String event) {
            if (handleUploadData(event))
                return;
            event = encryptWebContent(event);
            if (mViewNode != null) {
                mReturnedData = true;
                LogUtil.d(TAG, event);
                if (CoreInitialize.config().isEnabled()) {
                    if (isPageEvent(event)) {
                        LogUtil.d(TAG, "found hybrid page event, and update dom, update node");
                        onDOMChanged();
                        updateViewNodeForce();
                    }
                    final WebEvent web = new WebEvent(event, mViewNode, mPageName);
                    mMsgProcessor.persistEvent(web);
                }
            }
        }

        @com.uc.webview.export.JavascriptInterface
        @JavascriptInterface
        public void setUserId(String id) {
            try {
                GrowingIO.getInstance().setUserId(id);
                LogUtil.d(TAG, id);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage(), e);
            }
        }

        @com.uc.webview.export.JavascriptInterface
        @JavascriptInterface
        public void clearUserId(String nil) {
            try {
                GrowingIO.getInstance().clearUserId();
                LogUtil.d(TAG, "clearUserId");
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage(), e);
            }
        }

        @com.uc.webview.export.JavascriptInterface
        @JavascriptInterface
        public void setVisitor(String visitor) {
            try {
                GrowingIO.getInstance().setVisitor(new JSONObject(visitor));
                LogUtil.d(TAG, visitor);
            } catch (Exception e) {
                LogUtil.e(TAG, "setVisitor failed " + visitor);
            }
        }

        @com.uc.webview.export.JavascriptInterface
        @JavascriptInterface
        public void saveCustomEvent(String event) {
            LogUtil.d(TAG, event);
            try {
                mMsgProcessor.saveCustomEvent(new CustomEvent(new JSONObject(event), mPageName));
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage(), e);
            }
        }

        @com.uc.webview.export.JavascriptInterface
        @JavascriptInterface
        public void hoverNodes(final String message) {
            if (mViewNode == null || mWebView.get() == null) return;
            LogUtil.d(TAG, message);
            mWebView.get().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject object = new JSONObject(message);
                        String type = object.getString("t");
                        if (type.equals("snap")) {
                            List<ViewNode> nodes = getWebNodesFromEvent(object);
                            if (nodes.size() > 0) {
                                ViewNode node = nodes.get(0);
                                CircleGotWebSnapshotNodeEvent event = new CircleGotWebSnapshotNodeEvent(nodes, node.mWebElementInfo.mHost, node.mWebElementInfo.mPath);
                                EventCenter.getInstance().post(event);
                            }
                        }
                    } catch (JSONException e) {
                        LogUtil.d(TAG, e);
                    }
                }
            }, 100);
        }

    }

    private String encryptWebContent(String message) {
        if (CoreInitialize.config().getEncryptEntity() == null) {
            return message;
        }
        try {
            JSONObject jsonObject = new JSONObject(message);
            String v = jsonObject.getString("v");
            if (v != null)
                jsonObject.put("v", Util.encryptContent(v));
            message = jsonObject.toString();
        } catch (Exception ignore) {
        }
        try {
            JSONObject jsonObject = new JSONObject(message);
            JSONArray e = jsonObject.getJSONArray("e");
            if (e != null) {
                for (int i = 0; i < e.length(); i++) {
                    try {
                        String v = e.getJSONObject(i).getString("v");
                        if (v != null)
                            e.getJSONObject(i).put("v", Util.encryptContent(v));
                    } catch (Exception ignore) {
                    }
                }
            }
            message = jsonObject.toString();
        } catch (Exception ignore) {
        }

        return message;
    }


    private List<ViewNode> getWebNodesFromEvent(JSONObject object) throws JSONException {
        JSONArray elements = object.getJSONArray("e");
        int elemSize = elements.length();
        List<ViewNode> nodes = new ArrayList<ViewNode>(elemSize);
        View targetView = mWebView.get();
        String host = object.getString("d");
        String path = object.getString("p");
        String query = object.optString("q", null);
        boolean globalIsTrackingEditText = object.optBoolean("isTrackingEditText", false);
        final double scaledFactor = ScreenshotHelper.getScaledFactor();
        int[] location = new int[2];
        targetView.getLocationOnScreen(location);
        Rect visibleRect = new Rect();
        targetView.getGlobalVisibleRect(visibleRect);
        for (int i = 0; i < elemSize; i++) {
            JSONObject elem = elements.getJSONObject(i);
            ViewNode webNode = mViewNode.copyWithoutView();
            ViewNode.WebElementInfo info = new ViewNode.WebElementInfo();
            info.mHost = host;
            info.mPath = path;
            info.mQuery = query;
            info.mHref = elem.optString("h", null);
            info.mNodeType = elem.optString("nodeType", null);
            webNode.mWebElementInfo = info;
            webNode.mView = targetView;
            if (elem.has("isTrackingEditText")) {
                webNode.hybridIsTrackingEditText = elem.optBoolean("isTrackingEditText");
            } else {
                webNode.hybridIsTrackingEditText = globalIsTrackingEditText;
            }
            if (elem.opt("idx") != null) {
                webNode.mHasListParent = true;
                webNode.mLastListPos = elem.getInt("idx");
                webNode.mParentXPath = LinkedString.fromString(webNode.mOriginalParentXpath.toStringValue())
                        .append(Constants.WEB_PART_SEPARATOR)
                        .append(elem.getString("x"));
            } else {
                webNode.mParentXPath.append(Constants.WEB_PART_SEPARATOR).append(elem.getString("x"));
            }
            webNode.mViewContent = elem.optString("v", "");
            int cx = (int) elem.getDouble("ex");
            int cy = (int) elem.getDouble("ey");
            int cw = (int) elem.getDouble("ew");
            int ch = (int) elem.getDouble("eh");
            JSONArray hybridXpaths = elem.optJSONArray("patterns");
            if (hybridXpaths != null) {
                webNode.mParentXPath = makePatternXPath(webNode.mOriginalParentXpath.toStringValue(), hybridXpaths);
                LogUtil.i("GIO.PatternXPath", webNode.mPatternXPath);
            }
            webNode.mClipRect = new Rect(cx, cy, cw + cx, ch + cy);
            webNode.mClipRect.offset(location[0], location[1]);
            boolean visible = webNode.mClipRect.intersect(visibleRect);
            Screenshot screenshot = new Screenshot();
            screenshot.x = String.valueOf((int) (webNode.mClipRect.left * scaledFactor));
            screenshot.y = String.valueOf((int) (webNode.mClipRect.top * scaledFactor));
            screenshot.w = String.valueOf((int) (webNode.mClipRect.width() * scaledFactor));
            screenshot.h = String.valueOf((int) (webNode.mClipRect.height() * scaledFactor));
            webNode.mScreenshot = screenshot;
            nodes.add(webNode);
        }
        return nodes;
    }

    private LinkedString makePatternXPath(String nativeXpath, JSONArray hybridXpaths) {
        if (nativeXpath == null || nativeXpath.length() == 0
                || hybridXpaths == null || hybridXpaths.length() == 0) {
            return LinkedString.fromString("");
        }
        LinkedString linkedString = new LinkedString();
        try {
            for (int index = 0; index < hybridXpaths.length(); index++) {
                if (linkedString.length() > 0) {
                    linkedString.append(",");
                }
                try {
                    linkedString.append(nativeXpath).append(Constants.WEB_PART_SEPARATOR).append(hybridXpaths.getString(index));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "makePatternXPath failed: ", e);
        }
        return linkedString;
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void wrapWebChromeClient(View view) {
        checkClient();
        if (view.getTag(GrowingIO.GROWING_ALREADY_SET_INTERFACE) == null) {
            view.setTag(GrowingIO.GROWING_ALREADY_SET_INTERFACE, true);
            if (view instanceof WebView) {
                WebView webView = (WebView) view;
                webView.getSettings().setJavaScriptEnabled(true);
                webView.addJavascriptInterface(new VdsBridge(), JS_INTERFACE_NAME);
            } else if (ClassExistHelper.instanceOfX5WebView(view)) {
                com.tencent.smtt.sdk.WebView webView = (com.tencent.smtt.sdk.WebView) view;
                webView.getSettings().setJavaScriptEnabled(true);
                webView.addJavascriptInterface(new VdsBridge(), JS_INTERFACE_NAME);
            } else if (ClassExistHelper.instanceOfUcWebView(view)) {
                com.uc.webview.export.WebView webView = (com.uc.webview.export.WebView) view;
                webView.getSettings().setJavaScriptEnabled(true);
                webView.addJavascriptInterface(new VdsBridge(), JS_INTERFACE_NAME);
            }
        }
    }

    private boolean checkClient() {
        View view = mWebView.get();
        if (view != null) {
            if (view.getTag(GrowingIO.GROWING_WEB_CLIENT_KEY) != null)
                return false;
            if (view instanceof WebView) {
                WebView webView = (WebView) view;
                try {
                    setWebChromeClient(webView, this, WebChromeClient.class);
                } catch (Throwable e) {
                    LogUtil.d(TAG, e.getMessage(), e);
                    return false;
                }
            } else if (ClassExistHelper.instanceOfX5WebView(view)) {
                com.tencent.smtt.sdk.WebView webView = (com.tencent.smtt.sdk.WebView) view;
                mX5ChromeClient = getX5ChromeClient();
                try {
                    webView.setWebChromeClient((com.tencent.smtt.sdk.WebChromeClient) mX5ChromeClient);
                } catch (Exception ex) {
                    LogUtil.d(TAG, ex.getMessage(), ex);
                    return false;
                }
            } else if (ClassExistHelper.instanceOfUcWebView(view)) {
                com.uc.webview.export.WebView webView = (com.uc.webview.export.WebView) view;
                mUcChromeClient = getUcChromeClient();
                try {
                    webView.setWebChromeClient((com.uc.webview.export.WebChromeClient) mUcChromeClient);
                } catch (Exception ex) {
                    LogUtil.d(TAG, ex.getMessage(), ex);
                    return false;
                }
            }
            view.setTag(GrowingIO.GROWING_WEB_CLIENT_KEY, true);
        }
        return true;
    }

    private static void setWebChromeClient(WebView webView, WebChromeClient chromeClient, Class<?> clientClazz) {
        Object object = ReflectUtil.findFieldRecur(webView, "mProvider");
        if (object == null) {
            LogUtil.e(TAG, "setWebChromeClient: mProvider is null, WebView Hook 失败");
            webView.setWebChromeClient(chromeClient);
            return;
        }
        Method method = ReflectUtil.getMethod(object.getClass(), "setWebChromeClient", clientClazz);
        if (method != null) {
            try {
                method.invoke(object, chromeClient);
            } catch (Exception e) {
                webView.setWebChromeClient(chromeClient);
                LogUtil.d(TAG, e);
            }
        }
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        view.removeCallbacks(this);
        String url = view.getUrl();
        if (url == null)
            return;
        if (newProgress >= MIN_PROGRESS_FOR_HOOK) {
            view.postDelayed(this, HOOK_DELAY);
        } else {
            checkAndResetState(url);
            view.setTag(AbstractGrowingIO.GROWING_WEB_VIEW_URL, url);
        }
    }

    private void checkAndResetState(String currentUrl) {
        if (currentUrl.hashCode() != lastHostAndPortHash) {
            lastHostAndPortHash = currentUrl.hashCode();
            LogUtil.d(TAG, "checkAndResetState, found url changed, reset Hook State");
            mReturnedData = false;
        }
    }

    public void onVdsAgentProgressChanged(View webView, int progress) {
        if (webView instanceof WebView) {
            onProgressChanged((WebView) webView, progress);
        } else if (ClassExistHelper.instanceOfX5WebView(webView)) {
            onX5ProgressChanged((com.tencent.smtt.sdk.WebView) webView, progress);
        } else if (ClassExistHelper.instanceOfUcWebView(webView)) {
            onUcProgressChanged((com.uc.webview.export.WebView) webView, progress);
        }
    }

    public void onX5ProgressChanged(com.tencent.smtt.sdk.WebView webView, int progress) {
        webView.removeCallbacks(VdsJsHelper.this);
        String url = webView.getUrl();
        if (url == null) {
            return;
        }
        if (progress >= MIN_PROGRESS_FOR_HOOK) {
            webView.postDelayed(VdsJsHelper.this, HOOK_DELAY);
        } else {
            checkAndResetState(url);
            webView.setTag(AbstractGrowingIO.GROWING_WEB_VIEW_URL, url);
        }
    }

    public void onUcProgressChanged(com.uc.webview.export.WebView webView, int progress) {
        webView.removeCallbacks(VdsJsHelper.this);
        String url = webView.getUrl();
        if (url == null) {
            return;
        }
        if (progress >= MIN_PROGRESS_FOR_HOOK) {
            webView.postDelayed(VdsJsHelper.this, HOOK_DELAY);
        } else {
            checkAndResetState(url);
            webView.setTag(AbstractGrowingIO.GROWING_WEB_VIEW_URL, url);
        }
    }

    private com.tencent.smtt.sdk.WebChromeClient getX5ChromeClient() {
        if (mX5ChromeClient == null) {
            mX5ChromeClient = new com.tencent.smtt.sdk.WebChromeClient() {
                @Override
                public void onProgressChanged(com.tencent.smtt.sdk.WebView webView, int i) {
                    onX5ProgressChanged(webView, i);
                }
            };
        }
        return (com.tencent.smtt.sdk.WebChromeClient) mX5ChromeClient;
    }

    private com.uc.webview.export.WebChromeClient getUcChromeClient() {
        if (mUcChromeClient == null) {
            mUcChromeClient = new com.uc.webview.export.WebChromeClient() {
                @Override
                public void onProgressChanged(com.uc.webview.export.WebView webView, int i) {
                    onUcProgressChanged(webView, i);
                }
            };
        }
        return (com.uc.webview.export.WebChromeClient) mUcChromeClient;
    }

    private void loadUrlWithCheck(View webView, String url) {
        if (!ActivityUtil.isDestroy(webView.getContext())) {
            loadUrlWithCatch(webView, url);
        }
    }

    private void loadUrlWithCatch(View webView, String... urls) {
        if (isDestroyed(webView)) {
            LogUtil.d(TAG, "loadUrlWithCatch, webView has destroyed.");
            return;
        }
        try {
            if (webView instanceof WebView) {
                for (String url : urls) {
                    ((WebView) webView).loadUrl(url);
                }
            } else if (ClassExistHelper.instanceOfX5WebView(webView)) {
                for (String url : urls) {
                    ((com.tencent.smtt.sdk.WebView) webView).loadUrl(url);
                }
            } else if (ClassExistHelper.instanceOfUcWebView(webView)) {
                for (String url : urls) {
                    ((com.uc.webview.export.WebView) webView).loadUrl(url);
                }
            } else {
                throw new IllegalStateException("NOT SUPPORT THIS WEB VIEW");
            }
        } catch (Throwable e) {
            String message = e.getMessage();
            if (message != null && message.contains("call on destroyed WebView")) {
                LogUtil.d(TAG, e);
            } else {
                LogUtil.e(TAG, message, e);
            }
        }
    }

    /**
     * 判断WebView是否调用了destory方法
     * https://github.com/crosswalk-project/chromium-crosswalk/blob/master/android_webview/java/src/org/chromium/android_webview/AwContents.java
     */
    public static boolean isDestroyed(View webView) {
        if (!(webView instanceof WebView))
            return false;
        return WebViewUtil.isDestroyed((WebView) webView);
    }

    private void handleCustomEvent(JSONObject event) {
        LogUtil.d(TAG, event);
        try {
            mMsgProcessor.saveCustomEvent(new CustomEvent(event, mPageName));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void handlePageVariableEvent(JSONObject event) {
        LogUtil.d(TAG, event);
        try {
            mMsgProcessor.persistEvent(new PageVariableEvent(event, mPageName));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void handleEvar(JSONObject event) {
        LogUtil.d(TAG, event);
        try {
            mMsgProcessor.persistEvent(new ConversionEvent(event));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private boolean handleUploadData(String event) {
        try {
            JSONObject object = new JSONObject(event);
            // 限制query长度，避免过长导致事件的堆压
            String query = object.optString("q", "");
            if (!query.isEmpty() && query.length() > 5000) {
                object.put("q", query.substring(0, 5000));
            }
            String type = object.getString("t");
            if (type.equals("cstm")) {
                handleCustomEvent(object);
                return true;
            } else if (type.equals("pvar")) {
                handlePageVariableEvent(object);
                return true;
            } else if (type.equals("evar")) {
                handleEvar(object);
                return true;
            } else if (type.equals("ppl")) {
                handlePeopleEvent(object);
                return true;
            }
        } catch (Throwable e) {

        }
        return false;
    }

    private void handlePeopleEvent(JSONObject event) {
        LogUtil.d(TAG, event);
        try {
            mMsgProcessor.persistEvent(new PeopleEvent(event));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
