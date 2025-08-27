package com.growingio.android.sdk.heatmap;

import com.growingio.android.sdk.models.HeatMapData;
import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 郑童宇 on 2016/11/22.
 */

public class HeatMapResponse {
    private boolean success;
    private String reason;
    private HeatMapData[] data;

    private static String TAG = "GIO.HeatMapResponse";

    public HeatMapResponse(JSONObject jsonObject) {

        if (jsonObject != null)
            LogUtil.d("HeatMapResponse", jsonObject.toString());
        try {
            success = jsonObject.getBoolean("success");

            if (jsonObject.has("reason")) {
                reason = jsonObject.getString("reason");
            }

            if (jsonObject.has("data")) {
                data = HeatMapData.parseArray(jsonObject.getJSONArray("data"));
            }
        } catch (Throwable e) {
            LogUtil.e(TAG, "HeatMapResponse解析异常" + e);
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public HeatMapData[] getData() {
        return data;
    }

    public void setData(HeatMapData[] data) {
        this.data = data;
    }
}
