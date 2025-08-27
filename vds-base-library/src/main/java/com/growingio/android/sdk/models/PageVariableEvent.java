package com.growingio.android.sdk.models;

import android.text.TextUtils;

import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.MessageProcessor;

import org.json.JSONObject;

/**
 * Created by lishaojie on 2017/4/12.
 */

public class PageVariableEvent extends VPAEvent {
    public static final String TYPE_NAME = "pvar";
    private PageEvent mPage;
    private JSONObject mPVar;
    private JSONObject mWebEvent;

    public PageVariableEvent(PageEvent page, JSONObject pVar) {
        super(page.time);
        mPage = page;
        mPVar = pVar;
    }

    public PageVariableEvent(JSONObject webEvent, String pageName) throws Throwable {
        super(webEvent.optLong("tm") != 0 ? webEvent.optLong("tm") : System.currentTimeMillis());
        webEvent.put("s", CoreInitialize.sessionManager().getSessionIdInner());
        String d = webEvent.getString("d");
        webEvent.put("d", getAPPState().getSPN() + Constants.WEB_PART_SEPARATOR + d);

        String p = webEvent.optString("p");
        String prefix = pageName;
        if (GConfig.isRnMode) prefix = MessageProcessor.FAKE_PAGE_NAME;
        else if (pageName == null || pageName.isEmpty()) {
            prefix = CoreInitialize.messageProcessor().getPageNameWithPending();
        }
        webEvent.put("p", prefix + Constants.WEB_PART_SEPARATOR + p);
        String cs1 = getConfig().getAppUserId();
        if (!TextUtils.isEmpty(cs1)) {
            webEvent.put("cs1", cs1);
        }
        this.mWebEvent = webEvent;
    }

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    @Override
    public JSONObject toJson() {
        try {
            if (mWebEvent != null) {
                return mWebEvent;
            } else {
                JSONObject jsonObject = getCommonProperty();
                jsonObject.put("p", mPage.mPageName);
                jsonObject.put("ptm", mPage.time);
                //使用创建对象的json
                jsonObject.put("var", mPVar);
                return jsonObject;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
