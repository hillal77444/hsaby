package com.hillal.acc.data.room;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.data.model.Settings;
import com.hillal.acc.data.model.PendingOperation;
import com.hillal.acc.data.model.User;
import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.data.dao.CashboxDao;
import com.hillal.acc.data.room.migrations.Migration_2;
import com.hillal.acc.data.room.migrations.Migration_3;
import com.hillal.acc.data.room.migrations.Migration_4;
import com.hillal.acc.data.room.migrations.Migration_5;
import com.hillal.acc.data.room.Converters;

@Database(entities = {
        Account.class,
        Transaction.class,
        User.class,
        Settings.class,
        PendingOperation.class,
        Cashbox.class
}, version = 2, exportSchema = true)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;

    public abstract com.hillal.acc.data.room.AccountDao accountDao();
    public abstract com.hillal.acc.data.room.TransactionDao transactionDao();
    public abstract com.hillal.acc.data.room.SettingsDao settingsDao();
    public abstract com.hillal.acc.data.room.PendingOperationDao pendingOperationDao();
    public abstract com.hillal.acc.data.room.UserDao userDao();
    public abstract com.hillal.acc.data.dao.CashboxDao cashboxDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "accounting_database"
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            Log.d(TAG, "Database created - starting sync from server after delay");
                            // جلب البيانات من الخادم بعد إعادة الإنشاء مع تأخير 2 ثانية
                            new Thread(() -> {
                                try {
                                    Thread.sleep(2000); // تأخير 2 ثانية
                                } catch (InterruptedException e) {
                                    Log.e(TAG, "Sync delay interrupted", e);
                                }
                                Context appContext = context.getApplicationContext();
                                AppDatabase database = AppDatabase.getInstance(appContext);
                                com.hillal.acc.data.remote.DataManager dataManager = new com.hillal.acc.data.remote.DataManager(
                                    appContext,
                                    database.accountDao(),
                                    database.transactionDao(),
                                    database.pendingOperationDao()
                                );
                                com.hillal.acc.data.sync.SyncManager syncManager = new com.hillal.acc.data.sync.SyncManager(
                                    appContext,
                                    dataManager,
                                    database.accountDao(),
                                    database.transactionDao(),
                                    database.pendingOperationDao()
                                );
                                syncManager.performFullSync(new com.hillal.acc.data.sync.SyncManager.SyncCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Sync from server completed after destructive migration");
                                    }
                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "Sync from server failed after destructive migration: " + error);
                                    }
                                });
                            }).start();
                        }
                    })
                    .build();
                    Log.d(TAG, "Database instance created");
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Log.d(TAG, "Database created");
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            Log.d(TAG, "Database opened");
        }
    };
} 