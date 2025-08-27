package com.growingio.android.sdk.data;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Looper;

import com.growingio.android.sdk.base.event.DiagnoseEvent;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.DeviceUUIDFactory;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.data.net.HttpService;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.cp_annotation.Subscribe;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by anjihang on 16/4/7.
 */
class Diagnose {
    private static final String DETAIL_DATE = "detail_date";

    String dayDate;
    String detailDate;
    HashMap<String, Integer> eventCount;

    Diagnose(String dayDate, String detailDate) {
        this.dayDate = dayDate;
        this.detailDate = detailDate;
        eventCount = new HashMap<String, Integer>();
    }

    Diagnose(String dayDate, JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }
        try {
            this.dayDate = dayDate;

            eventCount = new HashMap<String, Integer>();
            Iterator<String> a = jsonObject.keys();
            while (a.hasNext()) {
                String key = a.next();
                if (key.equals(DETAIL_DATE)) {
                    detailDate = jsonObject.getString(DETAIL_DATE);
                    continue;
                }
                eventCount.put(key, jsonObject.getInt(key));
            }

            if (detailDate == null) {
                detailDate = DiagnoseLog.DIAGNOSE_DETAIL_DATE_FORMAT.format(new Date());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (eventCount != null) {
                for (String type : eventCount.keySet()) {
                    jsonObject.put(type, eventCount.get(type));
                }
            }

            jsonObject.put(DETAIL_DATE, detailDate);
        } catch (JSONException ignored) {
            ignored.printStackTrace();
        }
        return jsonObject.toString();
    }
}

public class DiagnoseLog {
    private final static long UploadLogDelay = 5 * 60 * 1000;
    private final String TYPE_UPLOAD_ALL = "TYPE_UPLOAD_ALL";

    @SuppressLint("StaticFieldLeak")
    private static DiagnoseLog sInstance = null;
    private static final String SP_NAME = "growingio_diagnose";
    private static SimpleDateFormat DIAGNOSE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    static SimpleDateFormat DIAGNOSE_DETAIL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss", Locale.US);

    private Context mContext;
    private HashMap<String, Diagnose> mDiagnoseMap;

    private DiagnoseLog(Context context) {
        mContext = context.getApplicationContext();
        readLogFromSP();
    }

    static void initialize(Context appContext) {
        if (null == sInstance) {
            sInstance = new DiagnoseLog(appContext);
        }
    }


    @Subscribe
    public static void onDiagnoseEvent(DiagnoseEvent event) {
        if (event.count == -1)
            saveLogIfEnabled(event.type);
        else
            saveLogIfEnabled(event.type, event.count);
    }

    public static void saveLogIfEnabled(String type) {
        if (CoreInitialize.config().isDiagnoseEnabled() && sInstance != null) {
            sInstance.saveLog(type);
        }
    }

    public static void saveLogIfEnabled(String type, int count) {
        if (CoreInitialize.config().isDiagnoseEnabled() && sInstance != null) {
            sInstance.saveLog(type, count);
        }
    }

