package com.growingio.android.sdk.base.event;

/**
 * author CliffLeopard
 * time   2018/7/10:下午2:47
 * email  gaoguanling@growingio.com
 */
public class HeatMapEvent {

    public EVENT_TYPE type;
    public HeatMapEvent(EVENT_TYPE event_type) {
        this.type = event_type;
    }

    public enum EVENT_TYPE {
        INIT,
        STATE_OFF,
        STATE_ON,
        SHOW,
        HIDE,
        UPDATE
    }
}
