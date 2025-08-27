package com.growingio.android.sdk.java_websocket;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;

import com.growingio.android.sdk.base.event.SocketStatusEvent;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.CustomEvent;
import com.growingio.android.sdk.collection.DeviceUUIDFactory;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.ConversionEvent;
import com.growingio.android.sdk.models.PageEvent;
import com.growingio.android.sdk.models.PageVariableEvent;
import com.growingio.android.sdk.models.PeopleEvent;
import com.growingio.android.sdk.models.VisitEvent;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.WindowHelper;
import com.growingio.eventcenter.EventCenter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 发送协议格式
 * Created by liangdengke on 2018/9/17.
 */
public class GioProtocol {
    private static final String TAG = "GIO.GioProtocol";

    // msg id
    private final static String CLIENT_INIT = "client_init";
    private final static String EDITOR_READY = "editor_ready";
    private final static String CLIENT_QUIT = "client_quit";
    private final static String EDITOR_QUIT = "editor_quit";
    private final static String MSG_ID_SDK_CLOSE = "sdk_closed";
    private final static String HYBRID_MESSAGE = "hybridEvent";
    private final static String TARGET_DISCONNECT = "target_disconnect";
    private final static String HEARTBEAT = "heartbeat";

    private static final long HEARTBEAT_INTERVAL = 30 * 1000;


    private String mSPN;
    private String mAI;
    private String mSdkVersion;
    private String mAppVersion;
    private Integer mProtocolVersion = 0;
    private WebSocket mHeartBeatSocket;
    ScheduledExecutorService mScheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private boolean isReady = false;

    public GioProtocol() {
        CoreAppState coreAppState = CoreInitialize.coreAppState();
        setAI(coreAppState.getProjectId());
        setSPN(coreAppState.getSPN());
        setAppVersion(CoreInitialize.config().getAppVersion());
        setSdkVersion(GConfig.GROWING_VERSION);
        setProtocolVersion(1);
    }

