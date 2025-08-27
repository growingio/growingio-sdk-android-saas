package com.growingio.android.sdk.collection;

/**
 * Created by xyz on 15/8/29.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.webkit.WebSettings;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.OaidHelper1010;
import com.growingio.android.sdk.utils.OaidHelper1013;
import com.growingio.android.sdk.utils.OaidHelper1025;
import com.growingio.android.sdk.utils.OaidHelper1100;
import com.growingio.android.sdk.utils.PermissionUtil;
import com.growingio.android.sdk.utils.PersistUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.UUID;

/**
 * 用于获取GIO SDK需要获取的一切硬件Id信息
 * <p>
 * - 所有init方法需线程安全, 多线程时， double check放在init方法中
 * - 可在get方法中进行懒加载
 */
public class DeviceUUIDFactory {
    private final String TAG = "Gio.DeviceUUIDFactory";

    private final String ORIGIN_IMEI = "";

    private String deviceId;
    private String imei = ORIGIN_IMEI;
    private String androidId = null;
    private String ip = null;
    private String userAgent = "";
    private String googleAdId;
    private String oaid;

    private static final String PREFS_FILE = "device_id.xml";
    private static final String PREFS_DEVICE_ID = "device_id";
    private static final String MAGIC_ANDROID_ID = "9774d56d682e549c"; // Error AndroidID
    private CoreAppState coreAppState;

    // 仅供AbstractGrowingIO此类使用
    boolean imeiEnable;
    boolean androidIdEnable;
    boolean googleIdEnable;
    boolean oaidEnable;
    OaidProvideConfig oaidProvideConfig;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAndroidId() {
        if (androidId == null) {
            initAndroidID(coreAppState.getGlobalContext());
        }
        return MAGIC_ANDROID_ID.equals(androidId) ? null : androidId;
    }


    public String getIMEI() {
        if (ORIGIN_IMEI.equals(imei)) {
            initIMEI();
        }
        return imei;
    }

    // only for preloadClass
    DeviceUUIDFactory() {
    }

    public DeviceUUIDFactory(Context context, Configuration configuration) {
        imeiEnable = configuration.imeiEnable;
        googleIdEnable = configuration.googleIdEnable;
        androidIdEnable = configuration.androidIdEnable;
        oaidEnable = configuration.oaidEnable;
        oaidProvideConfig = configuration.oaidProvideConfig;
        if (TextUtils.isEmpty(configuration.deviceId)) {
            PersistUtil.init(context);
            initDeviceId(context);
        } else {
            setDeviceId(configuration.deviceId);
        }
    }

    public void setCoreAppState(CoreAppState coreAppState) {
        this.coreAppState = coreAppState;
    }

