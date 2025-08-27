package com.growingio.android.sdk.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lishaojie on 16/3/10.
 */
public class WebEvent extends VPAEvent {
    private String mOriginalEvent;
    private ViewNode mTargetNode;
    private String mPageName;
    private JSONObject mJsonEvent;
    private int mSize = 0;
    private String type = "hybrid";

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getFullType() {
        if (type.equals(ActionEvent.CLICK_TYPE_NAME)) {
            return ActionEvent.FULL_CLICK_TYPE_NAME;
        } else {
            return type;
        }
    }

    public WebEvent(String event, @Nullable ViewNode viewNode, @NonNull String pageName) {
        super(System.currentTimeMillis());
        mOriginalEvent = event;
        mTargetNode = viewNode;
        mPageName = pageName;
    }

    @Override
    public JSONObject toJson() {
        if (mJsonEvent != null) return mJsonEvent;
        try {
            JSONObject object = new JSONObject(mOriginalEvent);
            type = object.getString("t");
            object.put("s", CoreInitialize.sessionManager().getSessionIdInner());
            addPrefix(object, "d", getAPPState().getSPN());
            addPrefix(object, "p", mPageName);
            String cs1 = getConfig().getAppUserId();
            if (!TextUtils.isEmpty(cs1)) {
                object.put("cs1", cs1);
            }
            if (ActionEvent.IMP_TYPE_NAME.equals(type)
                    || ActionEvent.CLICK_TYPE_NAME.equals(type)
                    || ActionEvent.CHANGE_TYPE_NAME.equals(type)) {
                JSONArray elements = object.getJSONArray("e");
                int elemSize = elements.length();
                mSize = elemSize;
                for (int i = 0; i < elemSize; i++) {
                    JSONObject elem = elements.getJSONObject(i);
                    if (mTargetNode != null){
                        if (elem.opt("idx") != null) {
                            addPrefix(elem, "x", mTargetNode.mOriginalParentXpath.toStringValue());
                        } else {
                            addPrefix(elem, "x", mTargetNode.mParentXPath.toStringValue());
                            if (mTargetNode.mLastListPos > -1) {
                                object.put("idx", mTargetNode.mLastListPos);
                            }
                        }
                    }
                    if (elem.has("ex")) {
                        elem.remove("ex");
                        elem.remove("ey");
                        elem.remove("ew");
                        elem.remove("eh");
                    }
                }
            } else if (type.equals(PageEvent.TYPE_NAME)) {
                mSize = 1;
                String rp = object.optString("rp");
                if (!TextUtils.isEmpty(rp)) {
                    addPrefix(object, "rp", mPageName);
                }
                JSONObject appVariable = getAPPState().getAppVariable();
                if (appVariable != null && appVariable.length() > 0) {
                    object.put("var", appVariable);
                }
            }
            if (object.opt("tm") == null) {
                object.put("tm", System.currentTimeMillis());
            }
            mJsonEvent = object;
            return object;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addPrefix(JSONObject object, String key, String prefix) throws JSONException {
        String ov = object.getString(key);
        if (ov != null) {
            object.put(key, prefix + Constants.WEB_PART_SEPARATOR + ov);
        }
    }

    @Override
    public int size() {
        return mSize;
    }
}
