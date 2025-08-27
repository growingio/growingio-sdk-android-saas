package com.growingio.android.sdk.base.event;

/**
 *
 * 用于WebSocket插件与SDK主体通信
 * author CliffLeopard
 * time   2018/7/3:下午3:19
 * email  gaoguanling@growingio.com
 */
public class SocketStatusEvent {

    public SocketStatus event_type;
    public Object obj;
    private SocketStatusEvent(SocketStatus event_type, Object object) {
        this.event_type = event_type;
        this.obj = object;
    }

    public static SocketStatusEvent ofStatus(SocketStatus socketStatus){
        return new SocketStatusEvent(socketStatus, null);
    }

    public static SocketStatusEvent ofStatusAndObj(SocketStatus socketStatus, Object object){
        return new SocketStatusEvent(socketStatus, object);
    }

    public enum SocketStatus {
        ERROR,                 // 表示发生异常错误  obj = Exception
        SERVER_STARTED,        // 表示本地WebSocket服务开启
        REMOTE_CLOSE,          // 远端关闭
        EDITOR_READY,
        EDITOR_QUIT,
        HYBRID_MESSAGE,
        OTHER_MESSAGE,         // 其他消息, obj = message
        CLIENT_QUIT
    }
}
