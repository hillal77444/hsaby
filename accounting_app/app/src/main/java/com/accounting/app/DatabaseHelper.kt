package com.accounting.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import org.json.JSONObject
import java.security.MessageDigest
import org.json.JSONArray

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "accounting.db"
        private const val DATABASE_VERSION = 1

        // جداول قاعدة البيانات
        private const val TABLE_USERS = "users"
        private const val TABLE_ACCOUNTS = "accounts"
        private const val TABLE_TRANSACTIONS = "transactions"
        private const val TABLE_SETTINGS = "settings"
        private const val TABLE_SYNC_QUEUE = "sync_queue"

        // أعمدة جدول المستخدمين
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PHONE = "phone"
        private const val COLUMN_PASSWORD_HASH = "password_hash"

        // أعمدة جدول الحسابات
        private const val COLUMN_ACCOUNT_ID = "id"
        private const val COLUMN_ACCOUNT_NUMBER = "account_number"
        private const val COLUMN_ACCOUNT_NAME = "account_name"
        private const val COLUMN_BALANCE = "balance"
        private const val COLUMN_PHONE_NUMBER = "phone_number"
        private const val COLUMN_IS_DEBTOR = "is_debtor"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"

        // أعمدة جدول المعاملات
        private const val COLUMN_TRANSACTION_ID = "id"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_CURRENCY = "currency"
        private const val COLUMN_ACCOUNT_ID_FK = "account_id"

        // أعمدة جدول الإعدادات
        private const val COLUMN_SETTING_ID = "id"
        private const val COLUMN_DARK_MODE = "dark_mode"
        private const val COLUMN_NOTIFICATIONS = "notifications"
        private const val COLUMN_SYNC_INTERVAL = "sync_interval"

        // أعمدة جدول قائمة المزامنة
        private const val COLUMN_QUEUE_ID = "id"
        private const val COLUMN_ACTION = "action"
        private const val COLUMN_DATA = "data"
        private const val COLUMN_TIMESTAMP = "timestamp"

        // إعدادات واجهة المستخدم
        private const val KEY_UI_VERSION = "ui_version"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // إنشاء جدول المستخدمين
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT NOT NULL,
                $COLUMN_PHONE TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD_HASH TEXT NOT NULL
            )
        """)

        // إنشاء جدول الحسابات
        db.execSQL("""
            CREATE TABLE $TABLE_ACCOUNTS (
                $COLUMN_ACCOUNT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ACCOUNT_NUMBER TEXT NOT NULL,
                $COLUMN_ACCOUNT_NAME TEXT NOT NULL,
                $COLUMN_BALANCE REAL NOT NULL,
                $COLUMN_PHONE_NUMBER TEXT,
                $COLUMN_IS_DEBTOR INTEGER DEFAULT 0,
                $COLUMN_NOTES TEXT,
                $COLUMN_CREATED_AT INTEGER,
                $COLUMN_UPDATED_AT INTEGER
            )
        """)

        // إنشاء جدول المعاملات
        db.execSQL("""
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COLUMN_TRANSACTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE INTEGER NOT NULL,
                $COLUMN_AMOUNT REAL NOT NULL,
                $COLUMN_DESCRIPTION TEXT NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_CURRENCY TEXT NOT NULL,
                $COLUMN_NOTES TEXT,
                $COLUMN_ACCOUNT_ID_FK INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_ACCOUNT_ID_FK) REFERENCES $TABLE_ACCOUNTS($COLUMN_ACCOUNT_ID)
            )
        """)

        // إنشاء جدول الإعدادات
        db.execSQL("""
            CREATE TABLE $TABLE_SETTINGS (
                $COLUMN_SETTING_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DARK_MODE INTEGER DEFAULT 0,
                $COLUMN_NOTIFICATIONS INTEGER DEFAULT 1,
                $COLUMN_SYNC_INTERVAL INTEGER DEFAULT 15,
                $COLUMN_KEY TEXT NOT NULL,
                $COLUMN_VALUE TEXT NOT NULL
            )
        """)

        // إنشاء جدول قائمة المزامنة
        db.execSQL("""
            CREATE TABLE $TABLE_SYNC_QUEUE (
                $COLUMN_QUEUE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ACTION TEXT NOT NULL,
                $COLUMN_DATA TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // حذف الجداول القديمة
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACCOUNTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SYNC_QUEUE")

        // إعادة إنشاء الجداول
        onCreate(db)
    }

    // وظائف المستخدم
    fun verifyLogin(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val passwordHash = hashPassword(password)
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_PASSWORD_HASH),
            "$COLUMN_USERNAME = ?",
            arrayOf(username),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val storedHash = cursor.getString(0)
            cursor.close()
            storedHash == passwordHash
        } else {
            cursor.close()
            false
        }
    }

    fun updateProfile(username: String, phone: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PHONE, phone)
        }
        db.update(TABLE_USERS, values, null, null)
    }

    fun verifyPassword(password: String): Boolean {
        val db = this.readableDatabase
        val passwordHash = hashPassword(password)
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_PASSWORD_HASH),
            null,
            null,
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val storedHash = cursor.getString(0)
            cursor.close()
            storedHash == passwordHash
        } else {
            cursor.close()
            false
        }
    }

    fun updatePassword(newPassword: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PASSWORD_HASH, hashPassword(newPassword))
        }
        db.update(TABLE_USERS, values, null, null)
    }

    // وظائف الإعدادات
    fun saveAppSettings(darkMode: Boolean, notifications: Boolean, syncInterval: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DARK_MODE, if (darkMode) 1 else 0)
            put(COLUMN_NOTIFICATIONS, if (notifications) 1 else 0)
            put(COLUMN_SYNC_INTERVAL, syncInterval)
        }
        db.update(TABLE_SETTINGS, values, null, null)
    }

    fun getAppSettings(): JSONObject {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_SETTINGS,
            null,
            null,
            null,
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val settings = JSONObject().apply {
                put("dark_mode", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DARK_MODE)) == 1)
                put("notifications", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTIFICATIONS)) == 1)
                put("sync_interval", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNC_INTERVAL)))
            }
            cursor.close()
            settings
        } else {
            cursor.close()
            JSONObject().apply {
                put("dark_mode", false)
                put("notifications", true)
                put("sync_interval", 15)
            }
        }
    }

    // وظائف المزامنة
    fun addToSyncQueue(action: String, data: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ACTION, action)
            put(COLUMN_DATA, data)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }
        db.insert(TABLE_SYNC_QUEUE, null, values)
    }

    fun getSyncQueue(): List<Pair<String, String>> {
        val db = this.readableDatabase
        val queue = mutableListOf<Pair<String, String>>()
        val cursor = db.query(
            TABLE_SYNC_QUEUE,
            arrayOf(COLUMN_ACTION, COLUMN_DATA),
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )
        while (cursor.moveToNext()) {
            val action = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTION))
            val data = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATA))
            queue.add(Pair(action, data))
        }
        cursor.close()
        return queue
    }

    fun clearSyncQueue() {
        val db = this.writableDatabase
        db.delete(TABLE_SYNC_QUEUE, null, null)
    }

    // وظائف مساعدة
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun deleteAllData() {
        val db = this.writableDatabase
        db.delete(TABLE_USERS, null, null)
        db.delete(TABLE_ACCOUNTS, null, null)
        db.delete(TABLE_TRANSACTIONS, null, null)
        db.delete(TABLE_SETTINGS, null, null)
        db.delete(TABLE_SYNC_QUEUE, null, null)
    }

    // وظائف الحسابات
    fun addAccount(accountNumber: String, accountName: String, balance: Double, phoneNumber: String?, isDebtor: Boolean, notes: String?): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ACCOUNT_NUMBER, accountNumber)
            put(COLUMN_ACCOUNT_NAME, accountName)
            put(COLUMN_BALANCE, balance)
            put(COLUMN_PHONE_NUMBER, phoneNumber)
            put(COLUMN_IS_DEBTOR, if (isDebtor) 1 else 0)
            put(COLUMN_NOTES, notes)
            put(COLUMN_CREATED_AT, System.currentTimeMillis())
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        val id = db.insert(TABLE_ACCOUNTS, null, values)
        
        // إضافة العملية للمزامنة
        if (id != -1L) {
            addToSyncQueue("ADD_ACCOUNT", values.toString())
        }
        
        return id
    }

    fun updateAccount(accountId: Long, accountNumber: String, accountName: String, balance: Double, phoneNumber: String?, isDebtor: Boolean, notes: String?) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ACCOUNT_NUMBER, accountNumber)
            put(COLUMN_ACCOUNT_NAME, accountName)
            put(COLUMN_BALANCE, balance)
            put(COLUMN_PHONE_NUMBER, phoneNumber)
            put(COLUMN_IS_DEBTOR, if (isDebtor) 1 else 0)
            put(COLUMN_NOTES, notes)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        db.update(TABLE_ACCOUNTS, values, "$COLUMN_ACCOUNT_ID = ?", arrayOf(accountId.toString()))
        
        // إضافة العملية للمزامنة
        addToSyncQueue("UPDATE_ACCOUNT", values.toString())
    }

    fun deleteAccount(accountId: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_ACCOUNTS, "$COLUMN_ACCOUNT_ID = ?", arrayOf(accountId.toString()))
        
        // إضافة العملية للمزامنة
        addToSyncQueue("DELETE_ACCOUNT", accountId.toString())
    }

    fun getAccount(accountId: Long): JSONObject? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_ACCOUNTS,
            null,
            "$COLUMN_ACCOUNT_ID = ?",
            arrayOf(accountId.toString()),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val account = JSONObject().apply {
                put("id", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID)))
                put("account_number", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_NUMBER)))
                put("account_name", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_NAME)))
                put("balance", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BALANCE)))
                put("phone_number", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER)))
                put("is_debtor", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DEBTOR)) == 1)
                put("notes", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)))
                put("created_at", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)))
                put("updated_at", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)))
            }
            cursor.close()
            account
        } else {
            cursor.close()
            null
        }
    }

    fun getAllAccounts(): JSONArray {
        val db = this.readableDatabase
        val accounts = JSONArray()
        val cursor = db.query(
            TABLE_ACCOUNTS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_ACCOUNT_NAME ASC"
        )
        while (cursor.moveToNext()) {
            val account = JSONObject().apply {
                put("id", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID)))
                put("account_number", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_NUMBER)))
                put("account_name", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_NAME)))
                put("balance", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_BALANCE)))
                put("phone_number", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER)))
                put("is_debtor", cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DEBTOR)) == 1)
                put("notes", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)))
                put("created_at", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)))
                put("updated_at", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)))
            }
            accounts.put(account)
        }
        cursor.close()
        return accounts
    }

    // وظائف المعاملات
    fun addTransaction(accountId: Long, date: Long, amount: Double, description: String, type: String, currency: String, notes: String?): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, date)
            put(COLUMN_AMOUNT, amount)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_TYPE, type)
            put(COLUMN_CURRENCY, currency)
            put(COLUMN_NOTES, notes)
            put(COLUMN_ACCOUNT_ID_FK, accountId)
        }
        val id = db.insert(TABLE_TRANSACTIONS, null, values)
        
        // تحديث رصيد الحساب
        updateAccountBalance(accountId, amount, type)
        
        // إضافة العملية للمزامنة
        if (id != -1L) {
            addToSyncQueue("ADD_TRANSACTION", values.toString())
        }
        
        return id
    }

    fun updateTransaction(transactionId: Long, date: Long, amount: Double, description: String, type: String, currency: String, notes: String?) {
        val db = this.writableDatabase
        
        // الحصول على المعاملة القديمة
        val oldTransaction = getTransaction(transactionId)
        if (oldTransaction != null) {
            // إلغاء تأثير المعاملة القديمة على رصيد الحساب
            val oldAmount = oldTransaction.getDouble("amount")
            val oldType = oldTransaction.getString("type")
            val accountId = oldTransaction.getLong("account_id")
            updateAccountBalance(accountId, -oldAmount, oldType)
        }
        
        val values = ContentValues().apply {
            put(COLUMN_DATE, date)
            put(COLUMN_AMOUNT, amount)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_TYPE, type)
            put(COLUMN_CURRENCY, currency)
            put(COLUMN_NOTES, notes)
        }
        db.update(TABLE_TRANSACTIONS, values, "$COLUMN_TRANSACTION_ID = ?", arrayOf(transactionId.toString()))
        
        // تطبيق تأثير المعاملة الجديدة على رصيد الحساب
        if (oldTransaction != null) {
            updateAccountBalance(oldTransaction.getLong("account_id"), amount, type)
        }
        
        // إضافة العملية للمزامنة
        addToSyncQueue("UPDATE_TRANSACTION", values.toString())
    }

    fun deleteTransaction(transactionId: Long) {
        val db = this.writableDatabase
        
        // الحصول على المعاملة قبل حذفها
        val transaction = getTransaction(transactionId)
        if (transaction != null) {
            // إلغاء تأثير المعاملة على رصيد الحساب
            val amount = transaction.getDouble("amount")
            val type = transaction.getString("type")
            val accountId = transaction.getLong("account_id")
            updateAccountBalance(accountId, -amount, type)
        }
        
        db.delete(TABLE_TRANSACTIONS, "$COLUMN_TRANSACTION_ID = ?", arrayOf(transactionId.toString()))
        
        // إضافة العملية للمزامنة
        addToSyncQueue("DELETE_TRANSACTION", transactionId.toString())
    }

    fun getTransaction(transactionId: Long): JSONObject? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COLUMN_TRANSACTION_ID = ?",
            arrayOf(transactionId.toString()),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val transaction = JSONObject().apply {
                put("id", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_ID)))
                put("date", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)))
                put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)))
                put("description", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)))
                put("type", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)))
                put("currency", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)))
                put("notes", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)))
                put("account_id", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID_FK)))
            }
            cursor.close()
            transaction
        } else {
            cursor.close()
            null
        }
    }

    fun getAccountTransactions(accountId: Long): JSONArray {
        val db = this.readableDatabase
        val transactions = JSONArray()
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COLUMN_ACCOUNT_ID_FK = ?",
            arrayOf(accountId.toString()),
            null,
            null,
            "$COLUMN_DATE DESC"
        )
        while (cursor.moveToNext()) {
            val transaction = JSONObject().apply {
                put("id", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_ID)))
                put("date", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)))
                put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)))
                put("description", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)))
                put("type", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)))
                put("currency", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)))
                put("notes", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)))
                put("account_id", cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID_FK)))
            }
            transactions.put(transaction)
        }
        cursor.close()
        return transactions
    }

    private fun updateAccountBalance(accountId: Long, amount: Double, type: String) {
        val db = this.writableDatabase
        val account = getAccount(accountId) ?: return
        
        val currentBalance = account.getDouble("balance")
        val isDebtor = account.getBoolean("is_debtor")
        val newBalance = when (type) {
            "DEPOSIT" -> if (isDebtor) currentBalance - amount else currentBalance + amount
            "WITHDRAW" -> if (isDebtor) currentBalance + amount else currentBalance - amount
            else -> currentBalance
        }
        
        val values = ContentValues().apply {
            put(COLUMN_BALANCE, newBalance)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        db.update(TABLE_ACCOUNTS, values, "$COLUMN_ACCOUNT_ID = ?", arrayOf(accountId.toString()))
    }

    // وظائف إدارة واجهة المستخدم
    fun saveUIVersion(version: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_KEY, KEY_UI_VERSION)
            put(COLUMN_VALUE, version.toString())
        }
        db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getUIVersion(): Int {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_SETTINGS,
            arrayOf(COLUMN_VALUE),
            "$COLUMN_KEY = ?",
            arrayOf(KEY_UI_VERSION),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            cursor.getString(0).toInt()
        } else {
            0
        }.also { cursor.close() }
    }
} 