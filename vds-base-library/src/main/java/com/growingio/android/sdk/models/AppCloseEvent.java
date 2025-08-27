package com.growingio.android.sdk.models;

import org.json.JSONObject;

public class AppCloseEvent extends VPAEvent {
    public static final String TYPE_NAME = "cls";

    public AppCloseEvent(PageEvent page, long closeTime) {
        super(closeTime);
        mPageName = page.mPageName;

    }

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject jsonObject = getCommonProperty();
            patchNetworkState(jsonObject);
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
