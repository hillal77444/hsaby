package com.hillal.acc.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.viewmodel.AccountViewModel
import com.hillal.acc.viewmodel.TransactionViewModel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountStatementFragment : Fragment() {
    private var accountViewModel: AccountViewModel? = null
    private var transactionViewModel: TransactionViewModel? = null
    private var fromDateInput: TextInputEditText? = null
    private var toDateInput: TextInputEditText? = null
    private var transactionsRecyclerView: RecyclerView? = null
    private var transactionsAdapter: TransactionsAdapter? = null
    private var accountNameText: TextView? = null
    private var accountBalanceText: TextView? = null
    private var totalDebitText: TextView? = null
    private var totalCreditText: TextView? = null
    private var accountId: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountViewModel =
            ViewModelProvider(this).get<AccountViewModel>(AccountViewModel::class.java)
        transactionViewModel = ViewModelProvider(this).get<TransactionViewModel>(
            TransactionViewModel::class.java
        )
        if (getArguments() != null) {
            accountId = getArguments()!!.getLong("accountId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account_statement, container, false)

        fromDateInput = view.findViewById<TextInputEditText>(R.id.fromDateInput)
        toDateInput = view.findViewById<TextInputEditText>(R.id.toDateInput)
        transactionsRecyclerView = view.findViewById<RecyclerView>(R.id.transactionsRecyclerView)
        accountNameText = view.findViewById<TextView>(R.id.accountNameText)
        accountBalanceText = view.findViewById<TextView>(R.id.accountBalanceText)
        totalDebitText = view.findViewById<TextView>(R.id.totalDebitText)
        totalCreditText = view.findViewById<TextView>(R.id.totalCreditText)

        transactionsAdapter = TransactionsAdapter()
        transactionsRecyclerView!!.setLayoutManager(LinearLayoutManager(getContext()))
        transactionsRecyclerView!!.setAdapter(transactionsAdapter)

        view.findViewById<View?>(R.id.generateStatementButton)
            .setOnClickListener(View.OnClickListener { v: View? -> generateStatement() })
        view.findViewById<View?>(R.id.exportPdfButton)
            .setOnClickListener(View.OnClickListener { v: View? -> exportToPdf() })

        loadAccountData()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ضبط insets للجذر لرفع المحتوى فوق أزرار النظام أو الشريط السفلي
        ViewCompat.setOnApplyWindowInsetsListener(
            view,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val bottom = insets!!.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                v!!.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottom
                )
                insets
            })
    }

    private fun loadAccountData() {
        accountViewModel!!.getAccountById(accountId)
            .observe(getViewLifecycleOwner(), Observer { account: Account? ->
                if (account != null) {
                    accountNameText!!.setText(account.getName())
                    accountBalanceText!!.setText(
                        String.format(
                            "الرصيد: %.2f",
                            account.getBalance()
                        )
                    )
                    loadTransactions()
                }
            })
    }

    private fun loadTransactions() {
        val fromDateStr =
            if (fromDateInput!!.getText() != null) fromDateInput!!.getText().toString() else ""
        val toDateStr =
            if (toDateInput!!.getText() != null) toDateInput!!.getText().toString() else ""

        val fromDate = parseDate(fromDateStr)
        val toDate = parseDate(toDateStr)

        if (fromDate != null && toDate != null) {
            transactionViewModel!!.getTransactionsByAccountAndDateRange(
                accountId,
                fromDate.getTime(),
                toDate.getTime()
            )
                .observe(
                    getViewLifecycleOwner(),
                    Observer { transactions: MutableList<Transaction?>? ->
                        this.updateTransactions(transactions)
                    })
        } else {
            transactionViewModel!!.getTransactionsByAccount(accountId)
                .observe(
                    getViewLifecycleOwner(),
                    Observer { transactions: MutableList<Transaction?>? ->
                        this.updateTransactions(transactions)
                    })
        }
    }

    private fun updateTransactions(transactions: MutableList<Transaction>) {
        transactionsAdapter!!.submitList(transactions)

        var totalDebit = 0.0
        var totalCredit = 0.0

        for (transaction in transactions) {
            if (transaction.getType() == "debit") {
                totalDebit += transaction.getAmount()
            } else {
                totalCredit += transaction.getAmount()
            }
        }

        totalDebitText!!.setText(String.format("إجمالي المدين: %.2f", totalDebit))
        totalCreditText!!.setText(String.format("إجمالي الدائن: %.2f", totalCredit))
    }

    private fun generateStatement() {
        val fromDateStr = fromDateInput!!.getText().toString()
        val toDateStr = toDateInput!!.getText().toString()

        if (fromDateStr.isEmpty() || toDateStr.isEmpty()) {
            return
        }

        val fromDate = parseDate(fromDateStr)
        val toDate = parseDate(toDateStr)

        if (fromDate != null && toDate != null) {
            transactionViewModel!!.getTransactionsByDateRange(fromDate.getTime(), toDate.getTime())
                .observe(
                    getViewLifecycleOwner(),
                    Observer { transactions: MutableList<Transaction>? ->
                        transactionsAdapter!!.submitList(transactions)
                        updateSummary(transactions!!)
                    })
        }
    }

    private fun updateSummary(transactions: MutableList<Transaction>) {
        var totalDebit = 0.0
        var totalCredit = 0.0

        for (transaction in transactions) {
            if ("debit" == transaction.getType()) {
                totalDebit += transaction.getAmount()
            } else {
                totalCredit += transaction.getAmount()
            }
        }

        totalDebitText!!.setText(String.format("إجمالي المدين: %.2f", totalDebit))
        totalCreditText!!.setText(String.format("إجمالي الدائن: %.2f", totalCredit))
    }

    private fun exportToPdf() {
        // TODO: Implement PDF export functionality
    }

    private fun parseDate(dateStr: String?): Date? {
        if (dateStr == null || dateStr.isEmpty()) return null

        try {
            return dateFormat.parse(dateStr)
        } catch (e: ParseException) {
            return null
        }
    }

    companion object {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }
}