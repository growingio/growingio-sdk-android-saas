package com.growingio.android.sdk.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

/**
 * Created by lishaojie on 2017/2/21.<br/>
 * ProcessLock提供了一种多进程同步锁的实现,因为Android平台没有提供主流
 * 的进程间锁,所以只能使用SQLiteDatabase中的Transaction来间接实现此功能。
 * 此实现只能支持同一个App内的多个进程,由于没有稳定的共享文件路径暂不能支持多个App间同步。
 */

public class ProcessLock extends SQLiteOpenHelper {
    /**
     * 用来间接实现进程同步的数据库对象
     */
    private SQLiteDatabase db;

    /**
     * ProcessLock实现了一个空的SQLiteOpenHelper来使用SQLiteDatabase的Transaction
     * @param context App的Context对象,用来获取数据库文件路径
     * @param name 进程同步锁的名称
     */
    public ProcessLock(Context context, String name) {
        super(context, name, null, 1);
    }

    /**
     * @param timeout 获取锁时为防止未知异常问题,可以设置超时时间来避免影响业务流程
     * @return 返回true说明成功获取到锁, false则说明获取超时
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean acquire(int timeout) {
        long begin = System.currentTimeMillis();
        while (true) {
            try {
                db = getWritableDatabase();
                db.beginTransaction();
                return true;
            } catch (SQLiteDatabaseLockedException e) {
                if (System.currentTimeMillis() - begin >= timeout) {
                    break;
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean isHold(){
        try {
            db = getWritableDatabase();
            db.beginTransaction();
            db.endTransaction();
            return false;
        }catch (SQLiteDatabaseLockedException e){
            return true;
        }
    }

    /**
     * 释放锁
     */
    public void release() {
        if (db != null) {
            try {
                db.endTransaction();
            } catch (Exception e) {
                e.printStackTrace();
            }
            db = null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
