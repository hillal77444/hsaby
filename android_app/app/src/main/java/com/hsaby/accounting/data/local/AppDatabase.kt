package com.hsaby.accounting.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hsaby.accounting.data.local.dao.AccountDao
import com.hsaby.accounting.data.local.dao.TransactionDao
import com.hsaby.accounting.data.local.dao.UserDao
import com.hsaby.accounting.data.local.entity.AccountEntity
import com.hsaby.accounting.data.local.entity.TransactionEntity
import com.hsaby.accounting.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
} 