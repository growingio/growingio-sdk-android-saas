package com.growingio.android.sdk.models;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.GInternal;
import com.growingio.android.sdk.utils.HurtLocker;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.NonUiContextUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Created by xyz on 15/12/26.
 */
public class VisitEvent extends VPAEvent {
    public static final String TYPE_NAME = "vst";
    public static final String FULL_TYPE_NAME = "visit";
    private static JSONObject visitObject;
    private boolean useCachedObject = false;

    private String googleId = null;

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    @Override
    public String getFullType() {
        return FULL_TYPE_NAME;
    }

    private VisitEvent(long time) {
        super(time);
    }

    public static VisitEvent makeVisitEvent() {
        return new VisitEvent(System.currentTimeMillis());
    }

    public static VisitEvent getCachedVisitEvent() {
        if (visitObject == null) {
            // 还没有生成过visit对象，不需要重发
            return null;
        }
        VisitEvent visitEvent = new VisitEvent(System.currentTimeMillis());
        visitEvent.useCachedObject = true;
        return visitEvent;
    }

    public JSONObject toJson() {
        if (useCachedObject && visitObject != null) {
            patchLocation(visitObject);
            patchChannel(visitObject);
            patchAndroidId(visitObject);
            patchIMEI(visitObject);
            patchGoogleId();
            return visitObject;
        }
        visitObject = getCommonProperty();
        try {
            patchLocation(visitObject);

            //--新增字段
            patchIMEI(visitObject);
            patchAndroidId(visitObject);
//            patchUUID(visitObject);
            //-- 新增字段结束

//            visitObject.put("b", "native");
            // @see http://stackoverflow.com/questions/4212320/get-the-current-language-in-device
            visitObject.put("l", Locale.getDefault().toString());

            visitObject.put("ch", getConfig().getChannel());
            Context context = getAPPState().getGlobalContext();

            DisplayMetrics metrics = NonUiContextUtil.getDisplayMetrics(context);
            visitObject.put("sh", metrics.heightPixels);
            visitObject.put("sw", metrics.widthPixels);

            visitObject.put("db", Build.BRAND == null ? "UNKNOWN" : Build.BRAND);
            visitObject.put("dm", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);
            visitObject.put("ph", isPhone(context) ? 1 : 0);

            visitObject.put("os", "Android");
            visitObject.put("osv", Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
            if (CoreInitialize.config().isHarmonyEnable()) {
                try {
                    Class<?> systemProperties = Class.forName("android.os.SystemProperties");
                    String harmonyVersion = HurtLocker.invokeStaticMethod(systemProperties, "get", new Class[]{String.class}, "ro.huawei.build.display.id");

                    if (isHarmonyOs() && !TextUtils.isEmpty(harmonyVersion)) {
                        visitObject.put("os", "Harmony");
                        visitObject.put("osv", harmonyVersion);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            PackageManager packageManager = context.getPackageManager();
            PackageInfo info = packageManager.getPackageInfo(context.getPackageName(),  0);
            // 根据新版测量协议删除
            visitObject.put("cv", info.versionName);
            visitObject.put("av", GConfig.GROWING_VERSION);
            visitObject.put("sn", packageManager.getApplicationLabel(context.getApplicationInfo()));
            visitObject.put("v", GConfig.sGrowingScheme);
            visitObject.put("fv", GInternal.getInstance().getFeaturesVersionJson());
            patchGoogleId();
        } catch (JSONException e) {
            LogUtil.d(TAG, "generation the Visit Event error", e);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.d(TAG, "get PackageInfo error", e);
        }
        return visitObject;
    }

    private boolean isHarmonyOs() {
        try {
            Class clz = Class.forName("com.huawei.system.BuildEx");
            Method method = clz.getMethod("getOsBrand");

            ClassLoader classLoader = clz.getClassLoader();
            if (classLoader != null && classLoader.getParent() == null) {
                return "harmony".equals(method.invoke(clz));
            }
        } catch (Throwable e) {
        }
        return false;
    }

    private void patchGoogleId() {
        if (googleId != null){
            try {
                visitObject.put("gaid", googleId);
            } catch (JSONException e) {
                LogUtil.e(TAG, e.getMessage(), e);
            }
        }
    }

    @Override
    public void backgroundWorker() {
        super.backgroundWorker();
        googleId = CoreInitialize.deviceUUIDFactory().getGoogleAdId();
    }

    private void patchChannel(JSONObject jsonObject) {
        try {
            jsonObject.put("ch", getConfig().getChannel());
        } catch (Exception e) {
            LogUtil.d(TAG, "patch Channel error ", e);
        }
    }


    private static boolean isPhone(Context context) {

        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int type = telephony.getPhoneType();

        return type != TelephonyManager.PHONE_TYPE_NONE;
    }

}
