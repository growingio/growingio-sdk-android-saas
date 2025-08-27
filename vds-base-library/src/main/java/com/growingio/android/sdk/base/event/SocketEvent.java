package com.growingio.android.sdk.base.event;

import org.json.JSONObject;

/**
 * author CliffLeopard
 * time   2018/7/10:下午1:49
 * email  gaoguanling@growingio.com
 */
public class SocketEvent {
    public String circleRoomNumber;
    public String wsUrl;
    public String key;
    public EVENT_TYPE type;
    public String message;
    public Class<?> loadClass;
    public JSONObject debuggerJson;

    public SocketEvent(EVENT_TYPE type, String circleRoomNumber, String wsUrl, String key) {
        this.type = type;
        this.circleRoomNumber = circleRoomNumber;
        this.wsUrl = wsUrl;
        this.key = key;
    }

    public SocketEvent(EVENT_TYPE type, String message) {
        this.type = type;
        this.message = message;
    }

    public SocketEvent(EVENT_TYPE type, Class<?> loadClass) {
        this.type = type;
        this.loadClass = loadClass;
    }


    public SocketEvent(EVENT_TYPE type, JSONObject debuggerJson) {
        this.type = type;
        this.debuggerJson = debuggerJson;
    }

    public SocketEvent(EVENT_TYPE type) {
        this.type = type;
    }

    public enum EVENT_TYPE {
        SEND,
        SEND_DEBUGGER,
        SCREEN_UPDATE
    }

}
