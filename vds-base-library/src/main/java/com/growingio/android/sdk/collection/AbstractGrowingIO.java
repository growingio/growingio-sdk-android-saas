package com.growingio.android.sdk.collection;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.growingio.android.sdk.base.BuildConfig;
import com.growingio.android.sdk.base.event.BgInitializeSDKEvent;
import com.growingio.android.sdk.base.event.InitializeSDKEvent;
import com.growingio.android.sdk.base.event.RefreshPageEvent;
import com.growingio.android.sdk.deeplink.DeepLinkEvent;
import com.growingio.android.sdk.deeplink.DeeplinkCallback;
import com.growingio.android.sdk.deeplink.DeeplinkManager;
import com.growingio.android.sdk.message.MessageHandler;
import com.growingio.android.sdk.models.PageEvent;
import com.growingio.android.sdk.utils.ArgumentChecker;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.PermissionUtil;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventCenterException;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by xyz on 2015/2/10.
 */
public class AbstractGrowingIO<T> {

    static final String TAG = "GrowingIO";
    private static GrowingIO sInstance = null;
    static String sProjectId;
    public static final Object sInstanceLock = new Object();
    protected GConfig mGConfig;
    ArgumentChecker mArgumentChecker;

    public static final int GROWING_TAG_KEY = 0x5042b06; //16进制的北京易数科技有限公司电话号码
    public static final int GROWING_WEB_CLIENT_KEY = GROWING_TAG_KEY + 1;
    public static final int GROWING_WEB_BRIDGE_KEY = GROWING_WEB_CLIENT_KEY + 1;
    public static final int GROWING_VIEW_NAME_KEY = GROWING_WEB_BRIDGE_KEY + 1;
    public static final int GROWING_VIEW_ID_KEY = GROWING_VIEW_NAME_KEY + 1;
    public static final int GROWING_INHERIT_INFO_KEY = GROWING_VIEW_ID_KEY + 1;
    public static final int GROWING_CONTENT_KEY = GROWING_INHERIT_INFO_KEY + 1;
    public static final int GROWING_MONITORING_VIEWTREE_KEY = GROWING_CONTENT_KEY + 1;
    public static final int GROWING_MONITORING_FOCUS_KEY = GROWING_MONITORING_VIEWTREE_KEY + 1;
    public static final int GROWING_BANNER_KEY = GROWING_MONITORING_FOCUS_KEY + 1;
    public static final int GROWING_IGNORE_VIEW_KEY = GROWING_BANNER_KEY + 1;
    public static final int GROWING_HEAT_MAP_KEY = GROWING_IGNORE_VIEW_KEY + 1;
    public static final int GROWING_HOOK_LISTENTER = GROWING_HEAT_MAP_KEY + 1;
    public static final int GROWING_TRACK_TEXT = GROWING_HOOK_LISTENTER + 1;      // 记录改EditText的文本值信息
    public static final int GROWING_RN_PAGE_KEY = GROWING_TRACK_TEXT + 1;
    public static final int GROWING_IGNORE_VIEW_IMP_KEY = GROWING_RN_PAGE_KEY + 1;
    public static final int GROWING_WEB_VIEW_URL = GROWING_IGNORE_VIEW_IMP_KEY + 1;
    public static final int GROWING_IMP_TAG_MARKED = GROWING_WEB_VIEW_URL + 1;
    public static final int GROWING_ALREADY_SET_INTERFACE = GROWING_IMP_TAG_MARKED + 1;
    public static final int GROWING_TRACK_WEB_VIEW = GROWING_ALREADY_SET_INTERFACE + 1;

    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    static CoreAppState getAPPState() {
        return CoreInitialize.coreAppState();
    }

