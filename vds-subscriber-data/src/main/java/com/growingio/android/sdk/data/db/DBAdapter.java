package com.growingio.android.sdk.data.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import com.growingio.android.sdk.base.event.OnCloseBufferEvent;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.CustomEvent;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.data.DataSubscriberInitialize;
import com.growingio.android.sdk.data.DiagnoseLog;
import com.growingio.android.sdk.message.HandleType;
import com.growingio.android.sdk.message.MessageHandler;
import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.AppCloseEvent;
import com.growingio.android.sdk.models.ConversionEvent;
import com.growingio.android.sdk.models.PageEvent;
import com.growingio.android.sdk.models.PageVariableEvent;
import com.growingio.android.sdk.models.PeopleEvent;
import com.growingio.android.sdk.models.VisitEvent;
import com.growingio.android.sdk.models.VisitorVarEvent;
import com.growingio.android.sdk.models.ad.ActivateEvent;
import com.growingio.android.sdk.models.ad.ReengageEvent;
import com.growingio.android.sdk.utils.Util;
import com.growingio.eventcenter.EventCenter;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by xyz on 2015/3/30.
 */
public class DBAdapter {
    private static final String TAG = "GIO.DBAdapter";
    private static final int CLOSE_DATABASE_DELAY = 20 * 1000;
    private static final double EVENT_DATA_MAX_SIZE = 2 * 1000 * 1024; // 2M

    private final DBHelper mDbHelper;
    private static DBAdapter sInstance = null;
    private final static Object mDbLocker = new Object();
    private CoreAppState coreAppState;

    // this is not thread safe
    public static void initialize(Context context) {
        if (sInstance != null) return;
        synchronized (mDbLocker) {
            sInstance = new DBAdapter(context);
        }
    }


    // this is not thread safe
    public static DBAdapter getsInstance() {
        return sInstance;
    }

    DBAdapter(Context context) {
        mDbHelper = new DBHelper(context);
        coreAppState = CoreInitialize.coreAppState();
        // networkState在SDK中首次调用是在MessageUploader中的uploadEvents,所以第一次生成visit事件时无法获取网络状态
        // 又因为networkState方法耗时较高需要放在非UI线程,并且DBAdapter的初始化必然是在第一次visit之前,所以将networkState在此处调用,
        coreAppState.networkState();
    }

    private enum Table {
        EVENTS("events");

        private String mName;

        Table(String name) {
            mName = name;
        }

        public String getName() {
            return mName;
        }
    }

    private static final String KEY_DATA = "data";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_EVENT_TYPE = "eventType";
    private static final String KEY_INSTANT = "instant";

    private static final String SQL_CREATE_EVENTS = "CREATE TABLE " + Table.EVENTS.getName() + " (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            KEY_EVENT_TYPE + " STRING NOT NULL," +
            KEY_DATA + " STRING NOT NULL," +
            KEY_CREATED_AT + " INTEGER NOT NULL," +
            KEY_INSTANT + " INTEGER NOT NULL DEFAULT 0);" +
            "CREATE INDEX IF NOT EXISTS instant_idx ON " + Table.EVENTS.getName() + " (" + KEY_INSTANT + ");" +
            "CREATE INDEX IF NOT EXISTS time_idx ON " + Table.EVENTS.getName() + " (" + KEY_CREATED_AT + ");";

    private static final String SQL_UPDATE_FROM_V2 = "ALTER TABLE " + Table.EVENTS.getName() + " ADD COLUMN " + KEY_INSTANT + " BOOLEAN DEFAULT 0;" +
            "CREATE INDEX IF NOT EXISTS instant_idx ON " + Table.EVENTS + " (" + KEY_INSTANT + ");" +
            "DROP INDEX time_idx;";
    private static final String SQL_UPDATE_FROM_V2_INSTANT = "UPDATE " + Table.EVENTS + " SET " + KEY_INSTANT + " = 1 WHERE " + KEY_EVENT_TYPE + " != 'imp';";

