package com.growingio.android.sdk.java_websocket;


import com.growingio.android.sdk.base.event.SocketStatusEvent;
import com.growingio.android.sdk.interfaces.SocketInterface;
import com.growingio.android.sdk.java_websocket.client.WebSocketClient;
import com.growingio.android.sdk.java_websocket.exceptions.WebsocketNotConnectedException;
import com.growingio.android.sdk.java_websocket.handshake.ServerHandshake;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.eventcenter.EventCenter;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by zyl on 15/5/10.
 */
public class DebuggerSocketMain extends WebSocketClient implements SocketInterface{
    private final static String TAG = "GrowingIO.WebCircleSocket";

    private GioProtocol gioProtocol;

    private DebuggerServer debuggerServer;

    public DebuggerSocketMain(String wsUrl, boolean usingMultiProcess) throws URISyntaxException {
        super(new URI(wsUrl));
        if (wsUrl.startsWith("wss")) {
            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, null); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates
                SSLSocketFactory factory = sslContext.getSocketFactory();// (SSLSocketFactory) SSLSocketFactory.getDefault();

                this.setSocket(factory.createSocket());
            } catch (Exception e) {
                LogUtil.e(TAG, "start ssl failed: ", e);
                EventCenter.getInstance().post(SocketStatusEvent.ofStatusAndObj(SocketStatusEvent.SocketStatus.ERROR, e));
            }
        }
        debuggerServer = new DebuggerServer(!usingMultiProcess);
    }

    @Override
    public boolean sendMessage(String content) {
        try {
            send(content);
            return true;
        } catch (NotYetConnectedException | WebsocketNotConnectedException ignore) {
            ignore.printStackTrace();
            return false;
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("onOpen");
        debuggerServer.setRemoteSocket(this);
        gioProtocol.sendAndroidInitMessage(this);
        gioProtocol.startSendHeartbeat(this);
    }

    @Override
    public void onMessage(String message) {
        debuggerServer.onMessage(this, message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LogUtil.d(TAG, "onClose, code=", code, ", reason=", reason, ", remote=", remote);
        gioProtocol.stopSendHeartbeat();
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("onError: " + ex);
        gioProtocol.stopSendHeartbeat();
        debuggerServer.onError(this, ex);
    }

    public void start() {
        if (gioProtocol == null){
            throw new IllegalStateException("must called setCallback and setGioProtocol first");
        }
        System.out.println("start: uri=" + getURI());
        connect();
        debuggerServer.setGioProtocol(gioProtocol);
        debuggerServer.start();
    }

    @Override
    public void stopAsync() {
        if (isReady()){
            gioProtocol.sendQuitMessage(this);
        }
        debuggerServer.stopAsync();
        close();
    }

    @Override
    public boolean isReady() {
        return gioProtocol.isReady();
    }

    @Override
    public int getPort(){
        return debuggerServer.getPort();
    }

    @Override
    public void setGioProtocol(Object protocol) {
        this.gioProtocol = (GioProtocol) protocol;
    }

    private static class DebuggerServer extends SocketMain{

        private final boolean fakeServer;

        private DebuggerServer(boolean fakeServer) {
            this.fakeServer = fakeServer;
        }

        @Override
        public void start() {
            if (!fakeServer){
                super.start();
            }
        }

        @Override
        public void stopAsync() {
            if (!fakeServer){
                super.stopAsync();
            }else{
                gioProtocol.setReady(false);
            }
        }

        @Override
        public boolean sendMessage(String content) {
            return !fakeServer && super.sendMessage(content);
        }

        public void setRemoteSocket(WebSocket remoteSocket){
            this.remoteSocket = remoteSocket;
        }
    }
}
