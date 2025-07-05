package com.hillal.acc.ui.dashboard

import com.hillal.acc.R

class DashboardFragment : androidx.fragment.app.Fragment() {
    private var binding: FragmentDashboardBinding? = null
    private var dashboardViewModel: DashboardViewModel? = null
    private var userPreferences: UserPreferences? = null
    private var progressDialog: ProgressDialog? = null
    private var db: AppDatabase? = null
    private var syncManager: SyncManager? = null
    private var migrationManager: MigrationManager? = null
    private var dataManager: DataManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        android.util.Log.d(DashboardFragment.Companion.TAG, "DashboardFragment onCreate started")

        try {
            // Initialize ViewModel
            val app: App = requireActivity().getApplication() as App
            db = app.getDatabase()
            val accountRepository: AccountRepository = AccountRepository(db.accountDao(), db)
            val transactionRepository =
                com.hillal.acc.data.repository.TransactionRepository((requireActivity().getApplication() as App).getDatabase())
            dashboardViewModel = ViewModelProvider(
                this,
                DashboardViewModelFactory(accountRepository, transactionRepository)
            )
                .get<DashboardViewModel>(DashboardViewModel::class.java)
            userPreferences = UserPreferences(requireContext())


            // Initialize SyncManager
            val dataManager: DataManager = DataManager(
                requireContext(),
                db.accountDao(),
                db.transactionDao(),
                db.pendingOperationDao()
            )
            this.dataManager = dataManager

            syncManager = SyncManager(
                requireContext(),
                dataManager,
                db.accountDao(),
                db.transactionDao(),
                db.pendingOperationDao()
            )

            android.util.Log.d(
                DashboardFragment.Companion.TAG,
                "DashboardViewModel initialized successfully"
            )
        } catch (e: java.lang.Exception) {
            android.util.Log.e(
                DashboardFragment.Companion.TAG,
                "Error initializing DashboardFragment: " + e.message,
                e
            )
            throw java.lang.RuntimeException("Failed to initialize DashboardFragment", e)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        db = AppDatabase.getInstance(requireContext())
        setupUI()
        return binding.getRoot()
    }

    private fun setupUI() {
        // إعدادات أخرى
        setupOtherSettings()
    }

