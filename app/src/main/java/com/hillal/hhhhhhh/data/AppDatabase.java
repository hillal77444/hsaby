package com.hillal.hhhhhhh.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.hillal.hhhhhhh.data.dao.AccountDao;
import com.hillal.hhhhhhh.data.dao.TransactionDao;
import com.hillal.hhhhhhh.data.dao.ReportDao;
import com.hillal.hhhhhhh.data.entities.Account;
import com.hillal.hhhhhhh.data.entities.Transaction;
import com.hillal.hhhhhhh.data.entities.Report;
import com.hillal.hhhhhhh.data.Converters;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Account.class, Transaction.class, Report.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public abstract AccountDao accountDao();
    public abstract TransactionDao transactionDao();
    public abstract ReportDao reportDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "accounting_database"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
} 