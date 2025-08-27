package com.growingio.android.sdk.collection;

import android.app.Application;

import androidx.annotation.IntDef;

import com.growingio.android.sdk.deeplink.DeeplinkCallback;
import com.growingio.android.sdk.message.MessageHandler;
import com.growingio.android.sdk.message.RealTimeMessageCallBack;
import com.growingio.android.sdk.utils.CustomerInterface;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Created by lishaojie on 16/7/17.
 */
public class AbstractConfiguration {
    Application context;
    String projectId;
    String urlScheme;
    String deviceId;
    String channel;
    String trackerHost;
    String adHost;
    String packageName;

    String dataHost = null;
    String reportHost = null;
    String tagsHost = null;
    String gtaHost = null;
    String wsHost = null;
    String assetsHost = null;
    String zone;
    double sampling = 1;
    boolean disabled = false;
    boolean gdprEnabled = true;
    boolean throttle = false;
    boolean debugMode = false;
    boolean testMode = false;
    boolean spmc = false;
    boolean collectWebViewUserAgent = true;
    boolean diagnose = false;
    boolean disableCellularImp = false;
    int bulkSize = 300;
    long sessionInterval = 30 * 1000L;
    long flushInterval = 15 * 1000L;
    long cellularDataLimit = 10 * 1024 * 1024L;
    boolean mutiprocess = false;
    boolean requireAppProcessesEnabled = false;
    DeeplinkCallback callback = null;
    boolean rnMode;
    boolean imeiEnable = false;
    boolean androidIdEnable = false;
    boolean googleIdEnable = false;
    boolean oaidEnable = false;
    OaidProvideConfig oaidProvideConfig = null;
    boolean uploadExceptionEnable = false;
    boolean harmonyEnable = false;
    boolean readClipBoardEnable = false;

    /* FOR auto bury */
    String hybridJSSDKUrlPrefix = null;
    String javaCirclePluginHost = null;
    boolean disableImpression = true;
    boolean trackWebView = true;

    boolean taobaoWebViewSupport = false;
    boolean isHashTagEnable = false;
    boolean disableImageViewCollection = true;
    int imageViewCollectionBitmapSize = 2048;
    boolean trackAllFragments = false;
    boolean useID = true;
    CustomerInterface.Encryption encryptEntity;

    /**
     * 设置两种运行模式
     * 性能优先：放弃遍历视图树来查找imp和change事件
     * 准确度优先：遍历视图树
     */
    @RunMode
    int runMode = AccuracyPriorityMode;
    public static final int AccuracyPriorityMode = 1;
    public static final int PerformancePriorityMode = 2;

    @IntDef({AccuracyPriorityMode, PerformancePriorityMode})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RunMode {
    }

    public Configuration setRunMode(@RunMode int runMode) {
        this.runMode = runMode;
        return (Configuration) this;
    }

    public Configuration setDeeplinkCallback(DeeplinkCallback callback) {
        this.callback = callback;
        return (Configuration) this;
    }

    public Configuration disableDataCollect() {
        this.gdprEnabled = false;
        return (Configuration) this;
    }


    public Configuration setMutiprocess(boolean isMutiprocess) {
        this.mutiprocess = isMutiprocess;
        return (Configuration) this;
    }

    public Configuration setRequireAppProcessesEnabled(boolean enabled) {
        this.requireAppProcessesEnabled = enabled;
        return (Configuration) this;
    }

    public Configuration supportTaobaoWebView(boolean enabled){
        this.taobaoWebViewSupport = enabled;
        return (Configuration) this;
    }

    public Configuration setPackageName(String packageName) {
        this.packageName = packageName;
        return (Configuration) this;
    }


    public Configuration setSampling(double sampling) {
        this.sampling = sampling;
        return (Configuration) this;
    }

    public Configuration setDisabled(boolean disabled) {
        this.disabled = disabled;
        return (Configuration) this;
    }

    public Configuration setThrottle(boolean throttle) {
        this.throttle = throttle;
        return (Configuration) this;
    }

