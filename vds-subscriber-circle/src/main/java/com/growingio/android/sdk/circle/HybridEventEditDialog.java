package com.growingio.android.sdk.circle;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.growingio.android.sdk.base.event.HeatMapEvent;
import com.growingio.android.sdk.circle.utils.CircleUtil;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.ipc.GrowingIOIPC;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.ViewTraveler;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.GJSONStringer;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.NonUiContextUtil;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.WindowHelper;
import com.growingio.eventcenter.EventCenter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by lishaojie on 16/8/25.
 */

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class HybridEventEditDialog extends DialogFragment implements CircleManager.SnapshotMessageListener {

    private static boolean hasEditDialog = false;
    private final static String TAG = "GIO.HybridEvent";
    public static final String DO_NOT_DRAW = "DO_NOT_DRAW";
    private static final String CONTENT_BUNDLE_KEY = "circle_content";
    @SuppressLint("StaticFieldLeak")
    private static WebView mWebView;
    private static WeakReference<HybridEventEditDialog> mWebViewAttachedDialog = new WeakReference<HybridEventEditDialog>(null);
    private static HybridCircleContent mContent;

    public HybridEventEditDialog() {
        ScreenshotHelper.initial();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        PendingStatus.mCanShowCircleTag = false;
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Light_NoTitleBar);
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.getWindow().getDecorView().setTag(DO_NOT_DRAW);
        prepareWebView(dialog.getContext());
        GrowingIO.ignoredView(dialog.getWindow().getDecorView());
        if (savedInstanceState != null) {
            String data = savedInstanceState.getString(CONTENT_BUNDLE_KEY);
            if (data != null) {
                mContent = new HybridCircleContent(data);
            }
        }
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    if (mWebView.canGoBack()) {
                        mWebView.goBack();
                    } else {
                        dismiss();
                    }
                    return true;
                }
                return false;
            }
        });
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setWebViewClient(mWebViewClient);
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mContent != null && mContent.data != null) {
            outState.putString(CONTENT_BUNDLE_KEY, mContent.data);
        }
    }

    @SuppressLint("JavascriptInterface")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        hasEditDialog = true;
        EventCenter.getInstance().post(new HeatMapEvent(HeatMapEvent.EVENT_TYPE.HIDE));
        mWebViewAttachedDialog = new WeakReference<HybridEventEditDialog>(this);
        return mWebView;
    }

    @Override
    public void onStart() {
        super.onStart();
        CircleManager manager = CircleManager.getInstance();
        if (manager != null)
            manager.removeFloatViews();

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        PendingStatus.mCanShowCircleTag = true;
        EventCenter.getInstance().post(new HeatMapEvent(HeatMapEvent.EVENT_TYPE.SHOW));
        super.onDismiss(dialog);
        detachWebView();
        hasEditDialog = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PendingStatus.mCanShowCircleTag = true;
        detachWebView();
        CircleManager manager = CircleManager.getInstance();
        manager.showCircleView(null);
        manager.setSnapshotMessageListener(null);
    }

    public static boolean hasEditDialog() {
        return hasEditDialog;
    }

    private void detachWebView() {
        if (mWebView != null && this == mWebViewAttachedDialog.get()) {
            mWebView.loadUrl("javascript:hideBody();", null);
            ViewGroup parent = (ViewGroup) mWebView.getParent();
            if (parent != null) {
                parent.removeView(mWebView);
            }
        }
    }

    WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onCloseWindow(WebView window) {
            dismiss();
        }
    };

    WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            CircleManager manager = CircleManager.getInstance();
            manager.setSnapshotMessageListener(HybridEventEditDialog.this);
            manager.refreshSnapshotWithType("touch", null, null);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.equals("growing.internal://close-web-view")) {
                dismiss();
                return true;
            }
            return false;
        }
    };

    @SuppressLint({"JavascriptInterface", "AddJavascriptInterface", "SetJavaScriptEnabled"})
    static void prepareWebView(Context context) {
        if (mWebView == null) {
            mWebView = new WebView(NonUiContextUtil.getWindowContext(context.getApplicationContext()));
            mWebView.getSettings().setJavaScriptEnabled(true);
            mWebView.getSettings().setDomStorageEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mWebView.setWebContentsDebuggingEnabled(GConfig.DEBUG);
            }
        }
        mWebView.clearHistory();
        mWebView.addJavascriptInterface(mContent, "_hybrid_circle_content");
        mWebView.loadUrl(NetworkConfig.getInstance().getCirclePageUrl());
    }

    public void setContent(Activity foregroundActivity, final List<ViewNode> hitViews, String path, String spn, final Runnable visibleCallback) {
        mContent = new HybridCircleContent(hitViews, foregroundActivity, path, spn);
        visibleCallback.run();
    }

    @Override
    public void onMessage(String message) {
        if (mWebView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mWebView.evaluateJavascript("_setGrowingIOFullHybridCircleData(" + message + ")", null);
            }
        }
    }

    static class HybridCircleContent {
        boolean haveWebNode;
        String data;
        CoreAppState coreAppState;

        HybridCircleContent(String data) {
            this.data = data;
            coreAppState = CoreInitialize.coreAppState();
        }

        HybridCircleContent(List<ViewNode> nodes, Activity foregroundActivity, String path, String domain) {
            coreAppState = CoreInitialize.coreAppState();
            GrowingIOIPC growingIOIPC = CoreInitialize.growingIOIPC();
            JSONObject dataObj = new JSONObject();
            try {
                PackageManager packageManager = coreAppState.getGlobalContext().getPackageManager();
                PackageInfo info = packageManager.getPackageInfo(coreAppState.getGlobalContext().getPackageName(), 0);
                dataObj.put("sdkVersion", GConfig.GROWING_VERSION);
                dataObj.put("projectId", coreAppState.getProjectId());
                dataObj.put("userId", growingIOIPC.getGioUserId());
                dataObj.put("accessToken", growingIOIPC.getToken());
                dataObj.put("appVersion", info.versionName);
                dataObj.put("platform", "Android");
                JSONArray pages = new JSONArray();
                JSONObject nativePage = new JSONObject();
                nativePage.put("domain", domain);
                nativePage.put("page", path);
                CharSequence title = foregroundActivity.getTitle();
                if (TextUtils.isEmpty(title)) {
                    title = Util.getSimpleClassName(foregroundActivity.getClass());
                }
                nativePage.put("title", title);
                nativePage.put("snapshot", "data:image/jpeg;base64," + Base64.encodeToString(
                        ScreenshotHelper.captureAllWindows(WindowHelper.getSortedWindowViews(), null), Base64.NO_WRAP));
                final JSONArray nativeElems = new JSONArray();
                final JSONArray webElems = new JSONArray();
                JSONObject webPage = null;
                if (nodes.size() > 0 && nodes.get(0).mWebElementInfo != null) {
                    ViewNode webNode = nodes.get(0);
                    haveWebNode = true;
                    webPage = new JSONObject();
                    webPage.put("domain", domain + Constants.WEB_PART_SEPARATOR + webNode.mWebElementInfo.mHost);
                    webPage.put("page", path + Constants.WEB_PART_SEPARATOR + webNode.mWebElementInfo.mPath);
                    webPage.put("query", webNode.mWebElementInfo.mQuery);
//                    webPage.put("pg", subPageName);
                }
                for (ViewNode node : nodes) {
                    ViewTraveler traveler = new ViewTraveler() {
                        @Override
                        public void traverseCallBack(ViewNode viewNode) {
                            if (haveWebNode) {
                                webElems.put(CircleUtil.getImpressObj(viewNode));
                            } else {
                                nativeElems.put(CircleUtil.getImpressObj(viewNode));
                            }
                        }
                    };
                    node.setViewTraveler(traveler);
                    traveler.traverseCallBack(node);
                    node.traverseChildren();
                }
                nativePage.put("e", nativeElems);
                pages.put(nativePage);
                if (webPage != null) {
                    webPage.put("e", webElems);
                    pages.put(webPage);
                }
                dataObj.put("pages", pages);
                dataObj.put("zone", NetworkConfig.getInstance().zoneInfo());
                String gtaHost = NetworkConfig.getInstance().getGtaHost();
                if (!TextUtils.isEmpty(gtaHost))
                    dataObj.put("gtaHost", URLEncoder.encode(gtaHost, "UTF-8"));

                data = new GJSONStringer().convertToString(dataObj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String getData() {
            LogUtil.d(TAG, "Data:\n"+data);
            return data;
        }

    }
}
