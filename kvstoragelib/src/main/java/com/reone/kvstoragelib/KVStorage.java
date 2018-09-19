package com.reone.kvstoragelib;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by wangxingsheng on 2018/9/13.
 * 键值对数据库，KV存储
 * 整理RN数据库部分代码，构建一个用于Android的 Key-Value型数据库，主要参考：{@link com.facebook.react.modules.storage.AsyncLocalStorageUtil}、{@link com.facebook.react.modules.storage.ReactDatabaseSupplier}
 * 测试页面 {@link com.ocj.oms.mobile.ui.KVStorageTestActivity}
 */
public class KVStorage {

    private static final String DATABASE_NAME = "KVStorage";
    private static final String TABLE_CATALYST = "catalystLocalStorage";
    private static final String KEY_COLUMN = "key";
    private static final String VALUE_COLUMN = "value";
    private static final int DATABASE_VERSION = 1;
    private static final int SLEEP_TIME_MS = 30;
    private static final int MAX_SQL_KEYS = 999;

    private static final String VERSION_TABLE_CREATE =
            String.format("CREATE TABLE %s (%s TEXT PRIMARY KEY, %s TEXT NOT NULL)", TABLE_CATALYST, KEY_COLUMN, VALUE_COLUMN);

    private static Context appContext = null;

    public static void init(Context appContext) {
        KVStorage.appContext = appContext.getApplicationContext();
    }

    /**
     * 获取数据库实例
     */
    @NonNull
    private static KVStorageDatabaseSupplier getSupplier() {
        if (appContext == null) {
            throw new IllegalArgumentException("KVStorage not initialized yet, need to call KVStorage.init(context) in Application.");
        }
        return KVStorageDatabaseSupplier.getInstance(appContext);
    }

