package com.growingio.android.sdk.autoburry;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.growingio.android.sdk.autoburry.util.FileUtil;
import com.growingio.android.sdk.base.event.NewIntentEvent;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ReflectUtil;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.bus.EventBus;
import com.growingio.eventcenter.bus.ThreadMode;
import com.xiaomi.mipush.sdk.MiPushMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 通知处理部分
 * - Notification notify时 记录notification的各种信息(title, content, from)
 * - Notification delete时
 * - Notification click时 匹配Notification各种信息, 并产生对应事件
 * Created by liangdengke on 2018/11/12.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class NotificationProcessor {
    private static final String TAG = "GIO.Notification";

    private static final String DIR_NAME = ".gio.push";
    private static final long MAX_TIME = 24 * 60 * 60 * 1000; // one day

    private static final String ACTION_HUAWEI_PUSH = "com.huawei.intent.action.PUSH";
    private static final String ACTION_HUAWEI_CLICK = "com.huawei.android.push.intent.CLICK";
    private static final String ACTION_XIAOMI_RECEIVE_MESSAGE = "com.xiaomi.mipush.RECEIVE_MESSAGE";

    @VisibleForTesting
    static final String GIO_ID_KEY = "__GIO_ID";
    @VisibleForTesting
    WeakHashMap<PendingIntent, String> pendingIntent2Ids = new WeakHashMap<>();

    private AtomicInteger gioIntentId = new AtomicInteger();
    private int myPid;
    File pushFile;
    private long lastHuaWeiPush;

    boolean enable;

    CoreAppState coreAppState;

    public NotificationProcessor(Context context, CoreAppState coreAppState){
        pushFile = new File(context.getFilesDir(), DIR_NAME);
        this.myPid = Process.myPid();
        enable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        this.coreAppState = coreAppState;
    }

    @VisibleForTesting
    NotificationProcessor(){

    }

    public boolean isEnable() {
        return enable;
    }

    /**
     * 在PendingIntent创建前，向Intent中添加GIO notification ID
     */
    public void hookPendingIntentCreateBefore(@NonNull Intent intent){
        if (!enable) return;
        if (isHooked(intent)){
            LogUtil.v(TAG, "hookPendingIntentCreate, and intent has been hooked, and return");
            return;
        }
        if (!ACTION_XIAOMI_RECEIVE_MESSAGE.equals(intent.getAction())){
            intent.putExtra(GIO_ID_KEY, myPid + "-_-" + gioIntentId.getAndIncrement());
        }
    }

    public void hookPendingIntentCreateAfter(@NonNull Intent intent, PendingIntent pendingIntent){
        if (!enable) return;
        String gioId = intent.getStringExtra(GIO_ID_KEY);
        if (gioId == null){
            if (ACTION_XIAOMI_RECEIVE_MESSAGE.equals(intent.getAction())){
                pendingIntent2Ids.put(pendingIntent, "XIAO_MI");
            }
            LogUtil.d(TAG, "hookPendingIntentCreateAfter, but gioId is null, maybe xiaomi push just return");
            return;
        }
        pendingIntent2Ids.put(pendingIntent, gioId);
    }

    private boolean isHooked(Intent intent){
        try{
            return intent.hasExtra(GIO_ID_KEY);
        }catch (Exception e){
            // 淘宝SDK会在Intent的Bundle中放置一个对象, readObject时会crash， 兼容保护
            LogUtil.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    public void onNotify(String tag, int id, Notification notification){
        if (!enable) return;
        if (notification.contentIntent != null && "XIAO_MI".equals(pendingIntent2Ids.get(notification.contentIntent))){
            LogUtil.d(TAG, "onNotify, and found xiaomi push, just return");
            return;
        }
        LogUtil.d(TAG, "onNotify, tag: ", tag, ", id=", id);
        NotificationInfo info = getNotificationInfo(notification);
        info.hasContentPending = checkAndStoreNotificationInfo(notification.contentIntent, "GIO$$ContentPending", info);
//        checkAndStoreNotificationInfo(notification.deleteIntent, "GIO$$DeleteIntent", info);
        sendMessageArrivedEvent(info.title, info.content);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onIntent(NewIntentEvent newIntentEvent){
        if (!enable) return;
        Intent intent = newIntentEvent.intent;
        if (intent == null){
            return;
        }
        if (isHooked(intent)){
            LogUtil.d(TAG, "onIntent, and found hooked intent: ", intent.getAction());
            String id = intent.getStringExtra(GIO_ID_KEY);
            intent.removeExtra(GIO_ID_KEY);
            if (id == null){
                LogUtil.d(TAG, "onIntent, and id is null, return");
                return;
            }
            if (ACTION_HUAWEI_PUSH.equals(intent.getAction())){
                lastHuaWeiPush = System.currentTimeMillis();
            }
            EventBus.getDefault().post(new NotificationReadEvent(id));
        }else{
            if (ACTION_HUAWEI_CLICK.equals(intent.getAction())
                    && (System.currentTimeMillis() - lastHuaWeiPush) > 5000){
                LogUtil.d(TAG, "HuaWei NC message received");
                handleHuaWeiNCMessage(intent);
            }
        }
    }

    public void onXiaoMiMessageArrived(MiPushMessage miPushMessage){
        if (!enable) return;
        LogUtil.d(TAG, "onXiaoMiMessageArrived: ", miPushMessage.toString());
        sendMessageArrivedEvent(miPushMessage.getTitle(), miPushMessage.getDescription());
    }


    public void onXiaoMiMessageClicked(MiPushMessage miPushMessage){
        if (!enable) return;
        LogUtil.d(TAG, "onXiaoMiMessageClicked: ", miPushMessage.toString());
        sendMessageClickedEvent(miPushMessage.getTitle(), miPushMessage.getDescription(), "GIO$$ContentPending");
    }

    private void handleHuaWeiNCMessage(Intent intent){
        try {
            String clickJson = intent.getStringExtra("click");
            JSONArray jsonArray = new JSONArray(clickJson);
            JSONObject cstmJSONObject = new JSONObject();
            String title = null;
            String content = null;
            for (int i =0; i < jsonArray.length(); i++){
                JSONObject current = jsonArray.getJSONObject(i);
                String titleStr = "notification_title";
                String contentStr = "notification_content";
                if (current.has(titleStr)){
                    title = current.optString(titleStr);
                }
                if (current.has(contentStr)){
                    content = current.optString(contentStr);
                }
            }
            sendMessageClickedEvent(title, content, "GIO$$ContentPending");
        }catch (Exception e){
            LogUtil.e(TAG, e.getMessage(), e);
        }
    }

    private boolean checkAndStoreNotificationInfo(PendingIntent pendingIntent, String actionTitle, NotificationInfo info){
        if (pendingIntent == null)
            return false;
        String intentGIOId = pendingIntent2Ids.get(pendingIntent);
        if (intentGIOId != null){
            NotificationActionInfo actionInfo = new NotificationActionInfo();
            actionInfo.actionTitle = actionTitle;
            actionInfo.info = info;
            EventBus.getDefault().post(new NotificationStoreEvent(actionInfo, intentGIOId));
        }else{
            LogUtil.d(TAG, "checkAndStoreNotificationInfo, but gio not found, actionTitle: ", actionTitle);
        }
        return true;
    }



    private NotificationInfo getNotificationInfo(Notification notification){
        NotificationInfo info = new NotificationInfo();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            info.title = notification.extras.getString(Notification.EXTRA_TITLE);
            info.content = notification.extras.getString(Notification.EXTRA_TEXT);
        }else{
            // TODO: 2018/11/22 暂时没有找到机器测试4.2， 4.3
            // parseNotificationTitleLowVersion(info, notification);
        }
        return info;
    }

    // 4.2， 4.3手机采集Notification的标题
    private void parseNotificationTitleLowVersion(NotificationInfo info, Notification notification){
        try {
            Class internalRIdClass = Class.forName("import com.android.internal.R.id");
            Object titleId = ReflectUtil.findField(internalRIdClass, null, "title");
            Object textId = ReflectUtil.findField(internalRIdClass, null, "text");
            if (titleId != null && textId != null){
                RemoteViews remoteViews = notification.contentView;
                List actions = ReflectUtil.findField(RemoteViews.class, remoteViews, "actions");
                if (actions != null){
                    Field viewIdField = null, methodNameField = null, valueField = null;
                    for (Object action: actions){
                        if (viewIdField == null){
                            viewIdField = ReflectUtil.findFieldObjRecur(action.getClass(), "viewId");
                            methodNameField = ReflectUtil.findFieldObjRecur(action.getClass(), "methodName");
                            valueField = ReflectUtil.findFieldObjRecur(action.getClass(), "value");
                        }
                        if (valueField == null)
                            continue;
                        if ("setText".equals(methodNameField.get(action))){
                            if ((int)viewIdField.get(action) == (int)titleId){
                                info.title = (String) valueField.get(action);
                            }else if ((int)viewIdField.get(action) == (int)textId){
                                info.content = (String) valueField.get(action);
                            }
                        }
                    }
                }
                Toast.makeText(CoreInitialize.coreAppState().getGlobalContext(), "获取标题: " + info.title + ", 内容: "  + info.content, Toast.LENGTH_SHORT).show();
                return;
            }
            LogUtil.d(TAG, "parse titleId failed");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            LogUtil.d(TAG, "parseNotification low version: failed");
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void storeNotificationInfo(NotificationStoreEvent storeEvent){
        String id = storeEvent.intentId;
        NotificationActionInfo actionInfo = storeEvent.actionInfo;
        LogUtil.d(TAG, "storeNotificationInfo: id=", id, ", actionInfo", actionInfo);
        initAndCleanDir();
        File toFile = new File(pushFile, id);
        if (toFile.exists()){
            LogUtil.e(TAG, "toFile exists, maybe some error");
            toFile.delete();
        }
        try {
            FileUtil.writeToFile(toFile, actionInfo.toJson());
        } catch (IOException e) {
            LogUtil.e(TAG, e.getMessage(), e);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onIntentGet(NotificationReadEvent event){
        String id = event.intentId;
        NotificationActionInfo actionInfo = getNotificationInfo(id);
        if (actionInfo == null){
            LogUtil.d(TAG, "onIntent, and actionInfo is null, return");
            return;
        }
        LogUtil.d(TAG, "onIntent, and found actionInfo: ", actionInfo);
        sendMessageClickedEvent(actionInfo.info.title, actionInfo.info.content, actionInfo.actionTitle);
    }

    private void sendMessageArrivedEvent(String title, String content){
        try {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put("notification_title", title);
            jsonObject.put("notification_content", content);
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GrowingIO.getInstance().track("notification_show", jsonObject);
                }
            });
        } catch (Exception e) {
            LogUtil.d(TAG, e);
        }
    }

    private void sendMessageClickedEvent(String title, String content, String actionTitle){
        try {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put("notification_title", title);
            jsonObject.put("notification_content", content);
            jsonObject.put("notification_action_title", actionTitle);
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GrowingIO.getInstance().track("notification_click", jsonObject);
                }
            });
        } catch (JSONException e) {
            LogUtil.d(TAG, e);
        }
    }

    public NotificationProcessor.NotificationActionInfo getNotificationInfo(String id){
        initAndCleanDir();
        File inFile = new File(pushFile, id);
        if (!inFile.exists()){
            return null;
        }
        try {
            String json = FileUtil.readFromFile(inFile);
            if (TextUtils.isEmpty(json)) {
                return null;
            } else {
                return NotificationProcessor.NotificationActionInfo.fromJson(json);
            }
        } catch (IOException e) {
            LogUtil.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    private synchronized void initAndCleanDir(){
        if (!pushFile.exists()){
            pushFile.mkdirs();
        }
        File[] files = pushFile.listFiles();
        if (files == null)
            return;
        long currentTime = System.currentTimeMillis();
        for (File file : files){
            if (currentTime - file.lastModified() > MAX_TIME){
                LogUtil.d(TAG, "clean file: ", file);
                file.delete();
            }
        }
    }

    public static class NotificationStoreEvent {
        private final NotificationActionInfo actionInfo;
        private final String intentId;

        private NotificationStoreEvent(NotificationActionInfo actionInfo, String intentId) {
            this.actionInfo = actionInfo;
            this.intentId = intentId;
        }
    }

    public static class NotificationReadEvent {
        final String intentId;

        private NotificationReadEvent(String intentId) {
            this.intentId = intentId;
        }
    }

    static class NotificationActionInfo{
        NotificationInfo info;
        String actionTitle;

        public String toJson(){
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("actionTitle", actionTitle);
                jsonObject.put("hasContentPending", info.hasContentPending);
                jsonObject.put("title", info.title);
                jsonObject.put("content", info.content);
                return jsonObject.toString();
            } catch (JSONException e) {
                LogUtil.d(TAG, e);
            }
            return null;
        }

        public static NotificationActionInfo fromJson(String json){
            try {
                JSONObject jsonObject = new JSONObject(json);
                NotificationActionInfo actionInfo = new NotificationActionInfo();
                actionInfo.actionTitle = jsonObject.optString("actionTitle");
                actionInfo.info = new NotificationInfo();
                actionInfo.info.title = jsonObject.optString("title");
                actionInfo.info.content = jsonObject.optString("content");
                actionInfo.info.hasContentPending = jsonObject.getBoolean("hasContentPending");
                return actionInfo;
            } catch (JSONException e) {
                LogUtil.d(TAG, e);
            }
            return null;
        }

        @Override
        public String toString() {
            return "NotificationActionInfo{" +
                    "info=" + info +
                    ", actionTitle='" + actionTitle + '\'' +
                    '}';
        }
    }

    static class NotificationInfo{
        String title;
        String content;
        boolean hasContentPending;

        @Override
        public String toString() {
            return "NotificationInfo{" +
                    "title='" + title + '\'' +
                    ", content='" + content + '\'' +
                    ", hasContentPending=" + hasContentPending +
                    '}';
        }
    }
}
