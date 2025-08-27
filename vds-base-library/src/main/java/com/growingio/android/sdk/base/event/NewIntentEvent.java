package com.growingio.android.sdk.base.event;

import android.content.Intent;

/**
 * Activity: onCreate, onNewIntent
 * Service:
 * Receiver: onReceiver
 * ContentProvider:
 * 触发一条Intent， 需要接收器统一处理
 * Created by liangdengke on 2018/11/13.
 */
public class NewIntentEvent {

    public final Intent intent;

    public NewIntentEvent(Intent intent) {
        this.intent = intent;
    }
}
