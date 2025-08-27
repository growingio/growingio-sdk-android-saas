package com.growingio.android.sdk.pending;

import android.os.Build;
import android.view.WindowManager;

import com.growingio.android.sdk.collection.CoreInitialize;

/**
 * author CliffLeopard
 * time   2018/7/2:下午8:50
 * email  gaoguanling@growingio.com
 * <p>
 * 存储运行时需要存储的临时状态，变量。  当这些变量变更的时候，要在各个进程之间进行同步；
 * 解决的问题： 圈选，mobileDebugger,SessionID LoginId 的同步
 */
public class PendingStatus {

    private static final String TAG = "GIO.PendingStatus";
    //-- 解放MessageProcessor

    /**
     * 圈选相关
     **/
    private static final String WEB_CIRCLE = "web";
    private static final String MOBILE_DEBUGGER = "debugger";
    public static final String DATA_CHECK = "data-check";
    public static final String APP_CIRCLE = "app";
    public static final String HEAT_MAP_CIRCLE = "heatmap";

    /**
     * 圈选相关临时状态
     **/
    public static int FLOAT_VIEW_TYPE = -1;

    private static boolean mIsEnable;

    public static String mLoginToken;           // 唤起圈选时带的token
    public static boolean mCanShowCircleTag = true;

    /**
     * 热图相关的
     */
    public static boolean mIsHeatMapOn = false;


    private static final int MODEL_NORMAL = 0;
    private static final int MODEL_APP_CIRCLE = 1;
    private static final int MODEL_APP_CIRCLE_SHOW_CIRCLED = 2;
    private static final int MODEL_APP_CIRCLE_SHOW_HEATMAP = 3;
    private static final int MODEL_DEBUGGER = 10;
    private static final int MODEL_DATA_CHECK = 11;
    private static final int MODEL_WEB_CIRCLE = 20;

    private static int mCacheSpecialModel = 0;

    public static boolean isValidMultiProcessState(){
        if (mCacheSpecialModel != MODEL_NORMAL)
            return false;
        int specialModel = CoreInitialize.growingIOIPC().getSpecialModel();
        if (specialModel != mCacheSpecialModel){
            mCacheSpecialModel = specialModel;
            return true;
        }
        return false;
    }

    private static void initFloatType() {
        FLOAT_VIEW_TYPE = WindowManager.LayoutParams.TYPE_TOAST;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= 26) {
                FLOAT_VIEW_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else if (Build.VERSION.SDK_INT > 24) {
                FLOAT_VIEW_TYPE = WindowManager.LayoutParams.TYPE_PHONE;
            } else {
                FLOAT_VIEW_TYPE = WindowManager.LayoutParams.TYPE_TOAST;
            }
        } else {
            FLOAT_VIEW_TYPE = WindowManager.LayoutParams.TYPE_PHONE;
        }
    }


    /**
     * Web Circle或者Debugger可用
     */
    public static boolean isProjection() {
        return isWebCircleEnabled() || isDebuggerEnabled() || isDataCheckEnable();
    }

    public static boolean isEnable() {
        return mIsEnable;
    }

    public static boolean isDataCheckEnable(){
        return mCacheSpecialModel == MODEL_DATA_CHECK;
    }

    public static boolean isAppCircleEnabled() {
        return mCacheSpecialModel > 0 && mCacheSpecialModel < 10;
    }

    public static boolean isWebCircleEnabled() {
        return mCacheSpecialModel == MODEL_WEB_CIRCLE;
    }

    public static boolean isDebuggerEnabled() {
        // TODO: 2019/1/7 MobileDebugger暂时与DataCheck不区分开来， 编码简单些
        return mCacheSpecialModel == MODEL_DEBUGGER || isDataCheckEnable();
    }

    public static void syncModelOnResume(){
        mCacheSpecialModel = CoreInitialize.growingIOIPC().getSpecialModel();
        mIsEnable = mCacheSpecialModel != MODEL_NORMAL;
    }

    public static void setSpecialModelFromType(String circleType){
        initFloatType();
        mIsEnable = true;
        if (APP_CIRCLE.equals(circleType)){
            mCacheSpecialModel = MODEL_APP_CIRCLE;
        }else if (MOBILE_DEBUGGER.equals(circleType)){
            mCacheSpecialModel = MODEL_DEBUGGER;
        }else if (WEB_CIRCLE.equals(circleType)){
            mCacheSpecialModel = MODEL_WEB_CIRCLE;
        }else if (DATA_CHECK.equals(circleType)){
            mCacheSpecialModel = MODEL_DATA_CHECK;
        }else{
            mIsEnable = false;
        }
        CoreInitialize.growingIOIPC().setSpecialModel(mCacheSpecialModel);
    }

    public static void disable(){
        mCacheSpecialModel = MODEL_NORMAL;
        CoreInitialize.growingIOIPC().setSpecialModel(MODEL_NORMAL);
    }
}
