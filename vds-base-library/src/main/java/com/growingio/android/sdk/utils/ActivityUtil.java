package com.growingio.android.sdk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Activity相关的工具类
 * Created by liangdengke on 2018/4/18.
 */
public final class ActivityUtil {
    private ActivityUtil(){}

    /**
     * @return 返回context对应的Activity if exist
     */
    @Nullable
    public static Activity findActivity(@NonNull Context context){
        if (!(context instanceof ContextWrapper)){
            return null;
        }
        ContextWrapper current = (ContextWrapper) context;
        while (true){
            if (current instanceof Activity){
                return (Activity) current;
            }
            Context parent = current.getBaseContext();
            if (!(parent instanceof ContextWrapper)) break;
            current = (ContextWrapper) parent;
        }
        return null;
    }

    /**
     * @return context对应的activity是否被销毁
     */
    public static boolean isDestroy(Context context){
        Activity activity = findActivity(context);
        if (activity != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return activity.isDestroyed();
            }
        }
        return false;
    }
}
