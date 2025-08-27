package com.growingio.android.sdk.collection;

import android.text.TextUtils;

import com.growingio.android.sdk.utils.Util;

/**
 * Created by 郑童宇 on 2016/09/29.
 */

public class NetworkConfig {

    private boolean isOP = GConfig.ISOP();
    /**
     * 默认服务地址Formatter和地址
     */
    private final String DEFAULT_END_POINT = isOP ? "" : "https://www.growingio.com";                           // 对应私部:customEndPoint
    private final String DEFAULT_FORMATTER_API_HOST = isOP ? "" : "https://api%s.growingio.com/v3";             // 对应私部:customApiHost
    private final String DEFAULT_FORMATTER_CRASH_REPORT_V2 = isOP ? "" : "https://crashapi%s.growingio.com/v2"; // 无对应私部地址
    private final String DEFAULT_FORMATTER_TAGS_HOST = isOP ? "" : "https://tags%s.growingio.com";              // 对应私部:customerTagsHost
    private final String DEFAULT_FORMATTER_WS_HOST = isOP ? "" : "wss://gta%s.growingio.com";                   // 对应私部:customerWsHost
    private final String DEFAULT_FORMATTER_WS_ENDPOINT = isOP ? "" : "/app/%s/circle/%s";                       // 对应私部:customerWsHost
    private final String DEFAULT_FORMATTER_WS_DATA_CHECK = "%s/feeds/apps/%s/exchanges/data-check/%s?clientType=sdk";
    private final String DEFAULT_FORMATTER_TRACK_HOST = isOP ? "" : "https://t%s.growingio.com/app";            // 对应私部:customReportHost
    private final String DEFAULT_ASSETS_HOST = isOP ? "" : "https://assets.giocdn.com";                         // 对应私部:customerAssetsHost
    private final String DEFAULT_HybridJSSDKUrlPrefix = isOP ? "" : "https://assets.giocdn.com/sdk/hybrid";     // 对应私部:customerHybridJSSDKUrlPrefix
    private final String DEFAULT_DEEPLINK = "https://t.growingio.com/app/%s/%s/devices";
    //    private final String DEFAULT_APPLINK = "https://testlink.growingio.com/app/at5/android/%s";               // 请求 APPLink 的自定义参数 (广告组测试环境，上线注释)
    private final String DEFAULT_AD_HOST = "https://t.growingio.com";

    /**
     * 地址通用部分
     */
    private final static String JAVA_CIRCLE_PLUGIN_URL = "%s/android/sdk/vds-plugin-v3.zip";
    private final String FORMATTER_CIRCLE_PAGE = "%s/apps/circle/embedded.html";
    private final String JS_HYBRID_URL = "%s/2.0/gio_hybrid.min.js?sdkVer=%s&platform=Android";
    private final String JS_CIRCLE_URL = "%s/1.1/vds_hybrid_circle_plugin.min.js?sdkVer=%s&platform=Android";
    private final String JS_WEB_CIRCLE_URL = "%s/2.0/vds_web_circle_plugin.min.js";
    private String zone = null;


    /**
     * 用户设置服务地址地址;
     */
    private String customApiHost;
    private String customReportHost = null;
    private String customEndPoint = null;
    private String customerGtaHost = null;
    private String customerTagsHost = null;
    private String customerWsHost = null;
    private String customerHybridJSSDKUrlPrefix = null;
    //--目前未使用，用的是用户的customEndPoint;
    private String customerJavaCirclePluginHost = null;
    // assets 一般放静态资源的服务（目前存放用户的 圈选结果页 和 圈选插件）
    private String customerAssetsHost = null;
    // 广告事件api，如activate和reengage事件
    private String customAdHost = null;


    private static NetworkConfig sInstance = new NetworkConfig();

    private NetworkConfig() {
    }

    public static NetworkConfig getInstance() {
        return sInstance;
    }

    @Deprecated
    public void setJavaCirclePluginHost(String javaCirclePluginHost) {
        if (!TextUtils.isEmpty(javaCirclePluginHost))
            customerJavaCirclePluginHost = formatHost(javaCirclePluginHost);
    }

    public void setDEFAULT_HybridJSSDKUrlPrefix(String urlPrefix) {
        if (!TextUtils.isEmpty(urlPrefix))
            customerHybridJSSDKUrlPrefix = formatHost(urlPrefix);
    }

    public void setGtaHost(String gtaHost) {
        if (!TextUtils.isEmpty(gtaHost))
            customerGtaHost = formatHost(gtaHost);
    }

    public void setWsHost(String wsHost) {
        if (!TextUtils.isEmpty(wsHost))
            customerWsHost = wsHost;
    }

    public void setTagsHost(String tagsHost) {
        if (!TextUtils.isEmpty(tagsHost))
            customerTagsHost = formatHost(tagsHost);
    }

    public void setApiHost(String trackerHost) {
        if (!TextUtils.isEmpty(trackerHost))
            customApiHost = formatHost(trackerHost);
    }

    public void setDataHost(String dataHost) {
        if (!TextUtils.isEmpty(dataHost))
            customEndPoint = formatHost(dataHost);
    }

    public void setAssetsHost(String assetsHost) {
        if (!TextUtils.isEmpty(assetsHost))
            customerAssetsHost = formatHost(assetsHost);
    }

