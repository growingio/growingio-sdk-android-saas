package com.growingio.android.sdk.models;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.collection.NetworkConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by denghuaxin on 2017/9/26.
 */

public class PatternServer {
    private String ai;
    private String domain;
    private String page;
    private String token;

    private String xpath;

    public PatternServer(String ai, String domain, String page, String token){
        this.ai = ai;
        this.domain = domain;
        this.page = page;
        this.token = token;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public JSONObject toJson(){
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ai", ai);
            jsonObject.put("cs1", CoreInitialize.config().getAppUserId());
            jsonObject.put("d", domain);
            jsonObject.put("p", page);
            jsonObject.put("gtaHost", NetworkConfig.getInstance().getGtaHost());
            jsonObject.put("x", xpath);
            jsonObject.put("s", CoreInitialize.sessionManager().getSessionIdInner());
            jsonObject.put("token", token);
            jsonObject.put("u", GrowingIO.getInstance().getDeviceId());
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}