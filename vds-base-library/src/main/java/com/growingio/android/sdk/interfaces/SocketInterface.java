package com.growingio.android.sdk.interfaces;

/**
 * WebSocket插件中Socket接口， 方便编程
 * Created by liangdengke on 2018/9/21.
 */
public interface SocketInterface {
    void start();
    void stopAsync();

    /**
     * @return 是否已经OK(与前端连接成功, 并且已经返回editor_ready)
     */
    boolean isReady();

    /**
     * @return 是否发送成功
     */
    boolean sendMessage(String message);
    int getPort();
    void setGioProtocol(Object protocol);
}
