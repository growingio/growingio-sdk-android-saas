package com.growingio.android.sdk.models;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by lishaojie on 2017/2/22.
 */

public class EventSID implements Serializable {

    private HashMap<String, Integer> data = new HashMap<String, Integer>();

    public EventSID() {
    }

    public int getSid(String type) {
        Integer sid = data.get(type);
        if (sid == null) {
            return 0;
        } else {
            return sid;
        }
    }

    public EventSID setSid(String type, int sid) {
        data.put(type, sid);
        return this;
    }
}