    // Must called from UI Thread
    public void initUserAgent() {
        if (!TextUtils.isEmpty(this.userAgent)) return;
        this.userAgent = System.getProperty("http.agent");
        if (TextUtils.isEmpty(userAgent)
                && PermissionUtil.hasInternetPermission()
                && GConfig.collectWebViewUserAgent) {
            try {
                this.userAgent = WebSettings.getDefaultUserAgent(CoreInitialize.coreAppState().getGlobalContext());
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
            }
        }
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    private synchronized void initIp() {
        if (PermissionUtil.hasInternetPermission()) {
            try {
                for (Enumeration<NetworkInterface> enNetI = NetworkInterface
                        .getNetworkInterfaces(); enNetI.hasMoreElements(); ) {
                    NetworkInterface netI = enNetI.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = netI
                            .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                            ip = inetAddress.getHostAddress() != null ? inetAddress.getHostAddress() : "";
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public String getIp() {
        if (ip == null) {
            initIp();
        }
        return this.ip;
    }

    public DeviceUUIDFactory setImeiEnable(boolean imeiEnable) {
        this.imeiEnable = imeiEnable;
        return this;
    }

    public DeviceUUIDFactory setAndroidIdEnable(boolean androidIdEnable) {
        this.androidIdEnable = androidIdEnable;
        return this;
    }

    public DeviceUUIDFactory setGoogleIdEnable(boolean googleIdEnable) {
        this.googleIdEnable = googleIdEnable;
        return this;
    }

    /**
     * 获取Imei
     * <strong>更改此方法需谨慎， 此方法会被disableImei在编译期修改</strong>
     */
    @SuppressLint("HardwareIds")
    private synchronized void initIMEI() {
        if (!imeiEnable
                || (!ORIGIN_IMEI.equals(imei) && imei != null)) {
            return;
        }
        if (PermissionUtil.checkReadPhoneStatePermission()) {
            try {
                TelephonyManager tm = (TelephonyManager) coreAppState.getGlobalContext().getSystemService(Context.TELEPHONY_SERVICE);
                imei = tm.getDeviceId();
            } catch (Throwable e) {
                LogUtil.d(TAG, "don't have permission android.permission.READ_PHONE_STATE,initIMEI failed ");
            }
        }
    }

    /**
     * 它在Android <=2.1 or Android >=2.3的版本是可靠、稳定的，但在2.2的版本并不是100%可靠的
     * 厂商定制系统的Bug：不同的设备可能会产生相同的ANDROID_ID：9774d56d682e549c。(摩托罗拉好像出现过这个问题)
     * 厂商定制系统的Bug：有些设备返回的值为null。
     * 设备差异：对于CDMA设备，ANDROID_ID和TelephonyManager.getDeviceId() 返回相同的值。
     * Android 手机被Root过的话，这个ID也可以被改变。
     * <strong>NOTE: 更改需谨慎， 此方法会受编译期的disableAndroidId的影响</strong>
     */
    public synchronized void initAndroidID(Context context) {
        if (!androidIdEnable || androidId != null)
            return;
        androidId = Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);
        if (androidId == null) {
            androidId = MAGIC_ANDROID_ID;
        }
    }

    /**
     * 初始化获取Google的广告Id, non-ui
     * <strong>NOTE: 更改需谨慎， 此方法会受编译期的disableAndroidId的影响</strong>
     */
    private synchronized void initGoogleAdId(Context context) {
        if (!googleIdEnable || !ClassExistHelper.issHasAdvertisingIdClient()) {
            googleAdId = null;
            return;
        }
        try {
            AdvertisingIdClient.Info info = AdvertisingIdClient.getAdvertisingIdInfo(context);
            googleAdId = info.getId();
        } catch (Throwable e) {
            LogUtil.d(TAG, "get google ad id failed");
        }
    }

    /**
     * 初始化OAID, non-ui
     * <strong>NOTE: 更改需谨慎, 此方法受编译期disableOAID的影响</strong>
     */
    private synchronized void initOAID(Context context) {
        if (!oaidEnable) {
            return;
        }

        // 高版本可能包含低版本的类, 需要优先判断是否为高版本
        try {
            if (oaidProvideConfig != null && oaidProvideConfig.getProvideOaidCallback()!=null) {
                oaid = oaidProvideConfig.getProvideOaidCallback().provideOaidJob(context);
            } else if (ClassExistHelper.isHasMSA1100()) {
                oaid = new OaidHelper1100(getOaidCert(context)).getOaid(context);
            } else if (ClassExistHelper.isHasMSA1025()) {
                oaid = new OaidHelper1025().getOaid(context);
            } else if (ClassExistHelper.isHasMSA1013()) {
                oaid = new OaidHelper1013().getOaid(context);
            } else if (ClassExistHelper.isHasMSA1010()) {
                oaid = new OaidHelper1010().getOaid(context);
            }
        } catch (Throwable ignored) {
            LogUtil.e(TAG, "not compatible with the version of oaid sdk");
        }
    }

    private synchronized String getOaidCert(Context context) {
        if (oaidProvideConfig != null && oaidProvideConfig.getProvideCertCallback()!=null) {
            return oaidProvideConfig.getProvideCertCallback().provideCertJob(context);
        }
        return loadPemFromAssetFile(context, context.getPackageName() + ".cert.pem");
    }

    private String loadPemFromAssetFile(Context context, String path) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(context.getAssets().open(path)));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
            return builder.toString();
        } catch (IOException ignored) {
            return "";
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }

        }
    }

