package com.growingio.android.sdk.models;

import android.app.Activity;
import android.text.TextUtils;

import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Created by xyz on 15/12/26.
 */
public class PageEvent extends VPAEvent {
    public static final String TYPE_NAME = "page";
    private String mLastPage;
    private String mTitle;
    private String mOrientation = "PORTRAIT";
    private long mPtm;

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getLastPage() {
        return mLastPage;
    }

    public PageEvent(String pageName, String lastPage, long ptm) {
        super(ptm);
        mPageName = pageName;
        mPtm = ptm;
        mLastPage = lastPage;
        Activity activity = getAPPState().getForegroundActivity();
        if (activity != null) {
            mOrientation = activity.getResources().getConfiguration().orientation == 1 ? "PORTRAIT" : "LANDSCAPE";
            if (!TextUtils.isEmpty(activity.getTitle())) {
                mTitle = activity.getTitle().toString();
            }
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = getCommonProperty();

        try {
            JSONObject appVariable = getAPPState().getAppVariable();
            if (appVariable != null && appVariable.length() > 0) {
                jsonObject.put("var", appVariable);
            }
//            patchLocation(jsonObject);
            // quick fix
            patchNetworkState(jsonObject);
            jsonObject.put("tm", mPtm);

            if (!TextUtils.isEmpty(mLastPage)) {
                jsonObject.put("rp", mLastPage);
            }

            jsonObject.put("o", mOrientation);

            jsonObject.put("tl", mTitle);
        } catch (JSONException e) {
            LogUtil.d(TAG, "generate page event error", e);
        }
        return jsonObject;
    }

}
