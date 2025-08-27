package com.growingio.android.sdk.utils;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.growingio.android.sdk.collection.AbstractGrowingIO;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.utils.rom.RomChecker;

import java.util.List;
import java.util.Map;

/**
 * Context相关的工具类(非Activity)
 * Created by liangdengke on 2018/9/4.
 */
public final class ContextUtil {

    private static final String TAG = "GIO.ContextUtil";

    private ContextUtil(){}

    /**
     * 对应于context.registerReceiver(receiver, intentFilter);
     * 华为EMUI对BroadcastReceiver数量限制在500一下,  未防止用户泄露broadcast crash在SDK端，
     * 将自身放置在EMUI的白名单中
     */
    public static void registerReceiver(@NonNull Context context, BroadcastReceiver receiver, IntentFilter intentFilter){
        registerReceiver(context, receiver, intentFilter, true);
    }

    private static void registerReceiver(Context context, BroadcastReceiver receiver, IntentFilter filter, boolean needCheckEMUI){
        try {
            context.registerReceiver(receiver, filter);
        }catch (Throwable throwable){
            if (needCheckEMUI
                    && RomChecker.isHuaweiRom()){
                boolean addWhiteListOk = eMUIAddWhiteList(context);
                if (addWhiteListOk){
                    LogUtil.d(TAG, "华为: add to WhiteList Success");
                    registerReceiver(context, receiver, filter, false);
                }else{
                    LogUtil.e(TAG, "华为: add to WhiteList Failed");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean eMUIAddWhiteList(Context context){
        try{
            Application application = (Application) context.getApplicationContext();
            Object loadedApk = ReflectUtil.findField(Application.class, application, "mLoadedApk");
            if (loadedApk == null)
                return false;
            Object receiverResource = ReflectUtil.findFieldRecur(loadedApk, "mReceiverResource");
            if (receiverResource == null)
                return false;
            List mWhiteList = ReflectUtil.findFieldRecur(receiverResource, "mWhiteList");
            if (mWhiteList == null){
                // for Android 8.0以上
                Map whiteListMap = ReflectUtil.findFieldRecur(receiverResource, "mWhiteListMap");
                if (whiteListMap == null)
                    return false;
                Object firstItem = whiteListMap.get("0");
                if (firstItem instanceof List)
                    mWhiteList = (List) firstItem;
                else
                    return false;
            }
            String packageName = context.getPackageName();
            if (mWhiteList.contains(packageName)){
                return false;
            }
            mWhiteList.add(packageName);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