    /**
     * 开启事务执行
     */
    public static <R> Observable<R> rxRunInTransaction(TransactionRunnable<SQLiteDatabase, R> func) {
        return Observable.create((ObservableEmitter<R> s) -> {
            try {
                getSupplier().get().beginTransaction();
                s.onNext(func.invoke(getSupplier().get()));
                getSupplier().get().setTransactionSuccessful();
            } finally {
                getSupplier().get().endTransaction();
                s.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 异步获取
     */
    public static Observable<String> rxGet(@NonNull String key) {
        return Observable.create((ObservableEmitter<String> s) -> {
            s.onNext(getItemImpl(getSupplier().get(), key));
            s.onComplete();
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 同步储存
     */
    public static boolean save(@NonNull String key, @Nullable String value) {
        return setItemImpl(getSupplier().get(), key, value);
    }

    /**
     * 异步存储
     * 此方法会覆盖原来的值
     */
    public static Observable<Boolean> rxSave(@NonNull String key, @Nullable String value) {
        return Observable.create((ObservableEmitter<Boolean> s) -> {
            s.onNext(save(key, value));
            s.onComplete();
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 同步合并json
     */
    public static boolean mergeJson(@NonNull String key, @Nullable String value) throws JSONException {
        return mergeImpl(getSupplier().get(), key, value);
    }

    /**
     * 异步存储
     * 如果value是json类型的，则会与已存在的值合并成新的json值存入数据库
     */
    public static Observable<String> rxMergeJson(@NonNull String key, @Nullable String value) {
        return Observable.create((ObservableEmitter<String> s) -> {
            mergeJson(key, value);
            s.onComplete();
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 获取所有key值
     */
    public static Observable<List<String>> rxGetAllKeys() {
        return Observable.create((ObservableEmitter<List<String>> s) -> {
            List<String> keys = new ArrayList<>();
            String[] columns = {KEY_COLUMN};
            try (Cursor cursor = getSupplier().get()
                    .query(TABLE_CATALYST, columns, null, null, null, null, null)) {
                if (cursor.moveToFirst()) {
                    do {
                        keys.add(cursor.getString(0));
                    } while (cursor.moveToNext());
                }
                s.onNext(keys);
            } catch (Exception e) {
                s.onError(e);
            }
            s.onComplete();
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 单独或批量删除key
     */
    public static Observable<Integer> rxRemove(String... keys) {
        return rxRunInTransaction(sqLiteDatabase -> {
            int lineCount = 0;
            for (int keyStart = 0; keyStart < keys.length; keyStart += MAX_SQL_KEYS) {
                int keyCount = Math.min(keys.length - keyStart, MAX_SQL_KEYS);
                lineCount += getSupplier().get().delete(
                        TABLE_CATALYST,
                        buildKeySelection(keyCount),
                        buildKeySelectionArgs(keys, keyStart, keyCount));
            }
            return lineCount;
        });
    }

    /**
     * 清空数据库
     */
    public static Observable<Integer> rxClear() {
        return Observable.create((ObservableEmitter<Integer> s) -> {
            s.onNext(getSupplier().clear());
            s.onComplete();
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns the value of the given key, or null if not found.
     */
    @Nullable
    private static String getItemImpl(SQLiteDatabase db, String key) {
        String[] columns = {VALUE_COLUMN};
        String[] selectionArgs = {key};

        try (Cursor cursor = db.query(
                TABLE_CATALYST,
                columns,
                KEY_COLUMN + "=?",
                selectionArgs,
                null,
                null,
                null)) {
            if (!cursor.moveToFirst()) {
                return null;
            } else {
                return cursor.getString(0);
            }
        }
    }

    /**
     * 设置给定键的值，如果成功则返回true，否则返回false。
     */
    private static boolean setItemImpl(SQLiteDatabase db, String key, String value) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_COLUMN, key);
        contentValues.put(VALUE_COLUMN, value);

        long inserted = db.insertWithOnConflict(
                TABLE_CATALYST,
                null,
                contentValues,
                SQLiteDatabase.CONFLICT_REPLACE);

        return (-1 != inserted);
    }

    /**
     * Does the actual merge of the (key, value) pair with the value stored in the database.
     * NB: This assumes that a database lock is already in effect!
     *
     * @return the errorCode of the operation
     */
    private static boolean mergeImpl(SQLiteDatabase db, String key, String value)
            throws JSONException {
        String oldValue = getItemImpl(db, key);
        String newValue;

        if (oldValue == null) {
            newValue = value;
        } else {
            JSONObject oldJSON = new JSONObject(oldValue);
            JSONObject newJSON = new JSONObject(value);
            deepMergeInto(oldJSON, newJSON);
            newValue = oldJSON.toString();
        }

        return setItemImpl(db, key, newValue);
    }

    /**
     * 合并两个{@link JSONObject}。
     * newJSON对象将通过覆盖其值或合并它们来与oldJSON对象合并（如果两个对象中相同键的值都是{@link JSONObject}类型）。
     * oldJSON将包含此合并的结果。
     */
    private static void deepMergeInto(JSONObject oldJSON, JSONObject newJSON)
            throws JSONException {
        Iterator<?> keys = newJSON.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            JSONObject newJSONObject = newJSON.optJSONObject(key);
            JSONObject oldJSONObject = oldJSON.optJSONObject(key);
            if (newJSONObject != null && oldJSONObject != null) {
                deepMergeInto(oldJSONObject, newJSONObject);
                oldJSON.put(key, oldJSONObject);
            } else {
                oldJSON.put(key, newJSON.get(key));
            }
        }
    }

    /**
     * Build the String required for an SQL select statement:
     * WHERE key IN (?, ?, ..., ?)
     * without 'WHERE' and with selectionCount '?'
     */
    public static String buildKeySelection(int selectionCount) {
        String[] list = new String[selectionCount];
        Arrays.fill(list, "?");
        return KEY_COLUMN + " IN (" + TextUtils.join(", ", list) + ")";
    }

    /**
     * Build the String[] arguments needed for an SQL selection, i.e.:
     * {a, b, c}
     * to be used in the SQL select statement: WHERE key in (?, ?, ?)
     */
    public static String[] buildKeySelectionArgs(String[] keys, int start, int count) {
        String[] selectionArgs = new String[count];
        System.arraycopy(keys, start, selectionArgs, 0, count);
        return selectionArgs;
    }

    public static void clearAndCloseDatabase() {
        getSupplier().clearAndCloseDatabase();
    }

    public interface TransactionRunnable<T, R> {
        R invoke(T t);
    }

    private static class KVStorageDatabaseSupplier extends SQLiteOpenHelper {
        @Nullable
        private SQLiteDatabase mDb;
        private Context mContext;
        private long mMaximumDatabaseSize = 6L * 1024L * 1024L; // 6 MB in bytes

        @SuppressLint("StaticFieldLeak")
        private static KVStorageDatabaseSupplier mInstance = null;

        public static KVStorageDatabaseSupplier getInstance(Context context) {
            if (mInstance == null) {
                synchronized (KVStorageDatabaseSupplier.class) {
                    if (mInstance == null && context != null) {
                        mInstance = new KVStorageDatabaseSupplier(context.getApplicationContext());
                    }
                }
            }
            return mInstance;
        }

        KVStorageDatabaseSupplier(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(VERSION_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion != newVersion) {
                deleteDatabase();
                onCreate(db);
            }
        }

        /**
         * Verify the database exists and is open.
         */
        private synchronized void ensureDatabase() {
            if (mDb != null && mDb.isOpen()) {
                return;
            }
            // Sometimes retrieving the database fails. We do 2 retries: first without database deletion
            // and then with deletion.
            SQLiteException lastSQLiteException = null;
            for (int tries = 0; tries < 2; tries++) {
                try {
                    if (tries > 0) {
                        deleteDatabase();
                    }
                    mDb = getWritableDatabase();
                    break;
                } catch (SQLiteException e) {
                    lastSQLiteException = e;
                }
                // Wait before retrying.
                try {
                    Thread.sleep(SLEEP_TIME_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
            if (mDb == null && lastSQLiteException != null) {
                throw lastSQLiteException;
            }
            // This is a sane limit to protect the user from the app storing too much data in the database.
            // This also protects the database from filling up the disk cache and becoming malformed
            // (endTransaction() calls will throw an exception, not rollback, and leave the db malformed).
            if (mDb != null) {
                mDb.setMaximumSize(mMaximumDatabaseSize);
            }
        }

        /**
         * Create and/or open the database.
         */
        public synchronized SQLiteDatabase get() {
            ensureDatabase();
            return mDb;
        }

        private synchronized void clearAndCloseDatabase() throws RuntimeException {
            try {
                clear();
                closeDatabase();
            } catch (Exception e) {
                // Clearing the database has failed, delete it instead.
                if (deleteDatabase()) {
                    return;
                }
                // Everything failed, throw
                throw new RuntimeException("Clearing and deleting database " + DATABASE_NAME + " failed");
            }
        }

        public synchronized int clear() {
            return get().delete(TABLE_CATALYST, null, null);
        }

        private synchronized boolean deleteDatabase() {
            closeDatabase();
            return mContext.deleteDatabase(DATABASE_NAME);
        }

        private synchronized void closeDatabase() {
            if (mDb != null && mDb.isOpen()) {
                mDb.close();
                mDb = null;
            }
        }
    }

}
