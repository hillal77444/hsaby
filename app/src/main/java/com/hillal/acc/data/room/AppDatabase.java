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
import com.hillal.acc.data.room.AccountDao;
import com.hillal.acc.data.room.TransactionDao;
import com.hillal.acc.data.room.SettingsDao;
import com.hillal.acc.data.room.PendingOperationDao;
import com.hillal.acc.data.room.UserDao;

@Database(entities = {
        Account.class,
        Transaction.class,
        User.class,
        Settings.class,
        PendingOperation.class,
        Cashbox.class
}, version = 6, exportSchema = true)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;

    public abstract AccountDao accountDao();
    public abstract TransactionDao transactionDao();
    public abstract SettingsDao settingsDao();
    public abstract PendingOperationDao pendingOperationDao();
    public abstract UserDao userDao();
    public abstract CashboxDao cashboxDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `Cashbox` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `createdAt` TEXT)");
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `cashbox_id` INTEGER DEFAULT -1");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "accounting_database"
                    )
                    .addMigrations(MIGRATION_1_2, new Migration_2(), new Migration_3(), new Migration_4(), new Migration_5())
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