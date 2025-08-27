package com.growingio.android.sdk.collection;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Pair;

import com.growingio.android.sdk.base.BuildConfig;
import com.growingio.android.sdk.base.event.DBInitDiagnose;
import com.growingio.android.sdk.deeplink.DeeplinkCallback;
import com.growingio.android.sdk.message.HandleType;
import com.growingio.android.sdk.message.MessageHandler;
import com.growingio.android.sdk.models.EventSID;
import com.growingio.android.sdk.models.VPAEvent;
import com.growingio.android.sdk.models.ViewAttrs;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.CustomerInterface;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ProcessLock;
import com.growingio.android.sdk.utils.Util;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventBus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by xyz on 2015/3/31.
 */
public class GConfig {
    private static final String TAG = "GIO.GConfig";

    public static boolean isPreflightChecked = false;
    public static boolean isEndPointLow = false;

    public static void resetPreflightStatus() {
        isPreflightChecked = false;
        isEndPointLow = false;
    }

    public static boolean isReplace = false;
    public static boolean sCanHook = false;
    public static boolean supportMultiCircle = true;
    public static boolean collectWebViewUserAgent = true;
    public static boolean DEBUG = false;
    public static boolean isRnMode = false;
    public static final String GROWING_VERSION = BuildConfig.VERSION_NAME + "_" + BuildConfig.GIT_SHA;
    public static boolean USE_ID = false;
    public static boolean CIRCLE_USE_ID = USE_ID;
    public static boolean USE_RN_OPTIMIZED_PATH = false;
    public static boolean USE_RN_NAVIGATOR_PAGE = false;

    public static boolean mSupportTaobaoWebView;

    public static final String AGENT_VERSION = BuildConfig.AGENT_VERSION;
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy_MM_dd", Locale.US);
    public static final String PREF_FILE_NAME = "growing_profile";
    private static final String PREF_ECSID_FILE_NAME = "growing_ecsid";
    private static final String PREF_SERVER_PREFERENCES_FILE_NAME = "growing_server_pref";
    private static final String PREF_FLOAT_X = "pref_float_x";
    private static final String PREF_FLOAT_Y = "pref_float_y";
    private static final String PREF_CELLULAR_DATA_SIZE = "pref_cellular_data_size";
    private static final String PREF_DATE = "pref_date";
    private static final String PREF_SHOW_CIRCLE_TIP = "pref_show_circle_tip";
    private static final String PREF_SHOW_TAG_SUCCESS = "pref_show_tag_success";
    private static final String PREF_VDS_PLUGIN_LAST_MODIFIED = "pref_vds_plugin_last_modified";
    private static final String PREF_SETTINGS_ETAG = "pref_settings_etag";
    private static final String PREF_SERVER_SETTINGS = "pref_server_settings";
    private static final String PREF_DISABLE_ALL = "pref_disable_all";
    private static final String PREF_ENABLE_THROTTLE = "pref_enable_throttle";
    private static final String PREF_SAMPLING_RATE = "pref_sampling_rate";
    private static final String PREF_ENABLE_IMP = "pref_enable_imp";
    private static final String PREF_DISABLE_CELLULAR_IMPRESSION = "pref_disable_cellular_impression";
    public static final String PREF_USER_ID_IN_APP = "pref_user_id_in_app";
    private static final String PREF_DEVICE_ACTIVATED = "pref_device_activated";
    private static final String PREF_DEVICE_ACTIVATE_INFO = "pref_device_activate_info";
    private static final String ESID_TYPE_ALL = "all";


    private Context mContext;

    private int mUploadBulkSize;
    private long mFlushInterval;
    public boolean mTestMode;

    private long mSessionInterval;
    private String mChannel;
    private boolean mDiagnoseEnabled = DEBUG;
    public static String sGrowingScheme;
    private boolean mShowTags = false;
    private boolean mTrackAllFragment;
    private boolean mHarmonyEnable;
    private boolean mReadClipBoardEnable;

