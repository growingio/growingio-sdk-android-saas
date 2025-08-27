package com.growingio.android.sdk.ipc;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.SystemUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GrowingIO进程间共享的变量
 * Created by liangdengke on 2018/8/15.
 */
public class GrowingIOIPC {
    private static final String TAG = "GIO.IPC";

    @VisibleForTesting VariableSharer variableSharer;

    // 按照既定顺序， 如果需要更改字节码的排序， 需要将ipc文件升级版本， 并且需要编写迁移代码, 类似数据库
    @VisibleForTesting
    int sessionIndex = -1;       // 0     String(10)
    int userIdIndex = -1;        // 1     String(1000)
    int lastPauseTimeIndex = -1; // 2     long
    int lastResumeTimeIndex =-1; // 3     long
    int visitorVarIndex = -1;    // 4     String(1000)
    int appVarIndex = -1;        // 5     String(1000)
    int isFirstIpcIndex = -1;    // 6     int              表示是否是第一次使用此IPC机制

    int specialModelIndex = -1;  /* 7     int              表示特殊模式(0 - 正常, 1 - App圈选中,
    2 - 圈选显示热图, 3 - 圈选显示已圈选, 4 - 圈选显示已圈选， 显示热, 5 - MobileDebugger中) */
    int tokensIndex = -1;        // 8     String(20)
    int wsUrlIndex = -1;         // 9     String(20)
    int gioUserIdIndex = -1;     // 10     String(20)  GIO用户的UserId，用于圈选

    private static final int MODEL_NORMAL = 0;

    public void init(Context context, GConfig config){
        File dirFile = new File(context.getFilesDir(), ".gio.dir");
        if (!dirFile.exists())
            dirFile.mkdirs();
        File ipcFile = new File(dirFile, "gio.ipc.1");
        variableSharer = new VariableSharer(ipcFile, config.isRequireAppProcessesEnabled(), Process.myPid());
        long startTime = System.currentTimeMillis();
        initVariableVersion1(context);
        LogUtil.d(TAG, "variableSharer init time: " + (System.currentTimeMillis() - startTime));
    }

    private void initVariableVersion1(Context context) {
        /* 0 */sessionIndex = variableSharer.addVariableEntity(VariableEntity.createStringVariable("sessionId", 10));
        /* 1 */userIdIndex = variableSharer.addVariableEntity(VariableEntity.createStringVariable("userId", 1000));
        /* 2 */lastPauseTimeIndex = variableSharer.addVariableEntity(VariableEntity.createLongVariable("lastPauseTime"));
        /* 3 */lastResumeTimeIndex = variableSharer.addVariableEntity(VariableEntity.createLongVariable("lastResumeTime"));
        /* 4 */visitorVarIndex = variableSharer.addVariableEntity(VariableEntity.createStringVariable("visitorVar", 1000));
        /* 5 */appVarIndex = variableSharer.addVariableEntity(VariableEntity.createStringVariable("appVar", 1000));
        /* 6 */isFirstIpcIndex = variableSharer.addVariableEntity(VariableEntity.createIntVariable("firstIpc"));
        /* 7 */specialModelIndex = variableSharer.addVariableEntity(VariableEntity.createIntVariable("specialModel"));
        /* 8 */tokensIndex = variableSharer.addVariableEntity(VariableEntity.createStringVariable("tokens", 20));
        /* 9 */wsUrlIndex = variableSharer.addVariableEntity(VariableEntity.createStringVariable("wsUrl", 20));
        /* 10 */gioUserIdIndex = variableSharer.addVariableEntity(VariableEntity.createStringVariable("gioUserId", 20));


        variableSharer.completeMetaData(context);
        if (isFirstInit()){
            // 应用冷启动
            variableSharer.putIntByIndex(specialModelIndex, MODEL_NORMAL);
            variableSharer.putStringByIndex(tokensIndex, null);
            variableSharer.putStringByIndex(wsUrlIndex, null);
            variableSharer.putStringByIndex(gioUserIdIndex, null);
            setAppVar(null);
            setVisitorVar(null);
            setLastPauseTime(System.currentTimeMillis());
            if (variableSharer.getIntByIndex(isFirstIpcIndex) == 0){
                // 第一次使用ipc文件机制， 需要迁移数据
                migrateData(context);
            }
        }
    }