    static GrowingIO customStart(Configuration configuration) {

        if (!CoreInitialize.checkInitializeSuccessfully()) {
            Log.e(TAG, "GIO 初始化失败");
            GConfig.sCanHook = false;
            return new GrowingIO.EmptyGrowingIO();
        }

        GConfig config = CoreInitialize.config();

        if (config.getSampling() > 0) {
            configuration.sampling = config.getSampling();
        }

        String deviceId = CoreInitialize.deviceUUIDFactory().getDeviceId();
        if (!Util.isInSampling(deviceId, configuration.sampling)) {
            // 抽样采集
            configuration.disableDataCollect();
            config.setGDPREnabled(false);
            Log.w(TAG, "改设备不在采集范围之内， 不在收集信息");
        }
        synchronized (sInstanceLock) {
            if (null == sInstance) {
                try {
                    sInstance = new GrowingIO(configuration);
                    if (CoreInitialize.growingIOIPC().isFirstInit()) {
                        CoreInitialize.growingIOIPC().setSessionId(UUID.randomUUID().toString());
                        CoreInitialize.messageProcessor().saveVisit(true);
                    }
                } catch (Throwable e) {
                    return new GrowingIO.EmptyGrowingIO();
                }
            }
        }
        return sInstance;
    }

    public static GrowingIO startWithConfiguration(Application application, final Configuration configuration) {
        if (sInstance != null) {
            Log.e(TAG, "GrowingIO 已经初始化");
            return sInstance;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Log.e(TAG, "GrowingIO 暂不支持Android 4.2以下版本");
            return new GrowingIO.EmptyGrowingIO();
        }

        if (!ThreadUtils.runningOnUiThread()) {
            throw new IllegalStateException("GrowingIO.startWithConfiguration必须在主线程中调用。");
        }

        if (!configuration.rnMode && BuildConfig.VERSION_NAME.contains("-track") || BuildConfig.VERSION_NAME.startsWith("track")) {
            Log.e(TAG, "使用的打点版本, RnMode true");
            configuration.setRnMode(true);
        }

        Resources resources = application.getResources();
        try {
            boolean gioenable = Boolean.valueOf(resources.getString(resources.getIdentifier("growingio_enable", "string", application.getPackageName())));
            if (!gioenable) {
                Log.e(TAG, "GrowingIO 提醒您: gradle.properties 中配置 gioenable为 false，GIO SDK 各项功能已被关闭，请在正式发版时打开！");
                return new GrowingIO.EmptyGrowingIO();
            }
        } catch (Exception ignore) {
            LogUtil.d(TAG, "GrowingIO SDK 开关 gioenable 未配置，使用默认值，即 SDK 各项功能开启");
        }

        configuration.context = application;

        if (TextUtils.isEmpty(configuration.projectId)) {
            configuration.projectId = GConfig.getProjectId();
            if (TextUtils.isEmpty(configuration.projectId)) {
                configuration.projectId = resources.getString(resources.getIdentifier("growingio_project_id", "string", application.getPackageName()));
                if (TextUtils.isEmpty(configuration.projectId)) {
                    throw new IllegalStateException("未检测到有效的项目ID, 请参考帮助文档 https://docs.growingio.com/v3/developer-manual/sdkintegrated/android-sdk/auto-android-sdk");
                }
            }
        }
        if (TextUtils.isEmpty(configuration.urlScheme)) {
            configuration.urlScheme = GConfig.getUrlScheme();
            if (TextUtils.isEmpty(configuration.urlScheme)) {
                configuration.urlScheme = resources.getString(resources.getIdentifier("growingio_url_scheme", "string", application.getPackageName()));
                if (TextUtils.isEmpty(configuration.urlScheme)) {
                    throw new IllegalStateException("未检测到有效的URL Scheme, 请参考帮助文档 https://docs.growingio.com/v3/developer-manual/sdkintegrated/android-sdk/auto-android-sdk");
                }
            }
        }
        if (TextUtils.isEmpty(configuration.channel)) {
            int channelId = resources.getIdentifier("growingio_channel", "string", application.getPackageName());
            if (channelId > 0) {
                try {
                    configuration.channel = resources.getString(channelId);
                } catch (Exception ignore) {
                }
            }
        }
        PermissionUtil.init(configuration.context);
        if (!PermissionUtil.hasInternetPermission() || !PermissionUtil.hasAccessNetworkStatePermission()) {
            if (configuration.debugMode) {
                throw new IllegalStateException("您的App没有网络权限, 请添加 INTERNET 和 ACCESS_NETWORK_STATE 权限");
            } else {
                Log.e(TAG, "您的App没有网络权限, 非Debug模式, 将会影响数据采集, 请获悉");
            }
        }
        GConfig.isRnMode = configuration.rnMode;
        if (!GConfig.isInstrumented()) {
            throw new IllegalStateException("GrowingIO无法正常启动, 请检查:\n" +
                    "1. 首次集成时请先Clean项目再重新编译.\n" +
                    "2. (Gradle环境) 确保已经启用了GrowingIO插件(在build.gradle > buildscript > dependencies 中添加 classpath: 'com.growingio.android:vds-gradle-plugin:" + BuildConfig.VERSION_NAME + "' 然后在app目录下的build.gradle中添加apply plugin: 'com.growingio.android'.\n" +
                    "3. (Ant环境) 将vds-class-rewriter.jar的路径添加到环境变量ANT_OPTS中.\n" +
                    "有疑问请参考帮助文档 https://docs.growingio.com/v3/developer-manual/sdkintegrated/android-sdk/auto-android-sdk , 或者联系在线客服 https://www.growingio.com/");
        }

        if (configuration.debugMode) {
            LogUtil.add(LogUtil.DebugUtil.getInstance());
        } else {
            LogUtil.add(LogUtil.ReleaseUitl.getInstance());
        }

        // 根据配置加载custom host
        setTrackerHost(configuration.trackerHost);

        setReportHost(configuration.reportHost);
        setDataHost(configuration.dataHost);
        setTagsHost(configuration.tagsHost);
        setGtaHost(configuration.gtaHost);
        setWsHost(configuration.wsHost);
        setAssetsHost(configuration.assetsHost);
        setAdHost(configuration.adHost);

        // 根据配置项加载zone信息
        setZone(configuration.zone);

        GrowingIO.sProjectId = configuration.projectId;
        try {
            EventCenter.getInstance().init(configuration.context);
        } catch (EventCenterException ignore) {
        }
        try {
            EventCenter.getInstance().post(new InitializeSDKEvent(configuration.context, configuration));
        } catch (Throwable throwable) {
            Log.e(TAG, "GIO 初始化失败");
            LogUtil.e(TAG, throwable.getMessage(), throwable);
            GConfig.sCanHook = false;
            return new GrowingIO.EmptyGrowingIO();
        }
        /**
         * 将子线程初始化任务BgInitializeSDKEvent转到主线程
         * 1. 子线程初始化任务要求在界面显示及发送事件前完成，初始化时需要发送vst事件
         * 2. 线程锁概率性引起ANR，参见HZPI-4790
         */
        EventCenter.getInstance().post(new BgInitializeSDKEvent(configuration.context, configuration));
        return GrowingIO.customStart(configuration);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    AbstractGrowingIO(final Configuration configuration) {
        mGConfig = CoreInitialize.config();
        mArgumentChecker = new ArgumentChecker(mGConfig);
        GConfig.sCanHook = true;
        GInternal.getInstance().addFeaturesVersion("av", GConfig.GROWING_VERSION);
        Log.i(TAG, "!!! Thank you very much for using GrowingIO. We will do our best to provide you with the best service. !!!");
        Log.i(TAG, "!!! GrowingIO version: " + GConfig.GROWING_VERSION + " !!!");
    }

    // just for EmptyGrowingIO
    AbstractGrowingIO() {
    }


    @Deprecated
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private AbstractGrowingIO(final Application application, String token, double sampling) {
        this(new AbstractConfiguration(token).setProjectId(token).setSampling(sampling).setContext(application));
    }

    public static GrowingIO getInstance() {
        synchronized (sInstanceLock) {
            if (null == sInstance) {
                Log.i(TAG, "GrowingIO 还未初始化");
                return new GrowingIO.EmptyGrowingIO();
            } else {
                return sInstance;
            }
        }
    }

    public void disableDataCollect() {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                mGConfig.setGDPREnabled(false);
            }
        });
    }

    public void enableDataCollect() {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                mGConfig.setGDPREnabled(true);
                CoreInitialize.messageProcessor().saveVisit(true);
                EventCenter.getInstance().post(new DeepLinkEvent(DeepLinkEvent.DEEPLINK_ACTIVATE));
            }
        });
    }


    public static void setViewID(final View view, final String id) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                view.setTag(GROWING_VIEW_ID_KEY, id);
            }
        });
    }

    /* Deprecated methods for old version */

    @Deprecated
    public static void setScheme(String scheme) {
        GConfig.sGrowingScheme = scheme;
    }

    @Deprecated
    public GrowingIO setThrottle(final boolean throttle) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                mGConfig.setThrottle(throttle);
            }
        });
        return (GrowingIO) this;
    }

    @Deprecated
    public GrowingIO disable() {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                mGConfig.disableAll();
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO resume() {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (mGConfig.isEnabled()) {
                    return;
                }
                LogUtil.d(TAG, "resume: GrowingIO 恢复采集");
                mGConfig.enableAll();
                EventCenter.getInstance().post(new RefreshPageEvent(true, true));
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO stop() {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (!mGConfig.isEnabled()) {
                    return;
                }
                LogUtil.d(TAG, "stop: GrowingIO 停止采集");
                mGConfig.disableAll();
            }
        });
        return (GrowingIO) this;
    }

    public String getDeviceId() {
        return CoreInitialize.deviceUUIDFactory().getDeviceId();
    }

    public String getVisitUserId() {
        return getDeviceId();
    }

    public String getSessionId() {
        return CoreInitialize.sessionManager().getSessionIdInner();
    }

    public GrowingIO setTestHandler(final Handler handler) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                MessageHandler.addCallBack(new MessageHandler.TestMessageCallBack(handler));
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO setGeoLocation(final double latitude, final double longitude) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                getAPPState().setLocation(latitude, longitude);
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO clearGeoLocation() {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                getAPPState().clearLocation();
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO setUserId(final String userId) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                getAPPState().setUserId(userId);
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO clearUserId() {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                getAPPState().clearUserId();
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO onNewIntent(final Activity activity, final Intent intent) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                CoreInitialize.deeplinkManager().handleIntent(intent, activity);
            }
        });
        return (GrowingIO) this;
    }

    @Deprecated
    public String getUserId() {
        return CoreInitialize.config().getAppUserId();
    }

    public GrowingIO setPeopleVariable(JSONObject variables) {
        if ((variables = mArgumentChecker.validJSONObject(variables)) != null) {
            final JSONObject finalVariables = variables;
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setPeopleVariable(finalVariables);
                }
            });
        }
        return (GrowingIO) this;
    }

    public GrowingIO setPeopleVariable(final String key, final Number value) {
        if (mArgumentChecker.isIllegalEventName(key) || mArgumentChecker.isIllegalValue(value)) {
            return (GrowingIO) this;
        }
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                // TODO: ugly, 这里会与设计目的有出入， 不过情况不会变的更坏....
                getAPPState().setPeopleVariable(key, value);
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO setPeopleVariable(final String key, final String value) {
        if (mArgumentChecker.isIllegalEventName(key) || mArgumentChecker.isIllegalValue(value)) {
            return (GrowingIO) this;
        }
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                getAPPState().setPeopleVariable(key, value);
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO setPeopleVariable(final String key, final boolean value) {
        if (mArgumentChecker.isIllegalEventName(key)) {
            return (GrowingIO) this;
        }
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                getAPPState().setPeopleVariable(key, value);
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO setEvar(JSONObject variable) {
        if ((variable = mArgumentChecker.validJSONObject(variable)) != null) {
            final JSONObject finalVariable = variable;
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setConversionVariable(finalVariable);
                }
            });
        }
        return (GrowingIO) this;
    }

    public GrowingIO setEvar(final String key, final Number value) {
        if (!mArgumentChecker.isIllegalEventName(key) && !mArgumentChecker.isIllegalValue(value)) {
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setConversionVariable(key, value);
                }
            });
        }
        return (GrowingIO) this;
    }

    public GrowingIO setEvar(final String key, final String value) {
        if (!mArgumentChecker.isIllegalEventName(key) && !mArgumentChecker.isIllegalValue(value))
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setConversionVariable(key, value);
                }
            });
        return (GrowingIO) this;
    }

    public GrowingIO setEvar(final String key, final boolean value) {
        if (!mArgumentChecker.isIllegalEventName(key))
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setConversionVariable(key, value);
                }
            });
        return (GrowingIO) this;
    }

    public GrowingIO setVisitor(JSONObject visitorVariable) {
        if (visitorVariable == null || visitorVariable.length() == 0) {
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setVisitorVariable(null);
                }
            });
        } else {
            visitorVariable = mArgumentChecker.validJSONObject(visitorVariable);
            if (visitorVariable != null) {
                final JSONObject finalVisitorVariable = visitorVariable;
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        getAPPState().setVisitorVariable(finalVisitorVariable);
                    }
                });
            }
        }
        return (GrowingIO) this;
    }

    public GrowingIO setAppVariable(JSONObject variable) {
        if ((variable = mArgumentChecker.validJSONObject(variable)) != null) {
            final JSONObject finalVariable = variable;
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setAppVariable(finalVariable);
                }
            });
        }
        return (GrowingIO) this;
    }

    public GrowingIO setAppVariable(final String key, final Number value) {
        if (!mArgumentChecker.isIllegalEventName(key) && !mArgumentChecker.isIllegalValue(value)) {
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setAppVariable(key, value);
                }
            });
        }
        return (GrowingIO) this;
    }

    public GrowingIO setAppVariable(final String key, final String value) {
        if (!mArgumentChecker.isIllegalEventName(key) && !mArgumentChecker.isIllegalValue(value))
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setAppVariable(key, value);
                }
            });
        return (GrowingIO) this;
    }

    public GrowingIO setAppVariable(final String key, final boolean value) {
        if (!mArgumentChecker.isIllegalEventName(key))
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setAppVariable(key, value);
                }
            });
        return (GrowingIO) this;
    }

    public GrowingIO setChannel(String channel) {
        if (channel.length() > 32) {
            channel = channel.substring(0, 32);
        }
        if (mGConfig == null) {
            Log.e(TAG, "Pls invoke GrowingIO.startTracking() first");
            return (GrowingIO) this;
        }
        final String finalChannel = channel;
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                mGConfig.setChannel(finalChannel);
                CoreInitialize.messageProcessor().saveVisit(false);
            }
        });
        return (GrowingIO) this;
    }

    @Deprecated
    public GrowingIO disableImpression() {
        if (mGConfig != null) {
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    mGConfig.disableImpression();
                }
            });
        }
        return (GrowingIO) this;
    }

    public GrowingIO setImp(final boolean enable) {
        if (mGConfig != null) {
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (!enable) {
                        mGConfig.disableImpression();
                    } else {
                        mGConfig.enableImpression();
                    }
                }
            });
        }
        return (GrowingIO) this;
    }

    private GrowingIO track(final CustomEvent event) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                CoreInitialize.messageProcessor().saveCustomEvent(event);
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO track(String eventName) {
        if (mArgumentChecker.isIllegalEventName(eventName)) {
            return (GrowingIO) this;
        }
        return track(new CustomEvent(eventName));
    }

    /**
     * @deprecated number字段废弃， 下个API变更版本删除此接口
     */
    @Deprecated
    public GrowingIO track(String eventName, Number number) {
        if (mArgumentChecker.isIllegalEventName(eventName) || mArgumentChecker.isIllegalValue(number)) {
            return (GrowingIO) this;
        }
        return track(new CustomEvent(eventName, number));
    }

    public GrowingIO track(String eventName, JSONObject variable) {
        if (mArgumentChecker.isIllegalEventName(eventName)
                || (variable = mArgumentChecker.validJSONObject(variable)) == null) {
            return (GrowingIO) this;
        }
        return track(new CustomEvent(eventName, variable));
    }

    /**
     * @deprecated number字段废弃， 下个API变更版本删除此接口
     */
    @Deprecated
    public GrowingIO track(String eventName, Number number, JSONObject variable) {
        if (mArgumentChecker.isIllegalEventName(eventName)
                || mArgumentChecker.isIllegalValue(number)
                || (variable = mArgumentChecker.validJSONObject(variable)) == null) {
            return (GrowingIO) this;
        }
        return track(new CustomEvent(eventName, number, variable));
    }

    /**
     * 该函数主要提供给cordova-plugin.
     * 用户解决cordova无pv事件问题, 需由用户主动调用。
     *
     * @param pagename
     * @param ptm
     * @deprecated 下个API变更版本删除此接口
     */
    @Deprecated
    public void trackPage(final String pagename, final String lastpage, final long ptm) {
        if (mArgumentChecker.isIllegalEventName(pagename))
            return;
        if (ptm < 0 || ptm > System.currentTimeMillis())
            return;
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                PageEvent pageEvent = new PageEvent(pagename, lastpage, ptm);
                CoreInitialize.messageProcessor().savePage(pageEvent, pagename);
            }
        });
    }

    /**
     * 该函数主要提供给cordova-plugin.
     * 用户解决cordova仅发visit事件的情况。
     *
     * @param pagename
     * @deprecated 下个API变更版本删除此API
     */
    @Deprecated
    public void saveVisit(String pagename) {
        if (mArgumentChecker.isIllegalEventName(pagename))
            return;
        // TODO: 2018/7/24 此接口在新版中应该删除
//        MessageProcessor.getInstance().saveVisit(pagename);
    }

    /**
     * Page事件，针对Rn和Cordova;
     *
     * @param pageName
     * @deprecated 下个API变更版本删除此API
     */
    @Deprecated
    public void trackPage(final String pageName) {
        if (mArgumentChecker.isIllegalEventName(pageName)) {
            return;
        }
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                CoreInitialize.messageProcessor().savePage(pageName);
            }
        });
    }

    /**
     * pageVariable，针对Rn和Cordova;
     *
     * @param pageName
     * @deprecated 下个API变更版本删除此API
     */
    @Deprecated
    public GrowingIO setPageVariable(final String pageName, JSONObject variable) {
        if (mArgumentChecker.isIllegalEventName(pageName)
                || (variable = mArgumentChecker.validJSONObject(variable)) == null) {
            return (GrowingIO) this;
        }
        final JSONObject finalVariable = variable;
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                getAPPState().setPageVariable(pageName, finalVariable);
            }
        });
        return (GrowingIO) this;
    }


    public static void setTrackerHost(String trackerHost) {
        NetworkConfig.getInstance().setApiHost(trackerHost);
    }

    public static void setAdHost(String adHost) {
        NetworkConfig.getInstance().setAdHost(adHost);
    }


    public static void setTagsHost(String tagsHost) {
        NetworkConfig.getInstance().setTagsHost(tagsHost);
    }

    public static void setGtaHost(String gtaHost) {
        NetworkConfig.getInstance().setGtaHost(gtaHost);
    }

    @Deprecated
    public static void setJavaCirclePluginHost(String javaCirclePluginHost) {
        NetworkConfig.getInstance().setJavaCirclePluginHost(javaCirclePluginHost);
    }

    public static void setWsHost(String wsHost) {
        NetworkConfig.getInstance().setWsHost(wsHost);
    }


    public static void setDataHost(String dataHost) {
        NetworkConfig.getInstance().setDataHost(dataHost);
    }

    public static void setAssetsHost(String assetsHost) {
        NetworkConfig.getInstance().setAssetsHost(assetsHost);
    }

    public static void setReportHost(String reportHost) {
        NetworkConfig.getInstance().setReportHost(reportHost);
    }


    public static void setHybridJSSDKUrlPrefix(String urlPrefix) {
        NetworkConfig.getInstance().setDEFAULT_HybridJSSDKUrlPrefix(urlPrefix);
    }

    /**
     * 设置数据接收的zone, 可能会包含UCloud，AWS等
     *
     * @param zone zone信息
     * @deprecated 很少有客户需要这个API， 请勿使用
     */
    @Deprecated
    public static void setZone(String zone) {
        if (!TextUtils.isEmpty(zone))
            NetworkConfig.getInstance().setZone(zone.trim());
    }

    public GrowingIO setImeiEnable(final boolean imeiEnable) {
        final DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
        final MessageProcessor messageProcessor = CoreInitialize.messageProcessor();
        if (deviceUUIDFactory.imeiEnable == imeiEnable
                || deviceUUIDFactory == null
                || messageProcessor == null) {
            return (GrowingIO) this;
        }
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                deviceUUIDFactory.setImeiEnable(imeiEnable);
                if (imeiEnable) {
                    messageProcessor.saveVisit(false);
                }
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO setGoogleAdIdEnable(final boolean googleAdIdEnable) {
        final DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
        final MessageProcessor messageProcessor = CoreInitialize.messageProcessor();
        if (deviceUUIDFactory.googleIdEnable == googleAdIdEnable
                || deviceUUIDFactory == null
                || messageProcessor == null) {
            return (GrowingIO) this;
        }
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                deviceUUIDFactory.setGoogleIdEnable(googleAdIdEnable);
                if (googleAdIdEnable) {
                    messageProcessor.saveVisit(false);
                }
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO setAndroidIdEnable(final boolean androidIdEnable) {
        final DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
        final MessageProcessor messageProcessor = CoreInitialize.messageProcessor();
        if (deviceUUIDFactory.androidIdEnable == androidIdEnable
                || deviceUUIDFactory == null
                || messageProcessor == null) {
            return (GrowingIO) this;
        }
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                deviceUUIDFactory.setAndroidIdEnable(androidIdEnable);
                if (androidIdEnable) {
                    messageProcessor.saveVisit(false);
                }
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO setOAIDEnable(final boolean oaidEnable) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
                if (deviceUUIDFactory != null) {
                    deviceUUIDFactory.oaidEnable = oaidEnable;
                }
            }
        });
        return (GrowingIO) this;
    }

    public GrowingIO setOAIDProvideConfig(final OaidProvideConfig config) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
                if (deviceUUIDFactory != null) {
                    deviceUUIDFactory.oaidProvideConfig = config;
                }
            }
        });
        return (GrowingIO) this;
    }


    /**
     * @param url 需要校验的https或http url
     * @return true: 参数url是GrowingIO的deeplink链接或GrowingIO的Applink链接
     */
    public boolean isDeepLinkUrl(@Nullable String url) {
        return DeeplinkManager.isDeepLinkUrl(url, null);
    }

    /**
     * 手动触发GrowingIO的deeplink处理逻辑， 根据传入的url
     * 处理GrowingIO的相应结果参数格式与错误信息见{@link DeeplinkCallback}
     *
     * @param url      对应需要处理的GrowingIO deeplink或applink url
     * @param callback 处理结果的回调, 如果callback为null, 回调会使用初始化时传入的默认deeplinkcallback
     * @return true: url是GrowingIO的deeplink链接格式 false: url不是GrowingIO的deeplink链接格式
     */
    public boolean doDeeplinkByUrl(@Nullable String url, @Nullable DeeplinkCallback callback) {
        return CoreInitialize.deeplinkManager().doDeeplinkByUrl(url, callback);
    }

    protected static void runOnUIThread(Runnable runnable) {
        if (ThreadUtils.runningOnUiThread()) {
            runnable.run();
        } else {
            Log.d(TAG, "[!提示!] GrowingIO API要求在主线程调用, 该方法将自动post到主线程执行");
            ThreadUtils.postOnUiThread(runnable);
        }
    }
}
