package com.hillal.acc.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hillal.acc.App
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.ui.accounts.AccountViewModel
import com.hillal.acc.ui.accounts.AccountViewModelFactory
import com.hillal.acc.ui.transactions.TransactionsViewModel
import java.util.Locale

class AccountsSummaryReportFragment : Fragment() {
    private var btnFilterYER: Button? = null
    private var btnFilterSAR: Button? = null
    private var btnFilterUSD: Button? = null
    private var recyclerView: RecyclerView? = null
    private var tvTotalBalance: TextView? = null
    private var tvTotalCredit: TextView? = null
    private var tvTotalDebit: TextView? = null
    private var adapter: AccountsSummaryAdapter? = null
    private var allAccounts: MutableList<Account> = ArrayList<Account>()
    private var allTransactions: MutableList<Transaction> = ArrayList<Transaction>()
    private var selectedCurrency = "يمني"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_accounts_summary_report, container, false)
        btnFilterYER = view.findViewById<Button>(R.id.btnFilterYER)
        btnFilterSAR = view.findViewById<Button>(R.id.btnFilterSAR)
        btnFilterUSD = view.findViewById<Button>(R.id.btnFilterUSD)
        recyclerView = view.findViewById<RecyclerView>(R.id.accountsSummaryRecyclerView)
        tvTotalBalance = view.findViewById<TextView>(R.id.tvTotalBalance)
        tvTotalCredit = view.findViewById<TextView>(R.id.tvTotalCredit)
        tvTotalDebit = view.findViewById<TextView>(R.id.tvTotalDebit)
        recyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        adapter = AccountsSummaryAdapter(ArrayList<AccountsSummaryAdapter.AccountSummary>())
        recyclerView!!.setAdapter(adapter)
        setupFilterButtons()
        loadData()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ضبط insets للجذر لرفع المحتوى مع الكيبورد وأزرار النظام
        ViewCompat.setOnApplyWindowInsetsListener(
            view,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                var bottom = insets!!.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (bottom == 0) {
                    bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                }
                v!!.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottom
                )
                insets
            })
    }

    private fun setupFilterButtons() {
        btnFilterYER!!.setOnClickListener(View.OnClickListener { v: View? ->
            selectedCurrency = "يمني"
            updateFilterButtons()
            loadData() // إعادة تحميل البيانات حسب العملة الجديدة
        })
        btnFilterSAR!!.setOnClickListener(View.OnClickListener { v: View? ->
            selectedCurrency = "سعودي"
            updateFilterButtons()
            loadData() // إعادة تحميل البيانات حسب العملة الجديدة
        })
        btnFilterUSD!!.setOnClickListener(View.OnClickListener { v: View? ->
            selectedCurrency = "دولار"
            updateFilterButtons()
            loadData() // إعادة تحميل البيانات حسب العملة الجديدة
        })
        updateFilterButtons()
    }

    private fun updateFilterButtons() {
        btnFilterYER!!.setBackgroundTintList(
            getResources().getColorStateList(
                if (selectedCurrency == "يمني") R.color.teal_700 else R.color.light_gray,
                null
            )
        )
        btnFilterYER!!.setTextColor(
            getResources().getColor(
                if (selectedCurrency == "يمني") android.R.color.white else R.color.primary_text,
                null
            )
        )
        btnFilterSAR!!.setBackgroundTintList(
            getResources().getColorStateList(
                if (selectedCurrency == "سعودي") R.color.teal_700 else R.color.light_gray,
                null
            )
        )
        btnFilterSAR!!.setTextColor(
            getResources().getColor(
                if (selectedCurrency == "سعودي") android.R.color.white else R.color.primary_text,
                null
            )
        )
        btnFilterUSD!!.setBackgroundTintList(
            getResources().getColorStateList(
                if (selectedCurrency == "دولار") R.color.teal_700 else R.color.light_gray,
                null
            )
        )
        btnFilterUSD!!.setTextColor(
            getResources().getColor(
                if (selectedCurrency == "دولار") android.R.color.white else R.color.primary_text,
                null
            )
        )
    }

    private fun loadData() {
        val app = requireActivity().getApplication() as App
        val factory = AccountViewModelFactory(app.getAccountRepository())
        val accountViewModel = ViewModelProvider(requireActivity(), factory).get<AccountViewModel>(
            AccountViewModel::class.java
        )
        val transactionsViewModel =
            ViewModelProvider(requireActivity()).get<TransactionsViewModel>(TransactionsViewModel::class.java)
        
        // مراقبة الحسابات
        accountViewModel.allAccounts.observe(
            getViewLifecycleOwner(),
            Observer { accounts: List<Account> ->
                allAccounts = accounts.toMutableList()
                updateReport()
            })
        
        // استخدام استعلام محسن للحصول على المعاملات حسب العملة المحددة
        transactionsViewModel.loadTransactionsByCurrency(selectedCurrency)
        
        // مراقبة المعاملات المحسنة
        transactionsViewModel.getTransactions()
            .observe(getViewLifecycleOwner(), Observer { transactions: MutableList<Transaction>? ->
                allTransactions = transactions?.toMutableList() ?: mutableListOf()
                updateReport()
            })
    }

    private fun updateReport() {
        val summaryList: MutableList<AccountsSummaryAdapter.AccountSummary> =
            ArrayList<AccountsSummaryAdapter.AccountSummary>()
        var totalCredit = 0.0
        var totalDebit = 0.0
        var totalBalance = 0.0
        
        // استخدام الاستعلامات المحسنة بدلاً من الحساب في الذاكرة
        for (account in allAccounts) {
            var credit = 0.0
            var debit = 0.0
            
            // فلترة المعاملات حسب الحساب والعملة - محسن
            val accountTransactions = allTransactions.filter { 
                it.getAccountId() == account.getId() && selectedCurrency == it.getCurrency() 
            }
            
            for (t in accountTransactions) {
                if (t.getType().equals("credit", ignoreCase = true) || t.getType() == "له") {
                    credit += t.getAmount()
                } else {
                    debit += t.getAmount()
                }
            }
            
            val balance = credit - debit
            summaryList.add(
                AccountsSummaryAdapter.AccountSummary(
                    account.getName(),
                    credit,
                    debit,
                    balance
                )
            )
            totalCredit += credit
            totalDebit += debit
            totalBalance += balance
        }
        
        adapter!!.setData(summaryList)
        tvTotalCredit!!.setText(String.format(Locale.US, "له: %,.0f", totalCredit))
        tvTotalDebit!!.setText(String.format(Locale.US, "عليه: %,.0f", totalDebit))
        tvTotalBalance!!.setText(String.format(Locale.US, "الرصيد: %,.0f", totalBalance))
    }
}