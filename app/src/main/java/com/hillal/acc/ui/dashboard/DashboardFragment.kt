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
import com.hillal.acc.databinding.FragmentDashboardBinding
import com.hillal.acc.ui.AccountStatementActivity
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
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
            db = app.database
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        db = AppDatabase.getInstance(requireContext())
        setupUI()
        return binding.root
    }

    private fun setupUI() {
        setupOtherSettings()
    }

    private fun setupOtherSettings() {
        // إعدادات أخرى
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "DashboardFragment onViewCreated started")
        try {
            setupClickListeners()
            observeData()
            updateUserName()
            migrationManager = MigrationManager(requireContext())
            migrationManager.migrateLocalData()
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
            appVersion = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
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

    private fun setupClickListeners() {
        binding.editProfileButton.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.editProfileFragment)
        }
        binding.addAccountButton.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.addAccountFragment)
        }
        binding.addTransactionButton.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.addTransactionFragment)
        }
        binding.reportButton.setOnClickListener {
            val intent = Intent(requireContext(), AccountStatementActivity::class.java)
            startActivity(intent)
        }
        binding.accountsCard.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.navigation_accounts)
        }
        binding.transactionsCard.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.transactionsFragment)
        }
        binding.reportsCard.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.navigation_reports)
        }
        binding.debtsCard.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.nav_summary)
        }
        binding.transferCard.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_dashboard_to_transfer)
        }
        binding.exchangeCard.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_dashboard_to_exchange)
        }
    }

    private fun observeData() {
        dashboardViewModel.totalDebtors.observe(viewLifecycleOwner) { total ->
            total?.let {
                binding.totalDebtors.text = String.format(Locale.US, "%d ", it.roundToInt())
            }
        }
        dashboardViewModel.totalCreditors.observe(viewLifecycleOwner) { total ->
            total?.let {
                binding.totalCreditors.text = String.format(Locale.US, "%d ", it.roundToInt())
            }
        }
        dashboardViewModel.accounts.observe(viewLifecycleOwner) { accounts ->
            accounts?.let {
                binding.totalAccounts.text = String.format(Locale.US, "%d", it.size)
            }
        }
        dashboardViewModel.netBalance.observe(viewLifecycleOwner) { balance ->
            balance?.let {
                binding.totalBalance.text = String.format(Locale.US, "%d يمني", balance.roundToInt())
            }
        }
    }

    private fun updateUserName() {
        val name: String = userPreferences.getUserName() ?: ""
        binding.userNameText.text = name
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
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun loadAccounts() {
        dashboardViewModel.accounts.observe(viewLifecycleOwner) { accounts ->
            // تحديث واجهة المستخدم بالحسابات الجديدة
            // يمكنك إضافة الكود الخاص بك هنا
        }
    }

    private fun loadTransactions() {
        dashboardViewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            // تحديث واجهة المستخدم بالمعاملات الجديدة
            // يمكنك إضافة الكود الخاص بك هنا
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog?.dismiss()
        progressDialog = null
        _binding = null
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
                    loadAccounts()
                    loadTransactions()
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
                            loadAccounts()
                            loadTransactions()
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

    companion object {
        private const val TAG = "DashboardFragment"
    }
} 