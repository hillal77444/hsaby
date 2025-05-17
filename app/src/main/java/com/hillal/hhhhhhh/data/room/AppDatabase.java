package com.hillal.hhhhhhh.data.room;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.hillal.hhhhhhh.data.Converters;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.model.Settings;
import com.hillal.hhhhhhh.data.model.PendingOperation;
import com.hillal.hhhhhhh.data.room.migrations.Migration_2;
import com.hillal.hhhhhhh.data.room.migrations.Migration_3;
import com.hillal.hhhhhhh.data.room.migrations.Migration_4;

@Database(entities = {Account.class, Transaction.class, Settings.class, PendingOperation.class}, version = 4, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static final String DATABASE_NAME = "accounting_database";
    private static volatile AppDatabase instance;

    public abstract AccountDao accountDao();
    public abstract TransactionDao transactionDao();
    public abstract SettingsDao settingsDao();
    public abstract PendingOperationDao pendingOperationDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .addMigrations(new Migration_2(), new Migration_3(), new Migration_4())
                            .addCallback(roomCallback)
                            .build();
                    Log.d(TAG, "Database instance created");
                }
            }
        }
        return instance;
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