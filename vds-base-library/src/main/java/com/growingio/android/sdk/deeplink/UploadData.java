package com.growingio.android.sdk.deeplink;

import android.os.Build;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.growingio.android.sdk.base.event.HttpCallBack;
import com.growingio.android.sdk.base.event.HttpEvent;
import com.growingio.android.sdk.base.event.SocketEvent;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.DeviceUUIDFactory;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.eventcenter.EventCenter;
import com.growingio.android.sdk.utils.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by denghuaxin on 2018/3/10.
 */

class UploadData {
    private final static String TAG = "GIO.deeplink.upload";
    private int retry = 3;
    private String mUrl;
    private String mRequestMethod;
    private Map<String, String> mHeaders;
    private byte[] mData;
    private JSONObject reengageEvent;

    public UploadData(JSONObject reengageEvent, byte[] data) {

        this.reengageEvent = reengageEvent;
        try {
            this.mRequestMethod = reengageEvent.getString("method");
            Pair<String, Map<String, String>> urlInfo = getUrlAndHeaderFromJSON(reengageEvent);
            if (urlInfo != null) {
                this.mUrl = urlInfo.first;
                this.mHeaders = urlInfo.second;
                this.mData = data;
            }
        } catch (JSONException jsonException) {
            LogUtil.e(TAG, jsonException.toString());
        }
    }

    private Pair<String, Map<String, String>> getUrlAndHeaderFromJSON(final JSONObject reengageEvent) {
        try {
            Map<String, String> header = new HashMap<>();
            String url = null;
            StringBuffer urlBuilder = new StringBuffer(reengageEvent.getString("host"));
            urlBuilder.append("?");
            Iterator<String> keys = reengageEvent.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                switch (key) {
                    case "host":
                        break;
                    case "header":
                        JSONObject headerJSON = reengageEvent.getJSONObject("header");
                        Iterator<String> headerKes = headerJSON.keys();
                        while (headerKes.hasNext()) {
                            String hKey = headerKes.next();
                            header.put(hKey, headerJSON.getString(hKey));
                        }
                        break;
                    case "method":
                        break;
                    default:
                        urlBuilder.append(key + "=").append(NetworkUtil.encode(reengageEvent.getString(key)));
                        if (keys.hasNext())
                            urlBuilder.append("&");
                        break;
                }
            }
            url = urlBuilder.toString();
            if (!header.isEmpty() && !TextUtils.isEmpty(url)) {
                return new Pair<>(url, header);
            }
        } catch (JSONException jsonException) {
            LogUtil.e(TAG, jsonException.toString());
        }
        return null;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getRequestMethod() {
        return mRequestMethod;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    public byte[] getData() {
        return this.mData;
    }

    public enum UploadType {
        REENGAGE
    }

    public static class Builder {
        private UploadType type;
        private DeeplinkInfo deeplinkInfo;

        public Builder setType(UploadType type) {
            this.type = type;
            return this;
        }

        public Builder setDeeplinkInfo(DeeplinkInfo deeplinkInfo) {
            this.deeplinkInfo = deeplinkInfo;
            return this;
        }

        public UploadData build() {
            UploadData data = null;
            switch (type) {
                case REENGAGE:
                    data = buildReengage();
                    break;
                default:
                    break;
            }
            return data;
        }

        private UploadData buildReengage() {

            JSONObject reengageEvent = getReengageEvent();
            return new UploadData(reengageEvent, null);
        }

        private JSONObject getReengageEvent() {
            JSONObject reengageEvent = new JSONObject();
            CoreAppState appState = CoreInitialize.coreAppState();
            DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
            GConfig gConfig = CoreInitialize.config();
            try {
                reengageEvent.put("t", "reengage");
                reengageEvent.put("u", GrowingIO.getInstance().getDeviceId());
                reengageEvent.put("d", appState.getSPN());
                reengageEvent.put("dm", Build.MODEL == null ? "" : Build.MODEL);
                reengageEvent.put("osv", Build.VERSION.RELEASE == null ? "" : Build.VERSION.RELEASE);
                reengageEvent.put("ui", deviceUUIDFactory.getIMEI());
                reengageEvent.put("iv", deviceUUIDFactory.getAndroidId());
                reengageEvent.put("link_id", deeplinkInfo.linkID);
                reengageEvent.put("click_id", deeplinkInfo.clickID);
                reengageEvent.put("tm_click", deeplinkInfo.clickTM);
                reengageEvent.put("tm", deeplinkInfo.tm);
                reengageEvent.put("cs1", gConfig.getAppUserId() == null ? "" : gConfig.getAppUserId());
                reengageEvent.put("gaid", deviceUUIDFactory.getGoogleAdId());
                reengageEvent.put("host", String.format(NetworkConfig.getInstance().getDeeplinkHost(), appState.getProjectId(), "android"));
                reengageEvent.put("method", "GET");
                JSONObject header = new JSONObject();
                header.put("ua", deviceUUIDFactory.getUserAgent());
                header.put("ip", deviceUUIDFactory.getIp());
                reengageEvent.put("header", header);
                reengageEvent.put("var", deeplinkInfo.customParams);
            } catch (JSONException jsonexception) {
            }
            return reengageEvent;
        }
    }


    public void upload() {
        if (CoreInitialize.config().isEnabled()){
            final HttpEvent httpEvent = new HttpEvent();
            httpEvent.setUrl(mUrl);
            if ("GET".equals(mRequestMethod.toUpperCase())){
                httpEvent.setRequestMethod(HttpEvent.REQUEST_METHOD.GET);
            }else{
                httpEvent.setRequestMethod(HttpEvent.REQUEST_METHOD.POST);
            }
            httpEvent.setHeaders(mHeaders);
            httpEvent.setData(mData);
            httpEvent.setCallBack(new HttpCallBack() {
                @Override
                public void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
                    if (responseCode == HttpURLConnection.HTTP_OK){
                        if (reengageEvent != null)
                            EventCenter.getInstance().post(new SocketEvent(SocketEvent.EVENT_TYPE.SEND_DEBUGGER, reengageEvent));
                        LogUtil.i(TAG, "upload success! url " + mUrl);
                    }else if (retry-- > 0){
                        EventCenter.getInstance().post(httpEvent);
                    }
                }
            });
            EventCenter.getInstance().post(httpEvent);
        }
    }
}
