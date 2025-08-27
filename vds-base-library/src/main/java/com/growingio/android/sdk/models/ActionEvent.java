package com.growingio.android.sdk.models;

import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xyz on 15/12/25.
 */
public class ActionEvent extends VPAEvent {
    public static final String CLICK_TYPE_NAME = "clck";
    public static final String IMP_TYPE_NAME = "imp";
    public static final String CHANGE_TYPE_NAME = "chng";
    public static final String FULL_CLICK_TYPE_NAME = "click";
    private static String TAG = "GIO.ActionEvent";

    public List<ActionStruct> elems = new ArrayList<ActionStruct>();
    private long ptm;
    private String type;
    private boolean instant;

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getFullType() {
        if (type.equals(CLICK_TYPE_NAME)) {
            return FULL_CLICK_TYPE_NAME;
        } else {
            return type;
        }
    }

    private ActionEvent(String type) {
        super(System.currentTimeMillis());
        this.type = type;
    }

    public void setPageTime(long ptm){
        this.ptm = ptm;
    }

    public static ActionEvent makeImpEvent() {
        ActionEvent event = new ActionEvent(ActionEvent.IMP_TYPE_NAME);
        event.instant = false;
        return event;
    }

    public static ActionEvent makeClickEvent() {
        ActionEvent event = new ActionEvent(ActionEvent.CLICK_TYPE_NAME);
        event.instant = true;
        return event;
    }

    public static ActionEvent makeChangeEvent() {
        ActionEvent event = new ActionEvent(ActionEvent.CHANGE_TYPE_NAME);
        event.instant = true;
        return event;
    }

    public ActionEvent copyWithoutElements() {
        ActionEvent copy = new ActionEvent(type);
        copy.ptm = ptm;
        copy.instant = instant;
        copy.time = time;
        copy.mPageName = mPageName;
        return copy;
    }

    public boolean isInstant() {
        return instant;
    }

    public JSONObject toJson() {
        JSONObject eventObject = null;
        if (elems.size() > 0) {
            eventObject = getCommonProperty();
            try {
                JSONArray impressArray = new JSONArray();
                for (ActionStruct actionStruct : elems) {
                    impressArray.put(actionStruct.toJson());
                }
                eventObject.put("ptm", ptm);
//                if (CLICK_TYPE_NAME.equals(type)) {
//                    patchLocation(eventObject);
//                    patchNetworkState(eventObject);
//                }
                eventObject.put("e", impressArray);

            } catch (JSONException e) {
                LogUtil.d(TAG, "generate common event property error", e);
            }
        }
        return eventObject;
    }

    @Override
    public int size() {
        return elems.size();
    }

    public String toString() {
        return type + " event with " + elems.size() + " elements ActionEvent@" + hashCode();
    }

    public ActionEvent clone() {
        ActionEvent cloneObj = new ActionEvent(this.type);
        cloneObj.instant = this.instant;
        cloneObj.ptm = this.ptm;
        cloneObj.time = this.time;
        cloneObj.mPageName = this.mPageName;
        cloneObj.elems = new ArrayList<ActionStruct>();
        cloneObj.elems.addAll(this.elems);
        return cloneObj;
    }
}
