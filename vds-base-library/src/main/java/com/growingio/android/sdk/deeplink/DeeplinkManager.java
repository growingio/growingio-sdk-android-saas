package com.growingio.android.sdk.deeplink;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.base.event.HttpCallBack;
import com.growingio.android.sdk.base.event.HttpEvent;
import com.growingio.android.sdk.base.event.NewIntentEvent;
import com.growingio.android.sdk.base.event.ValidUrlEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.DeviceUUIDFactory;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.models.ad.ActivateEvent;
import com.growingio.android.sdk.models.ad.ReengageEvent;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.NetworkUtil;
import com.growingio.android.sdk.utils.ObjectUtils;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * Created by denghuaxin on 2018/3/10.
 */

public class DeeplinkManager {
    private final static String TAG = "GIO.deeplink";

    /**
     * 由于onNewIntent方法在纯打点包中无法hook， 必须用户手动调用GrowingIO.onNewIntent(xxx);
     * 而在无埋点版本中， 回调可能出现多次， 这里做一个简单的时间去重
     */
    private WeakReference<Intent> lastIntentRef;
    @VisibleForTesting
    ClipboardManager cm;
    @VisibleForTesting
    GConfig mConfig;
    private Context mGlobalContext;
    //为了防止极端情况，activate 还没发出去，又触发导致发送多个 activate
    private boolean isActivateSended = false;
    private boolean isDigestedIntent = false;

    private DeeplinkInfo activateDeepLink;

    public DeeplinkManager(GConfig gConfig, Context context) {
        this.mConfig = gConfig;
        this.mGlobalContext = context;
    }

    @VisibleForTesting
    public DeeplinkManager() {
    }

    @Subscribe
    public void onActivityLifecycle(ActivityLifecycleEvent event) {
        if (event.event_type == ActivityLifecycleEvent.EVENT_TYPE.ON_CREATED
                || event.event_type == ActivityLifecycleEvent.EVENT_TYPE.ON_NEW_INTENT
                || event.event_type == ActivityLifecycleEvent.EVENT_TYPE.ON_STARTED
        ) {
            if (lastIntentRef != null && lastIntentRef.get() == event.getIntent()) {
                LogUtil.d(TAG, "handleIntent, and this intent has been dealt, return");
                return;
            }
            isDigestedIntent = handleIntent(event.getIntent(), event.getActivity());
        }

        if (event.event_type == ActivityLifecycleEvent.EVENT_TYPE.ON_RESUMED) {
            checkActivateStatus(event.getActivity());
        }
    }

    @Subscribe
    public void onActivateEvent(DeepLinkEvent event) {
        // 开启数据采集，触发设备激活事件发送
        if (event.getType() == DeepLinkEvent.DEEPLINK_ACTIVATE) {
            checkActivateStatus(CoreInitialize.coreAppState().getForegroundActivity());
        }
    }

