package com.growingio.android.sdk.collection;

import android.text.TextUtils;

import com.growingio.android.sdk.models.VPAEvent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lishaojie on 16/7/13.
 */
public class CustomEvent extends VPAEvent {
    public static final String TYPE_NAME = "cstm";
    String name;
    Number num;
    JSONObject variable;
    JSONObject webEvent;
    private long ptm;

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    public String getName() {
        return name;
    }

    public JSONObject getVariable() {
        return variable;
    }

    public Number getNum() {
        return num;
    }

    public JSONObject getWebEvent() {
        return webEvent;
    }

    public CustomEvent(JSONObject webEvent, String pageName) {
        super(System.currentTimeMillis());
        this.webEvent = webEvent;
        try {
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
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public CustomEvent(String eventName, Number number, JSONObject variable) {
        super(System.currentTimeMillis());
        this.name = eventName;
        this.variable = variable;
        this.num = number;
    }

    public CustomEvent(String eventName) {
        super(System.currentTimeMillis());
        this.name = eventName;
    }

    public CustomEvent(String eventName, Number number) {
        this(eventName, number, null);
    }


    public CustomEvent(String eventName, JSONObject variable) {
        this(eventName, null, variable);
    }

    public void setPageTime(long ptm) {
        this.ptm = ptm;
    }

    public boolean fromWebView() {
        return webEvent != null;
    }

    @Override
    public JSONObject toJson() {
        try {
            if (webEvent != null) {
                return webEvent;
            } else {
                JSONObject event = getCommonProperty();
                event.put("n", name);
                event.put("var", variable);
                event.put("ptm", ptm);
                event.put("num", num);
                return event;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
