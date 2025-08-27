package com.growingio.android.sdk.heatmap;

import android.annotation.TargetApi;
import android.os.Build;

import com.growingio.android.sdk.base.event.HttpCallBack;
import com.growingio.android.sdk.base.event.HttpEvent;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.eventcenter.EventCenter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Created by 郑童宇 on 2016/11/19.
 */

public class HeatMapApi {
    private static final String TAG = "GIO.HeatMapApi";

    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;
    private static final long HEAT_MAP_DURATION = 3 * ONE_DAY;
    private static final Object requestLocker = new Object();

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void getHeatMapData(String path, HeatMapCallback heatMapCallback) {
        requestHeatMapData(path, heatMapCallback);
    }

    private static void requestHeatMapData(String path, final HeatMapCallback heatMapCallback) {
        long currentTime = System.currentTimeMillis();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("domain", CoreInitialize.coreAppState().getSPN());
            jsonObject.put("path", path);
            jsonObject.put("beginTime", currentTime - HEAT_MAP_DURATION);
            jsonObject.put("endTime", currentTime);
            jsonObject.put("metric", "clck");
            jsonObject.put("withIndex", true);
        } catch (JSONException e) {
            LogUtil.d(TAG, "gen postHeatMapData json error");
        }

        String url = NetworkConfig.getInstance().getEndPoint() + Constants.HEAT_MAP_TAIL;
        HttpEvent event = HttpEvent.createCircleHttpEvent(url, jsonObject, false);
        event.setCallBack(new HttpCallBack() {
            @Override
            public void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
                synchronized (requestLocker) {
                    try {
                        final HeatMapResponse heatMapResponse = new HeatMapResponse(new JSONObject(new String(data)));
                        ThreadUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                heatMapCallback.getHeatMapFinish(heatMapResponse);
                            }
                        });
                    } catch (JSONException e) {
                        LogUtil.d(TAG, "parse the HeatMap error");
                    }
                }
            }
        });
        EventCenter.getInstance().post(event);
    }

    public interface HeatMapCallback {
        public void getHeatMapFinish(HeatMapResponse heatMapResponse);
    }
}
