package com.growingio.android.sdk.models.ad;

import android.os.Build;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.models.VPAEvent;
import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

abstract class BaseAdEvent extends VPAEvent {
    private static final String TAG = "BaseAdEvent";

    public BaseAdEvent() {
        this(System.currentTimeMillis());
    }

    private BaseAdEvent(long time) {
        super(time);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("imei", CoreInitialize.deviceUUIDFactory().getIMEI());
            json.put("adrid", CoreInitialize.deviceUUIDFactory().getAndroidId());
            json.put("ua", CoreInitialize.deviceUUIDFactory().getUserAgent());
            json.put("dm", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);
            json.put("osv", Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
            json.put("d", getAPPState().getSPN());
            json.put("t", getType());
            json.put("tm", getTime());
        } catch (JSONException e) {
            LogUtil.d(TAG, "generation the AD Event error", e);
        }
        return json;
    }
}