    private void migrateData(Context context){
        SharedPreferences growingProfile = context.getSharedPreferences(GConfig.PREF_FILE_NAME, Context.MODE_PRIVATE);
        String oldUserId = growingProfile.getString(GConfig.PREF_USER_ID_IN_APP, null);
        if (oldUserId != null){
            variableSharer.putStringByIndex(userIdIndex, oldUserId);
        }
    }

    public boolean isFirstInit() {
        return variableSharer.isFirstInit();
    }

    public List<Integer> getAlivePid(){
        Context context = CoreInitialize.coreAppState().getGlobalContext();
        return variableSharer.getAlivePid(SystemUtil.getRunningProcess(context));
    }

    // GIO用户登录后的UserId
    public void setGioUserId(String gioUserId){
        variableSharer.putStringByIndex(gioUserIdIndex, gioUserId);
    }

    public String getGioUserId(){
        return variableSharer.getStringByIndex(gioUserIdIndex);
    }

    /**
     * 保存登录后的token
     */
    public void setToken(String token){
        variableSharer.putStringByIndex(tokensIndex, token);
    }

    public String getToken(){
        return variableSharer.getStringByIndex(tokensIndex);
    }

    public void setWsServerUrl(String wsServerUrl){
        variableSharer.putStringByIndex(wsUrlIndex, wsServerUrl);
    }

    public String getWsServerUrl(){
        return variableSharer.getStringByIndex(wsUrlIndex);
    }

    public void setSpecialModel(int specialModel) {
        variableSharer.putIntByIndex(specialModelIndex, specialModel);
    }

    public int getSpecialModel() {
        return variableSharer.getIntByIndex(specialModelIndex);
    }

    public void setSessionId(String sessionId){
        GConfig.resetPreflightStatus();
        variableSharer.putStringByIndex(sessionIndex, sessionId);
    }

    public String getSessionId(){
        return variableSharer.getStringByIndex(sessionIndex);
    }

    public void setUserId(@Nullable String userId){
        variableSharer.putStringByIndex(userIdIndex, userId);
    }

    public String getUserId(){
        return variableSharer.getStringByIndex(userIdIndex);
    }

    public void setVisitorVar(@Nullable JSONObject jsonObject){
        setJsonObj(visitorVarIndex, jsonObject);
    }

    public JSONObject getVisitorVar(){
        return getJsonObj(visitorVarIndex);
    }

    public void setAppVar(@Nullable JSONObject jsonObject){
        setJsonObj(appVarIndex, jsonObject);
    }

    public JSONObject getAppVar(){
        return getJsonObj(appVarIndex);
    }

    private int addSelf(int index){
        while (true){
            int oldValue = variableSharer.getIntByIndex(index);
            if (variableSharer.compareAndSetIntByIndex(index, oldValue, ++oldValue)){
                return oldValue;
            }
        }
    }

    private JSONObject getJsonObj(int index){
        String json = variableSharer.getStringByIndex(index);
        if (json == null)
            return null;
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            LogUtil.d(TAG, "getJsonObj failed: " + index, e);
            return null;
        }
    }

    private void setJsonObj(int index, @Nullable JSONObject jsonObject){
        variableSharer.putStringByIndex(index, jsonObject == null ? null : jsonObject.toString());
    }

    public void setLastPauseTime(long lastPauseTime){
        variableSharer.putLongByIndex(lastPauseTimeIndex, lastPauseTime);
    }

    public void setLastResumeTime(long lastResumeTime){
        variableSharer.putLongByIndex(lastResumeTimeIndex, lastResumeTime);
    }

    public long getLastResumeTime(){
        return variableSharer.getLongByIndex(lastResumeTimeIndex);
    }

    public long getLastPauseTime(){
        return variableSharer.getLongByIndex(lastPauseTimeIndex);
    }

    public void dumpToLog(){
        String logStr = "GrowingIOIPC(" + "userId=" + getUserId() +
                ", specialModel=" + getSpecialModel() + ", sessionId" + getSessionId() + ")";
        LogUtil.e(TAG, logStr);
        variableSharer.dumpModCountInfo();
    }
}
