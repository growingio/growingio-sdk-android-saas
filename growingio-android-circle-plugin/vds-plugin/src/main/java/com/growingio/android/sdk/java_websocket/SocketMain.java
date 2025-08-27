package com.growingio.android.sdk.java_websocket;

import com.growingio.android.sdk.base.event.SocketStatusEvent;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.eventcenter.EventCenter;

/**
 * 所有涉及Socket通信的主进程通信
 * Created by liangdengke on 2018/9/19.
 */
abstract class SocketMain extends GioWsServer{
    private static final String TAG = "GIO.SocketMain";

    @Override
    public boolean isReady() {
        return gioProtocol.isReady();
    }

    @Override
    public void start() {
        if (gioProtocol == null){
            throw new IllegalStateException("you must init all properties");
        }
        super.start();
    }

    @Override
    public void stopAsync(){
        new Thread(){
            @Override
            public void run() {
                try {
                    if (isReady() && remoteSocket != null){
                        gioProtocol.sendQuitMessage(remoteSocket);
                    }
                    SocketMain.this.stop();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    protected void onRemoteConnect(WebSocket conn) {
        throw new IllegalStateException("should not have remote connect");
    }

    @Override
    protected void onLocalConnect(WebSocket conn) {
        if (gioProtocol.isReady()){
            LogUtil.d(TAG, "onLocalConnect, and fake Ready Message");
            gioProtocol.fakeEditorReadyMessage(conn);
        }
    }

    @Override
    public void onServerStarted() {
        EventCenter.getInstance().post(SocketStatusEvent.ofStatus(SocketStatusEvent.SocketStatus.SERVER_STARTED));
    }

    @Override
    protected void onRemoteClose(WebSocket conn) {
        EventCenter.getInstance().post(SocketStatusEvent.ofStatus(SocketStatusEvent.SocketStatus.REMOTE_CLOSE));
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (isRemoteSocket(conn)){
            gioProtocol.onMessage(message);
        }else{
            boolean shouldSendRemote = remoteSocket != null && remoteSocket.isOpen();
            System.out.println("多进程消息(" + shouldSendRemote + "): " + message);
            if (shouldSendRemote){
                remoteSocket.send(message);
            }
            String msgId = gioProtocol.parseMsgId(message);
            if (gioProtocol.isClientQuit(msgId)){
                for (WebSocket localWebSocket: localSockets){
                    if (localWebSocket == conn)
                        continue;
                    if (!localWebSocket.isClosed()){
                        localWebSocket.send(message);
                    }
                }
                EventCenter.getInstance().post(SocketStatusEvent.ofStatus(SocketStatusEvent.SocketStatus.CLIENT_QUIT));
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (isRemoteSocket(conn)){
            gioProtocol.setReady(false);
            EventCenter.getInstance().post(SocketStatusEvent.ofStatusAndObj(SocketStatusEvent.SocketStatus.ERROR, ex));
        }else {
            ex.printStackTrace();
        }
    }
}