    public static void uploadImmediate() {
        if (sInstance != null) {
            sInstance.uploadDiagnoseLogRunnable.uploadImmediate();
        }
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    private void readLogFromSP() {
        mDiagnoseMap = new HashMap<String, Diagnose>();
        Map<String, ?> map = getSharedPreferences().getAll();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            try {
                String date = entry.getKey();
                mDiagnoseMap.put(date, new Diagnose(date, new JSONObject(entry.getValue().toString())));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void saveLogToSP(String date, Diagnose diagnose) {
        try {
            getSharedPreferences().edit().putString(date, diagnose.toJson()).commit();
        } catch (Throwable t) {
            LogUtil.d(t);
        }
    }

    private synchronized void saveLog(String type, int count) {
        if (count == 0) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new AssertionError();
        }

        if (type == TYPE_UPLOAD_ALL && !GConfig.ISOP()) {
            uploadAll();
            return;
        }

        Date date = new Date();
        String currentDate = DIAGNOSE_DATE_FORMAT.format(date);
        String detailDate = DIAGNOSE_DETAIL_DATE_FORMAT.format(date);
        Diagnose diagnose = mDiagnoseMap.get(currentDate);

        if (diagnose == null) {
            diagnose = new Diagnose(currentDate, detailDate);
            mDiagnoseMap.put(currentDate, diagnose);
        }
        Integer current = diagnose.eventCount.get(type);
        Integer newCount = current != null ? current + count : count;
        diagnose.eventCount.put(type, newCount);
        saveLogToSP(currentDate, diagnose);

        if (mDiagnoseMap.size() > 0) {
            uploadDiagnoseLogRunnable.beginWaitForUpload();
        }
    }

    private void saveLog(String type) {
        saveLog(type, 1);
    }

    private void uploadAll() {
        Set<Map.Entry<String, Diagnose>> mDiagnoseMapEntrySet = mDiagnoseMap.entrySet();

        try {
            Object[] diagnoseMapEntryArray = mDiagnoseMapEntrySet.toArray();

            int diagnoseMapEntryArrayLength = diagnoseMapEntryArray.length;

            Map.Entry<String, Diagnose> diagnoseMapEntry;

            for (int i = 0; i < diagnoseMapEntryArrayLength; i++) {
                diagnoseMapEntry = (Map.Entry<String, Diagnose>) diagnoseMapEntryArray[i];
                upload(diagnoseMapEntry.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void upload(Diagnose diagnose) {
        if (diagnose == null) {
            return;
        }

        CoreAppState state = CoreInitialize.coreAppState();
        DeviceUUIDFactory deviceUUIDFactory = CoreInitialize.deviceUUIDFactory();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        headers.put("X-GrowingIO-UID", deviceUUIDFactory.getDeviceId());

        String ai = state.getProjectId();

        StringBuilder urlAppend = new StringBuilder(390);
        try {
            urlAppend.append(NetworkConfig.getInstance().crashReportEndPoint()).append("/").append(ai).append("/android/faults?")
                    .append("stm=").append(System.currentTimeMillis()).append('&')
                    .append("av=").append(URLEncoder.encode(CoreInitialize.config().getAppVersion(), "UTF-8")).append('&')
                    .append("cv=").append(GConfig.GROWING_VERSION).append('&')
                    .append("uid=").append(deviceUUIDFactory.getDeviceId()).append('&')
                    .append("appid=").append(state.getSPN()).append('&')
                    .append("os=").append("Android").append('&')
                    .append("osv=").append(Build.VERSION.SDK_INT).append('&')
                    .append("db=").append(URLEncoder.encode(Build.BRAND, "UTF-8")).append('&')
                    .append("dm=").append(URLEncoder.encode(Build.MODEL, "UTF-8")).append('&')
                    .append("date=").append(diagnose.detailDate);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (diagnose.eventCount != null && !diagnose.eventCount.isEmpty()) {
            for (String type : diagnose.eventCount.keySet()) {
                urlAppend.append("&").append(type).append("=").append(diagnose.eventCount.get(type));
            }
        }

        try {
            if (new HttpService.Builder()
                    .uri(urlAppend.toString())
                    .headers(headers)
                    .build()
                    .performRequest()
                    .first == HttpURLConnection.HTTP_OK) {
                mDiagnoseMap.remove(diagnose.dayDate);
                getSharedPreferences().edit().remove(diagnose.dayDate).commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private UploadDiagnoseLogRunnable uploadDiagnoseLogRunnable = new UploadDiagnoseLogRunnable();

    private class UploadDiagnoseLogRunnable implements Runnable {
        private boolean waitingForUploading = false;

        private void beginWaitForUpload() {
            if (!waitingForUploading) {
                waitingForUploading = true;
                DataSubscriberInitialize.messageUploader().getHandler().postDelayed(this, UploadLogDelay);
//                EventBus.getDefault().post(new HandleEvent(this, HandleEvent.MessageHandleType.POSTDELAY, UploadLogDelay));
            }
        }

        private void uploadImmediate() {
            waitingForUploading = true;
            DataSubscriberInitialize.messageUploader().getHandler().removeCallbacks(this);
            DataSubscriberInitialize.messageUploader().getHandler().post(this);
//            EventBus.getDefault().post(new RemoveEvent(this));
//            EventBus.getDefault().post(new HandleEvent(this, HandleEvent.MessageHandleType.POST));

        }

        @Override
        public void run() {
            saveLog(TYPE_UPLOAD_ALL);

            waitingForUploading = false;
        }
    }
}
