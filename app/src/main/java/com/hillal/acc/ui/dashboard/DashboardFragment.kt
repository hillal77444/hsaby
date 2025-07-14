package com.hillal.acc.ui.dashboard

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.hillal.acc.R
import com.hillal.acc.App
import com.hillal.acc.data.preferences.UserPreferences
import com.hillal.acc.data.remote.DataManager
import com.hillal.acc.data.repository.AccountRepository
import com.hillal.acc.data.repository.TransactionRepository
import com.hillal.acc.data.room.AppDatabase
import com.hillal.acc.data.sync.MigrationManager
import com.hillal.acc.data.sync.SyncManager
import com.hillal.acc.data.model.ServerAppUpdateInfo

import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.platform.ComposeView
import com.hillal.acc.data.room.AccountDao
import com.hillal.acc.data.room.TransactionDao
import com.hillal.acc.data.room.PendingOperationDao
import com.hillal.acc.ui.AccountStatementComposeActivity
import com.hillal.acc.util.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

class DashboardFragment : Fragment() {
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var userPreferences: UserPreferences
    private var progressDialog: ProgressDialog? = null
    private lateinit var db: AppDatabase
    private lateinit var syncManager: SyncManager
    private lateinit var migrationManager: MigrationManager
    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Log.d(TAG, "DashboardFragment onCreate started")
        try {
            val app = requireActivity().application as App
            db = app.getDatabase()
            val accountRepository = AccountRepository(db.accountDao(), db)
            val transactionRepository = TransactionRepository(db)
            dashboardViewModel = ViewModelProvider(this, DashboardViewModelFactory(accountRepository, transactionRepository))
                .get(DashboardViewModel::class.java)
            userPreferences = UserPreferences(requireContext())
            dataManager = DataManager(
                requireContext(),
                db.accountDao(),
                db.transactionDao(),
                db.pendingOperationDao()
            )
            syncManager = SyncManager(
                requireContext(),
                dataManager,
                db.accountDao(),
                db.transactionDao(),
                db.pendingOperationDao()
            )
            Log.d(TAG, "DashboardViewModel initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing DashboardFragment: ${e.message}", e)
            throw RuntimeException("Failed to initialize DashboardFragment", e)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    userName = userPreferences.getUserName() ?: "",
                    onEditProfile = { Navigation.findNavController(requireView()).navigate(R.id.editProfileFragment) },
                    onAddAccount = { Navigation.findNavController(requireView()).navigate(R.id.addAccountFragment) },
                    onAddTransaction = { Navigation.findNavController(requireView()).navigate(R.id.addTransactionFragment) },
                    onReport = { startActivity(Intent(requireContext(), AccountStatementComposeActivity::class.java)) },
                    onAccounts = { Navigation.findNavController(requireView()).navigate(R.id.navigation_accounts) },
                    onTransactions = { Navigation.findNavController(requireView()).navigate(R.id.transactionsFragment) },
                    onReports = { Navigation.findNavController(requireView()).navigate(R.id.navigation_reports) },
                    onDebts = { Navigation.findNavController(requireView()).navigate(R.id.nav_summary) },
                    onTransfer = {
                        Navigation.findNavController(requireView()).navigate(R.id.transferFragment)
                    },
                    onExchange = { Navigation.findNavController(requireView()).navigate(R.id.action_dashboard_to_exchange) }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "DashboardFragment onViewCreated started")
        try {
            migrationManager = MigrationManager(requireContext())
            val preferencesManager = PreferencesManager(requireContext())
            val sessionExpiry = preferencesManager.getSessionExpiry()
            if (isSubscriptionExpired(sessionExpiry)) {
                Toast.makeText(requireContext(), "انتهى الاشتراك، لم يتم ترحيل أي بيانات. يرجى تجديد الاشتراك للاستمتاع بمزايا المزامنة.", Toast.LENGTH_LONG).show()
            } else {
                migrationManager.migrateLocalData()
            }
            sendUserDetailsToServer()
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            })
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                var bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
                if (bottom == 0) {
                    bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom
                }
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottom)
                insets
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated: ${e.message}", e)
        }
    }

    private fun sendUserDetailsToServer() {
        val lastSeenTimestamp = System.currentTimeMillis()
        var appVersion = "Unknown"
        try {
            appVersion = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error getting app version: ${e.message}")
        }
        val deviceName = "${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
        val requestBody = JSONObject()
        try {
            requestBody.put("last_seen", lastSeenTimestamp)
            requestBody.put("android_version", appVersion)
            requestBody.put("device_name", deviceName)
        } catch (e: JSONException) {
            Log.e(TAG, "Error creating JSON for user details: ${e.message}")
            return
        }
        dataManager.updateUserDetails(requestBody, object : DataManager.ApiCallback {
            override fun onSuccess(updateInfo: ServerAppUpdateInfo?) {
                Log.d(TAG, "User details updated successfully on server.")
            }
            override fun onError(error: String) {
                Log.e(TAG, "Failed to update user details on server: $error")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_dashboard, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_full_sync) {
            showLoadingDialog("جاري المزامنة الكاملة...")
            syncManager.performFullSync(object : SyncManager.SyncCallback {
                override fun onSuccess() {
                    hideLoadingDialog()
                    showSuccessDialog("تمت المزامنة الكاملة بنجاح")
                }
                override fun onError(error: String) {
                    hideLoadingDialog()
                    showErrorSnackbar("فشلت المزامنة الكاملة: $error")
                }
            })
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        Thread {
            val noAccounts = db.accountDao().getAllAccountsSync().isEmpty()
            val noCashboxes = db.cashboxDao().getAllSync().isEmpty()
            if (noAccounts && noCashboxes) {
                requireActivity().runOnUiThread {
                    showLoadingDialog("جاري المزامنة التلقائية...")
                    syncManager.performFullSync(object : SyncManager.SyncCallback {
                        override fun onSuccess() {
                            hideLoadingDialog()
                            showSuccessDialog("تمت المزامنة التلقائية بنجاح")
                        }
                        override fun onError(error: String) {
                            hideLoadingDialog()
                            showErrorSnackbar("فشلت المزامنة التلقائية: $error")
                        }
                    })
                }
            }
        }.start()
    }

    private fun showLoadingDialog(message: String) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(requireContext())
            progressDialog?.setCancelable(false)
        }
        progressDialog?.setMessage(message)
        progressDialog?.show()
    }

    private fun hideLoadingDialog() {
        progressDialog?.takeIf { it.isShowing }?.dismiss()
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("نجاح")
            .setMessage(message)
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }

    private fun isSubscriptionExpired(sessionExpiry: String?): Boolean {
        if (sessionExpiry.isNullOrBlank()) return false
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
            val expiry = parser.parse(sessionExpiry)
            val now = Date()
            expiry != null && now.before(expiry).not()
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "DashboardFragment"
    }
} 