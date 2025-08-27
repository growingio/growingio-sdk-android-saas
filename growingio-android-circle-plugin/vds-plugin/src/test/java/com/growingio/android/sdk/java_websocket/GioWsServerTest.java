package com.growingio.android.sdk.java_websocket;

import com.growingio.android.sdk.java_websocket.client.WebSocketClient;
import com.growingio.android.sdk.java_websocket.framing.CloseFrame;
import com.growingio.android.sdk.java_websocket.framing.Framedata;
import com.growingio.android.sdk.java_websocket.handshake.ClientHandshake;
import com.growingio.android.sdk.java_websocket.handshake.ServerHandshake;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventBus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.net.URI;
import java.nio.ByteBuffer;

import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
public class GioWsServerTest {

    private class TestServer extends GioWsServer{

        String message;
        boolean started;

        @Override
        public void stopAsync() {

        }

        public void waitUntilStarted(){
            while (!started){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            super.onOpen(conn, handshake);
            System.out.println("open: " + conn);
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void onServerStarted() {
            started = true;
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            this.message = message;
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            ex.printStackTrace();
        }
    }

    class Client extends WebSocketClient{

        boolean end;
        boolean error;

        public Client(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            sendFragmentedFrame(Framedata.Opcode.TEXT, ByteBuffer.wrap("GIO".getBytes()), false);
            sendFragmentedFrame(Framedata.Opcode.TEXT, ByteBuffer.wrap("GROWINGIO".getBytes()), true);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            end = true;
        }

        @Override
        public void onMessage(String message) {

        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (code == CloseFrame.ABNORMAL_CLOSE) {
                error = true;
            }
        }

        @Override
        public void onError(Exception ex) {
            ex.printStackTrace();
        }

        public void waitUntilEnd(){
            while (!end){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void waitUntilError(){
            while (!end){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testFrameData() throws Exception{
        TestServer server = new TestServer();
        server.start();
        server.waitUntilStarted();
        int port = server.getPort();
        Client client = new Client(URI.create("ws://localhost:" + port));
        client.connect();
        client.waitUntilEnd();
        assert "GIOGROWINGIO".equals(server.message);
    }

    @Test
    public void testWebsocketNotConnectedException() throws Exception{
        Whitebox.setInternalState(EventBus.class, "defaultInstance", mock(EventBus.class));
        Whitebox.setInternalState(EventCenter.getInstance(), "initStart", true);

        TestServer server = new TestServer();
        server.start();
        server.waitUntilStarted();
        int port = server.getPort();
        Client client = new Client(URI.create("ws://localhost:" + port));
        client.connect();
        client.waitUntilEnd();

        // 触发1006， 但是不会抛出异常
        server.stop();
        client.waitUntilError();
        client.send("after server socket stop");

        assert "GIOGROWINGIO".equals(server.message);
    }
}