    // cellular threshold is 1 MB
    private long mCellularDataLimit;
    private int mTotalCellularDataSize;

    // 这些配置最终展现的效果(merged from user and server settings)
    private boolean mEnabled = true;
    private boolean mEnableImp = true;
    private boolean mDisableCellularImp = false;
    private boolean mThrottle = false;

    // 用户进行的设置(only changed by user)
    private boolean gEnabled = true;
    private boolean gEnableImp = false;
    private boolean gDisableCellularImp = false;
    private boolean gThrottle = false;

    // 服务端进行的设置(only changed by server settings)
    private boolean sEnabled = true;
    private boolean sEnableImp = true;
    private boolean sDisableCellularImp = false;
    private boolean sThrottle = false;

    private boolean mGDPREnabled = true;
    private double mSampling = -1;
    private boolean mTrackWebView;
    private boolean mIsHashTagEnable = false;
    private boolean mInstantFiltersInitialized = false;
    private HashMap<String, ArrayList<ViewAttrs>> mInstantFilters;
    private ProcessLock esidLock;
    private boolean isMultiProcessEnabled = false;
    private boolean mRequireAppProcessesEnabled = true;
    private boolean isImageViewCollectionEnable = true;
    private int imageViewCollectionSize = 2048;
    private DeeplinkCallback deeplinkCallback = null;
    private CustomerInterface.Encryption encryptEntity = null;
    private OnConfigChangeListener mConfigChangeListener = null;
    private SharedPreferences mSharedPreference;
    private SharedPreferences mServerSharedPreference;

    private String mAppVersion;

    private int mRunMode;

    public DeeplinkCallback getDeeplinkCallback() {
        return this.deeplinkCallback;
    }

    public CustomerInterface.Encryption getEncryptEntity() {
        return this.encryptEntity;
    }

    public OnConfigChangeListener getConfigChangeListener() {
        return mConfigChangeListener;
    }

    public void setConfigChangeListener(OnConfigChangeListener configChangeListener) {
        mConfigChangeListener = configChangeListener;
    }

    public static boolean isInstrumented() {
        return isReplace || isRnMode;
    }

    public boolean isMultiProcessEnabled() {
        return isMultiProcessEnabled;
    }

    public boolean isRequireAppProcessesEnabled() {
        return mRequireAppProcessesEnabled;
    }

    public int getImageViewCollectionSize() {
        return imageViewCollectionSize;
    }

    public boolean isImageViewCollectionEnable() {
        return isImageViewCollectionEnable;
    }

    public static String getProjectId() {
        return null;
    }

    // 别用这个方法，plugin 字节码处理会重新赋值
    public static String getUrlScheme() {
        return null;
    }

    public String getsGrowingScheme() {
        return sGrowingScheme;
    }

    // called by check, not the enable value
    public boolean isEnabled() {
        return mGDPREnabled && mEnabled;
    }

    public double getSampling() {
        return mSampling;
    }

    public boolean shouldSendImp() {
        return mEnableImp;
    }

    public boolean shouldTrackAllFragment() {
        return mTrackAllFragment;
    }

    public boolean isHarmonyEnable() {
        return mHarmonyEnable;
    }

    public boolean isReadClipBoardEnable() {
        return mReadClipBoardEnable;
    }

    public boolean isHashTagEnable() {
        return mIsHashTagEnable;
    }

    public void disableImpression() {
        gEnableImp = false;
        mergeUserAndServerSettings();
    }

    public void mergeUserAndServerSettings() {
        mEnableImp = gEnableImp && sEnableImp;
        mEnabled = gEnabled && sEnabled;
        mDisableCellularImp = gDisableCellularImp || sDisableCellularImp;
        mThrottle = gThrottle || sThrottle;
    }

    public void enableImpression() {
        gEnableImp = true;
        mergeUserAndServerSettings();
    }

    public boolean isCellularImpDisabled() {
        return mDisableCellularImp;
    }