    private fun setupOtherSettings() {
        // هنا يمكن إضافة إعدادات أخرى
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d(
            DashboardFragment.Companion.TAG,
            "DashboardFragment onViewCreated started"
        )

        try {
            setupClickListeners()
            observeData()
            updateUserName()
            migrationManager = MigrationManager(requireContext())

            // تشغيل الترحيل تلقائياً عند فتح الصفحة
            migrationManager.migrateLocalData()

            // Send user details to server
            sendUserDetailsToServer()

            // إغلاق التطبيق عند الضغط على زر الرجوع في هذه الصفحة
            requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                object : OnBackPressedCallback(true) {
                    public override fun handleOnBackPressed() {
                        requireActivity().finish() // يغلق التطبيق
                    }
                }
            )

            // ضبط insets للجذر لرفع المحتوى مع الكيبورد وأزرار النظام
            ViewCompat.setOnApplyWindowInsetsListener(
                view,
                androidx.core.view.OnApplyWindowInsetsListener { v: android.view.View?, insets: WindowInsetsCompat? ->
                    var bottom: Int =
                        insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
                    if (bottom == 0) {
                        bottom =
                            insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom
                    }
                    v!!.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        bottom
                    )
                    insets
                })
        } catch (e: java.lang.Exception) {
            android.util.Log.e(
                DashboardFragment.Companion.TAG,
                "Error in onViewCreated: " + e.message,
                e
            )
        }
    }

    private fun sendUserDetailsToServer() {
        val lastSeenTimestamp = java.lang.System.currentTimeMillis()
        var appVersion: kotlin.String? = "Unknown"
        try {
            appVersion = requireContext().getPackageManager()
                .getPackageInfo(requireContext().getPackageName(), 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            android.util.Log.e(
                DashboardFragment.Companion.TAG,
                "Error getting app version: " + e.message
            )
        }

        val deviceName: kotlin.String = Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")"

        val requestBody: JSONObject = JSONObject()
        try {
            requestBody.put("last_seen", lastSeenTimestamp)
            requestBody.put("android_version", appVersion)
            requestBody.put("device_name", deviceName)
        } catch (e: JSONException) {
            android.util.Log.e(
                DashboardFragment.Companion.TAG,
                "Error creating JSON for user details: " + e.message
            )
            return
        }

        if (dataManager != null) {
            dataManager.updateUserDetails(requestBody, object : ApiCallback {
                override fun onSuccess(updateInfo: ServerAppUpdateInfo?) {
                    android.util.Log.d(
                        DashboardFragment.Companion.TAG,
                        "User details updated successfully on server."
                    )
                }

                override fun onError(error: kotlin.String?) {
                    android.util.Log.e(
                        DashboardFragment.Companion.TAG,
                        "Failed to update user details on server: " + error
                    )
                }
            })
        } else {
            android.util.Log.e(DashboardFragment.Companion.TAG, "DataManager is not initialized.")
        }
    }

    private fun setupClickListeners() {
        // زر تعديل الملف الشخصي
        binding.editProfileButton.setOnClickListener({ v ->
            findNavController(requireView()).navigate(
                R.id.editProfileFragment
            )
        })

        // زر إضافة حساب جديد
        binding.addAccountButton.setOnClickListener({ v ->
            findNavController(requireView()).navigate(
                R.id.addAccountFragment
            )
        })

        // زر إضافة معاملة جديدة
        binding.addTransactionButton.setOnClickListener({ v ->
            findNavController(requireView()).navigate(
                R.id.addTransactionFragment
            )
        })

        // زر كشف الحساب (التقارير)
        binding.reportButton.setOnClickListener({ v ->
            val intent: Intent = Intent(requireContext(), AccountStatementActivity::class.java)
            startActivity(intent)
        })

        // بطاقات شبكة الروابط المختصرة
        binding.accountsCard.setOnClickListener({ v -> findNavController(requireView()).navigate(R.id.navigation_accounts) })

        binding.transactionsCard.setOnClickListener({ v ->
            findNavController(requireView()).navigate(
                R.id.transactionsFragment
            )
        })

        binding.reportsCard.setOnClickListener({ v -> findNavController(requireView()).navigate(R.id.navigation_reports) })

        binding.debtsCard.setOnClickListener({ v -> findNavController(requireView()).navigate(R.id.nav_summary) })

        binding.transferCard.setOnClickListener({ v -> findNavController(requireView()).navigate(R.id.action_dashboard_to_transfer) })

        binding.exchangeCard.setOnClickListener({ v -> findNavController(requireView()).navigate(R.id.action_dashboard_to_exchange) })
    }

    private fun observeData() {
        dashboardViewModel!!.getTotalDebtors()
            .observe(getViewLifecycleOwner(), androidx.lifecycle.Observer { total: kotlin.Double? ->
                if (total != null) {
                    binding.totalDebtors.setText(
                        kotlin.String.format(
                            java.util.Locale.US,
                            "%d ",
                            java.lang.Math.round(total).toInt()
                        )
                    )
                }
            })

        dashboardViewModel!!.getTotalCreditors()
            .observe(getViewLifecycleOwner(), androidx.lifecycle.Observer { total: kotlin.Double? ->
                if (total != null) {
                    binding.totalCreditors.setText(
                        kotlin.String.format(
                            java.util.Locale.US,
                            "%d ",
                            java.lang.Math.round(total).toInt()
                        )
                    )
                }
            })

        dashboardViewModel!!.getAccounts().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { accounts: kotlin.collections.MutableList<com.hillal.acc.data.model.Account?>? ->
                if (accounts != null) {
                    binding.totalAccounts.setText(
                        kotlin.String.format(
                            java.util.Locale.US,
                            "%d",
                            accounts.size
                        )
                    )
                }
            })

        dashboardViewModel!!.getNetBalance().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { balance: kotlin.Double? ->
                if (balance != null) {
                    binding.totalBalance.setText(
                        kotlin.String.format(
                            java.util.Locale.US,
                            "%d يمني",
                            java.lang.Math.round(balance).toInt()
                        )
                    )
                }
            })
    }

    private fun updateUserName() {
        val userName = userPreferences.getUserName()
        if (!userName.isEmpty()) {
            binding.userNameText.setText(userName)
        }
    }

    private fun showLoadingDialog(message: kotlin.String?) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(requireContext())
            progressDialog.setCancelable(false)
        }
        progressDialog.setMessage(message)
        progressDialog.show()
    }

    private fun hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss()
        }
    }

    private fun showSuccessDialog(message: kotlin.String?) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("نجاح")
            .setMessage(message)
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun showErrorSnackbar(message: kotlin.String?) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show()
    }

    private fun loadAccounts() {
        dashboardViewModel!!.getAccounts().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { accounts: kotlin.collections.MutableList<com.hillal.acc.data.model.Account?>? -> })
    }

    private fun loadTransactions() {
        dashboardViewModel!!.getTransactions().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { transactions: kotlin.collections.MutableList<com.hillal.acc.data.model.Transaction?>? -> })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (progressDialog != null) {
            progressDialog.dismiss()
            progressDialog = null
        }
        binding = null
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_dashboard, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): kotlin.Boolean {
        if (item.getItemId() == R.id.action_full_sync) {
            showLoadingDialog("جاري المزامنة الكاملة...")
            syncManager.performFullSync(object : SyncCallback {
                override fun onSuccess() {
                    hideLoadingDialog()
                    showSuccessDialog("تمت المزامنة الكاملة بنجاح")
                    loadAccounts()
                    loadTransactions()
                }

                override fun onError(error: kotlin.String?) {
                    hideLoadingDialog()
                    showErrorSnackbar("فشلت المزامنة الكاملة: " + error)
                }
            })
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        // تحقق إذا كانت القاعدة فارغة (لا يوجد حسابات ولا صناديق)
        java.lang.Thread(java.lang.Runnable {
            val noAccounts = db.accountDao().getAllAccountsSync().isEmpty()
            val noCashboxes = db.cashboxDao().getAllSync().isEmpty()
            if (noAccounts && noCashboxes) {
                requireActivity().runOnUiThread(java.lang.Runnable {
                    showLoadingDialog("جاري المزامنة التلقائية...")
                    syncManager.performFullSync(object : SyncCallback {
                        override fun onSuccess() {
                            hideLoadingDialog()
                            showSuccessDialog("تمت المزامنة التلقائية بنجاح")
                            loadAccounts()
                            loadTransactions()
                        }

                        override fun onError(error: kotlin.String?) {
                            hideLoadingDialog()
                            showErrorSnackbar("فشلت المزامنة التلقائية: " + error)
                        }
                    })
                })
            }
        }).start()
    }

    companion object {
        private const val TAG = "DashboardFragment"
    }
}