    /**
     * 获取Google的广告Id
     * 不允许在主线程调用此方法， 此方法将阻塞主线程
     */
    public String getGoogleAdId() {
        if (googleAdId == null) {
            initGoogleAdId(coreAppState.getGlobalContext());
        }
        return googleAdId;
    }

    public String getOaid() {
        if (oaid == null) {
            initOAID(coreAppState.getGlobalContext());
        }
        return oaid;
    }

    private synchronized void initDeviceId(Context context) {
        if (TextUtils.isEmpty(this.deviceId)) {
            this.deviceId = PersistUtil.fetchDeviceId();
            if (TextUtils.isEmpty(this.deviceId)) {
                getOldDeviceId(context);

                if (!TextUtils.isEmpty(deviceId)) {
                    return;
                }

                calculateDeviceId(context);
            }
        }
    }

    private void getOldDeviceId(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);
        final String id = prefs.getString(PREFS_DEVICE_ID, null);

        if (!TextUtils.isEmpty(id)) {
            this.deviceId = id;
            PersistUtil.saveDeviceId(this.deviceId);
        }
    }

    /**
     * Returns a unique UUID for the current android device.  As with all UUIDs, this unique ID is "very highly likely"
     * to be unique across all Android devices.  Much more so than ANDROID_ID is.
     * <p>
     * The UUID is generated by using ANDROID_ID as the base key if appropriate, falling back on
     * TelephonyManager.getDeviceID() if ANDROID_ID is known to be incorrect, and finally falling back
     * on a random UUID that's persisted to SharedPreferences if getDeviceID() does not return a
     * usable value.
     * <p>
     * In some rare circumstances, this ID may change.  In particular, if the device is factory reset a new device ID
     * may be generated.  In addition, if a user upgrades their phone from certain buggy implementations of Android 2.2
     * to a newer, non-buggy version of Android, the device ID may change.  Or, if a user uninstalls your app on
     * a device that has neither a proper Android ID nor a Device ID, this ID may change on reinstallation.
     * <p>
     * Note that if the code falls back on using TelephonyManager.getDeviceId(), the resulting ID will NOT
     * change after a factory reset.  Something to be aware of.
     * <p>
     * Works around a bug in Android 2.2 for many devices when using ANDROID_ID directly.
     *
     * @return a UUID that may be used to uniquely identify your device for most purposes.
     * @see <a href='http://code.google.com/p/android/issues/detail?id=10603'>http://code.google.com/p/android/issues/detail?id=10603</a>
     * @see ://code.google.com/p/android/issues/detail?id=10603
     */
    @SuppressLint("HardwareIds")
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void calculateDeviceId(Context context) {
        initAndroidID(context);
        // Use the Android ID unless it's broken, in which case fallback on deviceId,
        // unless it's not available, then fallback on a random number which we store
        // to a prefs file
        if (!TextUtils.isEmpty(androidId) && !MAGIC_ANDROID_ID.equals(androidId)) {
            deviceId = UUID.nameUUIDFromBytes(androidId.getBytes(Charset.forName("UTF-8"))).toString();
        } else {
            initIMEI();
            if (!TextUtils.isEmpty(imei)) {
                deviceId = UUID.nameUUIDFromBytes(imei.getBytes(Charset.forName("UTF-8"))).toString();
            }
        }

        if (TextUtils.isEmpty(deviceId)) {
            deviceId = UUID.randomUUID().toString();
        }

        // Write the value out to the prefs file
        PersistUtil.saveDeviceId(deviceId);
    }
}
