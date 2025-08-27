package com.growingio.android.sdk.java_websocket;

import java.lang.reflect.Method;

/**
 * Created by liangdengke on 2018/9/19.
 */
public class DebuggerSocketMainTest {

    //@Test
    public void testSocket() throws Exception{
//        String wsUrl = "ws://localhost:8081";
        String wsUrl = "wss://gta.growingio.com/app/85864a8537ab43a2a066feb1855c1783/circle/QppfLYsF0l4DFhAF";
        DebuggerSocketMain main = new DebuggerSocketMain(wsUrl, false);
        Method callback = getClass().getDeclaredMethod("onCallback", int.class, String.class, Object.class);
        GioProtocol gioProtocol = new GioProtocol();
        main.setGioProtocol(gioProtocol);
        main.start();
        Thread.sleep(30000);
    }


    private void onCallback(int code, String msg, Object obj){

    }
}