    public String getAppUserId() {
        return CoreInitialize.growingIOIPC().getUserId();
    }

    public void cleanUserId() {
        CoreInitialize.growingIOIPC().setUserId(null);
        if (mConfigChangeListener != null) {
            mConfigChangeListener.onUserIdChanged(null);
        }
    }

    public String getAppVersion() {
        if (mAppVersion == null) {
            try {
                mAppVersion = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            } catch (Throwable e) {
                // PM died exception
                LogUtil.e(TAG, e.getMessage(), e);
            }
        }
        return mAppVersion;
    }

    public int getRunMode() {
        return mRunMode;
    }

    public void setAppUserId(String uid) {
        CoreInitialize.growingIOIPC().setUserId(uid);
        if (mConfigChangeListener != null) {
            mConfigChangeListener.onUserIdChanged(uid);
        }
    }

    public HashMap<String, ArrayList<ViewAttrs>> getInstantFilters() {
        return mInstantFilters;
    }

    public void setChannel(String channel) {
        mChannel = channel;
    }

    public boolean isDiagnoseEnabled() {
        return mDiagnoseEnabled;
    }

    public boolean isTestMode() {
        return mTestMode || PendingStatus.isEnable();
    }

    public int getUploadBulkSize() {
        return mUploadBulkSize;
    }

    public long getFlushInterval() {
        return mFlushInterval;
    }

    public String getChannel() {
        return mChannel;
    }

    public boolean shouldTrackWebView() {
        return mTrackWebView;
    }

    long getSessionInterval() {
        return mSessionInterval;
    }

    @VisibleForTesting
    GConfig() {

    }

