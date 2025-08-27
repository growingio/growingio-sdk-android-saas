package com.growingio.android.sdk.api;

import android.util.Log;

import com.growingio.android.sdk.base.event.HttpCallBack;
import com.growingio.android.sdk.base.event.HttpEvent;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.DeviceUUIDFactory;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.ipc.GrowingIOIPC;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.eventcenter.EventCenter;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * Created by xyz on 15/11/2.
 */
public class LoginAPI implements HttpCallBack {

    final static String TAG = "GIO.LoginAPI";
    private final Object mTokenLocker = new Object();
    private HttpCallBack httpCallBack;
    private GrowingIOIPC growingIOIPC;

    public LoginAPI(){
        growingIOIPC = CoreInitialize.growingIOIPC();
    }

    public void setHttpCallBack(HttpCallBack httpCallBack) {
        this.httpCallBack = httpCallBack;
    }

    public void login(String loginToken) {
        LogUtil.d(TAG, "login with loginToken: ", loginToken);
        JSONObject jsonObject = new JSONObject();
        DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
        try {
            jsonObject.put("grantType", "login_token");
            jsonObject.put("deviceId", deviceUUIDFactory.getDeviceId());
            jsonObject.put("loginToken", loginToken);
        } catch (JSONException e) {
            Log.d(TAG, "gen login json error");
        }

        String url = NetworkConfig.getInstance().getEndPoint() + Constants.ENDPOINT_TAIL;
        HttpEvent event = HttpEvent.createCircleHttpEvent(url, jsonObject, false);
        event.setCallBack(this);
        EventCenter.getInstance().post(event);
    }


    @Deprecated
    public void logout() {
        if (growingIOIPC != null){
            growingIOIPC.setToken(null);
            growingIOIPC.setUserId(null);
        }
    }

    @Override
    public void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
        if (responseCode == HttpURLConnection.HTTP_OK && data.length > 0){
            synchronized (mTokenLocker) {
                try {
                    JSONObject rep = new JSONObject(new String(data));
                    String token = rep.getString("accessToken");
                    String userId  = rep.getString("userId");
                    if (growingIOIPC != null){
                        growingIOIPC.setToken(token);
                        growingIOIPC.setGioUserId(userId);
                    }
                    Log.i(TAG, "get access token by login token success");
                } catch (JSONException e) {
                    LogUtil.d(TAG, "parse the loginToken error");
                }
            }
        }
        httpCallBack.afterRequest(responseCode,data, mLastModified, mResponseHeaders);
    }
}
