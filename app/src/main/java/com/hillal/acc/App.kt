package com.hillal.acc

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.hillal.acc.data.repository.AccountRepository
import com.hillal.acc.data.repository.SettingsRepository
import com.hillal.acc.data.repository.TransactionRepository
import com.hillal.acc.data.room.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class App : Application() {
    private var database: AppDatabase? = null
    private var accountRepository: AccountRepository? = null
    private var transactionRepository: TransactionRepository? = null
    private var settingsRepository: SettingsRepository? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        instance = this


        // Set up uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler { thread: Thread?, throwable: Throwable? ->
            val errorMessage = "=== تفاصيل الخطأ ===\n\n" +
                    "نوع الخطأ: " + throwable!!.javaClass.getSimpleName() + "\n" +
                    "الرسالة: " + throwable.message + "\n\n" +
                    "=== تفاصيل الخطأ التقنية ===\n" +
                    Log.getStackTraceString(throwable) + "\n\n" +
                    "=== معلومات النظام ===\n" +
                    "نظام التشغيل: Android " + Build.VERSION.RELEASE + "\n" +
                    "الجهاز: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                    "وقت حدوث الخطأ: " + SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(
                Date()
            )
            Log.e(TAG, errorMessage)


            // Copy error to clipboard
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("تفاصيل الخطأ", errorMessage)
            clipboard.setPrimaryClip(clip)


            // Show error dialog
            handler.post(Runnable {
                Toast.makeText(
                    this,
                    "حدث خطأ غير متوقع. تم نسخ التفاصيل إلى الحافظة.",
                    Toast.LENGTH_LONG
                ).show()
                System.exit(1)
            })
        })

        try {
            Log.d(TAG, "Initializing application...")


            // Initialize database
            Log.d(TAG, "Initializing database...")
            database = AppDatabase.getInstance(getApplicationContext())
            Log.d(TAG, "Database initialized successfully")


            // Initialize repositories
            Log.d(TAG, "Initializing repositories...")
            accountRepository = AccountRepository(database!!.accountDao(), database)
            transactionRepository = TransactionRepository(database)
            settingsRepository = SettingsRepository(this)
            Log.d(TAG, "Repositories initialized successfully")

            Log.d(TAG, "Application initialized successfully")
        } catch (e: Exception) {
            val errorMessage = "=== تفاصيل الخطأ ===\n\n" +
                    "نوع الخطأ: " + e.javaClass.getSimpleName() + "\n" +
                    "الرسالة: " + e.message + "\n\n" +
                    "=== تفاصيل الخطأ التقنية ===\n" +
                    Log.getStackTraceString(e) + "\n\n" +
                    "=== معلومات النظام ===\n" +
                    "نظام التشغيل: Android " + Build.VERSION.RELEASE + "\n" +
                    "الجهاز: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                    "وقت حدوث الخطأ: " + SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(
                Date()
            )

            Log.e(TAG, errorMessage)


            // Copy error to clipboard
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("تفاصيل الخطأ", errorMessage)
            clipboard.setPrimaryClip(clip)


            // Show error dialog
            handler.post(Runnable {
                Toast.makeText(
                    this,
                    "حدث خطأ أثناء تهيئة التطبيق. تم نسخ التفاصيل إلى الحافظة.",
                    Toast.LENGTH_LONG
                ).show()
                System.exit(1)
            })
        }
    }

    fun getDatabase(): AppDatabase {
        checkNotNull(database) { "Database is null. Make sure to initialize the Application class." }
        return database!!
    }

    fun getAccountRepository(): AccountRepository {
        checkNotNull(accountRepository) { "AccountRepository is null. Make sure to initialize the Application class." }
        return accountRepository!!
    }

    fun getTransactionRepository(): TransactionRepository {
        checkNotNull(transactionRepository) { "TransactionRepository is null. Make sure to initialize the Application class." }
        return transactionRepository!!
    }

    fun getSettingsRepository(): SettingsRepository {
        checkNotNull(settingsRepository) { "SettingsRepository is null. Make sure to initialize the Application class." }
        return settingsRepository!!
    }

    val transactionDao: TransactionDao?
        get() = database!!.transactionDao()

    companion object {
        private const val TAG = "App"
        private var instance: App? = null
        fun getInstance(): App {
            checkNotNull(instance) { "Application instance is null. Make sure to initialize the Application class." }
            return instance!!
        }
    }
}