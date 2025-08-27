package com.growingio.android.sdk.collection;

import com.growingio.android.sdk.utils.CustomerInterface;

/**
 * Created by liangdengke on 2018/7/14.
 */
public class Configuration extends AbstractConfiguration {

    private boolean enableNotificationTrack;

    public Configuration(String projectId) {
        super(projectId);
    }

    public Configuration() {
    }


    public Configuration setEncryptEntity(CustomerInterface.Encryption encryptEntity) {
        this.encryptEntity = encryptEntity;
        return (Configuration) this;
    }

    public Configuration enablePushTrack() {
        enableNotificationTrack = true;
        return this;
    }

    public boolean isEnableNotificationTrack() {
        return enableNotificationTrack;
    }

    public Configuration setImageViewCollectionBitmapSize(int size) {

        this.imageViewCollectionBitmapSize = size;
        return this;
    }

    public Configuration disableImageViewCollection(boolean disable) {
        this.disableImageViewCollection = disable;
        return this;
    }

    public Configuration setDisableImpression(boolean disableImpression) {
        this.disableImpression = disableImpression;
        return this;
    }

    public Configuration setTrackWebView(boolean trackWebView) {
        this.trackWebView = trackWebView;
        return this;
    }

    public Configuration setHashTagEnable(boolean hashTagEnable) {
        this.isHashTagEnable = hashTagEnable;
        return this;
    }

    public Configuration trackAllFragments() {
        trackAllFragments = true;
        return this;
    }

    public Configuration setHybridJSSDKUrlPrefix(String urlPrefix) {
        this.hybridJSSDKUrlPrefix = urlPrefix;
        return this;
    }

    public Configuration setJavaCirclePluginHost(String javaCirclePluginHost) {
        this.javaCirclePluginHost = javaCirclePluginHost;
        return this;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "hybridJSSDKUrlPrefix='" + hybridJSSDKUrlPrefix + '\'' +
                ", javaCirclePluginHost='" + javaCirclePluginHost + '\'' +
                ", disableImpression=" + disableImpression +
                ", trackWebView=" + trackWebView +
                ", isHashTagEnable=" + isHashTagEnable +
                ", disableImageViewCollection=" + disableImageViewCollection +
                ", imageViewCollectionBitmapSize=" + imageViewCollectionBitmapSize +
                ", trackAllFragments=" + trackAllFragments +
                ", useID=" + useID +
                ", context=" + context +
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
                ", enablePushTrack='" + enableNotificationTrack + "'" +
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
                ", encryptEntity=" + encryptEntity +
                '}';
    }
}