    public Configuration setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return (Configuration) this;
    }

    public Configuration setRnMode(boolean rnMode) {
        this.rnMode = rnMode;
        return (Configuration) this;
    }

    public Configuration setTestMode(boolean testMode) {
        this.testMode = testMode;
        return (Configuration) this;
    }

    public Configuration supportMultiProcessCircle(boolean spmc) {
        this.spmc = spmc;
        return (Configuration) this;
    }

    public Configuration collectWebViewUserAgent(boolean collectUserAgent) {
        this.collectWebViewUserAgent = collectUserAgent;
        return (Configuration) this;
    }

    public Configuration setDiagnose(boolean diagnose) {
        this.diagnose = diagnose;
        return (Configuration) this;
    }

    public Configuration setChannel(String channel) {
        this.channel = channel;
        return (Configuration) this;
    }

    public Configuration setFlushInterval(long flushInterval) {
        this.flushInterval = flushInterval;
        return (Configuration) this;
    }

    public Configuration setCellularDataLimit(long cellularDataLimit) {
        this.cellularDataLimit = cellularDataLimit;
        return (Configuration) this;
    }

    public Configuration setURLScheme(String urlScheme) {
        this.urlScheme = urlScheme;
        return (Configuration) this;
    }

    public Configuration setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return (Configuration) this;
    }

    public Configuration setProjectId(String projectId) {
        this.projectId = projectId;
        return (Configuration) this;
    }

    public Configuration setBulkSize(int bulkSize) {
        this.bulkSize = bulkSize;
        return (Configuration) this;
    }

    public Configuration setSessionInterval(long sessionInterval) {
        this.sessionInterval = sessionInterval;
        return (Configuration) this;
    }

    public Configuration setApp(Application app) {
        this.context = app;
        return (Configuration) this;
    }

    public Configuration disableCellularImp() {
        disableCellularImp = true;
        return (Configuration) this;
    }

    public Configuration setTrackerHost(String trackerHost) {
        this.trackerHost = trackerHost;
        return (Configuration) this;
    }

    public Configuration setAdHost(String adHost) {
        this.adHost = adHost;
        return (Configuration) this;
    }

    public Configuration setDataHost(String dataHost) {
        this.dataHost = dataHost;
        return (Configuration) this;
    }

    public Configuration setReportHost(String reportHost) {
        this.reportHost = reportHost;
        return (Configuration) this;
    }

    public Configuration setTagsHost(String tagsHost) {
        this.tagsHost = tagsHost;
        return (Configuration) this;
    }

    public Configuration setGtaHost(String gtaHost) {
        this.gtaHost = gtaHost;
        return (Configuration) this;
    }

    public Configuration setWsHost(String wsHost) {
        this.wsHost = wsHost;
        return (Configuration) this;
    }

    public Configuration setZone(String zone) {
        this.zone = zone;
        return (Configuration) this;
    }

    public Configuration setAssetsHost(String zone) {
        this.assetsHost = zone;
        return (Configuration) this;
    }

    public Configuration setAndroidIdEnable(boolean androidIdEnable) {
        this.androidIdEnable = androidIdEnable;
        return (Configuration) this;
    }

    public Configuration setImeiEnable(boolean imeiEnable) {
        this.imeiEnable = imeiEnable;
        return (Configuration) this;
    }

    public Configuration setGoogleAdIdEnable(boolean googleIdEnable) {
        this.googleIdEnable = googleIdEnable;
        return (Configuration) this;
    }

    public Configuration setOAIDProvideConfig(OaidProvideConfig config) {
        this.oaidProvideConfig = config;
        return (Configuration) this;
    }

    public Configuration setOAIDEnable(boolean oaidEnable) {
        this.oaidEnable = oaidEnable;
        return (Configuration) this;
    }

    public Configuration setUploadExceptionEnable(boolean uploadExceptionEnable) {
        this.uploadExceptionEnable = uploadExceptionEnable;
        return (Configuration) this;
    }

    public Configuration setHarmonyEnable(boolean harmonyEnable) {
        this.harmonyEnable = harmonyEnable;
        return (Configuration) this;
    }

    public Configuration setReadClipBoardEnable(boolean readClipBoardEnable) {
        this.readClipBoardEnable = readClipBoardEnable;
        return (Configuration) this;
    }

    AbstractConfiguration(String projectId) {
        this.projectId = projectId;
    }

    AbstractConfiguration() {

    }

    public Configuration setContext(Application context) {
        this.context = context;
        return (Configuration) this;
    }

    //--实时数据上报接口
    public Configuration addRealTimeMessageCallBack(final RealTimeMessageCallBack callBack) {
        MessageHandler.addCallBack(callBack);
        return (Configuration) this;
    }
}
