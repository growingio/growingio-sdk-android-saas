package com.growingio.android.sdk.java_websocket;

import java.net.InetAddress;

/**
 * Created by liangdengke on 2018/9/3.
 */
public class WebCircleServerTest {

    private WebCircleSocketMain main;

    //@Test
    public void testServer() throws Exception{
        GioProtocol gioProtocol = new GioProtocol();
        gioProtocol.setSPN("com.test.spn");
        gioProtocol.setAI("This is My AI");
        gioProtocol.setAppVersion("2.3.4");
        gioProtocol.setSdkVersion("2.5.0-beta");

        main = new WebCircleSocketMain(){
            @Override
            protected boolean isLocal(InetAddress hostName) {
                return false;
            }
        };
        main.setGioProtocol(gioProtocol);
        main.start();
        Thread.sleep(40000);
        main = null;
    }
}