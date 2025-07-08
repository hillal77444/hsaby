package com.hillal.acc

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.navigateUp
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.hillal.acc.data.repository.AccountRepository
import com.hillal.acc.data.room.AppDatabase
import com.hillal.acc.data.update.AppUpdateHelper
import com.hillal.acc.databinding.ActivityMainBinding
import com.hillal.acc.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.hillal.acc.data.room.AccountDao
import com.hillal.acc.data.room.TransactionDao
import com.hillal.acc.data.room.PendingOperationDao

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var navController: NavController? = null
    private var accountRepository: AccountRepository? = null
    private val appBarConfiguration: AppBarConfiguration? = null
    private var authViewModel: AuthViewModel? = null
    private var app: App? = null
    private var db: AppDatabase? = null
    private var appUpdateHelper: AppUpdateHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        try {
            // Initialize view binding
            binding = ActivityMainBinding.inflate(getLayoutInflater())
            setContentView(binding!!.getRoot())
            // دعم insets للـ Toolbar ليظهر أسفل شريط الحالة بشكل متناسق
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false)
            val toolbar = findViewById<View>(R.id.toolbar)
            ViewCompat.setOnApplyWindowInsetsListener(
                toolbar,
                OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                    val top = insets!!.getInsets(WindowInsetsCompat.Type.systemBars()).top
                    v!!.setPadding(0, top, 0, 0)
                    insets
                })
            Log.d(TAG, "Layout inflated successfully")

            // Setup toolbar
            setSupportActionBar(binding!!.appBarMain.toolbar)
            Log.d(TAG, "Toolbar set successfully")

            // Initialize App instance first
            app = getApplication() as App
            checkNotNull(app) { "Application instance is null" }
            Log.d(TAG, "Application instance initialized")

            // Initialize repository
            accountRepository = app!!.getAccountRepository()
            checkNotNull(accountRepository) { "AccountRepository is null" }
            Log.d(TAG, "AccountRepository initialized successfully")

            // Setup navigation
            val navHostFragment = getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?
            checkNotNull(navHostFragment) { "NavHostFragment not found" }

            navController = navHostFragment.navController
            checkNotNull(navController) { "NavController is null" }

            // Initialize AuthViewModel
            authViewModel = AuthViewModel(getApplication())


            // Check if user is logged in
            if (authViewModel!!.isLoggedIn()) {
                // User is logged in, navigate to dashboard
                navController!!.navigate(R.id.navigation_dashboard)
                binding!!.bottomNavigation.setVisibility(View.VISIBLE)
            } else {
                // User is not logged in, navigate to login
                navController!!.navigate(R.id.loginFragment)
                binding!!.bottomNavigation.setVisibility(View.GONE)
            }

            // Setup bottom navigation
            binding!!.bottomNavigation.setOnNavigationItemSelectedListener(navListener)

            db = AppDatabase.getInstance(getApplicationContext())
            setupUI()

            // تهيئة مدير التحديثات
            appUpdateHelper = AppUpdateHelper(
                this,
                db!!.accountDao(),
                db!!.transactionDao(),
                db!!.pendingOperationDao()
            )

            // بعد إعداد BottomNavigationView
            val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            ViewCompat.setOnApplyWindowInsetsListener(
                bottomNavigationView,
                OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                    val bottom = insets!!.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                    v!!.setPadding(0, 0, 0, bottom)
                    insets
                })
        } catch (e: IllegalStateException) {
            val errorMessage = "=== خطأ في تهيئة التطبيق ===\n\n" +
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
            Log.e(TAG, errorMessage, e)
            showErrorAndExit(errorMessage)
        }
    }

    override fun onResume() {
        super.onResume()
        // التحقق من وجود تحديثات عند استئناف النشاط
        appUpdateHelper!!.checkForUpdates(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // معالجة نتيجة التحديث
        appUpdateHelper!!.onActivityResult(requestCode, resultCode)
    }

    private fun setupUI() {
        // إعدادات أخرى
        setupOtherSettings()
    }

    private fun setupOtherSettings() {
        // هنا يمكن إضافة إعدادات أخرى
    }

    override fun onSupportNavigateUp(): Boolean {
        return navigateUp(navController!!, appBarConfiguration!!)
                || super.onSupportNavigateUp()
    }

    private fun showErrorAndExit(errorMessage: String?) {
        AlertDialog.Builder(this)
            .setTitle("خطأ في التطبيق")
            .setMessage(errorMessage)
            .setPositiveButton(
                "خروج",
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> finish() })
            .setCancelable(false)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()
        if (id == R.id.action_settings) {
            try {
                findNavController(this, R.id.nav_host_fragment_content_main)
                    .navigate(R.id.navigation_settings)
                return true
            } catch (e: Exception) {
                val errorMessage = "=== خطأ في التنقل ===\n\n" +
                        "نوع الخطأ: " + e.javaClass.getSimpleName() + "\n" +
                        "الرسالة: " + e.message + "\n\n" +
                        "=== تفاصيل الخطأ التقنية ===\n" +
                        Log.getStackTraceString(e)
                Log.e(TAG, errorMessage, e)
                Toast.makeText(this, "حدث خطأ أثناء التنقل إلى الإعدادات", Toast.LENGTH_LONG).show()
                return false
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun getAccountRepository(): AccountRepository {
        return accountRepository!!
    }

    override fun onBackPressed() {
        // إذا كنا في لوحة التحكم، نعرض مربع حوار للتأكيد قبل الخروج
        if (navController!!.currentDestination!!.id == R.id.navigation_dashboard) {
            AlertDialog.Builder(this)
                .setTitle("تأكيد الخروج")
                .setMessage("هل تريد الخروج من التطبيق؟")
                .setPositiveButton(
                    "نعم",
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> finish() })
                .setNegativeButton("لا", null)
                .show()
        } else {
            // في أي صفحة أخرى، نترك سلوك الرجوع الافتراضي
            super.onBackPressed()
        }
    }

    private val navListener: BottomNavigationView.OnNavigationItemSelectedListener =
        object : BottomNavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                val itemId = item.getItemId()

                if (itemId == R.id.nav_dashboard) {
                    navController!!.navigate(R.id.navigation_dashboard)
                    return true
                } else if (itemId == R.id.nav_add_account) {
                    navController!!.navigate(R.id.navigation_accounts)
                    return true
                } else if (itemId == R.id.nav_transactions) {
                    navController!!.navigate(R.id.transactionsFragment)
                    return true
                } else if (itemId == R.id.nav_reports) {
                    navController!!.navigate(R.id.navigation_reports)
                    return true
                }
                return false
            }
        }

    // دالة للتحكم في ظهور/إخفاء شريط التنقل السفلي
    fun setBottomNavigationVisibility(isVisible: Boolean) {
        binding!!.bottomNavigation.setVisibility(if (isVisible) View.VISIBLE else View.GONE)
    }

    val accountDao: AccountDao?
        get() = (getApplication() as App).getDatabase().accountDao()

    val transactionDao: TransactionDao?
        get() = (getApplication() as App).getDatabase().transactionDao()

    val pendingOperationDao: PendingOperationDao?
        get() = (getApplication() as App).getDatabase().pendingOperationDao()

    companion object {
        private const val TAG = "MainActivity"
    }
}