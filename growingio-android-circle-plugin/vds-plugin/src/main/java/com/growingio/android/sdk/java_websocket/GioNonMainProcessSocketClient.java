package com.growingio.android.sdk.java_websocket;

import com.growingio.android.sdk.base.event.SocketStatusEvent;
import com.growingio.android.sdk.interfaces.SocketInterface;
import com.growingio.android.sdk.java_websocket.client.WebSocketClient;
import com.growingio.android.sdk.java_websocket.handshake.ServerHandshake;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.eventcenter.EventCenter;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 非主进程的WebSocket客户端,
 * 没有使用SSL
 * Created by liangdengke on 2018/9/19.
 */
public class GioNonMainProcessSocketClient extends WebSocketClient implements SocketInterface{
    private static final String TAG = "GIO.NonMainSocket";

    private GioProtocol gioProtocol;

    public GioNonMainProcessSocketClient(String serverUrl) throws URISyntaxException {
        super(new URI(serverUrl));
    }

    @Override
    public void start(){
        if (gioProtocol == null)
            throw new IllegalStateException("must call setGioProtocol before start");
        connect();
    }

    @Override
    public void stopAsync() {
        if (isReady()){
            gioProtocol.sendQuitMessage(this);
        }
        close();
    }

    @Override
    public boolean isReady() {
        return gioProtocol.isReady();
    }

    @Override
    public void onOpen(ServerHandshake handShakeData) {
        LogUtil.d(TAG, "onOpen");
    }

    public boolean sendMessage(String message){
        try{
            send(gioProtocol.dealWithOldVersionSDK(message));
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public void setGioProtocol(Object protocol) {
        this.gioProtocol = (GioProtocol) protocol;
    }

    @Override
    public void onMessage(String message) {
        gioProtocol.onMessage(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LogUtil.d(TAG, "onClose, code=", code, ", reason=", reason, ", remote=", remote);
    }

    @Override
    public void onError(Exception ex) {
        LogUtil.e(TAG, "onError", ex);
        EventCenter.getInstance().post(SocketStatusEvent.ofStatusAndObj(SocketStatusEvent.SocketStatus.ERROR, ex));
    }
}
