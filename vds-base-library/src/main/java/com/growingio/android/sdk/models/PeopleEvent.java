package com.growingio.android.sdk.models;

import org.json.JSONObject;

/**
 * Created by lishaojie on 2017/4/13.
 */

public class PeopleEvent extends ConversionEvent {
    public static final String TYPE_NAME = "ppl";

    public PeopleEvent(JSONObject props, long ptm) {
        super(props, ptm);
    }

    public PeopleEvent(JSONObject webEvent) throws Throwable{
        super(webEvent);
        super.mWebEvent = webEvent;
    }

    @Override
    public String getType() {
        return TYPE_NAME;
    }
}
