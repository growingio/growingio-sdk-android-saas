package com.growingio.android.sdk.utils;

import android.webkit.WebView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by liangdengke on 2018/7/14.
 */
public class WebViewUtil {
    private static final String TAG = "GIO.WebViewUtil";

    public static boolean isDestroyed(WebView webView){
        try{
            Field providerField = WebView.class.getDeclaredField("mProvider");
            providerField.setAccessible(true);
            Object provider = providerField.get(webView);
            if ("android.webkit.WebViewClassic".equals(provider)){
                return isDestroyedWebViewClassic(provider);
            }
            Field awContentField = provider.getClass().getDeclaredField("mAwContents");
            awContentField.setAccessible(true);
            Object awContent = awContentField.get(provider);

            Method isDestroyed = awContent.getClass().getDeclaredMethod("isDestroyed", int.class);
            isDestroyed.setAccessible(true);

            Object isDestroy = isDestroyed.invoke(awContent, 0);
            if (isDestroy instanceof Boolean){
                return (Boolean) isDestroy;
            }
        }catch (Exception e){
            // 有部分Chromium 代码被混淆, 反射必定报错, 另外在新版的WebView中, 内部有isDestroyed判断， 不会触发Bug， 可以安全忽略该异常
            LogUtil.d(TAG, "isDestroyed() should ignore: ", e.getMessage());
        }

        return false;
    }

    private static boolean isDestroyedWebViewClassic(Object webViewClassic) throws Exception{
        Field field = webViewClassic.getClass().getDeclaredField("mWebViewCore");
        field.setAccessible(true);
        return field.get(webViewClassic) == null;
    }
}
