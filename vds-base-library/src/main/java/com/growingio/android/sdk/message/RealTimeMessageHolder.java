package com.growingio.android.sdk.message;


import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.DeviceUUIDFactory;
import com.growingio.android.sdk.collection.GrowingIO;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * author CliffLeopard
 * time   2018/4/20:上午11:31
 * email  gaoguanling@growingio.com
 */
final class RealTimeMessageHolder implements MessageHandler.MessageCallBack {

    public RealTimeMessageCallBack callBack;

    public RealTimeMessageHolder(RealTimeMessageCallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    public void handleMessage(int what, Object... obj) {
        if (what == HandleType.DB_SAVE_EVENT) {
            JSONObject event = null;
            String type = null;
            DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
            try {
                type = (String) obj[0];
                event = new JSONObject((String) obj[2]);
                event.put("u", GrowingIO.getInstance().getDeviceId());

                if (type.equals("vst")) {
                    try {
                        event.put("ui", deviceUUIDFactory.getIMEI());
                        event.put("iv", deviceUUIDFactory.getAndroidId());
                    } catch (Exception ignore) {

                    }
                }
            } catch (JSONException ignore) {
            }
            callBack.receivedMessage(type, event);
        }
    }
}