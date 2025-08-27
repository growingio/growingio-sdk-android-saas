package com.growingio.android.sdk.models;

import com.growingio.android.sdk.utils.JsonUtil;

import org.json.JSONObject;

/**
 * Created by liangdengke on 2018/6/26.
 */
public class VisitorVarEvent extends ConversionEvent{

    public static final String TYPE_NAME = "vstr";

    public VisitorVarEvent(JSONObject vstVar, long time) {
        super(vstVar, time);
    }

    @Override
    public String getType() {
        return TYPE_NAME;
    }
}
