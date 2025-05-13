import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.accountingapp.R
import com.example.accountingapp.databinding.ActivityMainBinding
import com.example.accountingapp.web.WebAppInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webAppInterface: WebAppInterface
    private lateinit var db: AppDatabase
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        webAppInterface = WebAppInterface(this)
        db = AppDatabase.getDatabase(this)
        webView = binding.webView

        // ... existing code ...

        binding.loginButton.setOnClickListener {
            // ... existing login logic ...
        }

        binding.registerButton.setOnClickListener {
            // ... existing register logic ...
        }

        // ... existing code ...
    }

    private fun onLoginSuccess() {
        // حفظ حالة تسجيل الدخول
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("username", username)
            putString("password", password)
            apply()
        }
        
        // مزامنة البيانات من السيرفر
        lifecycleScope.launch {
            try {
                syncData()
                // بعد نجاح المزامنة، انتقل للصفحة الرئيسية
                loadMainPage()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error syncing data: ${e.message}")
                // حتى في حالة فشل المزامنة، انتقل للصفحة الرئيسية
                loadMainPage()
            }
        }
    }

    private suspend fun syncData() {
        withContext(Dispatchers.IO) {
            try {
                // 1. مزامنة الحسابات
                val accountsResponse = webAppInterface.getAccounts()
                if (accountsResponse.isSuccessful) {
                    val accounts = accountsResponse.body()
                    accounts?.let { accountList ->
                        // حفظ الحسابات في قاعدة البيانات المحلية
                        db.accountDao().insertAll(accountList)
                    }
                }

                // 2. مزامنة القيود المحاسبية
                val entriesResponse = webAppInterface.getEntries()
                if (entriesResponse.isSuccessful) {
                    val entries = entriesResponse.body()
                    entries?.let { entryList ->
                        // حفظ القيود في قاعدة البيانات المحلية
                        db.entryDao().insertAll(entryList)
                    }
                }

                // 3. مزامنة أي بيانات أخرى ضرورية
                // يمكن إضافة المزيد من عمليات المزامنة هنا

                Log.d("MainActivity", "Data sync completed successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during data sync: ${e.message}")
                throw e
            }
        }
    }

    private fun loadMainPage() {
        // تحميل الصفحة الرئيسية
        webView.loadUrl("file:///android_asset/index.html")
    }
} 