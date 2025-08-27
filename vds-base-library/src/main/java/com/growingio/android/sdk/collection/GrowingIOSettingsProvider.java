package com.growingio.android.sdk.collection;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.growingio.android.sdk.deeplink.DeeplinkManager;
import com.growingio.android.sdk.ipc.GrowingIOIPC;
import com.growingio.android.sdk.utils.Util;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventCenterException;

/**
 * 用于子线程预加载一些类, 加快初始化时间
 */
public class GrowingIOSettingsProvider extends ContentProvider {
    private static final String TAG = "GIO.provider";


    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null)
            return false;
        preload(context);
        return true;
    }

    // 留一个preLoad的方法， 如有需要可以给子进程加速
    public static void preload(Context context){
        // SharedPreferences会开启一个后台子线程进行预加载
        try{
            context.getSharedPreferences("growing_profile", Context.MODE_PRIVATE);
            context.getSharedPreferences("growing_persist_data", Context.MODE_PRIVATE);
            context.getSharedPreferences("growing_server_pref", Context.MODE_PRIVATE);
        }catch (Throwable ignore){/* ignore UserStorage Service Died Exception */}
        new Thread(){
            @Override
            public void run() {
                preLoadClass();
            }
        }.start();
    }

    private static void preLoadClass(){
        new GrowingIO();
        new CoreAppState();
        try{
            EventCenter.getInstance().init(null);
        }catch (EventCenterException ignore){}
        try{
            Util.isInSampling("xxx", 1);
            new GConfig(null);
            new GrowingIOIPC();
        }catch (Throwable ignore){}
        new MessageProcessor(null, null);
        new DeviceUUIDFactory();
        new Configuration();
        new CoreInitialize();
        new DeeplinkManager();
        new SessionManager();
        loadClass("com.growingio.android.sdk.autoburry.AutoBuryAppState");
    }

    private static void loadClass(String className){
        try {
            Class clazz = GrowingIOSettingsProvider.class.getClassLoader().loadClass(className);
            clazz.newInstance();
        } catch (Throwable ignore) {
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
