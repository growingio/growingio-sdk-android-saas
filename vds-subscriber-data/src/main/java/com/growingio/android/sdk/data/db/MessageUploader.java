package com.growingio.android.sdk.data.db;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.data.DiagnoseLog;
import com.growingio.android.sdk.data.net.HttpService;
import com.growingio.android.sdk.encrypt.XORUtils;
import com.growingio.android.sdk.message.HandleType;
import com.growingio.android.sdk.message.MessageHandler;
import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.VisitEvent;
import com.growingio.android.sdk.snappy.Snappy;
import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by xyz on 15/10/28.
 */
public class MessageUploader {
    static final String TAG = "GIO.MessageUploader";

    private static final int FLUSH_CELLULAR_EVENT = 1;
    private static final int CLEAN_STALE_DATA = 2;
    private static final int FLUSH_ALL_EVENT = 3;

    private static final int MAX_RETRY_TIMES = 2;
    private static final long MILLIS_OF_DAY = 86400000;

    public enum UPLOAD_TYPE {
        CUSTOM("cstm"),
        PV("pv"),
        IMP("imp"),
        INSTANT_IMP("imp"),
        NON_INSTANT_IMP("imp"),
        OTHER("other"),
        AD("ctvt");

        private final String apiType;

        @Override
        public String toString() {
            return apiType;
        }

        UPLOAD_TYPE(String apiType) {
            this.apiType = apiType;
        }
    }

    private AtomicInteger mCellularDataCount = new AtomicInteger(0);
    private AtomicInteger mImpressDataCount = new AtomicInteger(0);

    private final MessageUploaderHandler mHandler;
    private GConfig mGConfig;
    private Map<String, String> fullEventTypeMap;
    private static boolean sIsDebug = false;

    public static void setDebug(boolean debug) {
        sIsDebug = debug;
    }

    private String getFullType(String type) {
        String full = fullEventTypeMap.get(type);
        if (full == null) {
            return type;
        } else {
            return full;
        }
    }

    private CoreAppState getAPPState() {
        return CoreInitialize.coreAppState();
    }

    DBAdapter getDBAdapter() {
        DBAdapter adapter = DBAdapter.getsInstance();
        if (adapter == null) {
            DBAdapter.initialize(getAPPState().getGlobalContext());
            adapter = DBAdapter.getsInstance();
        }
        return adapter;
    }

    public MessageUploader(Context context) {
        fullEventTypeMap = new HashMap<String, String>();
        fullEventTypeMap.put(VisitEvent.TYPE_NAME, VisitEvent.FULL_TYPE_NAME);
        fullEventTypeMap.put(ActionEvent.CLICK_TYPE_NAME, ActionEvent.FULL_CLICK_TYPE_NAME);
        HandlerThread thread = new HandlerThread(TAG, Thread.MIN_PRIORITY);
        thread.start();
        mHandler = new MessageUploaderHandler(thread.getLooper());
        mGConfig = CoreInitialize.config();
    }

