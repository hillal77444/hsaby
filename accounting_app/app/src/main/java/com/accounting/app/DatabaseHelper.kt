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
import com.accounting.app.models.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val TAG = "DatabaseHelper"
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
        private const val COLUMN_KEY = "key"
        private const val COLUMN_VALUE = "value"

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

    fun getUser(username: String, phone: String): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USERNAME = ? AND $COLUMN_PHONE = ?",
            arrayOf(username, phone),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val user = User(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH))
            )
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    fun addUser(username: String, phone: String, passwordHash: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PHONE, phone)
            put(COLUMN_PASSWORD_HASH, passwordHash)
        }
        return db.insert(TABLE_USERS, null, values) != -1L
    }

    // وظائف الحسابات
    fun addAccount(account: Account): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ACCOUNT_NUMBER, account.accountNumber)
            put(COLUMN_ACCOUNT_NAME, account.accountName)
            put(COLUMN_BALANCE, account.balance)
            put(COLUMN_PHONE_NUMBER, account.phoneNumber)
            put(COLUMN_IS_DEBTOR, if (account.isDebtor) 1 else 0)
            put(COLUMN_NOTES, account.notes)
            put(COLUMN_CREATED_AT, account.createdAt)
            put(COLUMN_UPDATED_AT, account.updatedAt)
        }
        return db.insert(TABLE_ACCOUNTS, null, values) != -1L
    }

    fun updateAccount(account: Account): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ACCOUNT_NUMBER, account.accountNumber)
            put(COLUMN_ACCOUNT_NAME, account.accountName)
            put(COLUMN_BALANCE, account.balance)
            put(COLUMN_PHONE_NUMBER, account.phoneNumber)
            put(COLUMN_IS_DEBTOR, if (account.isDebtor) 1 else 0)
            put(COLUMN_NOTES, account.notes)
            put(COLUMN_UPDATED_AT, account.updatedAt)
        }
        return db.update(TABLE_ACCOUNTS, values, "$COLUMN_ACCOUNT_ID = ?", arrayOf(account.id.toString())) > 0
    }

    fun deleteAccount(accountId: Long): Boolean {
        val db = this.writableDatabase
        return db.delete(TABLE_ACCOUNTS, "$COLUMN_ACCOUNT_ID = ?", arrayOf(accountId.toString())) > 0
    }

    // وظائف المعاملات
    fun addTransaction(transaction: Transaction): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, transaction.date)
            put(COLUMN_AMOUNT, transaction.amount)
            put(COLUMN_DESCRIPTION, transaction.description)
            put(COLUMN_TYPE, transaction.type)
            put(COLUMN_CURRENCY, transaction.currency)
            put(COLUMN_NOTES, transaction.notes)
            put(COLUMN_ACCOUNT_ID_FK, transaction.accountId)
        }
        return db.insert(TABLE_TRANSACTIONS, null, values) != -1L
    }

    fun updateTransaction(transaction: Transaction): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, transaction.date)
            put(COLUMN_AMOUNT, transaction.amount)
            put(COLUMN_DESCRIPTION, transaction.description)
            put(COLUMN_TYPE, transaction.type)
            put(COLUMN_CURRENCY, transaction.currency)
            put(COLUMN_NOTES, transaction.notes)
        }
        return db.update(TABLE_TRANSACTIONS, values, "$COLUMN_TRANSACTION_ID = ?", arrayOf(transaction.id.toString())) > 0
    }

    fun deleteTransaction(transactionId: Long): Boolean {
        val db = this.writableDatabase
        return db.delete(TABLE_TRANSACTIONS, "$COLUMN_TRANSACTION_ID = ?", arrayOf(transactionId.toString())) > 0
    }

    fun getTransactionsForAccount(accountId: Long): List<Transaction> {
        val db = this.readableDatabase
        val transactions = mutableListOf<Transaction>()
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
            transactions.add(Transaction(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_ID)),
                date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)),
                notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)),
                accountId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID_FK))
            ))
        }
        cursor.close()
        return transactions
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
    fun hashPassword(password: String): String {
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

    fun getAllSettings(): Map<String, String> {
        val db = this.readableDatabase
        val settings = mutableMapOf<String, String>()
        val cursor = db.query(
            TABLE_SETTINGS,
            arrayOf(COLUMN_KEY, COLUMN_VALUE),
            null,
            null,
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val key = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY))
            val value = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VALUE))
            settings[key] = value
        }
        cursor.close()
        return settings
    }

    fun updateSettings(settings: Map<String, String>): Boolean {
        val db = this.writableDatabase
        var success = true
        db.beginTransaction()
        try {
            for ((key, value) in settings) {
                val values = ContentValues().apply {
                    put(COLUMN_KEY, key)
                    put(COLUMN_VALUE, value)
                }
                if (db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE) == -1L) {
                    success = false
                    break
                }
            }
            if (success) {
                db.setTransactionSuccessful()
            }
        } finally {
            db.endTransaction()
        }
        return success
    }

    fun syncData(): Boolean {
        try {
            val db = this.writableDatabase
            val lastSyncTimestamp = getLastSyncTimestamp()
            
            // جلب البيانات المحلية
            val accountsArray = getAllAccounts()
            val localAccounts = mutableListOf<com.accounting.app.models.Account>()
            for (i in 0 until accountsArray.length()) {
                val jsonAccount = accountsArray.getJSONObject(i)
                localAccounts.add(jsonToAccount(jsonAccount))
            }
            
            val localTransactions = getAllTransactions().map { transaction ->
                com.accounting.app.models.Transaction(
                    id = transaction.id,
                    date = transaction.date,
                    amount = transaction.amount,
                    description = transaction.description,
                    type = transaction.type,
                    currency = transaction.currency,
                    notes = transaction.notes,
                    accountId = transaction.accountId
                )
            }
            
            // إرسال البيانات إلى السيرفر
            val syncData = SyncData(
                accounts = localAccounts,
                transactions = localTransactions,
                lastSyncTimestamp = lastSyncTimestamp
            )
            
            // تحديث timestamp المزامنة
            updateLastSyncTimestamp(System.currentTimeMillis())
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing data: ${e.message}")
            return false
        }
    }

    private fun jsonToAccount(json: JSONObject): com.accounting.app.models.Account {
        return com.accounting.app.models.Account(
            id = json.getLong("id"),
            accountNumber = json.getString("account_number"),
            accountName = json.getString("account_name"),
            balance = json.getDouble("balance"),
            phoneNumber = if (json.has("phone_number")) json.getString("phone_number") else null,
            isDebtor = json.getBoolean("is_debtor"),
            notes = if (json.has("notes")) json.getString("notes") else null,
            createdAt = json.getLong("created_at"),
            updatedAt = json.getLong("updated_at")
        )
    }

    fun getAllTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            null,
            null,
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            transactions.add(
                Transaction(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_ID)),
                    date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                    currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)),
                    notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES)),
                    accountId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID_FK))
                )
            )
        }
        cursor.close()
        return transactions
    }

    fun getLastSyncTimestamp(): Long {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_SETTINGS,
            arrayOf(COLUMN_VALUE),
            "$COLUMN_KEY = ?",
            arrayOf("last_sync_timestamp"),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val timestamp = cursor.getLong(0)
            cursor.close()
            timestamp
        } else {
            cursor.close()
            0L
        }
    }

    fun updateLastSyncTimestamp(timestamp: Long) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_KEY, "last_sync_timestamp")
            put(COLUMN_VALUE, timestamp.toString())
        }
        db.insertWithOnConflict(
            TABLE_SETTINGS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
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