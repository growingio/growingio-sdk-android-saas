package com.growingio.android.sdk.models.ad;

import android.text.TextUtils;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.deeplink.DeeplinkInfo;
import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class ReengageEvent extends BaseAdEvent {
    private static final String TAG = "ReengageEvent";

    public static final String TYPE_NAME = "reengage";

    private final DeeplinkInfo mDeeplinkInfo;

    public ReengageEvent(DeeplinkInfo deeplinkInfo) {
        mDeeplinkInfo = deeplinkInfo;
    }


    @Override
    public String getType() {
        return TYPE_NAME;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        try {
            json.put("link_id", mDeeplinkInfo.linkID);
            json.put("click_id", mDeeplinkInfo.clickID);
            json.put("tm_click", mDeeplinkInfo.clickTM);
            json.put("var", mDeeplinkInfo.customParams);

            String cs1 = getConfig().getAppUserId();
            if (!TextUtils.isEmpty(cs1)) {
                json.put("cs1", cs1);
            }
        } catch (JSONException e) {
            LogUtil.d(TAG, "generation the Reengage Event error", e);
        }
        return json;
    }
}
