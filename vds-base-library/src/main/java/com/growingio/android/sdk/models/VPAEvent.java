package com.growingio.android.sdk.models;

import android.text.TextUtils;

import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ThreadUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by xyz on 15/12/25.
 */
public abstract class VPAEvent {
    public static final String TAG = "GIO.VPAEvent";
    public static final String GLOBAL_EVENT_SEQUENCE_ID = "gesid";
    public static final String EACH_TYPE_EVENT_SEQUENCE_ID = "esid";

    long time;
    public String mPageName;

    public VPAEvent(long time){
        this.time = time;
    }

    protected CoreAppState getAPPState() {
        return CoreInitialize.coreAppState();
    }

    protected GConfig getConfig() {
        return CoreInitialize.config();
    }

    public long getTime() {
        return time;
    }

    protected void patchLocation(JSONObject jsonObject) {
        try {
            jsonObject.put("lat", getAPPState().getLatitude());
            jsonObject.put("lng", getAPPState().getLongitude());
        } catch (Exception e) {
            LogUtil.d(TAG, "patch location error ", e);
        }
    }

    protected void patchNetworkState(JSONObject jsonObject) {
        try {
            jsonObject.put("r", getAPPState().getNetworkStateName());
        } catch (Exception e) {
            LogUtil.d(TAG, "patch NetWorkState value error: ", e);
        }
    }

    /**
     * 发送数据，添加androidId
     *
     * @param jsonObject
     */
    protected void patchAndroidId(JSONObject jsonObject) {
        try {
            jsonObject.put("adrid", CoreInitialize.deviceUUIDFactory().getAndroidId());
        } catch (Exception e) {
            LogUtil.d(TAG, "patch androidId value error: ", e);
        }
    }

    /**
     * 发送数据，添加 IMEI
     *
     * @param jsonObject
     */
    protected void patchIMEI(JSONObject jsonObject) {
        try {
            jsonObject.put("imei", CoreInitialize.deviceUUIDFactory().getIMEI());
        } catch (Exception e) {
            LogUtil.d(TAG, "patch imei value error: ", e);
        }
    }

    public abstract String getType();

    public String getFullType() {
        return getType();
    }

    public int size() {
        return 1;
    }

    protected JSONObject getCommonProperty() {
        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("s", CoreInitialize.sessionManager().getSessionIdInner());
            jsonObject.put("t", getType());
            jsonObject.put("tm", time);

            String spn = getAPPState().getSPN();
            jsonObject.put("d", spn);
            if (mPageName != null) {
                jsonObject.put("p", mPageName);
            }


            String cs1 = getConfig().getAppUserId();
            if (!TextUtils.isEmpty(cs1)) {
                jsonObject.put("cs1", cs1);
            }

        } catch (JSONException e) {
            LogUtil.d(TAG, "generate common event property error", e);
        }

        return jsonObject;
    }

    public abstract JSONObject toJson();

    /**
     * 用于补充耗时的属性填充
     * 目前由DataSubscriber调用, 主要获取Google的GoogleAdId
     */
    public void backgroundWorker(){
        if (ThreadUtils.runningOnUiThread()){
            throw new IllegalStateException("backgroundWorker don't allow run on UI Thread");
        }
    }
}
