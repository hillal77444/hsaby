package com.hillal.hhhhhhh.data.room;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.model.Settings;
import com.hillal.hhhhhhh.data.model.PendingOperation;
import com.hillal.hhhhhhh.data.model.User;
import com.hillal.hhhhhhh.data.room.migrations.Migration_2;
import com.hillal.hhhhhhh.data.room.migrations.Migration_3;
import com.hillal.hhhhhhh.data.room.migrations.Migration_4;
import com.hillal.hhhhhhh.data.room.migrations.Migration_5;
import com.hillal.hhhhhhh.data.room.Converters;

@Database(entities = {
        Account.class,
        Transaction.class,
        User.class,
        Settings.class,
        PendingOperation.class
}, version = 1, exportSchema = true)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;

    public abstract com.hillal.hhhhhhh.data.room.AccountDao accountDao();
    public abstract com.hillal.hhhhhhh.data.room.TransactionDao transactionDao();
    public abstract SettingsDao settingsDao();
    public abstract PendingOperationDao pendingOperationDao();
    public abstract UserDao userDao();

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