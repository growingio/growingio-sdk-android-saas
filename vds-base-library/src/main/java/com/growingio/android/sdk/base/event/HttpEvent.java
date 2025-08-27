package com.growingio.android.sdk.base.event;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.ipc.GrowingIOIPC;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * author CliffLeopard
 * time   2018/7/5:下午5:31
 * email  gaoguanling@growingio.com
 */
public class HttpEvent {
    private HttpCallBack callBack;
    private String url;
    private  REQUEST_METHOD requestMethod;
    private Map<String, String> headers;
    private byte[] data;
    private long mSinceModified = 0;

    public HttpCallBack getCallBack() {
        return callBack;
    }

    public void setCallBack(HttpCallBack callBack) {
        this.callBack = callBack;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public REQUEST_METHOD getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(REQUEST_METHOD requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getmSinceModified() {
        return mSinceModified;
    }

    public void setmSinceModified(long mSinceModified) {
        this.mSinceModified = mSinceModified;
    }

    public enum REQUEST_METHOD {
        GET,
        POST,
    }

    /**
     * 返回一个组装好headers的HttpEvent
     * @param url 请求地址
     * @param body body 体
     */
    public static HttpEvent createCircleHttpEvent(String url, JSONObject body, boolean isGetMethod){
        HttpEvent httpEvent = new HttpEvent();
        httpEvent.setUrl(url);
        if (body != null){
            httpEvent.setData(body.toString().getBytes());
        }

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        GrowingIOIPC growingIOIPC = CoreInitialize.growingIOIPC();
        if (growingIOIPC != null){
            headers.put("token", growingIOIPC.getToken());
        }
        headers.put("accountId", CoreInitialize.coreAppState().getProjectId());
        httpEvent.setHeaders(headers);

        if (isGetMethod){
            httpEvent.setRequestMethod(REQUEST_METHOD.GET);
        }else{
            httpEvent.setRequestMethod(REQUEST_METHOD.POST);
        }
        return httpEvent;
    }
}
