package com.hillal.hhhhhhh.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.User;
import com.hillal.hhhhhhh.data.room.TransactionDao;
import com.hillal.hhhhhhh.data.room.AccountDao;
import com.hillal.hhhhhhh.data.room.UserDao;

@Database(entities = {Transaction.class, Account.class, User.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
    public abstract AccountDao accountDao();
    public abstract UserDao userDao();
} 