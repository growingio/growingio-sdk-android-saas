package com.growingio.android.sdk.autoburry;

import android.content.Context;

import java.io.File;

/**
 * 用于将Push信息持久化到文件系统中
 * Created by liangdengke on 2018/11/14.
 */
public class PushFileManager {
    private static final String TAG = "GIO.PushFileManager";

    private Context context;
    private File pushFile;

    public PushFileManager(Context context){
        this.context = context;
    }

}
