package com.growingio.android.sdk.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.util.SparseArray;
import android.view.Surface;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.AbsSeekBar;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.growingio.android.sdk.collection.AbstractGrowingIO;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.models.ActionStruct;
import com.growingio.android.sdk.models.ViewAttrs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.growingio.android.sdk.models.ViewNode.ANONYMOUS_CLASS_NAME;

/**
 * Created by zyl on 15/5/10.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class Util {

    private static final int MAX_CONTENT_LENGTH = 100;
    private static SparseArray<String> mIdMap;
    private static Set<Integer> mBlackListId;
    public static final Matcher ID_PATTERN_MATCHER = Pattern.compile("#[\\+\\.a-zA-Z0-9_-]+").matcher("");
    private static LruCache<Class, String> sClassNameCache = new LruCache<Class, String>(100);


    public static String getSimpleClassName(Class clazz) {
        String name = sClassNameCache.get(clazz);
        if (TextUtils.isEmpty(name)) {
            name = clazz.getSimpleName();
            if (TextUtils.isEmpty(name)) {
                name = ANONYMOUS_CLASS_NAME;
            }
            synchronized (Util.class) {
                sClassNameCache.put(clazz, name);
            }
            ClassExistHelper.checkCustomRecyclerView(clazz, name);
        }
        return name;
    }

    public static String getViewContent(View view, String bannerText) {
        String value = "";
        Object contentTag = view.getTag(GrowingIO.GROWING_CONTENT_KEY);
        if (contentTag != null) {
            value = String.valueOf(contentTag);
        } else {
            if (view instanceof EditText) {
                if (view.getTag(GrowingIO.GROWING_TRACK_TEXT) != null) {
                    if (!Util.isPasswordInputType(((EditText) view).getInputType())) {
                        CharSequence sequence = Util.getEditTextText((EditText) view);
                        value = sequence == null ? "" : sequence.toString();
                    }
                }
            } else if (view instanceof RatingBar) {
                value = String.valueOf(((RatingBar) view).getRating());
            } else if (view instanceof Spinner) {
                Object item = ((Spinner) view).getSelectedItem();
                if (item instanceof String) {
                    value = (String) item;
                } else {
                    View selected = ((Spinner) view).getSelectedView();
                    if (selected instanceof TextView && ((TextView) selected).getText() != null) {
                        value = ((TextView) selected).getText().toString();
                    }
                }
            } else if (view instanceof SeekBar) {
                value = String.valueOf(((SeekBar) view).getProgress());
            } else if (view instanceof RadioGroup) {
                RadioGroup group = (RadioGroup) view;
                View selected = group.findViewById(group.getCheckedRadioButtonId());
                if (selected instanceof RadioButton && ((RadioButton) selected).getText() != null) {
                    value = ((RadioButton) selected).getText().toString();
                }
            } else if (view instanceof TextView) {
                if (((TextView) view).getText() != null) {
                    value = ((TextView) view).getText().toString();
                }
            } else if (view instanceof ImageView) {
                if (!TextUtils.isEmpty(bannerText)) {
                    value = bannerText;
                }
            } else if (view instanceof WebView && !WebViewUtil.isDestroyed((WebView) view) || ClassExistHelper.instanceOfX5WebView(view)) {
                // 后台获取imp时， getUrl必须在主线程
                Object url = view.getTag(AbstractGrowingIO.GROWING_WEB_VIEW_URL);
                if (url == null) {
                    if (ThreadUtils.runningOnUiThread()) {
                        if (view instanceof WebView) {
                            url = ((WebView) view).getUrl();
                        } else {
                            url = ((com.tencent.smtt.sdk.WebView) view).getUrl();
                        }
                    } else {
                        postCheckWebViewStatus(view);
                        throw new RuntimeException("WebView getUrl must called on UI Thread");
                    }
                }
                if (url instanceof String) {
                    value = (String) url;
                }
            }
            if (TextUtils.isEmpty(value)) {
                if (bannerText != null) {
                    value = bannerText;
                } else if (view.getContentDescription() != null) {
                    value = view.getContentDescription().toString();
                }
            }
        }
        return truncateViewContent(value);
    }

    private static void postCheckWebViewStatus(final View webView) {
        LogUtil.d("GIO.Util", "postCheckWebViewStatus: ", webView);
        ThreadUtils.postOnUiThread(new Runnable() {
            @Override
            public void run() {
                String url = null;
                if (webView instanceof WebView) {
                    url = ((WebView) webView).getUrl();
                } else if (ClassExistHelper.instanceOfX5WebView(webView)) {
                    url = ((com.tencent.smtt.sdk.WebView) webView).getUrl();
                }
                if (url != null) {
                    webView.setTag(AbstractGrowingIO.GROWING_WEB_VIEW_URL, url);
                }
            }
        });
    }

    public static String truncateViewContent(String value) {
        if (value == null) return "";
        if (!TextUtils.isEmpty(value)) {
            if (value.length() > MAX_CONTENT_LENGTH) {
                value = value.substring(0, MAX_CONTENT_LENGTH);
            }
        }
        return encryptContent(value);
    }

    public static boolean isListView(View view) {
        return (view instanceof AdapterView
                || (ClassExistHelper.instanceOfAndroidXRecyclerView(view))
                || (ClassExistHelper.instanceOfAndroidXViewPager(view))
                || (ClassExistHelper.instanceOfSupportRecyclerView(view))
                || (ClassExistHelper.instanceOfSupportViewPager(view)));
    }

    public static boolean isInstant(JSONObject elem, ArrayList<ViewAttrs> filters, String domain) throws JSONException {
        for (ViewAttrs filter : filters) {
            if (filter.webElem && filter.domain.equals(domain)
                    && (filter.xpath == null || Util.isIdentifyXPath(filter.xpath, elem.getString("x")))
                    && (filter.index == null || filter.index.equals(String.valueOf(elem.optInt("idx", -1))))
                    && (filter.content == null || filter.content.equals(elem.optString("v")))
                    && (filter.href == null || filter.href.equals(elem.optString("h"))
                    && (filter.query == null || filter.query.equals(elem.optString("q"))))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInstant(ActionStruct elem, ArrayList<ViewAttrs> filters) {
        for (ViewAttrs filter : filters) {
            if (!filter.webElem
                    && ((filter.xpath == null || Util.isIdentifyXPath(filter.xpath, elem.xpath == null ? null : elem.xpath.toStringValue()))
                    && (filter.index == null || filter.index.equals(String.valueOf(elem.index)))
                    && (filter.content == null || filter.content.equals(elem.content))))
                return true;
        }
        return false;
    }

    public static boolean isIdentifyXPath(String filterXPath, String elemXPath) {
        if (filterXPath.charAt(0) == '*') {
            if (!GConfig.USE_ID) return false;
            return elemXPath.endsWith(filterXPath.substring(1));
        } else if (filterXPath.charAt(0) == '/') {
            return isIdentifyPatternServerXPath(filterXPath, elemXPath)
                    || filterXPath.equals(Util.ID_PATTERN_MATCHER.reset(elemXPath).replaceAll(""));
        }
        return false;
    }

    /**
     * 判断此filterXPath是否能够代表elemXpath，包含PatternServer
     * PatternServer: /MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/RelativeLayout[0]/LinearLayout[1]#ps_layout_a/AppCompatButton[*]#ps_layout_a_b1
     * Xpath: /MainWindow/LinearLayout[0]/FrameLayout[1]/FitWindowsLinearLayout[0]#action_bar_root/ContentFrameLayout[1]/RelativeLayout[0]/LinearLayout[1]#ps_layout_a/AppCompatButton[1]#ps_layout_a_b1
     */
    public static boolean isIdentifyPatternServerXPath(String filterXpath, String elemXpath) {
        if (filterXpath == null || elemXpath == null)
            return filterXpath == elemXpath;
        int filterIndex = 0;
        for (int elemIndex = 0; elemIndex < elemXpath.length(); elemIndex++) {
            if (filterIndex == filterXpath.length())
                return false;
            char filterChar = filterXpath.charAt(filterIndex);
            char elemChar = elemXpath.charAt(elemIndex);
            if (filterChar == elemChar) {
                filterIndex++;
            } else if (filterChar != '*'
                    || ('0' > elemChar && elemChar != '-')
                    || elemChar > '9') {
                if (filterChar == '*' && elemChar == ']') {
                    filterIndex++;
                    elemIndex--;
                } else {
                    return false;
                }
            }
        }
        return filterIndex == filterXpath.length();
    }


    public static String getIdName(View view, boolean fromTagOnly) {
        Object idTag = view.getTag(GrowingIO.GROWING_VIEW_ID_KEY);
        if (idTag instanceof String)
            return (String) idTag;
        if (fromTagOnly)
            return null;
        if (mIdMap == null)
            mIdMap = new SparseArray<String>();
        if (mBlackListId == null)
            mBlackListId = new HashSet<Integer>();
        final int id = view.getId();
        if (id > 0x7f000000 && !mBlackListId.contains(id)) {
            String idName = mIdMap.get(id);
            if (idName != null)
                return idName;
            synchronized (Util.class) {
                try {
                    idName = view.getResources().getResourceEntryName(id);
                    mIdMap.put(id, idName);
                    return idName;
                } catch (Exception ignored) {
                    mBlackListId.add(id);
                }
            }
        }
        return null;
    }

    public static Bundle getMetaData(Context context) {
        final String packageName = context.getPackageName();
        try {
            final ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle configBundle = appInfo.metaData;
            if (null == configBundle) {
                configBundle = new Bundle();
            }
            return configBundle;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Can't configure GrowingIO with package name " + packageName, e);
        }
    }

    public static boolean isPackageManagerDiedException(Throwable e) {
        if (e instanceof RuntimeException
                && e.getMessage() != null
                && (e.getMessage().contains("Package manager has died") || e.getMessage().contains("DeadSystemException"))) {
            Throwable cause = getLastCause(e);
            if (cause == null) return false;
            if (cause instanceof DeadObjectException
                    || cause.getClass().getName().equals("android.os.TransactionTooLargeException")) {
                return true;
            }
        }
        return false;
    }

    //获取异常栈中最底层的 Throwable Cause
    public static Throwable getLastCause(Throwable cause) {
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    public static int calcBannerItemPosition(@NonNull List bannerContent, int position) {
        return position % bannerContent.size();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static String md5(String s) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes(Charset.forName("US-ASCII")), 0, s.length());
            byte[] magnitude = digest.digest();
            BigInteger bi = new BigInteger(1, magnitude);
            String hash = String.format("%0" + (magnitude.length << 1) + "x", bi);
            return hash;
        } catch (Throwable e) {
            LogUtil.d("util", e);
        }
        return "";
    }

    public static boolean isInSampling(String deviceId, double sampling) {
        if (sampling <= 0) {
            return false;
        }
        if (sampling >= 0.9999) {
            return true;
        }

        char[] uuid = md5(deviceId).toCharArray();

        long bar = 100000;
        long rightValue = (long) ((sampling + 1.0f / bar) * bar);
        long value = 1;
        for (int i = uuid.length - 1; i >= 0; i--) {
            char n = uuid[i];
            value = ((value * 256) + n) % bar;
        }
        return value < rightValue;
    }

    public static boolean isIgnoredView(View view) {
        return view.getTag(GrowingIO.GROWING_IGNORE_VIEW_KEY) != null;
    }

    public static boolean isTrackWebView(View webView) {
        return webView.getTag(GrowingIO.GROWING_TRACK_WEB_VIEW) != null;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static String getViewName(View view) {
        if (view instanceof Switch || view instanceof ToggleButton) {
            return "开关";
        } else if (view instanceof CheckBox) {
            return "复选框";
        } else if (view instanceof RadioGroup) {
            return "单选框";
        } else if (view instanceof Button) {
            return "按钮";
        } else if (view instanceof EditText) {
            return "输入框";
        } else if (view instanceof ImageView) {
            return "图片";
        } else if (view instanceof WebView || ClassExistHelper.instanceOfX5WebView(view)) {
            return "H5元素";
        } else if (view instanceof TextView) {
            return "文字";
        } else {
            return "其他元素";
        }
    }

    public static boolean isViewClickable(View view) {
        return view.isClickable() || view instanceof RadioGroup || view instanceof Spinner || view instanceof AbsSeekBar
                || (view.getParent() != null && view.getParent() instanceof AdapterView
                && ((AdapterView) view.getParent()).isClickable());
    }

    public static int dp2Px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static int sp2Px(Context context, float sp) {
        final float scale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (sp * scale + 0.5f);
    }

    public static String getProcessNameForDB(Context context) {
        String processName = null;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // currentProcessName只有在4.4及以上手机才有这个私有API
            try {
                @SuppressLint("PrivateApi")
                Class<?> activityThread = Class.forName("android.app.ActivityThread");
                Method currentProcessName = activityThread.getDeclaredMethod("currentProcessName");
                processName = (String) currentProcessName.invoke(null);
            } catch (Exception e) {
                LogUtil.d(e);
            }
        }

        if (TextUtils.isEmpty(processName)) {
            // For Android P
            final File cmdline = new File("/proc/" + android.os.Process.myPid() + "/cmdline");
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(cmdline));
                processName = reader.readLine().trim();
            } catch (Exception e) {
                LogUtil.d(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {/*ignore*/}
                }
            }
        }

        if (TextUtils.isEmpty(processName) || processName.equals(context.getPackageName()))
            return "";
        return processName + ".";
    }

    public static void getVisibleRectOnScreen(View view, Rect rect, boolean ignoreOffset, int[] screenLocation) {
        if (ignoreOffset) {
            view.getGlobalVisibleRect(rect);
        } else {
            if (screenLocation == null || screenLocation.length != 2) {
                screenLocation = new int[2];
            }
            view.getLocationOnScreen(screenLocation);
            rect.set(0, 0, view.getWidth(), view.getHeight());
            rect.offset(screenLocation[0], screenLocation[1]);
        }
    }

    public static void getVisibleRectOnScreen(View view, Rect rect, boolean ignoreOffset) {
        getVisibleRectOnScreen(view, rect, ignoreOffset, null);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static int getScreenOrientation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    public static void sendMessage(Handler handler, int what, Object... obj) {
        if (handler != null) {
            handler.obtainMessage(what, obj).sendToTarget();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void callJavaScript(View view, String methodName, Object... params) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("try{(function(){");
        stringBuilder.append(methodName);
        stringBuilder.append("(");
        String separator = "";
        for (Object param : params) {
            stringBuilder.append(separator);
            separator = ",";
            if (param instanceof String) {
                stringBuilder.append("'");
                param = ((String) param).replace("'", "\'");
                StringBuilder builder = new StringBuilder();
                GJSONStringer.stringWithoutQuotation(builder, (String) param);
                param = builder.toString();
            }
            stringBuilder.append(param);
            if (param instanceof String) {
                stringBuilder.append("'");
            }
        }
        stringBuilder.append(");})()}catch(ex){console.log(ex);}");
        try {
            String jsCode = stringBuilder.toString();
            if (view instanceof WebView) {
                WebView webView = (WebView) view;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(jsCode, null);
                } else {
                    webView.loadUrl("javascript:" + jsCode);
                }
            } else if (ClassExistHelper.instanceOfX5WebView(view)) {
                com.tencent.smtt.sdk.WebView webView = (com.tencent.smtt.sdk.WebView) view;
                webView.evaluateJavascript(jsCode, null);
            }
        } catch (Exception e) {
            LogUtil.d("WebView", "call javascript failed ", e);
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void saveToFile(byte[] data, String dest) throws IOException {
        File destFile = new File(dest);
        File parentFile = destFile.getParentFile();
        if (parentFile.isDirectory() || parentFile.mkdirs()) {
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(destFile);
                fileOutputStream.write(data);
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        LogUtil.i("Util", e.getMessage());
                    }
                    destFile.setReadable(true);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static void saveToFile(InputStream stream, String dest) throws IOException {
        File destFile = new File(dest);
        File parentFile = destFile.getParentFile();
        if (parentFile.isDirectory() || parentFile.mkdirs()) {
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(destFile);
                byte[] buffer = new byte[4096 * 2];
                int read = stream.read(buffer);
                while (read > 0) {
                    fileOutputStream.write(buffer, 0, read);
                    read = stream.read(buffer);
                }
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        LogUtil.d("Util", e);
                    }
                    destFile.setReadable(true);
                }
            }
        }
    }

    public static boolean isHttpUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        return url.startsWith(Constants.HTTP_PROTOCOL_PREFIX);
    }

    public static boolean isHttpsUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        return url.startsWith(Constants.HTTPS_PROTOCOL_PREFIX);
    }

    public static int getVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            LogUtil.d("Util", e);
        }
        return 0;
    }

    /**
     * 此方法理论上不应该放到Util，也不合适放在AppState中，我重构过程中抽出了LocationControl，如果重构可以合到Master我会会放过去
     *
     * @param currentLatitude
     * @param currentLongitude
     * @param lastLatitude
     * @param lastLongitude
     * @param currentTime
     * @param lastSetLocationTime
     * @return
     */
    public static boolean shouldSetLocation(double currentLatitude, double currentLongitude, double lastLatitude, double lastLongitude, long currentTime, long lastSetLocationTime) {
        double locationDiffAbsSum = Math.abs(currentLatitude - lastLatitude) + Math.abs(currentLongitude - lastLongitude);
        // 如果数据无变化，则不需要重发
        if (locationDiffAbsSum == 0) {
            return false;
        }
        // 如果经度差的绝对值和维度差的绝对值之和超过0.05或经纬度有变化且距离上次设置经纬度时间超过5分钟
        if (locationDiffAbsSum > 0.05 || currentTime - lastSetLocationTime > 300000) {
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static boolean isPasswordInputType(int inputType) {
        final int variation = inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION);
        return variation == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD)
                || variation == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                || variation == (EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD)
                || variation == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    }


    public static CharSequence getEditTextText(TextView textView) {
        try {
            Field mText = TextView.class.getDeclaredField("mText");
            mText.setAccessible(true);
            return (CharSequence) mText.get(textView);
        } catch (Throwable e) {
            LogUtil.d("Util", e);
        }
        return null;
    }


    /**
     * 加密失败,或者数据为空返回原值
     *
     * @return
     */
    public static String encryptContent(String content) {
        final CustomerInterface.Encryption entity = CoreInitialize.config().getEncryptEntity();
        if (entity == null || TextUtils.isEmpty(content)) {
            return content;
        }
        try {
            return entity.encrypt(content);
        } catch (Exception ignore) {
            LogUtil.e("加密失败", "V字段加密算法崩溃，传回content");
            return content;
        }
    }
}
