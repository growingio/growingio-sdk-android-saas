package com.growingio.android.sdk.java_websocket;

import com.growingio.android.sdk.interfaces.SocketInterface;
import com.growingio.android.sdk.java_websocket.exceptions.InvalidDataException;
import com.growingio.android.sdk.java_websocket.exceptions.InvalidFrameException;
import com.growingio.android.sdk.java_websocket.framing.Framedata;
import com.growingio.android.sdk.java_websocket.handshake.ClientHandshake;
import com.growingio.android.sdk.java_websocket.server.WebSocketServer;
import com.growingio.android.sdk.java_websocket.util.Charsetfunctions;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 用于GIO多进程发送数据的服务端
 * Created by liangdengke on 2018/9/3.
 */
public abstract class GioWsServer extends WebSocketServer implements SocketInterface{
    private static final String TAG = "GIO.GioWsServer";

    protected Set<WebSocket> localSockets = Collections.synchronizedSet(new HashSet<WebSocket>());
    protected WebSocket remoteSocket = null;
    protected GioProtocol gioProtocol;
    private Framedata framedata;

    public GioWsServer() {
        super(new InetSocketAddress(0));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        InetSocketAddress address = conn.getRemoteSocketAddress();
        System.out.println("onOpen: " + address);
        if (isLocal(address.getAddress())){
            localSockets.add(conn);
            onLocalConnect(conn);
        }else{
            if (remoteSocket != null){
                System.err.println("remoteSocket is not null, something oop..");
            }
            remoteSocket = conn;
            onRemoteConnect(conn);
        }
    }

    @Override
    public void onFragment(WebSocket conn, Framedata fragment) {
        if (framedata == null){
            framedata = fragment;
        }else{
            try {
                framedata.append(fragment);
            } catch (InvalidFrameException e) {
                framedata = null;
                return;
            }
        }
        if (fragment.isFin()){
            try {
                String message = Charsetfunctions.stringUtf8(framedata.getPayloadData());
                onMessage(conn, message);
            } catch (InvalidDataException e) {
                e.printStackTrace();
            }
            framedata = null;
        }
    }

    @Override
    public void setGioProtocol(Object gioProtocol) {
        this.gioProtocol = (GioProtocol) gioProtocol;
    }

    @Override
    public boolean sendMessage(String content){
        if (remoteSocket != null){
            try{
                remoteSocket.send(gioProtocol.dealWithOldVersionSDK(content));
                return true;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

    protected void onLocalConnect(WebSocket conn){

    }

    protected void onRemoteConnect(WebSocket conn){

    }

    protected void onRemoteClose(WebSocket remoteSocket){

    }

    protected void onLocalClose(){}

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (isLocalSocket(conn)){
            localSockets.remove(conn);
            onLocalClose();
        }else{
            remoteSocket = null;
            onRemoteClose(conn);
        }
    }

    protected boolean isLocalSocket(WebSocket webSocket){
        return localSockets.contains(webSocket);
    }

    protected boolean isRemoteSocket(WebSocket webSocket){
        return remoteSocket == webSocket;
    }

    protected boolean isLocal(InetAddress address){
        return address.isLoopbackAddress();
    }
}