    /**
     * 发送激活事件(ui主线程)
     * 1. 剪贴板数据受隐私政策影响，支持隐私政策不申明时禁止读取剪切板数据
     * 2. 打开隐私政策后,发送设备激活事件，同时发送到用户自定义实现的 {@link DeeplinkCallback} 中
     */
    @VisibleForTesting
    @MainThread
    private void checkActivateStatus(Activity activity) {
        if (isActivateSended || !mConfig.isEnabled()) {
            return;
        }

        if (mConfig.isDeviceActivated()) {
            isActivateSended = true;
            return;
        }
        CoreInitialize.deviceUUIDFactory().initUserAgent();//设置设备的agent
        if (mConfig.isReadClipBoardEnable()) {
            if (cm == null) {
                cm = (ClipboardManager) mGlobalContext.getSystemService(CLIPBOARD_SERVICE);
            }
            // Android 10 限制剪切板获取时机，只有输入法或者焦点APP才有权限获取剪切板
            // 在application中直接调用enableDataCollect可能导致获取剪切板内容失败
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null) {
                activity.getWindow().getDecorView().post(new Runnable() {
                    @Override
                    public void run() {
                        checkClipBoardAndSendActivateEvent();
                    }
                });
            } else {
                checkClipBoardAndSendActivateEvent();
            }
        } else {
            submitActivateEvent();
        }
    }

    private void checkClipBoardAndSendActivateEvent() {
        final DeeplinkInfo tempDeepLink = new DeeplinkInfo();
        //数据耗时操作
        final boolean success = checkClipBoard(tempDeepLink);

        DeeplinkManager.this.activateDeepLink = tempDeepLink;
        if (success) {
            sendInfoToDefferCallback(activateDeepLink);
            LogUtil.d(TAG, "用户通过延迟深度链接方式打开，收到参数准备传给 DeepLinkCallback");
        } else {
            LogUtil.d(TAG, "非延迟深度链接方式打开应用" + isDigestedIntent);
            // 若无最新的剪贴板信息，则加载上一次未发送的激活信息
            String data = mConfig.getActivateInfo();
            if (data.isEmpty() || !parseClipBoardInfo(data, activateDeepLink)) {
                activateDeepLink = null;//置为空，防止传空值数据
            }
        }
        submitActivateEvent();
    }

    private void submitActivateEvent() {
        isActivateSended = true;
        if (activateDeepLink != null) {
            //当deeplink不为空时补发reengage事件
            sendReengage(activateDeepLink);
        }
        //原有逻辑：若intent被消费，则不上传剪贴板的数据,即applink优先度高于activate
        final ActivateEvent activateEvent = isDigestedIntent ? new ActivateEvent() : new ActivateEvent(activateDeepLink);
        CoreInitialize.messageProcessor().persistEvent(activateEvent);
        CoreInitialize.config().setDeviceActivated();
        mConfig.setActivateInfo("");//清空剪贴板数据
    }


    /**
     * @param intent   intent 啦
     * @param activity activity 啦
     * @return intent 是否被 GIO 消费啦
     */
    public boolean handleIntent(Intent intent, Activity activity) {
        lastIntentRef = new WeakReference<>(intent);
        EventCenter.getInstance().post(new NewIntentEvent(intent));
        Uri data = intent == null ? null : intent.getData();
        if (data == null) {
            return false;
        }
        if (data.getScheme() == null) {
            return false;
        }
        if (data.getScheme().startsWith("growing.")) {
            ValidUrlEvent event = new ValidUrlEvent(data, activity, ValidUrlEvent.DEEPLINK);
            EventCenter.getInstance().post(event);
            intent.setData(null);
            return true;
        }
        if (data.getHost() == null) return false;
        if (isDeepLinkUrl(null, data)) {
            ValidUrlEvent event = new ValidUrlEvent(data, activity, ValidUrlEvent.APPLINK);
            EventCenter.getInstance().post(event);
            intent.setData(null);
            return true;
        }
        return false;
    }

    public static boolean isDeepLinkUrl(@Nullable String url, @Nullable Uri uri) {
        if (uri == null) {
            if (url == null) {
                return false;
            }
            uri = Uri.parse(url);
        }
        String host = uri.getHost();
        String scheme = uri.getScheme();
        if (host == null || scheme == null) return false;
        if (!"http".equals(scheme) && !"https".equals(scheme))
            return false;
        return "gio.ren".equals(host)
                || "datayi.cn".equals(host)
                || host.endsWith(".datayi.cn");
    }

    public boolean doDeeplinkByUrl(@Nullable String url, @Nullable DeeplinkCallback callback) {
        if (!isDeepLinkUrl(url, null)) {
            LogUtil.d(TAG, "doDeeplinkByUrl, but url is not DeeplinkUrl, just return false: ", url);
            return false;
        }
        handleAppLink(parseTrackerId(url), false, true, callback);
        return true;
    }

    @Subscribe
    public void onValidSchemaUrlIntent(ValidUrlEvent event) {
        if (CoreInitialize.coreAppState().getForegroundActivity() == null) {
            CoreInitialize.coreAppState().setForegroundActivity(event.activity);
        }
        CoreInitialize.deviceUUIDFactory().initUserAgent();
        switch (event.type) {
            case ValidUrlEvent.DEEPLINK:
                String openConsole = event.data.getQueryParameter("openConsoleLog");
                if (!TextUtils.isEmpty(openConsole)) {

                    if ("YES".equalsIgnoreCase(openConsole)) {
                        LogUtil.add(LogUtil.DebugUtil.getInstance());
                    }
                }

                if (TextUtils.isEmpty(event.data.getQueryParameter("link_id"))) {
                    LogUtil.e(TAG, "onValidSchemaUrlIntent, but not found link_id, return");
                    return;
                }
                handleDeepLink(event.data);
                break;
            case ValidUrlEvent.APPLINK:
                if (TextUtils.isEmpty(event.data.getPath())) {
                    LogUtil.e(TAG, "onValidSchemaUrlIntent, but not valid applink, return");
                    return;
                }
                handleAppLink(parseTrackerId(event.data.toString()), true, false, null);
                break;
        }
    }

    String parseTrackerId(@NonNull String url) {
        String schemePart;
        if (url.startsWith("https://")) {
            schemePart = "https://";
        } else {
            schemePart = "http://";
        }
        return url.substring(url.indexOf("/", schemePart.length()) + 1);
    }

    /**
     * 中端不区分 DeepLink 和 AppLink ，大致流程是一样的，唯一的明显区别就是 AppLink 比 DeepLink 多请求一次网络数据
     *
     * @param trackId 为 applink 的 path
     */
    @VisibleForTesting
    void handleAppLink(String trackId, final boolean sendReengage, boolean isInApp, final DeeplinkCallback callback) {
        final long wakeTime = System.currentTimeMillis();
        Map<String, String> mHeaders = new HashMap<>();
        DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
        mHeaders.put("ua", deviceUUIDFactory.getUserAgent());
        mHeaders.put("ip", deviceUUIDFactory.getIp());
        if (CoreInitialize.config().isEnabled()) {
            final HttpEvent httpEvent = new HttpEvent();
            httpEvent.setUrl(NetworkConfig.getInstance().getAppLinkParamsUrl(trackId, isInApp));
            httpEvent.setRequestMethod(HttpEvent.REQUEST_METHOD.GET);
            httpEvent.setHeaders(mHeaders);
            httpEvent.setCallBack(new HttpCallBack() {
                @Override
                public void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
                    onReceiveAppLinkArgs(responseCode, data, sendReengage, wakeTime, callback);
                }
            });
            EventCenter.getInstance().post(httpEvent);
        }
    }

    void onReceiveAppLinkArgs(Integer responseCode, byte[] body, boolean sendReengage, final long wakeTime, final DeeplinkCallback callback) {
        final DeeplinkInfo info = new DeeplinkInfo();
        int errorCode = DeeplinkCallback.SUCCESS;
        try {
            if (responseCode == HttpURLConnection.HTTP_OK) {
                JSONObject rep = new JSONObject(new String(body));
                int code = rep.getInt("code");
                String msg = rep.optString("msg");

                if (code == 200) {
                    JSONObject data = rep.getJSONObject("data");
                    info.clickID = data.getString("click_id");
                    info.linkID = data.getString("link_id");
                    info.clickTM = data.getString("tm_click");
                    info.customParams = data.getString("custom_params");
                    info.tm = System.currentTimeMillis();
                    if (sendReengage) {
                        sendReengage(info);
                    }
                } else {
                    errorCode = code;
                    LogUtil.d(TAG, "onReceiveApplinkArgs returnCode error: ", errorCode, ": ", msg);
                }
            } else {
                errorCode = DeeplinkCallback.ERROR_NET_FAIL;
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "parse the applink params error \n" + e.toString());
            errorCode = DeeplinkCallback.ERROR_EXCEPTION;
        }
        final Map<String, String> params;
        if (errorCode == DeeplinkCallback.SUCCESS) {
            params = new HashMap<>();
            errorCode = parseJson(info.customParams, params);
        } else {
            params = null;
        }
        final int finalErrorCode = errorCode;
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeeplinkCallback deeplinkCallback = callback;
                if (deeplinkCallback == null) {
                    deeplinkCallback = mConfig.getDeeplinkCallback();
                }
                if (deeplinkCallback != null) {
                    deeplinkCallback.onReceive(params, finalErrorCode, System.currentTimeMillis() - wakeTime);
                }
            }
        });
    }

    private void handleDeepLink(Uri data) {
        DeeplinkInfo deeplinkInfo = new DeeplinkInfo();
        String dataUri = data.toString();
        Uri uri = Uri.parse(dataUri.replace("&amp;", "&"));
        deeplinkInfo.linkID = uri.getQueryParameter("link_id");
        deeplinkInfo.clickID = uri.getQueryParameter("click_id") != null ? uri.getQueryParameter("click_id") : "";
        deeplinkInfo.clickTM = uri.getQueryParameter("tm_click") != null ? uri.getQueryParameter("tm_click") : "";
        deeplinkInfo.customParams = uri.getQueryParameter("custom_params");
        deeplinkInfo.tm = System.currentTimeMillis();
        sendReengage(deeplinkInfo);
        if (mConfig.getDeeplinkCallback() != null) {
            Map<String, String> params = new HashMap<>();
            int result = parseJson(deeplinkInfo.customParams, params);
            mConfig.getDeeplinkCallback().onReceive(params, result, 0);
        }
    }

    private int parseJson(String json, Map<String, String> map) {

        if (TextUtils.isEmpty(json)) {
            map.clear();
            return DeeplinkCallback.NO_QUERY;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if ("_gio_var".equals(key))
                    continue;
                map.put(key, jsonObject.getString(key));
            }
        } catch (JSONException jsonException) {
            map.clear();
            return DeeplinkCallback.PARSE_ERROR;
        }
        return DeeplinkCallback.SUCCESS;
    }

    private void sendReengage(DeeplinkInfo deeplinkInfo) {
        if (TextUtils.isEmpty(deeplinkInfo.customParams)) {
            deeplinkInfo.customParams = "{}";
        }
        CoreInitialize.messageProcessor().persistEvent(new ReengageEvent(deeplinkInfo));
    }

    private void reengage(String linkID, String clickID, String tmClick, String customParams, long tm) {
        if (TextUtils.isEmpty(customParams)) {
            customParams = "{}";
        }
        DeeplinkInfo info = new DeeplinkInfo();
        info.linkID = linkID;
        info.clickID = clickID;
        info.clickTM = tmClick;
        info.customParams = customParams;
        info.tm = tm;
        UploadData uploadData = new UploadData.Builder()
                .setType(UploadData.UploadType.REENGAGE)
                .setDeeplinkInfo(info)
                .build();
        uploadData.upload();
    }


    /**
     * 剪切板数据类型不做校验, 不同浏览器返回MIME类型不同
     * 1. 夸克返回 ClipDescription.MIMETYPE_TEXT_HTML
     * 2. 小米原生浏览器返回 ClipDescription.MIMETYPE_TEXT_PLAIN
     * <p>
     * 点击短链跳转下载后打开的 app ，期望剪贴板被前端落地页写入自定义参数等信息
     * ZWSP : https://zh.wikipedia.org/wiki/%E9%9B%B6%E5%AE%BD%E7%A9%BA%E6%A0%BC
     * 剪贴板数据解析后格式：
     * {
     * "typ":"gads",
     * "link_id":"xxxxx",
     * "click_id":"xxxx",
     * "tm_click":"",
     * "scheme":"xxxxxx",
     * "v1":{
     * "custom_params":{
     * }
     * },
     * "v2":{
     * }
     *
     * @return true 是 Deffer 延迟深度链接，拿到了有效的剪贴板数据
     * false 是普通打开，剪贴板里没有广告组写进去的数据
     */
    @VisibleForTesting
    boolean checkClipBoard(DeeplinkInfo info) {
        try {
            ClipData clipData = cm != null ? cm.getPrimaryClip() : null;
            if (clipData == null) return false;
            if (clipData.getItemCount() == 0) return false;
            ClipData.Item item = clipData.getItemAt(0);
            CharSequence charSequence = item.coerceToText(mGlobalContext);
            if (charSequence == null || charSequence.length() == 0) return false;
            char zero = (char) 8204;
            StringBuilder binaryList = new StringBuilder();
            for (int i = 0; i < charSequence.length(); i++) {
                binaryList.append(charSequence.charAt(i) == zero ? 0 : 1);
            }
            final int SINGLE_CHAR_LENGTH = 16;
            if (binaryList.length() % 16 != 0) {
                return false;
            }
            ArrayList<String> bs = new ArrayList<>();
            int i = 0;
            while (i < binaryList.length()) {
                bs.add(binaryList.substring(i, i + SINGLE_CHAR_LENGTH));
                i += SINGLE_CHAR_LENGTH;
            }
            StringBuilder listString = new StringBuilder();
            for (String s : bs) {
                listString.append((char) Integer.parseInt(s, 2));
            }

            String data = listString.toString();
            if (parseClipBoardInfo(data, info)) {
                //保存剪切板数据
                mConfig.setActivateInfo(data);
                //clean up the clip board
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cm.clearPrimaryClip();
                } else {
                    cm.setPrimaryClip(ClipData.newPlainText(null, null));
                }
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            LogUtil.e(TAG, e.toString());
            return false;

        }
    }

    boolean parseClipBoardInfo(String data, DeeplinkInfo info) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            if (!"gads".equals(jsonObject.getString("typ"))) {
                return false;
            }
            if (!ObjectUtils.equals(mConfig.getsGrowingScheme(), jsonObject.getString("scheme"))) {
                LogUtil.d(TAG, "非此应用的延迟深度链接， urlsheme 不匹配，期望为：" + mConfig.getsGrowingScheme() + "， 实际为：" + jsonObject.getString("scheme"));
                return false;
            }
            info.linkID = jsonObject.getString("link_id");
            info.clickID = jsonObject.getString("click_id");
            info.clickTM = jsonObject.getString("tm_click");
            JSONObject v1 = jsonObject.getJSONObject("v1");
            String customParams = v1.getString("custom_params");
            info.customParams = NetworkUtil.decode(customParams);
            info.tm = System.currentTimeMillis();

            return true;
        } catch (JSONException e) {
            LogUtil.e(TAG, "Clipboard 解析异常 ", e);
        }
        return false;
    }

    private void sendInfoToDefferCallback(DeeplinkInfo info) {
        if (info == null) return;
        try {
            JSONObject params = new JSONObject(info.customParams);
            final Map<String, String> paramsMap = new HashMap<>();
            final int result = parseJson(params.toString(), paramsMap);
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mConfig.getDeeplinkCallback() != null) {
                        mConfig.getDeeplinkCallback().onReceive(paramsMap, result, 0);
                    }
                }
            });
        } catch (JSONException e) {
            LogUtil.e(TAG, "deeplink info 解析异常 ", e);
        }
    }
}
