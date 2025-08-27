package com.growingio.android.sdk.collection;

/**
 * Created by liangdengke on 2018/7/14.
 */
public class Configuration extends AbstractConfiguration{

    public Configuration(String projectId) {
        super(projectId);
    }

    public Configuration() {
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "context=" + context +
                ", projectId='" + projectId + '\'' +
                ", urlScheme='" + urlScheme + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", channel='" + channel + '\'' +
                ", trackerHost='" + trackerHost + '\'' +
                ", dataHost='" + dataHost + '\'' +
                ", reportHost='" + reportHost + '\'' +
                ", tagsHost='" + tagsHost + '\'' +
                ", gtaHost='" + gtaHost + '\'' +
                ", wsHost='" + wsHost + '\'' +
                ", zone='" + zone + '\'' +
                ", sampling=" + sampling +
                ", disabled=" + disabled +
                ", gdprEnabled=" + gdprEnabled +
                ", throttle=" + throttle +
                ", debugMode=" + debugMode +
                ", testMode=" + testMode +
                ", spmc=" + spmc +
                ", collectWebViewUserAgent=" + collectWebViewUserAgent +
                ", diagnose=" + diagnose +
                ", disableCellularImp=" + disableCellularImp +
                ", bulkSize=" + bulkSize +
                ", sessionInterval=" + sessionInterval +
                ", flushInterval=" + flushInterval +
                ", cellularDataLimit=" + cellularDataLimit +
                ", mutiprocess=" + mutiprocess +
                ", callback=" + callback +
                ", rnMode=" + rnMode +
                ", imeiEnable=" + imeiEnable +
                ", androidIdEnable=" + androidIdEnable +
                ", googleIdEnable=" + googleIdEnable +
                ", oaidEnable=" + oaidEnable +
                ", uploadExceptionEnable=" + uploadExceptionEnable +
                ", harmonyEnable=" + harmonyEnable +
                '}';
    }
}