    public GConfig(AbstractConfiguration configuration) {
        mContext = configuration.context.getApplicationContext();
        DEBUG = configuration.debugMode;
        isRnMode = configuration.rnMode;
        // 与iOS保持一致，flush最小为5s
        mFlushInterval = Math.max(configuration.flushInterval, 5 * 1000L);
        mUploadBulkSize = configuration.bulkSize;
        mSessionInterval = configuration.sessionInterval;
        // 这几个变量区分用户设置的与服务器下派的配置
        gEnabled = !configuration.disabled;
        gDisableCellularImp = configuration.disableCellularImp;
        gEnableImp = !configuration.disableImpression;
        gThrottle = configuration.throttle;

        mSupportTaobaoWebView = configuration.taobaoWebViewSupport;
        mGDPREnabled = configuration.gdprEnabled;
        mCellularDataLimit = configuration.cellularDataLimit;
        mTestMode = configuration.testMode;
        supportMultiCircle = configuration.spmc;
        collectWebViewUserAgent = configuration.collectWebViewUserAgent;
        mDiagnoseEnabled = configuration.diagnose;
        mChannel = configuration.channel;
        mTrackAllFragment = configuration.trackAllFragments;
        mHarmonyEnable = configuration.harmonyEnable;
        mReadClipBoardEnable = configuration.readClipBoardEnable;
        mRunMode = configuration.runMode;
        mTrackWebView = configuration.trackWebView;
        mIsHashTagEnable = configuration.isHashTagEnable;
        USE_ID = configuration.useID;
        CIRCLE_USE_ID = USE_ID;
        sGrowingScheme = configuration.urlScheme;
        isMultiProcessEnabled = configuration.mutiprocess;
        mRequireAppProcessesEnabled = configuration.requireAppProcessesEnabled;
        isImageViewCollectionEnable = !configuration.disableImageViewCollection;
        deeplinkCallback = configuration.callback;
        encryptEntity = configuration.encryptEntity;
        esidLock = new ProcessLock(mContext, "growingio.lock");

        mSharedPreference = mContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        mServerSharedPreference = mContext.getSharedPreferences(PREF_SERVER_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    public void onBgInit(DeviceUUIDFactory deviceUUIDFactory) {
        readConfigFromPref();
        mergeUserAndServerSettings();

        if (mDiagnoseEnabled) {
            if (Util.isInSampling(deviceUUIDFactory.getDeviceId(), DEBUG ? 1 : 0.01)) {
                EventBus.getDefault().getExecutorService().submit(new Runnable() {
                    @Override
                    public void run() {
                        EventCenter.getInstance().post(new DBInitDiagnose());
                    }
                });
            } else {
                mDiagnoseEnabled = false;
            }
        }
        LogUtil.d(TAG, this.toString());
    }

    public static void enableRNOptimizedPath() {
        USE_RN_OPTIMIZED_PATH = true;
    }

    public static void disableRNOptimizedPath() {
        USE_RN_OPTIMIZED_PATH = false;
    }

    // USE_RN_OPTIMIZED_PATH用于指示当前RN应用是否开启元素XPath的优化
    public static boolean isUsingRNOptimizedPath() {
        return USE_RN_OPTIMIZED_PATH;
    }

    // 默认开启NNavigatorPage同时也开启元素XPath的优化
    public static void enableRNNavigatorPage() {
        USE_RN_NAVIGATOR_PAGE = true;
        USE_RN_OPTIMIZED_PATH = true;
    }

    public static void disableRNNavigatorPage() {
        USE_RN_NAVIGATOR_PAGE = false;
    }

    // USE_RN_NAVIGATOR_PAGE用于指示当前RN应用是否启用Navigator作为页面切换的控制器
    public static boolean isUsingRNNavigatorPage() {
        return USE_RN_NAVIGATOR_PAGE;
    }


    /**
     * The config below is saved in preference
     */
    private SharedPreferences getSharedPreferences() {
        return mSharedPreference;
    }

    private SharedPreferences getServerPreferences() {
        return mServerSharedPreference;
    }

    /**
     * 参考文档： <a href='https://codes.growingio.com/w/prd/mobile/sdk_data/'>https://codes.growingio.com/w/prd/mobile/sdk_data/</a><br/>
     * 每次调用都会自动累加记录对应的sid，尽量合并多次调用，不能重复调用。
     *
     * @param type 事件类型
     * @param size 事件包含具体条数，应该来自{@link VPAEvent#size()}
     * @return first:全局事件累计个数，second:和当前类型累计个数
     */
    public Pair<Integer, Integer> getAndAddEsid(String type, int size) {
        if (isMultiProcessEnabled()) {
            return getAndAddEsidFromFile(type, size);
        } else {
            return getAndAddEsidFromSP(type, size);
        }
    }

    private Pair<Integer, Integer> getAndAddEsidFromSP(String type, int size) {
        SharedPreferences sp = mContext.getSharedPreferences(PREF_ECSID_FILE_NAME, Context.MODE_PRIVATE);
        int esid = sp.getInt(type, 0);
        int gesid = sp.getInt(ESID_TYPE_ALL, 0);
        sp.edit().putInt(type, esid + size).putInt(ESID_TYPE_ALL, gesid + size).commit();
        return new Pair<Integer, Integer>(gesid, esid);
    }

    private Pair<Integer, Integer> getAndAddEsidFromFile(String type, int size) {
        EventSID sid = null;
        esidLock.acquire(1000);
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(mContext.openFileInput(PREF_ECSID_FILE_NAME));
            try {
                sid = (EventSID) inputStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (Exception ignored) {
        } finally {
            if (sid == null) {
                sid = new EventSID();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        int esid = sid.getSid(type);
        int gesid = sid.getSid(ESID_TYPE_ALL);
        sid.setSid(type, esid + size).setSid(ESID_TYPE_ALL, gesid + size);
        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(mContext.openFileOutput(PREF_ECSID_FILE_NAME, Context.MODE_PRIVATE));
            outputStream.writeObject(sid);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        esidLock.release();
        return new Pair<Integer, Integer>(gesid, esid);
    }

    public boolean shouldShowCircleTip() {
        return getSharedPreferences().getBoolean(PREF_SHOW_CIRCLE_TIP, true);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void setShowCircleTip(boolean show) {
        getSharedPreferences().edit().putBoolean(PREF_SHOW_CIRCLE_TIP, show).apply();
    }

    public boolean shouldShowTagSuccess() {
        return getSharedPreferences().getBoolean(PREF_SHOW_TAG_SUCCESS, true);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void setShowTagSuccess(boolean show) {
        getSharedPreferences().edit().putBoolean(PREF_SHOW_TAG_SUCCESS, show).apply();
    }

    public boolean shouldShowTags() {
        return mShowTags;
    }

    public void setShowTags(boolean show) {
        mShowTags = show;
    }

    public Point getFloatPosition() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        int x = sharedPreferences.getInt(PREF_FLOAT_X, -1);
        int y = sharedPreferences.getInt(PREF_FLOAT_Y, -1);
        return new Point(x, y);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void saveFloatPosition(int x, int y) {
        SharedPreferences sharedPreferences = getSharedPreferences();
        sharedPreferences.edit().putInt(PREF_FLOAT_X, x).putInt(PREF_FLOAT_Y, y).apply();
    }

    public void saveETagForSettings(String etag) {
        getSharedPreferences().edit().putString(PREF_SETTINGS_ETAG, etag).commit();
    }

    public String getSettingsETag() {
        return getSharedPreferences().getString(PREF_SETTINGS_ETAG, "");
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void saveServerSettings(String settings) {
        getServerPreferences().edit().putString(PREF_SERVER_SETTINGS, settings).apply();
        updateServerSettings(settings);
        mergeUserAndServerSettings();
        MessageHandler.handleMessage(HandleType.CONFIG_SAVE_SERVER_SETTINGS, settings);
    }

    public boolean prepareInstantFilters() {
        if (!mInstantFiltersInitialized) {
            mInstantFiltersInitialized = true;
            String serverSettings = getServerPreferences().getString(PREF_SERVER_SETTINGS, null);
            if (serverSettings != null) {
                try {
                    updateWhiteListTags(new JSONObject(serverSettings).getJSONArray("tags"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return mInstantFilters != null;
    }

    private void updateWhiteListTags(JSONArray whiteList) throws JSONException {
        mInstantFilters = new HashMap<String, ArrayList<ViewAttrs>>();
        for (int i = 0; i < whiteList.length(); i++) {
            JSONObject tag = whiteList.getJSONObject(i);
            String xpath = tag.optString("x");
            if (TextUtils.isEmpty(xpath)) continue;
            int idx = tag.optInt("idx", -1);
            ViewAttrs filter = new ViewAttrs();
            String page = tag.optString("p", null);
            filter.domain = tag.optString("d");
            filter.webElem = filter.domain.contains(Constants.WEB_PART_SEPARATOR);
            filter.xpath = xpath;
            filter.content = tag.optString("v", null);
            filter.index = idx != -1 ? String.valueOf(idx) : null;
            ArrayList<ViewAttrs> filters = mInstantFilters.get(page);
            if (filters == null) {
                filters = new ArrayList<ViewAttrs>(1);
                mInstantFilters.put(page, filters);
            }
            filters.add(filter);
        }
    }

    /**
     * copy 自IOS:
     * net     imp     throttle  释义                  丢弃
     * true    true    false    全网浏览量全发
     * *       false   false    全网不发浏览量
     * false   true    false    仅wifi下发送并浏览量全发
     * true    *       true     全网金发白名单
     * false   *       true     仅在wifi下发送并只发白名单
     * <p>
     * - net: 是否移动网络发送数据
     * - imp: 是否采集imp
     * - throttle: 是否只发白名单
     */
    private void updateServerSettings(String settings) {
        if (settings == null) return;
        try {
            JSONObject settingObject = new JSONObject(settings);
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            if (settingObject.has("disabled")) {
                sEnabled = !settingObject.getBoolean("disabled");
                if (!sEnabled) {
                    LogUtil.i(TAG, "GrowingIO Warning ：GIO 服务端下发关闭 SDK 指令，采集数据、圈选等功能关闭");
                }
                editor.putBoolean(PREF_DISABLE_ALL, !sEnabled);
            }
            if (settingObject.has("sampling")) {
                mSampling = settingObject.getDouble("sampling");
                editor.putFloat(PREF_SAMPLING_RATE, (float) mSampling);
            }
            if (settingObject.has("throttle")) {
                sThrottle = settingObject.getBoolean("throttle");
                editor.putBoolean(PREF_ENABLE_THROTTLE, sThrottle);
            }
            if (settingObject.has("imp")) {
                sEnableImp = settingObject.getBoolean("imp");
                editor.putBoolean(PREF_ENABLE_IMP, sEnableImp);
            }

            if (settingObject.has("net")) {
                boolean net = settingObject.getBoolean("net");
                sDisableCellularImp = !net;
                editor.putBoolean(PREF_DISABLE_CELLULAR_IMPRESSION, sDisableCellularImp);
            }
            editor.commit();
            if (settingObject.has("tags")) {
                updateWhiteListTags(settingObject.getJSONArray("tags"));
            }
        } catch (Exception ex) {
            LogUtil.e(TAG, ex.getMessage(), ex);
        }
    }

    private void readConfigFromPref() {
        SharedPreferences sp = getSharedPreferences();
        if (sp.contains(PREF_ENABLE_THROTTLE)) {
            sThrottle = sp.getBoolean(PREF_ENABLE_THROTTLE, false);
        }
        if (sp.contains(PREF_ENABLE_IMP)) {
            sEnableImp = sp.getBoolean(PREF_ENABLE_IMP, true);
        }
        if (sp.contains(PREF_DISABLE_CELLULAR_IMPRESSION)) {
            sDisableCellularImp = sp.getBoolean(PREF_DISABLE_CELLULAR_IMPRESSION, false);
        }
        if (sp.contains(PREF_DISABLE_ALL)) {
            sEnabled = !sp.getBoolean(PREF_DISABLE_ALL, false);
        }
        if (sp.contains(PREF_SAMPLING_RATE)) {
            mSampling = sp.getFloat(PREF_SAMPLING_RATE, 1f);
        }
        if (sp.contains(PREF_SERVER_SETTINGS)) {
            // Migrate server settings from default preference to standalone preference to optimize initial speed.
            getServerPreferences().edit().putString(PREF_SERVER_SETTINGS, sp.getString(PREF_SERVER_SETTINGS, null)).commit();
            sp.edit().remove(PREF_SERVER_SETTINGS).commit();
        }
        readCellularDataSize();
    }

    public boolean canSendByCellular() {
        readCellularDataSize();
        return mTotalCellularDataSize < mCellularDataLimit;
    }

    public void increaseCellularDataSize(int dataSize) {
        if (isNewDay()) {
            getSharedPreferences().edit().putInt(PREF_CELLULAR_DATA_SIZE, dataSize).commit();
            mTotalCellularDataSize = dataSize;
        } else {
            int preSize = getSharedPreferences().getInt(PREF_CELLULAR_DATA_SIZE, 0);
            mTotalCellularDataSize = preSize + dataSize;
            LogUtil.d("GIO.GConfig", "cellular data usage: ", mTotalCellularDataSize);
            getSharedPreferences().edit().putInt(PREF_CELLULAR_DATA_SIZE, mTotalCellularDataSize).commit();
        }
    }

    private boolean isNewDay() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        String today = mDateFormat.format(new Date(System.currentTimeMillis()));
        String date = sharedPreferences.getString(PREF_DATE, "");
        if (!TextUtils.equals(today, date)) {
            sharedPreferences.edit().putString(PREF_DATE, today).commit();
            return true;
        } else {
            return false;
        }
    }

    private void readCellularDataSize() {
        if (isNewDay()) {
            mTotalCellularDataSize = 0;
            getSharedPreferences().edit().putInt(PREF_CELLULAR_DATA_SIZE, 0).commit();
        } else {
            mTotalCellularDataSize = getSharedPreferences().getInt(PREF_CELLULAR_DATA_SIZE, 0);
        }
    }

    public void setGDPREnabled(boolean mGDPREnabled) {
        this.mGDPREnabled = mGDPREnabled;
        if (!mGDPREnabled) {
            LogUtil.i(TAG, "GrowingIO Warning ：SDK 关闭采集数据、圈选等功能");
        }
    }

    public long getVdsPluginLastModified() {
        return getSharedPreferences().getLong(PREF_VDS_PLUGIN_LAST_MODIFIED, 0);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void setVdsPluginLastModified(long modified) {
        getSharedPreferences().edit().putLong(PREF_VDS_PLUGIN_LAST_MODIFIED, modified).apply();
    }

    public void setThrottle(boolean throttle) {
        if (getSharedPreferences().contains(PREF_ENABLE_THROTTLE)) return;
        mThrottle = throttle;
    }

    public void disableAll() {
        if (getSharedPreferences().contains(PREF_DISABLE_ALL)) return;
        mEnabled = false;
    }

    public void enableAll() {
        mEnabled = true;
    }

    public boolean isThrottled() {
        return mThrottle;
    }

    /**
     * 如果是OP环境也返回true
     *
     * @return
     */
    public boolean isDeviceActivated() {
        LogUtil.d(TAG, "pref_device_activated:" + getSharedPreferences().getBoolean(PREF_DEVICE_ACTIVATED, false));
        return getSharedPreferences().getBoolean(PREF_DEVICE_ACTIVATED, false) || GConfig.ISOP();
    }

    public void setDeviceActivated() {
        getSharedPreferences().edit().putBoolean(PREF_DEVICE_ACTIVATED, true).apply();
        MessageHandler.handleMessage(HandleType.CONFIG_DEVICE_ACTIVATED);
    }

    public void setActivateInfo(String activateInfo) {
        LogUtil.d(TAG, "PREF_DEVICE_ACTIVATE_INFO 保存：" + activateInfo);
        getSharedPreferences().edit().putString(PREF_DEVICE_ACTIVATE_INFO, activateInfo).apply();
    }

    public String getActivateInfo() {
        return getSharedPreferences().getString(PREF_DEVICE_ACTIVATE_INFO, "");
    }

    @Override
    public String toString() {
        return "GrowingIO 配置信息：\nDEBUG: " + DEBUG + "\n" +
                "Enabled: " + mEnabled + "\n" +
                "USE_ID: " + USE_ID + "\n" +
                "Context: " + mContext + "\n" +
                "Test Mode: " + mTestMode + "\n" +
                "Upload bulk size: " + mUploadBulkSize + "\n" +
                "Flush interval: " + mFlushInterval + "\n" +
                "Session interval: " + mSessionInterval + "\n" +
                "Channel: " + mChannel + "\n" +
                "Track all fragments: " + mTrackAllFragment + "\n" +
                "Enable WebView: " + mTrackWebView + "\n" +
                "Enable HashTag: " + mIsHashTagEnable + "\n" +
                "Cellular data limit: " + mCellularDataLimit + "\n" +
                "Total cellular data size: " + mTotalCellularDataSize + "\n" +
                "Sampling: " + mSampling + "\n" +
                "Enable impression: " + shouldSendImp() + "\n" +
                "Enable MultiProcess: " + isMultiProcessEnabled + "\n" +
                "Enable RequireAppProcesses: " + mRequireAppProcessesEnabled + "\n" +
                "Throttle: " + mThrottle + "\n" +
                "Disable cellular impression: " + mDisableCellularImp + "\n" +
                "Instant filter initialized: " + mInstantFiltersInitialized;
    }

    public static boolean ISOP() {
        return BuildConfig.VERSION_NAME.startsWith("OP");
    }

    public static boolean ISRN() {
        return BuildConfig.VERSION_NAME.startsWith("RN");
    }
}
