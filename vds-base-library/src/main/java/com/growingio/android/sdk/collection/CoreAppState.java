package com.growingio.android.sdk.collection;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.base.event.RefreshPageEvent;
import com.growingio.android.sdk.base.event.ScreenStatusEvent;
import com.growingio.android.sdk.base.event.net.NetWorkChangedEvent;
import com.growingio.android.sdk.ipc.GrowingIOIPC;
import com.growingio.android.sdk.models.VisitorVarEvent;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.NetworkUtil;
import com.growingio.android.sdk.utils.ObjectUtils;
import com.growingio.android.sdk.utils.PersistUtil;
import com.growingio.android.sdk.utils.SimpleJSONVariableUpdateHelper;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.WeakSet;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * 公用的AppState，存储着公用临时变量
 * - 存储AppVariable, PeopleVariable, UserId
 * - 存储经纬度信息
 * - 兼职触发激活事件
 * - 监听并保存网络状态
 * - 监听并保存屏幕是否高亮
 * - 存储了当前resume的Activity
 * Created by liangdengke on 2018/7/4.
 */
public class CoreAppState {

    public static final String TAG = "GIO.AppState";

    public static final int NETWORK_UNKNOWN = -1;
    public static final int NETWORK_OFFLINE = 0;
    public static final int NETWORK_CELLULAR_ONLINE = 1;
    public static final int NETWORK_WIFI_ONLINE = 2;

    @VisibleForTesting
    static String lastUserId;
    @VisibleForTesting
    long mLastSetLocationTime = 0;

    private Context mGlobalContext;
    @VisibleForTesting
    GConfig mConfig;

    private JSONObject mAppVariable = new JSONObject();
    private JSONObject mPeopleVariable = new JSONObject();
    private Double mLastLat;
    private Double mLastLon;
    private boolean screenOn = true;
    private String networkStateName;
    private int mNetworkState = NETWORK_UNKNOWN;
    private int mCurrentRootWindowsHashCode = -1;
    private MessageProcessor msgProcessor;
    private GrowingIOIPC growingIOIPC;
    private SessionManager sessionManager;

    private String mSpn;

    private WeakHashMap<Object, SimpleJSONVariableUpdateHelper> mPageVariableHelpers = new WeakHashMap<Object, SimpleJSONVariableUpdateHelper>();

    private WeakReference<Activity> mResumedActivity = new WeakReference<>(null);
    private WeakReference<Activity> mForeGroundActivity = new WeakReference<>(null);

    @VisibleForTesting
    WeakHashMap<Activity, WeakSet<Dialog>> mActivitiesWithGioDialogs = null;

    public void setMsgProcessor(MessageProcessor msgProcessor) {
        this.msgProcessor = msgProcessor;
    }

