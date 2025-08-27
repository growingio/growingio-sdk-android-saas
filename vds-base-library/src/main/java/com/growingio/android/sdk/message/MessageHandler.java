package com.growingio.android.sdk.message;

import android.os.Handler;

import com.growingio.android.sdk.models.VPAEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * author CliffLeopard
 * time   2018/4/20:上午11:53
 * email  gaoguanling@growingio.com
 */
public class MessageHandler {
    private static Set<MessageCallBack> callBacks = Collections.synchronizedSet(new HashSet<MessageCallBack>());

    public static void addCallBack(MessageCallBack handler) {
        callBacks.add(handler);
    }

    public static void addCallBack(RealTimeMessageCallBack callBack) {
        callBacks.add(new RealTimeMessageHolder(callBack));
    }

    public static void handleMessage(int what, Object... obj) {
        if (readyToSend()) {
            for (MessageCallBack callBack : callBacks) {
                if (callBack instanceof RealTimeMessageHolder) {
                    if (HandleType.DB_SAVE_EVENT == what && obj != null && obj.length == 3) {
                        callBack.handleMessage(what, obj);
                    }
                } else {
                    if (HandleType.SAVE_EVENT == what && obj != null && obj.length == 1) {
                        callBack.handleMessage(what, ((VPAEvent) obj[0]).toJson());
                    } else {
                        callBack.handleMessage(what, obj);
                    }
                }
            }
        }
    }

    private static boolean isEmpty() {
        return callBacks.isEmpty();
    }

    public static boolean readyToSend() {
        return !isEmpty();
    }

    public interface MessageCallBack {
        void handleMessage(int what, Object... obj);
    }

    public static class TestMessageCallBack implements MessageCallBack {
        private Handler handler;

        public TestMessageCallBack(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void handleMessage(int what, Object... obj) {
            handler.obtainMessage(what, obj).sendToTarget();
        }
    }
}
