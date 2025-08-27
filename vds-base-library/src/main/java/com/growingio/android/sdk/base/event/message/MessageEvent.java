package com.growingio.android.sdk.base.event.message;

/**
 * author CliffLeopard
 * time   2018/7/2:下午8:40
 * email  gaoguanling@growingio.com
 */
public class MessageEvent {

    public MessageType messageType;

    public MessageEvent(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public enum MessageType {
        IMP,
    }
}
