package com.growingio.android.sdk.autoburry;

import android.view.View;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lishaojie on 16/3/9.
 */
public class VdsJsBridgeManager {
    private static final String TAG = "GIO.VdsManager";
    private static VdsJsBridgeManager mInstance;
    private List<SnapshotCallback> mSnapshotCallbacks;

    private VdsJsBridgeManager() {
        mSnapshotCallbacks = new ArrayList<SnapshotCallback>();
    }

    public static VdsJsBridgeManager getInstance() {
        if (mInstance == null) {
            mInstance = new VdsJsBridgeManager();
        }
        return mInstance;
    }

    public void registerSnapshotCallback(SnapshotCallback callback) {
        if (mSnapshotCallbacks.indexOf(callback) == -1)
            mSnapshotCallbacks.add(callback);
    }

    public boolean unregisterSnapshotCallback(SnapshotCallback callback) {
        return mSnapshotCallbacks.remove(callback);
    }

    public void onSnapshotFinished(VdsJsHelper helper, List<ViewNode> nodes) {
        for (SnapshotCallback callback : mSnapshotCallbacks) {
            callback.onSnapshotFinished(nodes);
        }
    }

    public static boolean isWebViewHooked(View webView) {
        Object bridgeTag = webView.getTag(GrowingIO.GROWING_WEB_BRIDGE_KEY);
        return bridgeTag != null && bridgeTag instanceof VdsJsHelper;
    }

    public static void hookWebViewIfNeeded(View webView) {
        GConfig config = CoreInitialize.config();
        if (config == null
                || Util.isIgnoredView(webView)
                || (!config.shouldTrackWebView() && !Util.isTrackWebView(webView))) {
            return;
        }
        VdsJsHelper bridgeTag = (VdsJsHelper) webView.getTag(GrowingIO.GROWING_WEB_BRIDGE_KEY);
        if (bridgeTag == null) {
            bridgeTag = new VdsJsHelper(webView);
            webView.setTag(GrowingIO.GROWING_WEB_BRIDGE_KEY, bridgeTag);
        }
        bridgeTag.updateViewNodeForce();
        LogUtil.d(TAG, "hookWebViewIfNeeded: hooked ", webView);

    }

    /**
     * @deprecated TODO: 此方法在imp重做后删除
     */
    @Deprecated
    public static void refreshImpressionForce(final View webView) {
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Object bridgeTag = webView.getTag(GrowingIO.GROWING_WEB_BRIDGE_KEY);
                if (bridgeTag instanceof VdsJsHelper) {
                    ((VdsJsHelper) bridgeTag).impressAllElements();
                    ((VdsJsHelper) bridgeTag).updateViewNodeForce();
                }
            }
        });
    }

    public interface SnapshotCallback {
        void onSnapshotFinished(List<ViewNode> nodes);
    }
}