    private static final String SQL_DELETE_EVENTS =
            "DROP TABLE IF EXISTS " + Table.EVENTS.getName();

    private class DBHelper extends SQLiteOpenHelper {

        static final int DATABASE_VERSION = 3;
        static final String DATABASE_NAME = "growing.db";

        DBHelper(Context context) {
            super(context, Util.getProcessNameForDB(context) + DATABASE_NAME, null, DATABASE_VERSION);
        }

        @SuppressLint("SQLiteString")
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_EVENTS);
            MessageHandler.handleMessage(HandleType.DB_CREATE_DB);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 2) {
                db.execSQL(SQL_UPDATE_FROM_V2);
                db.execSQL(SQL_UPDATE_FROM_V2_INSTANT);
                MessageHandler.handleMessage(HandleType.DB_UPGRADE_DB);
                return;
            }
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(SQL_DELETE_EVENTS);
            onCreate(db);
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    private Runnable mCloseDatabase = new Runnable() {
        @Override
        public void run() {
            try {
                synchronized (mDbLocker) {
                    SQLiteDatabase db = mDbHelper.getReadableDatabase();
                    db.close();
                }
                EventCenter.getInstance().post(new OnCloseBufferEvent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void closeDatabaseDelayed() {
        Handler handler = DataSubscriberInitialize.messageUploader().getHandler();
        handler.removeCallbacks(mCloseDatabase);
        handler.postDelayed(mCloseDatabase, CLOSE_DATABASE_DELAY);
    }

    public void saveEvent(String type, boolean instant, String json) {
        if (GConfig.DEBUG) {
            Log.w(TAG, "save " + (instant ? "instant" : "non-instant") + " Message: " + json);
        }
        long dataSize = json.getBytes().length;
        if (dataSize > EVENT_DATA_MAX_SIZE) {
            DiagnoseLog.saveLogIfEnabled("event data is too large, size: " + dataSize + " bytes");
            return;
        }
        MessageHandler.handleMessage(HandleType.DB_SAVE_EVENT, type, String.valueOf(instant), json);
        synchronized (mDbLocker) {
            SQLiteDatabase db = null;
            try {
                db = mDbHelper.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put(KEY_EVENT_TYPE, type);
                values.put(KEY_DATA, json);
                values.put(KEY_CREATED_AT, System.currentTimeMillis());
                values.put(KEY_INSTANT, instant);
                if (-1 == db.insert(Table.EVENTS.getName(), null, values)) {
                    DiagnoseLog.saveLogIfEnabled("dbw");
                }
            } catch (Exception e) {
                DiagnoseLog.saveLogIfEnabled("dbo");
            } finally {
                if (db != null) {
                    closeDatabaseDelayed();
                }
            }
        }
    }

    void cleanupEvents(long times) {
        MessageHandler.handleMessage(HandleType.DB_CLEAN_EVENT, times);
        synchronized (mDbLocker) {
            SQLiteDatabase db = null;
            try {
                db = mDbHelper.getWritableDatabase();
                int deleted = db.delete(Table.EVENTS.getName(), KEY_CREATED_AT + " <= ?", new String[]{String.valueOf(times)});
                DiagnoseLog.saveLogIfEnabled("delete", deleted);
            } catch (SQLiteDiskIOException e) {
                DiagnoseLog.saveLogIfEnabled("dbioc");
            } catch (SQLException e) {
                DiagnoseLog.saveLogIfEnabled("dbo");
            } catch (Throwable e) {
                DiagnoseLog.saveLogIfEnabled("throwable");
            } finally {
                if (db != null) {
                    closeDatabaseDelayed();
                }
            }
        }
    }

    @SuppressWarnings("NewApi")
    long cleanupEvents(String deleteStr, String... selectionArgs) {
        MessageHandler.handleMessage(HandleType.DB_CLEAN_EVENT, deleteStr, selectionArgs);
        synchronized (mDbLocker) {
            SQLiteDatabase db = null;
            int count = 0;
            try {
                db = mDbHelper.getWritableDatabase();
                count = db.delete(Table.EVENTS.getName(), deleteStr, selectionArgs);
            } catch (SQLiteDiskIOException ignored) {
                DiagnoseLog.saveLogIfEnabled("dbioc");
            } catch (SQLException e) {
                DiagnoseLog.saveLogIfEnabled("dboc");
            } catch (Exception e) {
                DiagnoseLog.saveLogIfEnabled(e.getClass().getSimpleName());
                if (GConfig.DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                if (db != null) {
                    closeDatabaseDelayed();
                }
            }
            return count;
        }
    }

    Pair<String, List<String>> generateDataString(MessageUploader.UPLOAD_TYPE type) {
        MessageHandler.handleMessage(HandleType.DB_READ_DB, type.name());
        switch (type) {
            case AD:
                return generateDataString(String.format("SELECT _id, %s FROM %s WHERE (%s = '%s' or %s = '%s') ORDER BY _id LIMIT 50",
                        KEY_DATA, Table.EVENTS, KEY_EVENT_TYPE, ActivateEvent.TYPE_NAME, KEY_EVENT_TYPE, ReengageEvent.TYPE_NAME), null);
            case CUSTOM:
                return generateDataString(String.format("SELECT _id, %s FROM %s WHERE (%s = '%s' or %s = '%s' or %s = '%s' or %s = '%s' or %s = '%s') ORDER BY _id LIMIT 50",
                        KEY_DATA, Table.EVENTS, KEY_EVENT_TYPE, CustomEvent.TYPE_NAME, KEY_EVENT_TYPE, PageVariableEvent.TYPE_NAME, KEY_EVENT_TYPE,
                        ConversionEvent.TYPE_NAME, KEY_EVENT_TYPE, PeopleEvent.TYPE_NAME, KEY_EVENT_TYPE, VisitorVarEvent.TYPE_NAME), null);
            case PV:
                return generateDataString(String.format("SELECT _id, %s FROM %s WHERE %s = '%s' OR %s = '%s'  OR %s = '%s' ORDER BY _id LIMIT 50",
                        KEY_DATA, Table.EVENTS, KEY_EVENT_TYPE, PageEvent.TYPE_NAME, KEY_EVENT_TYPE, VisitEvent.TYPE_NAME, KEY_EVENT_TYPE, AppCloseEvent.TYPE_NAME), null);
            case OTHER:
                return generateDataString(String.format("SELECT _id, %s FROM %s WHERE ( %s = '%s' OR %s = '%s') ORDER BY _id LIMIT 50",
                        KEY_DATA, Table.EVENTS, KEY_EVENT_TYPE, ActionEvent.CLICK_TYPE_NAME, KEY_EVENT_TYPE, ActionEvent.CHANGE_TYPE_NAME), null);
            case INSTANT_IMP:
                return generateDataString(String.format("SELECT _id, %s FROM %s WHERE %s = 1 AND %s = '%s' ORDER BY _id LIMIT 50",
                        KEY_DATA, Table.EVENTS, KEY_INSTANT, KEY_EVENT_TYPE, ActionEvent.IMP_TYPE_NAME), null);
            case NON_INSTANT_IMP:
                return generateDataString(String.format("SELECT _id, %s FROM %s WHERE %s = 0 AND %s = '%s' ORDER BY _id LIMIT 50",
                        KEY_DATA, Table.EVENTS, KEY_INSTANT, KEY_EVENT_TYPE, ActionEvent.IMP_TYPE_NAME), null);
            case IMP:
                return generateDataString(String.format("SELECT _id, %s FROM %s WHERE %s = '%s' ORDER BY _id LIMIT 50",
                        KEY_DATA, Table.EVENTS, KEY_EVENT_TYPE, ActionEvent.IMP_TYPE_NAME), null);
            default:
                return null;
        }
    }

    long cleanDataString(MessageUploader.UPLOAD_TYPE type, String lastId) {
        MessageHandler.handleMessage(HandleType.DB_CLEAN_EVENT, type.name(), lastId);
        switch (type) {
            case AD:
                return cleanupEvents(String.format(
                                "_id <= ? AND (%s = ? OR %s = ?)", KEY_EVENT_TYPE, KEY_EVENT_TYPE),
                        lastId, ActivateEvent.TYPE_NAME, ReengageEvent.TYPE_NAME);
            case CUSTOM:
                return cleanupEvents(String.format(
                                "_id <= ? AND (%s = ? OR %s = ? OR %s = ? OR %s = ? OR %s = ?)", KEY_EVENT_TYPE, KEY_EVENT_TYPE, KEY_EVENT_TYPE, KEY_EVENT_TYPE, KEY_EVENT_TYPE),
                        lastId, CustomEvent.TYPE_NAME, PeopleEvent.TYPE_NAME, PageVariableEvent.TYPE_NAME, ConversionEvent.TYPE_NAME, VisitorVarEvent.TYPE_NAME);
            case PV:
                return cleanupEvents(String.format(
                                "_id <= ? AND (%s = ? OR %s = ? OR %s = ?)", KEY_EVENT_TYPE, KEY_EVENT_TYPE, KEY_EVENT_TYPE),
                        lastId, PageEvent.TYPE_NAME, VisitEvent.TYPE_NAME, AppCloseEvent.TYPE_NAME);
            case OTHER:
                return cleanupEvents(String.format(
                                "_id <= ? AND (%s = ? OR %s = ?)", KEY_EVENT_TYPE, KEY_EVENT_TYPE),
                        lastId, ActionEvent.CLICK_TYPE_NAME, ActionEvent.CHANGE_TYPE_NAME);
            case INSTANT_IMP:
                return cleanupEvents(String.format(
                                "_id <= ? AND %s = 1 AND %s = ?", KEY_INSTANT, KEY_EVENT_TYPE),
                        lastId, ActionEvent.IMP_TYPE_NAME);
            case NON_INSTANT_IMP:
                return cleanupEvents(String.format(
                                "_id <= ? AND %s = 0 AND %s = ?", KEY_INSTANT, KEY_EVENT_TYPE),
                        lastId, ActionEvent.IMP_TYPE_NAME);
            case IMP:
                return cleanupEvents(String.format("_id <= ? AND %s = ?", KEY_EVENT_TYPE),
                        lastId, ActionEvent.IMP_TYPE_NAME);
            default:
                return 0;
        }
    }

    private Pair<String, List<String>> generateDataString(String queryString, String[] selectionArgs) {
        synchronized (mDbLocker) {
            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            Cursor c = db.rawQuery(queryString, selectionArgs);
            List<String> dataStrs = new LinkedList<String>();
            String lastId = null;
            long sumDataSize = 0L;
            try {
                while (c.moveToNext()) {
                    String data = c.getString(c.getColumnIndex(KEY_DATA));
                    long dataSize = data.getBytes().length;
                    if (dataSize > EVENT_DATA_MAX_SIZE) {
                        DiagnoseLog.saveLogIfEnabled("event data is too large, size: " + dataSize + " bytes");
                        continue;
                    }
                    sumDataSize += dataSize;
                    // Check if the data size is within the limit
                    if (sumDataSize < EVENT_DATA_MAX_SIZE * 2) {
                        dataStrs.add(data);
                        lastId = c.getString(c.getColumnIndex("_id"));
                    } else {
                        break;
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
                closeDatabaseDelayed();
            }
            if (!dataStrs.isEmpty() && lastId != null)
                return new Pair<String, List<String>>(lastId, dataStrs);
            return null;
        }
    }
}
