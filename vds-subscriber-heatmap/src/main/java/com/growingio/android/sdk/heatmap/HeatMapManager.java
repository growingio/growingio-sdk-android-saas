package com.growingio.android.sdk.heatmap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

import com.growingio.android.sdk.autoburry.AutoBuryAppState;
import com.growingio.android.sdk.autoburry.AutoBuryObservableInitialize;
import com.growingio.android.sdk.autoburry.VdsJsBridgeManager;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.ipc.GrowingIOIPC;
import com.growingio.android.sdk.models.HeatMapData;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.ViewTraveler;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.FloatWindowManager;
import com.growingio.android.sdk.utils.ObjectUtils;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;

import org.json.JSONException;
import org.json.JSONObject;

import static android.view.View.GONE;
import static com.growingio.android.sdk.pending.PendingStatus.FLOAT_VIEW_TYPE;

/**
 * Creator: tongyuzheng
 * Create Time: 2017/11/19
 * Description: 控制热图数据的请求和热图页面的显示
 */

public class HeatMapManager {
    private final static String TAG = "GIO.HeatMapManager";

    private boolean initHeatMapView = false;

    private HeatMapView heatMapView;
    private HeatMapNodeTraveler heatMapNodeTraveler;

    private final static Object sInstanceObject = new Object();
    private static HeatMapManager sInstance;

    private CoreAppState coreAppState;
    private AutoBuryAppState autoBuryAppState;
    private GrowingIOIPC growingIOIPC;

    private HeatMapManager() {
        coreAppState = CoreInitialize.coreAppState();
        autoBuryAppState = AutoBuryObservableInitialize.autoBuryAppState();
        growingIOIPC = CoreInitialize.growingIOIPC();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    @SuppressLint("RtlHardcoded")
    public void initHeatMapView() {
        if (initHeatMapView) {
            return;
        }

        heatMapView = new HeatMapView(coreAppState.getGlobalContext());
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        WindowManager.LayoutParams layoutParams =
                new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                        FLOAT_VIEW_TYPE, flags, PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.setTitle("HeatMapView");
        FloatWindowManager.getInstance().addView(heatMapView, layoutParams);

        heatMapView.setVisibility(GONE);

        heatMapNodeTraveler = new HeatMapNodeTraveler(heatMapView);

        initHeatMapView = true;
    }

    public static HeatMapManager getInstance() {
        synchronized (sInstanceObject) {
            if (sInstance == null) {
                sInstance = new HeatMapManager();
            }
        }
        return sInstance;
    }

    public boolean isHeatMapOn() {
        return PendingStatus.mIsHeatMapOn;
    }

    public void showHeatMapView() {
        heatMapView.show();
        heatMapNodeTraveler.beginTraverseImmediately();
    }

    public void hideHeatMapView() {
        heatMapView.hide();
        if (heatMapNodeTraveler != null) {
            heatMapNodeTraveler.stopTraverse();
        }
    }

    public void traverseNodeImmediately() {
        if (PendingStatus.mIsHeatMapOn) {
            heatMapNodeTraveler.beginTraverseImmediately();
        }
    }

    public void setHeatMapState(boolean heatMapOn) {
        PendingStatus.mIsHeatMapOn = heatMapOn;

        if (heatMapOn) {
            getHeatMapData();
        } else {
            callWebViewHeatMap();
        }
    }

    public void getHeatMapData() {
        heatMapView.clearData();
        heatMapNodeTraveler.clear();

        final String pageName = autoBuryAppState.getPageName();

        HeatMapApi.getHeatMapData(pageName, new HeatMapApi.HeatMapCallback() {
            @Override
            public void getHeatMapFinish(HeatMapResponse heatMapResponse) {
                if (heatMapResponse == null) {
                    getHeatMapDataFail("请求热图数据失败");
                    return;
                }

                if (heatMapResponse.isSuccess()) {
                    getHeatMapDataSuccess(heatMapResponse.getData(), pageName);
                } else {
                    getHeatMapDataFail("请求热图数据失败:" + heatMapResponse.getReason());
                }
            }
        });

        callWebViewHeatMap();
    }

    private void getHeatMapDataSuccess(HeatMapData[] heatMapDataArray, String pageName) {
        if (ObjectUtils.equals(autoBuryAppState.getPageName(), pageName)) {
            heatMapNodeTraveler.updateHeatMapDataArray(heatMapDataArray);
            heatMapView.updateData(heatMapDataArray);
        }
    }

    private void getHeatMapDataFail(String failMessage) {
        Toast.makeText(coreAppState.getGlobalContext(), failMessage, Toast.LENGTH_SHORT).show();
    }

    //  All WebView methods must be called on the same thread.
    private void callWebViewHeatMap() {
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = coreAppState.getResumedActivity();
                if (activity != null) {
                    ViewHelper.traverseWindow(activity.getWindow().getDecorView(), "", mWebHeatMapTraveler);
                }
            }
        });
    }

    private ViewTraveler mWebHeatMapTraveler = new ViewTraveler() {
        @Override
        public void traverseCallBack(ViewNode viewNode) {
            if (viewNode.mView instanceof WebView || ClassExistHelper.instanceOfX5WebView(viewNode.mView)) {
                View webView = viewNode.mView;
                if (VdsJsBridgeManager.isWebViewHooked(webView)) {
                    if (PendingStatus.mIsHeatMapOn) {
                        JSONObject openHeatMapObject = new JSONObject();
                        try {
                            openHeatMapObject.put("ai", coreAppState.getProjectId());
                            openHeatMapObject.put("d", coreAppState.getSPN());
                            openHeatMapObject.put("p", CoreInitialize.messageProcessor().getPageNameWithPending());
                            openHeatMapObject.put("token", growingIOIPC.getToken());
                        } catch (JSONException e) {
                            Log.d(TAG, "generate openHeatMapObject json error :" + e);
                        }

                        Util.callJavaScript(webView, "_vds_hybrid.showHeatMap", openHeatMapObject);
                    } else {
                        Util.callJavaScript(webView, "_vds_hybrid.hideHeatMap");
                    }
                }
            }
        }
    };
}