    public void startSendHeartbeat(WebSocket conn){
        mHeartBeatSocket = conn;
        mScheduledExecutor.schedule(mHeartbeatTask, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void stopSendHeartbeat(){
        mHeartBeatSocket = null;
    }

    public void setProtocolVersion(Integer protocolVersion) {
        this.mProtocolVersion = protocolVersion;
    }

    public void setSPN(String mSPN) {
        this.mSPN = mSPN;
    }

    public void setAI(String mAI) {
        this.mAI = mAI;
    }

    public void setSdkVersion(String mSdkVersion) {
        this.mSdkVersion = mSdkVersion;
    }

    public void setAppVersion(String mAppVersion) {
        this.mAppVersion = mAppVersion;
    }

    public void onMessage(String message){
        if (isEmptyMessage(message)) return;
        try{
            JSONObject jsonObject = new JSONObject(message);
            String msgId = parseMsgId(jsonObject);
            LogUtil.d(TAG, "onMessage, and msgId is:", msgId);
            if (isEditorReady(msgId)){
                setReady(true);
                EventCenter.getInstance().post(SocketStatusEvent.ofStatus(SocketStatusEvent.SocketStatus.EDITOR_READY));
            }else if (isEditorQuit(msgId)){
                setReady(false);
                EventCenter.getInstance().post(SocketStatusEvent.ofStatus(SocketStatusEvent.SocketStatus.EDITOR_QUIT));
            }else if(isClientQuit(msgId)){
                setReady(false);
                EventCenter.getInstance().post(SocketStatusEvent.ofStatus(SocketStatusEvent.SocketStatus.CLIENT_QUIT));
            }else if (HYBRID_MESSAGE.equals(msgId)){
                EventCenter.getInstance().post(SocketStatusEvent.ofStatusAndObj(SocketStatusEvent.SocketStatus.HYBRID_MESSAGE, jsonObject));
            }else if (MSG_ID_SDK_CLOSE.equals(msgId)){
                EventCenter.getInstance().post(SocketStatusEvent.ofStatus(SocketStatusEvent.SocketStatus.EDITOR_QUIT));
            }else{
                EventCenter.getInstance().post(SocketStatusEvent.ofStatusAndObj(SocketStatusEvent.SocketStatus.OTHER_MESSAGE, message));
            }
        }catch (Throwable throwable){
            LogUtil.e(TAG, throwable.getMessage(), throwable);
        }
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    /**
     * 旧版SDK存在Bug: 横屏情况下， 截图宽高与真实宽高比例相反, 前端无法判断属于是否需要显示横屏提示, 这里进行翻转
     */
    public String dealWithOldVersionSDK(String message){
        if (mProtocolVersion == null || mProtocolVersion == 0){
            CoreAppState appState = CoreInitialize.coreAppState();
            Activity foregroundActivity = null;
            if (appState == null || (foregroundActivity = appState.getForegroundActivity()) == null){
                return message;
            }
            boolean isPortrait = foregroundActivity.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;
            if (isPortrait)
                return message;
            try {
                JSONObject jsonObject = new JSONObject(message);
                if (!jsonObject.has("page") || !jsonObject.has("screenshot")){
                    return message;
                }
                int width = jsonObject.getInt("screenshotWidth");
                int height = jsonObject.getInt("screenshotHeight");
                jsonObject.put("screenshotHeight", width);
                jsonObject.put("screenshotWidth", height);
                return jsonObject.toString();
            } catch (JSONException e) {
                return message;
            }
        }else {
            return message;
        }
    }

    public void sendAndroidInitMessage(WebSocket webSocket){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("ai", mAI);
            jsonObject.put("msgId", CLIENT_INIT);
            if (mProtocolVersion != null && mProtocolVersion > 0){
                jsonObject.put("protocolVersion", mProtocolVersion);
            }
            // data check 需要外层添加一个时间戳
            jsonObject.put("tm", System.currentTimeMillis());
            jsonObject.put("spn", mSPN);
            jsonObject.put("sdkVersion", mSdkVersion);
            jsonObject.put("appVersion", mAppVersion);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println("sendAndInitMessage");
        webSocket.send(jsonObject.toString());
    }

    public void fakeEditorReadyMessage(WebSocket webSocket){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("msgId", EDITOR_READY);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        webSocket.send(jsonObject.toString());
    }

    private void sendHeartbeatMessage(WebSocket webSocket) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("msgId", HEARTBEAT);
            jsonObject.put("ai", mAI);
            jsonObject.put("spn", mSPN);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        webSocket.send(jsonObject.toString());
    }

    public void sendQuitMessage(WebSocket webSocket) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("msgId", CLIENT_QUIT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        webSocket.send(jsonObject.toString());
    }

    private Runnable mHeartbeatTask = new Runnable() {
        @Override
        public void run() {
            if (mHeartBeatSocket != null){
                sendHeartbeatMessage(mHeartBeatSocket);
            }
            mScheduledExecutor.schedule(this, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        }
    };

    public boolean isEditorReady(String msgId){
        return EDITOR_READY.equals(msgId);
    }

    public boolean isEditorQuit(String msgId){
        return EDITOR_QUIT.equals(msgId) || TARGET_DISCONNECT.equals(msgId);
    }

    public boolean isClientQuit(String msgId){
        return CLIENT_QUIT.equals(msgId);
    }

    public boolean isEmptyMessage(String message){
        return " ".equals(message) || message == null || message.length() == 0;
    }

    private String parseMsgId(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("msgId");
    }

    public String parseMsgId(String message){
        try {
            return parseMsgId(new JSONObject(message));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String sendScreenUpdate(){
        LogUtil.e(TAG, "sendScreenUpdate:");
        String mEncodeScreenshot;
        View[] allViews = WindowHelper.getSortedWindowViews();
        byte[] screenshotData = ScreenshotHelper.captureAllWindows(allViews, null);
        JSONObject updateJson = new JSONObject();
        try {
            updateJson.put("msgId", "screen_update");
            updateJson.put("screenshotWidth", ScreenshotHelper.getScaledShort());
            updateJson.put("screenshotWidth", ScreenshotHelper.getScaledShort());
            updateJson.put("screenshotHeight", ScreenshotHelper.getScaledLong());
            mEncodeScreenshot = "data:image/jpeg;base64," + Base64.encodeToString(screenshotData, Base64.NO_WRAP);
            updateJson.put("screenshot", mEncodeScreenshot);
            LogUtil.d(TAG, "向Debugger发送 screen_update："); // updateJson.toString()
            return updateJson.toString();
        } catch (Exception e) {
            LogUtil.d(TAG, "屏幕更新失败");
            return null;
        }
    }

    public static String sendDebuggerInit() {
        LogUtil.d(TAG, "DebuggerInit");
        JSONObject debuggerJson = new JSONObject();
        GConfig config = CoreInitialize.config();
        CoreAppState appState = CoreInitialize.coreAppState();
        DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();

        try {
            debuggerJson.put("msgId", "client_info");
            debuggerJson.put("sdkVersion", GConfig.GROWING_VERSION);
            debuggerJson.put("u", deviceUUIDFactory.getDeviceId());
            debuggerJson.put("cs1", config.getAppUserId());

            try {
                JSONObject locate = new JSONObject();
                {
                    TelephonyManager manager = (TelephonyManager) CoreInitialize.coreAppState().getGlobalContext().getSystemService(Context.TELEPHONY_SERVICE);
                    final int MCC_LENGTH = 3;
                    String sim = manager.getSimOperator();
                    if (!TextUtils.isEmpty(sim) && sim.length() > MCC_LENGTH) {
                        locate.put("countryCode", new StringBuffer(sim).insert(MCC_LENGTH, '-').toString());
                    }
                    //--目前还没有
                    //                locate.put("country", "");
                    //                locate.put("region", "");
                    //                locate.put("city", "");
                }
                debuggerJson.put("locate", locate);
            } catch (Exception e) {
                LogUtil.e(TAG, "位置信息错误");
            }
            JSONObject device = new JSONObject();
            {
                device.put("appVersion", config.getAppVersion());
                device.put("appChannel", config.getChannel());

                JSONObject screenSize = new JSONObject();
                {
                    screenSize.put("w", ScreenshotHelper.getScaledShort());
                    screenSize.put("h", ScreenshotHelper.getScaledLong());
                }
                device.put("screenSize", screenSize);

                device.put("os", "Android");
                device.put("osVersion", Build.VERSION.SDK_INT);
                device.put("deviceBrand", Build.BRAND);
                device.put("deviceType", Build.TYPE);
                device.put("deviceModel", Build.MODEL);
            }
            debuggerJson.put("device", device);
            debuggerJson.put("page", CoreInitialize.messageProcessor().getPageNameWithoutPending());
            debuggerJson.put("referralPage", null);


            debuggerJson.put("ppl", appState.getPeopleVariable());
            if (appState.getForegroundActivity() != null)
                debuggerJson.put("pvar", appState.getPageVariable());
            debuggerJson.put("evar", appState.getConversionVariable());
            LogUtil.d(TAG, "向Debugger发送 client_info："); //+ debuggerJson.toString()
            return debuggerJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d(TAG, "DebuggerInit 失败:" + e.getMessage());
        }
        return null;
    }

    public static String sendDebuggerStr(JSONObject debuggerJson){
        LogUtil.d(TAG, "向Debugger发送数据");
        String type = null;
        try {
            type = debuggerJson.getString("t");
        } catch (JSONException jsonException) {
        }

        DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
        CoreAppState coreAppState = CoreInitialize.coreAppState();

        if ("reengage".equals(type)) {
            try {
                debuggerJson.put("msgId", "server_action");
                debuggerJson.put("u", deviceUUIDFactory.getDeviceId());
                return debuggerJson.toString();
            } catch (JSONException ignore) {
                LogUtil.d(TAG, "向Debugger发送数据失败：" + ignore.toString());
            }
        }
        String ai = coreAppState.getProjectId();
        String uri = String.format(Locale.US, "%s/%s/android/%s?stm=%d", NetworkConfig.getInstance().apiEndPoint(), ai, getUploadEventType(type), System.currentTimeMillis());
        try {
            debuggerJson.put("msgId", "server_action");
            debuggerJson.put("uri", uri);
            debuggerJson.put("u", deviceUUIDFactory.getDeviceId());
        } catch (Exception e) {
            LogUtil.d(TAG, "获取信息失败");
        }

        LogUtil.d(TAG, "向Debugger发送 server_action：" + debuggerJson.toString());
        return debuggerJson.toString();
    }

    private static String getUploadEventType(String type) {
        if (type == null)
            return "other";
        if (type.equals(CustomEvent.TYPE_NAME)
                || type.equals(PageVariableEvent.TYPE_NAME)
                || type.equals(ConversionEvent.TYPE_NAME)
                || type.equals(PeopleEvent.TYPE_NAME)
        ) {
            return "cstm";
        } else if (type.equals(PageEvent.TYPE_NAME) || type.equals(VisitEvent.TYPE_NAME)) {
            return "pv";
        } else if (type.equals(ActionEvent.IMP_TYPE_NAME)) {
            return "imp";
        } else {
            return "other";
        }
    }
}
