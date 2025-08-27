package com.growingio.android.sdk.base.event.message;

/**
 * author CliffLeopard
 * time   2018/7/2:下午7:16
 * email  gaoguanling@growingio.com
 */
public class HandleEvent {
    public Runnable runnable;
    public MessageHandleType handleType;
    public long delayTime;

    public HandleEvent(Runnable runnable,MessageHandleType handleType){
        this.runnable = runnable;
        this.handleType = handleType;
    }
    public HandleEvent(Runnable runnable,MessageHandleType handleType,long delayTime){
        this.runnable = runnable;
        this.handleType = handleType;
        this.delayTime = delayTime;
    }

    public enum MessageHandleType {
        POST,
        POSTDELAY
    }
}