    public void setReportHost(String reportHost) {
        if (!TextUtils.isEmpty(reportHost))
            customReportHost = formatHost(reportHost);
    }

    public void setAdHost(String adHost) {
        if (!TextUtils.isEmpty(adHost)) {
            this.customAdHost = adHost;
        }
    }

    public void setZone(String zone) {
        if (!TextUtils.isEmpty(zone))
            this.zone = zone;
    }

    public String zoneInfo() {
        return TextUtils.isEmpty(zone) ? "" : "-" + zone;
    }

    public String apiEndPoint() {
        if (!needPreflight()) {
            return customApiHost + "/v3";
        }

        return GConfig.isEndPointLow ? String.format(DEFAULT_FORMATTER_API_HOST, "-cn") : String.format(DEFAULT_FORMATTER_API_HOST, "-os");
    }

    public boolean needPreflight() {
        return customApiHost == null || TextUtils.isEmpty(customApiHost) || customApiHost.startsWith(String.format(DEFAULT_FORMATTER_API_HOST, "-os"));
    }


    public String adHost() {
        return TextUtils.isEmpty(customAdHost) ? DEFAULT_AD_HOST : customAdHost;
    }

    public String crashReportEndPoint() {
        return String.format(DEFAULT_FORMATTER_CRASH_REPORT_V2, zoneInfo());
    }

    public String tagsHost() {
        return TextUtils.isEmpty(customerTagsHost) ? String.format(DEFAULT_FORMATTER_TAGS_HOST, zoneInfo()) : customerTagsHost;
    }


    public String getWSEndPointFormatter() {
        return TextUtils.isEmpty(customerWsHost) ? String.format(DEFAULT_FORMATTER_WS_HOST, zoneInfo()) + DEFAULT_FORMATTER_WS_ENDPOINT : customerWsHost + DEFAULT_FORMATTER_WS_ENDPOINT;
    }

    public String getMobileLinkUrl() {
        return getEndPoint() + Constants.WEB_CIRCLE_TAIL;
    }

    public String trackHost() {
        return TextUtils.isEmpty(customReportHost) ? String.format(DEFAULT_FORMATTER_TRACK_HOST, zoneInfo()) : customReportHost;
    }

    public String getEndPoint() {
        return TextUtils.isEmpty(customEndPoint) ? DEFAULT_END_POINT : customEndPoint;
    }

    public String getAssetsHost() {
        return TextUtils.isEmpty(customerAssetsHost) ? DEFAULT_ASSETS_HOST : customerAssetsHost;
    }

    public String getTargetApiEventPoint() {
        return getEndPoint() + Constants.EVENT_TAIL;
    }

    public String getJS_CIRCLE_URL() {
        return String.format(JS_CIRCLE_URL, TextUtils.isEmpty(customerHybridJSSDKUrlPrefix) ? DEFAULT_HybridJSSDKUrlPrefix : customerHybridJSSDKUrlPrefix, GConfig.GROWING_VERSION);
    }

    public String getJS_HYBRID_URL() {
        return String.format(JS_HYBRID_URL, TextUtils.isEmpty(customerHybridJSSDKUrlPrefix) ? DEFAULT_HybridJSSDKUrlPrefix : customerHybridJSSDKUrlPrefix, GConfig.GROWING_VERSION);
    }

    public String getJSWebCircleUrl() {
        return String.format(JS_WEB_CIRCLE_URL, TextUtils.isEmpty(customerHybridJSSDKUrlPrefix) ? DEFAULT_HybridJSSDKUrlPrefix : customerHybridJSSDKUrlPrefix);
    }

    public String getGtaHost() {
        return customerGtaHost;
    }

    public String getTargetApiRealTimePoint() {
        return getEndPoint() + Constants.REALTIME_TAIL;
    }

    public String getWsDataCheckUrl(String hostWithSchema, String ai, String roomId) {
        return String.format(DEFAULT_FORMATTER_WS_DATA_CHECK, hostWithSchema, ai, roomId);
    }

    public String getCirclePageUrl() {
        return String.format(FORMATTER_CIRCLE_PAGE, getAssetsHost());
    }

    public String getJavaCirclePluginUrl() {
        return String.format(JAVA_CIRCLE_PLUGIN_URL, getAssetsHost());
    }

    public String getXPathRankAPI() {
        return getEndPoint() + Constants.XRANK_TAIL;
    }

    private String formatHost(String host) {
        host = host.trim();
        if (!Util.isHttpsUrl(host) && !Util.isHttpUrl(host)) {
            host = Constants.HTTPS_PROTOCOL_PREFIX + host;
        }
        if (host.endsWith("/"))
            host = host.substring(0, host.length() - 1);
        return host;
    }

    public String getDeeplinkHost() {
        return DEFAULT_DEEPLINK;
    }

    public String getAppLinkParamsUrl(String trackId, boolean isInApp) {
        CoreAppState state = CoreInitialize.coreAppState();
        String ai = state.getProjectId();
        String spn = state.getSPN();
        String cl = isInApp ? "inapp" : "defer";
        return "https://" +
                "t.growingio.com" +
//                "testlink.growingio.com" +
                "/app/at6/" + cl + "/android/" +
                ai + "/" + spn + "/" + trackId;
    }
}
