package com.growingio.android.sdk.models;

import android.text.TextUtils;

import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lishaojie on 2017/4/13.
 */

public class ConversionEvent extends VPAEvent {
    public static final String TYPE_NAME = "evar";
    private JSONObject mVariable;
    protected JSONObject mWebEvent;

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    public ConversionEvent(JSONObject props, long ptm) {
        super(ptm);
        mVariable = props;
    }


    public ConversionEvent(JSONObject webEvent) throws Throwable{
        super(webEvent.optLong("tm") != 0 ? webEvent.optLong("tm") : System.currentTimeMillis());
        webEvent.put("s", CoreInitialize.sessionManager().getSessionIdInner());
        String d = webEvent.getString("d");
        webEvent.put("d", getAPPState().getSPN() + Constants.WEB_PART_SEPARATOR + d);
        String cs1 = getConfig().getAppUserId();
        if (!TextUtils.isEmpty(cs1)) {
            webEvent.put("cs1", cs1);
        }
        this.mWebEvent = webEvent;
    }

    @Override
    public JSONObject toJson() {
        try {
            if (mWebEvent != null) {
                return mWebEvent;
            }else{
                JSONObject jsonObject = getCommonProperty();
                try {
                    jsonObject.put("var", mVariable);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return jsonObject;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