    public void setGrowingIOIPC(GrowingIOIPC growingIOIPC) {
        this.growingIOIPC = growingIOIPC;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public JSONObject getAppVariable() {
        return growingIOIPC.getAppVar();
    }

    public JSONObject getVisitorVariable() {
        return growingIOIPC.getVisitorVar();
    }

    public void setVisitorVariable(JSONObject visitorVariable) {
        growingIOIPC.setVisitorVar(visitorVariable);
        if (visitorVariable != null) {
            msgProcessor.persistEvent(new VisitorVarEvent(visitorVariable, System.currentTimeMillis()));
        }
    }

    // PageVariable将在MessageProcessor中被设置为null
    public SimpleJSONVariableUpdateHelper getPageVariableHelper(Object page) {
        SimpleJSONVariableUpdateHelper helper = mPageVariableHelpers.get(page);
        // page作为WeakHashMap的key, value中不能包含改page的强引用.
        final WeakReference<Object> pageRef = new WeakReference<>(page);
        if (helper == null) {
            helper = new SimpleJSONVariableUpdateHelper() {
                @Override
                public void afterUpdated() {
                    Object innerPage = pageRef.get();
                    if (innerPage != null) {
                        msgProcessor.onPageVariableUpdated(innerPage);
                    }
                }
            };
            mPageVariableHelpers.put(page, helper);
        }
        return helper;
    }

    /**
     * GrowingIO对用户弹出了对话框
     *
     * @param activity 弹出对话框所在Activity
     * @param dialog   弹出的dialog
     */
    public void onGIODialogShow(@NonNull Activity activity, @NonNull Dialog dialog) {
        LogUtil.d(TAG, "onGIODialogShow: dialog ----> ", dialog);
        if (mActivitiesWithGioDialogs == null) {
            mActivitiesWithGioDialogs = new WeakHashMap<>();
        }
        WeakSet<Dialog> dialogs = mActivitiesWithGioDialogs.get(activity);
        if (dialogs == null) {
            dialogs = new WeakSet<>();
            mActivitiesWithGioDialogs.put(activity, dialogs);
        }
        dialogs.add(dialog);
    }

    @VisibleForTesting
    void hideGIODialog(@NonNull Activity activity) {
        if (mActivitiesWithGioDialogs == null) {
            return;
        }
        WeakSet<Dialog> dialogs = mActivitiesWithGioDialogs.get(activity);
        if (dialogs == null || dialogs.isEmpty())
            return;
        for (Dialog dialog : dialogs) {
            if (dialog != null && dialog.isShowing()) {
                LogUtil.d(TAG, "hideGIODialog, one dialog not hide: ===> ", dialog);
                dialog.dismiss();
            }
        }
        dialogs.clear();
        mActivitiesWithGioDialogs.remove(activity);
    }

    private SimpleJSONVariableUpdateHelper mAppVariableHelper = new SimpleJSONVariableUpdateHelper(mAppVariable) {
        @Override
        public void afterUpdated() {
            msgProcessor.onAppVariableUpdated();
        }
    };

    private void saveAppVar(JSONObject appVariable) {
        mAppVariable = appVariable;
        growingIOIPC.setAppVar(appVariable);
    }

    private SimpleJSONVariableUpdateHelper mConversionVariableHelper = new SimpleJSONVariableUpdateHelper() {
        @Override
        public void afterUpdated() {
            JSONObject jsonObject = getVariable();
            if (jsonObject == null)
                return;
            msgProcessor.setEvar(jsonObject);
            setVariable(new JSONObject());
        }
    };

    private SimpleJSONVariableUpdateHelper mPeopleVariableHelper = new SimpleJSONVariableUpdateHelper(mPeopleVariable) {
        @Override
        public void afterUpdated() {
            JSONObject jsonObject = getVariable();
            if (jsonObject == null)
                return;
            msgProcessor.setPeople(jsonObject);
            setVariable(new JSONObject());
        }
    };

    public String getSPN() {
        return mSpn;
    }

    public void setSPN(String spn) {
        this.mSpn = spn;
    }

    public String getProjectId() {
        return GrowingIO.sProjectId;
    }

    public String getNetworkStateName() {
        return networkStateName;
    }

    /**
     * Location更新后需要重发一次Visit事件
     */
    public void setLocation(double latitude, double longitude) {
        long currentTime = System.currentTimeMillis();
        // For Ticket PI-10656
        if (Math.abs(latitude) < 0.00001 && Math.abs(longitude) < 0.00001) {
            LogUtil.d(TAG, "found invalid latitude and longitude, and return: ", latitude, ", ", longitude);
            return;
        }
        if ((mLastLat == null || mLastLon == null) || Util.shouldSetLocation(latitude, longitude, mLastLat, mLastLon, currentTime, mLastSetLocationTime)) {
            mLastLat = latitude;
            mLastLon = longitude;
            mLastSetLocationTime = currentTime;
            if (getResumedActivity() != null) {
                msgProcessor.saveVisit(false);
            } else {
                // https://codes.growingio.com/D5687
                LogUtil.d(TAG, "setLocation, but resume Activity is null, next resume send visit");
                sessionManager.nextResumeResendVisit();
            }
        }
    }

    public Double getLatitude() {
        return mLastLat;
    }

    public Double getLongitude() {
        return mLastLon;
    }

    public void clearLocation() {
        mLastLat = null;
        mLastLon = null;
    }


    public Activity getForegroundActivity() {
        //TODO  更精细的和ResumedActivity融合
        return mForeGroundActivity.get();
    }

    public void setForegroundActivity(Activity activity) {
        //TODO  更精细的和ResumedActivity融合
        mForeGroundActivity = new WeakReference<>(activity);
    }

    public int networkState() {
        if (mNetworkState == NETWORK_UNKNOWN) {
            mNetworkState = queryNetworkState();
        }
        return mNetworkState;
    }

    private int queryNetworkState() {
        int networkState = NETWORK_OFFLINE;
        networkStateName = NetworkUtil.NETWORK_UNKNOWN;
        try {
            final ConnectivityManager cm =
                    (ConnectivityManager) mGlobalContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    networkState = NETWORK_WIFI_ONLINE;
                    networkStateName = NetworkUtil.NETWORK_WIFI;
                } else {
                    networkState = NETWORK_CELLULAR_ONLINE;
                    networkStateName = NetworkUtil.getMobileNetworkTypeName(activeNetwork.getSubtype(), activeNetwork.getSubtypeName());
                }
            }
        } catch (Exception ignored) {
        }
        return networkState;
    }


    public Activity getResumedActivity() {
        return mResumedActivity.get();
    }

    public void setResumedActivity(Activity activity) {
        mForeGroundActivity = new WeakReference<>(activity);
        mResumedActivity = new WeakReference<>(activity);
    }

    public int getCurrentRootWindowsHashCode() {
        if (mCurrentRootWindowsHashCode == -1
                && mForeGroundActivity != null && mForeGroundActivity.get() != null) {
            //该时间点， 用户理论上setContentView已经结束
            mCurrentRootWindowsHashCode = mForeGroundActivity.get().getWindow().getDecorView().hashCode();
        }
        return mCurrentRootWindowsHashCode;
    }

    public Context getGlobalContext() {
        return mGlobalContext;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Subscribe(priority = 1000) // 应该采用一个最高优先级的
    public void onActivityLifeCycleChange(ActivityLifecycleEvent event) {
        Activity activity = event.getActivity();
        if (activity == null) {
            Log.d(TAG, "onActivityLifeCycleChanged, but activity not found, return");
            return;
        }
        switch (event.event_type) {
            case ON_CREATED:
                setForegroundActivity(activity);
                if (!activity.isChild()) {
                    mCurrentRootWindowsHashCode = -1;
                }
                break;
            case ON_NEW_INTENT:
                break;
            case ON_RESUMED:
                LogUtil.d(TAG, "onActivityResumed ", activity);
                setResumedActivity(activity);
                if (!activity.isChild()) {
                    mCurrentRootWindowsHashCode = activity.getWindow().getDecorView().hashCode();
                }
                break;
            case ON_PAUSED:
                LogUtil.d(TAG, "onActivityPaused ", activity);
                if (!activity.isChild()) {
                    mCurrentRootWindowsHashCode = -1;
                }
                mResumedActivity = new WeakReference<>(null);
                break;

            case ON_DESTROYED:
                LogUtil.d(TAG, "onActivityDestroyed ", activity);
                mPageVariableHelpers.remove(activity);
                hideGIODialog(activity);
                break;
        }
    }

    @Subscribe
    public synchronized void onNetworkChanged(NetWorkChangedEvent event) {
        mNetworkState = NETWORK_UNKNOWN;
    }

    CoreAppState(GConfig gConfig, Context context) {
        mGlobalContext = context;
        PersistUtil.init(mGlobalContext);
        mConfig = gConfig;
        if (GConfig.isReplace) {
            Toast.makeText(mGlobalContext, GConfig.GROWING_VERSION, Toast.LENGTH_SHORT).show();
        }
    }

    @VisibleForTesting
    public CoreAppState() {

    }


    @Subscribe
    public void onScreenStatusChanged(ScreenStatusEvent event) {
        this.screenOn = event.type != ScreenStatusEvent.ScreenStatusType.SCREEN_OFF;
    }

    public boolean isScreenOn() {
        return screenOn;
    }

    /**
     * 1. userid设置从空到非空，visit不发送，sessionid不发送
     * 2. userid设置从非空到空，visit不发送，sessionid不发送
     * 3. userid设置从"A"到"B", visit发送，sessionid发送
     * 4. userid设置从"A"到空再到"B", visit发送，sessionid发送
     * 5. userid设置从"A"到"A"，visit不发送，sessionID不发送
     */
    void setUserId(String userId) {
        if (TextUtils.isEmpty(userId)) {
            // to null, never send visit, just return
            clearUserId();
            return;
        }

        if (userId.length() > 1000) {
            Log.e(TAG, ErrorLog.USER_ID_TOO_LONG);
            return;
        }

        // to non-null
        String oldUserId = mConfig.getAppUserId();
        if (ObjectUtils.equals(userId, oldUserId)) {
            LogUtil.d(TAG, "setUserId, but the userId is same as the old userId, just return");
            return;
        }
        mConfig.setAppUserId(userId);
        if (TextUtils.isEmpty(oldUserId)) {
            if (TextUtils.isEmpty(lastUserId) || ObjectUtils.equals(userId, lastUserId)) {
                lastUserId = userId;
//              发送 page 和 ios 逻辑统一
                EventCenter.getInstance().post(new RefreshPageEvent(true, false));
                return;
            }
        }
        lastUserId = userId;
        if (mConfig.isEnabled()) {
            sessionManager.updateSessionByUserIdChanged();
            EventCenter.getInstance().post(new RefreshPageEvent(true, false));
        }
    }

    void clearUserId() {
        String userId = mConfig.getAppUserId();
        if (userId != null) {
            lastUserId = userId;
        }
        //清除SP缓存userid
        mConfig.cleanUserId();
    }

    void setAppVariable(JSONObject variable) {
        mAppVariableHelper.update(variable);
        saveAppVar(mAppVariableHelper.getVariable());
        mAppVariableHelper.setVariable(new JSONObject());
    }

    void setAppVariable(String key, Object value) {
        mAppVariableHelper.update(key, value);
        saveAppVar(mAppVariableHelper.getVariable());
//        mAppVariableHelper.setVariable(new JSONObject());
    }

    public JSONObject getPeopleVariable() {
        if (mPeopleVariableHelper != null)
            return mPeopleVariableHelper.getVariable();

        return null;
    }

    void setPeopleVariable(JSONObject variable) {
        mPeopleVariableHelper.update(variable);
    }

    void setPeopleVariable(String key, Object value) {
        mPeopleVariableHelper.update(key, value);
    }

    void setConversionVariable(JSONObject variable) {
        mConversionVariableHelper.update(variable);
    }

    public JSONObject getConversionVariable() {
        if (mConversionVariableHelper != null) {
            return mConversionVariableHelper.getVariable();
        }
        return null;
    }

    void setConversionVariable(String key, Object value) {
        mConversionVariableHelper.update(key, value);
    }


    // TODO: 2018/7/4 感觉PageVariable不应该这样设置， 应该跟PageObject绑定
    public JSONObject getPageVariable() {
        if (getResumedActivity() != null) {
            SimpleJSONVariableUpdateHelper helper = getPageVariableHelper(getResumedActivity());
            if (helper != null) {
                return helper.getVariable();
            }
        }
        return null;
    }

    void setPageVariable(Object page, JSONObject variable) {
        getPageVariableHelper(page).update(variable);
    }

    void setPageVariable(Object page, String key, Object value) {
        getPageVariableHelper(page).update(key, value);
    }
}
