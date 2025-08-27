package com.growingio.android.sdk.java_websocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * Web圈App的主进程服务
 * Created by liangdengke on 2018/9/17.
 */
public class WebCircleSocketMain extends SocketMain{

    @Override
    protected void onRemoteConnect(WebSocket conn) {
        System.out.println("onRemoteConnect");
        gioProtocol.sendAndroidInitMessage(conn);
    }
}
