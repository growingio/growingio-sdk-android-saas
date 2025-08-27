package com.growingio.android.sdk.burry;

import com.growingio.android.sdk.collection.MessageProcessor;

/**
 * author CliffLeopard
 * time   2018/7/3:下午2:14
 * email  gaoguanling@growingio.com
 */
public class BuryMessageProcessor {
    private MessageProcessor coreMessageProcessor;

    public BuryMessageProcessor(MessageProcessor messageProcessor){
        coreMessageProcessor = messageProcessor;
    }
}