    // called from DataSubscriberInitialize
    public void afterInit() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                HttpService.sSystemDefaultFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
            }
        });
        // clean the stale data
        mHandler.obtainMessage(CLEAN_STALE_DATA).sendToTarget();
        if (mGConfig.isEnabled()) {
            // 首次启动flush时间为5s, 避免用户配置过长时间导致事件无法上报
            flushEvent(true, true, 0);
        }
    }

    // called from DBAdapter for delay close DB
    public Handler getHandler() {
        return mHandler;
    }

    public void newEventSaved(boolean instant, int size) {
        flushEvent(false, instant, size);
    }

    private void flushEvent(boolean firstFlush, boolean instant, int size) {
        if (getAPPState().networkState() == CoreAppState.NETWORK_WIFI_ONLINE) {
            if (isNeedFlushImmediately(mCellularDataCount.get() + mImpressDataCount.addAndGet(size))) {
                flushAllEvent();
                LogUtil.d(TAG, "non-instant event saved: ", size, "/", mCellularDataCount.get(), "  flush data now");
            } else {
                flushAllDelayed(firstFlush ? 5 * 1000L : mHandler.getUploadDelayedTime());
                LogUtil.d(TAG, "non-instant event saved: ", size, "/", mCellularDataCount.get(), "  flush data later");
            }
        } else if (getAPPState().networkState() == CoreAppState.NETWORK_CELLULAR_ONLINE && mGConfig.canSendByCellular() && instant) {
            if (isNeedFlushImmediately(mCellularDataCount.addAndGet(size))) {
                flushCellularData();
                LogUtil.d(TAG, "instant event saved: ", size, "/", mCellularDataCount.get(), "  flush data now");
            } else {
                flushCellularDataDelayed(firstFlush ? 5 * 1000L : mHandler.getUploadDelayedTime());
                LogUtil.d(TAG, "instant event saved: ", size, "/", mCellularDataCount.get(), "  flush data later");
            }
        }
        MessageHandler.handleMessage(HandleType.MU_NEW_EVENT_SAVED, instant, size, mCellularDataCount.get(), mImpressDataCount.get());
    }

    private boolean isNeedFlushImmediately(int count) {
        return count > mGConfig.getUploadBulkSize() || mGConfig.isTestMode();
    }

    private void flushAllEvent() {
        mHandler.removeMessages(FLUSH_ALL_EVENT);
        mHandler.sendEmptyMessage(FLUSH_ALL_EVENT);
    }

    public void flushCellularData() {
        mHandler.removeMessages(FLUSH_CELLULAR_EVENT);
        mHandler.sendEmptyMessage(FLUSH_CELLULAR_EVENT);

        DiagnoseLog.uploadImmediate();
    }

    private void flushAllDelayed(long flushInterval) {
        if (!mHandler.hasMessages(FLUSH_ALL_EVENT)) {
            mHandler.sendEmptyMessageDelayed(FLUSH_ALL_EVENT, flushInterval);
        }
    }

    private void flushCellularDataDelayed(long flushInterval) {
        if (!mHandler.hasMessages(FLUSH_CELLULAR_EVENT)) {
            mHandler.sendEmptyMessageDelayed(FLUSH_CELLULAR_EVENT, flushInterval);
        }
    }

    private class MessageUploaderHandler extends Handler {

        private long uploadDelayedTime = 15 * 1000;

        private final static long UPLOAD_MAX_DELAYED_TIME = 60 * 1000L;

        MessageUploaderHandler(Looper looper) {
            super(looper);
        }

        long getUploadDelayedTime() {
            long defaultUploadInterval = CoreInitialize.config().getFlushInterval();
            if (uploadDelayedTime >= defaultUploadInterval) {
                return uploadDelayedTime;
            }
            uploadDelayedTime = CoreInitialize.config().getFlushInterval();
            return uploadDelayedTime;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == FLUSH_CELLULAR_EVENT || msg.what == FLUSH_ALL_EVENT) {
                if (!requestPreflightChecked()) {
                    return;
                }
            }

            switch (msg.what) {
                case FLUSH_CELLULAR_EVENT:
                    mHandler.removeMessages(FLUSH_CELLULAR_EVENT);
                    if (getAPPState().networkState() >= CoreAppState.NETWORK_CELLULAR_ONLINE && mGConfig.canSendByCellular()) {
                        if (uploadInstantEvents()) {
                            mCellularDataCount.set(0);
                        }
                    }
                    break;
                case FLUSH_ALL_EVENT:
                    mHandler.removeMessages(FLUSH_ALL_EVENT);
                    if (getAPPState().networkState() == CoreAppState.NETWORK_WIFI_ONLINE) {
                        uploadAllEvents();
                    }
                    break;
                case CLEAN_STALE_DATA:
                    cleanStaleData();
                    mHandler.sendEmptyMessageDelayed(CLEAN_STALE_DATA, MILLIS_OF_DAY);
                    break;
            }
        }

        private void uploadAllEvents() {
            if (uploadEventsInTurn(new UPLOAD_TYPE[]{UPLOAD_TYPE.AD, UPLOAD_TYPE.PV, UPLOAD_TYPE.CUSTOM, UPLOAD_TYPE.IMP, UPLOAD_TYPE.OTHER})) {
                mImpressDataCount.set(0);
                mCellularDataCount.set(0);
            }
        }

        private boolean uploadInstantEvents() {
            return uploadEventsInTurn(new UPLOAD_TYPE[]{UPLOAD_TYPE.AD, UPLOAD_TYPE.PV, UPLOAD_TYPE.CUSTOM, UPLOAD_TYPE.INSTANT_IMP, UPLOAD_TYPE.OTHER});
        }

        private boolean requestPreflightChecked() {
            if (GConfig.isPreflightChecked || !NetworkConfig.getInstance().needPreflight()) {
                return true;
            }
            String ai = getAPPState().getProjectId();
            long currentTimeMillis = System.currentTimeMillis();
            String host = NetworkConfig.getInstance().apiEndPoint();
            String uri = String.format(Locale.US, "%s/%s/android/%s?stm=%d", host, ai, UPLOAD_TYPE.PV, currentTimeMillis);
            HttpService httpService = new HttpService.Builder()
                    .uri(uri)
                    .requestMethod("OPTIONS")
                    .build();
            if (GConfig.DEBUG) {
                Log.w(TAG, "preflight: " + uri);
            }

            Pair<Integer, byte[]> result = httpService.performRequest();
            if (result.first == HttpURLConnection.HTTP_OK) {
                DiagnoseLog.saveLogIfEnabled("preflight");
                GConfig.isPreflightChecked = true;
                GConfig.isEndPointLow = false;
                uploadDelayedTime = 0;
                LogUtil.d(TAG, "requestPreflightChecked success.");
                return true;
            } else if (result.first == HttpURLConnection.HTTP_FORBIDDEN) {
                GConfig.isEndPointLow = true;
                GConfig.isPreflightChecked = true;
                LogUtil.d(TAG, "requestPreflightChecked 403.");
                return true;
            } else {
                uploadDelayedTime = Math.min(UPLOAD_MAX_DELAYED_TIME, uploadDelayedTime * 2);
                LogUtil.d(TAG, "requestPreflightChecked failed.");
                return false;
            }
        }

        /**
         * 依次上传这些类型的事件 
         * 依次： 50条A + 50条B + 50条C + 50条A + 50条B
         * 中间判断移动网络流量
         *
         * @return true -- 不需要重试
         */
        private boolean uploadEventsInTurn(UPLOAD_TYPE[] uploadTypes) {
            boolean hasMore = true, nonRetry = true;
            while (hasMore) {
                hasMore = false;
                nonRetry = true;
                for (UPLOAD_TYPE type : uploadTypes) {
                    Pair<Boolean, Boolean> hasMoreAndNeedRetry = uploadEvents(type);
                    hasMore |= hasMoreAndNeedRetry.first;
                    nonRetry &= !hasMoreAndNeedRetry.second;
                    if (getAPPState().networkState() == CoreAppState.NETWORK_CELLULAR_ONLINE && !mGConfig.canSendByCellular())
                        break;
                }
            }
            return nonRetry;
        }


        // return: <hasMore, needRetry>
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private Pair<Boolean, Boolean> uploadEvents(UPLOAD_TYPE type) {
            Pair<String, List<String>> eventData = null;
            int retryCount = 0;
            int network = getAPPState().networkState();
            boolean hasMore = false;
            DBAdapter dbAdapter = getDBAdapter();
            while (retryCount < MAX_RETRY_TIMES) {
                try {
                    try {
                        eventData = dbAdapter.generateDataString(type);
                        if (eventData != null) {
                            int count = uploadData(type, eventData.second);
                            if (count > 0) {
                                hasMore = dbAdapter.cleanDataString(type, eventData.first) > 0;
                                if (network == CoreAppState.NETWORK_CELLULAR_ONLINE) {
                                    mGConfig.increaseCellularDataSize(count);
                                }
                                break;
                            } else if (++retryCount >= MAX_RETRY_TIMES) {
                                // Upload failed, retry this session later.
                                return Pair.create(false, true);
                            }
                        } else {
                            return Pair.create(false, false);
                        }
                        // TODO: 2018/11/20 JSONException异常在uploadData中被捕获, 需要修复(在protocol buffer中修复)
                    } catch (JSONException ignored) {
                        LogUtil.d(TAG, "generate data string error");
                        dbAdapter.cleanDataString(type, eventData.first);
                    }
                } catch (Throwable t) {
                    if (t instanceof OutOfMemoryError) {
                        DiagnoseLog.saveLogIfEnabled("oomr");
                    } else if (t instanceof SQLiteCantOpenDatabaseException) {
                        DiagnoseLog.saveLogIfEnabled("dbo");
                    } else {
                        DiagnoseLog.saveLogIfEnabled(t.getClass().getSimpleName());
                        if (GConfig.DEBUG) {
                            t.printStackTrace();
                        }
                    }
                    retryCount++;
                }
            }
            return Pair.create(hasMore, false);
        }

        /**
         * 数据需要压缩+加密才可以上传
         * https://codes.growingio.com/w/api_v3_interface/
         */
        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        private int uploadData(UPLOAD_TYPE type, List<String> dataStrs) throws JSONException {
            MessageHandler.handleMessage(HandleType.MU_UPLOAD_EVENT, type.name(), dataStrs);
            JSONArray jsonArray = new JSONArray();
            int eventSize = 0;
            Map<String, Integer> uploadEventTypeCount = new HashMap<String, Integer>();
            String ai = getAPPState().getProjectId();
            String deviceId = CoreInitialize.deviceUUIDFactory().getDeviceId();
            long currentTimeMillis = System.currentTimeMillis();
            String host = type == UPLOAD_TYPE.AD ? NetworkConfig.getInstance().adHost() + "/app" : NetworkConfig.getInstance().apiEndPoint();
            String uri = String.format(Locale.US, "%s/%s/android/%s?stm=%d", host, ai, type, currentTimeMillis);

            try {
                for (String d : dataStrs) {
                    JSONObject jsonObject = new JSONObject(d);
                    String t = jsonObject.optString("t");
                    // TODO patch U这一步为什么不放在入库前
                    jsonObject.put("u", deviceId);
                    int size = 1;
                    if (type == UPLOAD_TYPE.INSTANT_IMP || type == UPLOAD_TYPE.NON_INSTANT_IMP || type == UPLOAD_TYPE.OTHER) {
                        JSONArray elems = jsonObject.optJSONArray("e");
                        if (elems != null) {
                            size = elems.length();
                        }
                    }
                    eventSize += size;
                    String uploadType = getFullType(t) + "u";
                    Integer uploadSize = uploadEventTypeCount.get(uploadType);
                    if (uploadSize == null) {
                        uploadEventTypeCount.put(uploadType, size);
                    } else {
                        uploadEventTypeCount.put(uploadType, uploadSize + size);
                    }
                    jsonArray.put(jsonObject);
                }
            } catch (JSONException ignored) {
                DiagnoseLog.saveLogIfEnabled("jsonu");
            }
            if (jsonArray.length() == 0) {
                return 0;
            }
            String data = jsonArray.toString();
            byte[] compressData = null;
            Map<String, String> headers = new HashMap<String, String>();
            if (sIsDebug) {
                try {
                    compressData = data.getBytes("UTF-8");
                    headers.put("Content-Type", "application/json");
                } catch (UnsupportedEncodingException e) {
                    LogUtil.e(TAG, e.getMessage(), e);
                }
            } else {
                compressData = Snappy.compress(data.getBytes(Charset.forName("UTF-8")));
                compressData = XORUtils.encrypt(compressData, (int) (currentTimeMillis & 0xFF));
            }
            if (GConfig.DEBUG) {
                LogUtil.d(TAG, "uploading ", data);
            }
            DiagnoseLog.saveLogIfEnabled("request");

            if (mGConfig.isDiagnoseEnabled()) {
                headers.put("X-GrowingIO-UID", deviceId);
            }
            LogUtil.d(TAG, "uri: ", uri, "\n data: ", data);
            // 加快GC
            data = null;
            headers.put("X-Compress-Codec", "2");
            headers.put("X-Crypt-Codec", "1");
            HttpService httpService = new HttpService.Builder()
                    .uri(uri)
                    .requestMethod("POST")
                    .headers(headers)
                    .body(compressData)
                    .build();
            Pair<Integer, byte[]> result = httpService.performRequest();
            if (result.first == HttpURLConnection.HTTP_OK) {
                MessageHandler.handleMessage(HandleType.MU_UPLOAD_EVENT_SUCCESS, dataStrs);
                DiagnoseLog.saveLogIfEnabled("success");
                DiagnoseLog.saveLogIfEnabled("upload", eventSize);
                if (!uploadEventTypeCount.isEmpty()) {
                    for (Map.Entry<String, Integer> uploadSize : uploadEventTypeCount.entrySet()) {
                        DiagnoseLog.saveLogIfEnabled(uploadSize.getKey(), uploadSize.getValue());
                    }
                }
                return compressData == null ? 0 : compressData.length;
            } else if (result.first == HttpURLConnection.HTTP_ENTITY_TOO_LARGE) {
                // delete data when result code is 413
                return compressData == null ? 0 : compressData.length;
            } else if (result.first == HttpURLConnection.HTTP_FORBIDDEN) {
                // preflight check again
                GConfig.isPreflightChecked = false;
                GConfig.isEndPointLow = false;
                return 0;
            } else {
                return 0;
            }
        }

        private void cleanStaleData() {
            long now = System.currentTimeMillis();
            long sevenDayBefore = (now - MILLIS_OF_DAY * 7) / MILLIS_OF_DAY * MILLIS_OF_DAY;
            getDBAdapter().cleanupEvents(sevenDayBefore);
        }
    }

}
