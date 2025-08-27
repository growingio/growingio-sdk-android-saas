package com.growingio.android.sdk.models.ad;

import android.text.TextUtils;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.deeplink.DeeplinkInfo;
import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class ActivateEvent extends BaseAdEvent {
    private static final String TAG = "ActivateEvent";

    public static final String TYPE_NAME = "activate";

    private String mGoogleId;
    private String mOaid;
    private DeeplinkInfo mDeeplinkInfo;

    public ActivateEvent() {
    }

    public ActivateEvent(DeeplinkInfo deeplinkInfo) {
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
            if (!TextUtils.isEmpty(mGoogleId)) {
                json.put("gaid", mGoogleId);
            }
            if (!TextUtils.isEmpty(mOaid)) {
                json.put("oaid", mOaid);
            }

            if (mDeeplinkInfo != null) {
                json.put("link_id", mDeeplinkInfo.linkID);
                json.put("click_id", mDeeplinkInfo.clickID);
                json.put("tm_click", mDeeplinkInfo.clickTM);
                json.put("cl", "defer");
            }
        } catch (JSONException e) {
            LogUtil.d(TAG, "generation the Activate Event error", e);
        }
        return json;
    }

    @Override
    public void backgroundWorker() {
        super.backgroundWorker();
        mGoogleId = CoreInitialize.deviceUUIDFactory().getGoogleAdId();
        mOaid = CoreInitialize.deviceUUIDFactory().getOaid();
    }
}
