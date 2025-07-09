package com.hillal.acc.ui.reports

import android.content.Intent
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
import androidx.navigation.Navigation.findNavController
import com.google.android.material.button.MaterialButton
import com.hillal.acc.R
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.ui.AccountStatementActivity
import com.hillal.acc.viewmodel.AccountViewModel
import com.hillal.acc.viewmodel.TransactionViewModel
import java.util.Locale

// تم تحويل هذه الشاشة إلى Compose (انظر ReportsScreen.kt). يمكن حذف هذا الملف لاحقًا بعد التأكد من عمل الشاشة الجديدة.
class ReportsFragment : Fragment() {
    private var accountViewModel: AccountViewModel? = null
    private var transactionViewModel: TransactionViewModel? = null
    private var totalDebtorsText: TextView? = null
    private var totalCreditorsText: TextView? = null
    private var netBalanceText: TextView? = null
    private var totalTransactionsText: TextView? = null
    private var averageTransactionText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_reports, container, false)


        // Initialize ViewModels
        accountViewModel =
            ViewModelProvider(this).get(AccountViewModel::class.java)
        transactionViewModel = ViewModelProvider(this).get<TransactionViewModel>(
            TransactionViewModel::class.java
        )


        // Initialize views
        totalDebtorsText = view.findViewById<TextView>(R.id.total_debtors)
        totalCreditorsText = view.findViewById<TextView>(R.id.total_creditors)
        netBalanceText = view.findViewById<TextView>(R.id.net_balance)
        totalTransactionsText = view.findViewById<TextView>(R.id.total_transactions)
        averageTransactionText = view.findViewById<TextView>(R.id.average_transaction)


        // ربط زر كشف الحساب التفصيلي
        val viewAccountStatementButton =
            view.findViewById<MaterialButton>(R.id.viewAccountStatementButton)
        viewAccountStatementButton.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(getActivity(), AccountStatementActivity::class.java)
            startActivity(intent)
        })


        // زر تقرير ملخص الحسابات
        val btnAccountsSummaryReport =
            view.findViewById<MaterialButton>(R.id.btnAccountsSummaryReport)
        btnAccountsSummaryReport.setOnClickListener(View.OnClickListener { v: View? ->
            findNavController(
                v!!
            )
                .navigate(R.id.action_reports_to_accountsSummaryReport)
        })

        // زر كشف الصندوق
        val btnCashboxStatement = view.findViewById<MaterialButton>(R.id.btnCashboxStatement)
        btnCashboxStatement.setOnClickListener(View.OnClickListener { v: View? ->
            findNavController(
                v!!
            )
                .navigate(R.id.cashboxStatementFragment)
        })


        // Observe data
        observeTransactions()

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

    private fun observeTransactions() {
        transactionViewModel!!.getAllTransactions()
            .observe(getViewLifecycleOwner(), Observer { transactions: MutableList<Transaction>? ->
                var totalDebit = 0.0
                var totalCredit = 0.0
                var count = 0
                var sum = 0.0
                if (transactions != null) {
                    for (transaction in transactions) {
                        if ("debit" == transaction.getType()) {
                            totalDebit += transaction.getAmount()
                        } else if ("credit" == transaction.getType()) {
                            totalCredit += transaction.getAmount()
                        }
                        sum += transaction.getAmount()
                        count++
                    }
                }
                totalDebtorsText!!.setText(
                    String.format(
                        Locale.ENGLISH,
                        "إجمالي المدينين: %.2f",
                        totalDebit
                    )
                )
                totalCreditorsText!!.setText(
                    String.format(
                        Locale.ENGLISH,
                        "إجمالي الدائنين: %.2f",
                        totalCredit
                    )
                )
                netBalanceText!!.setText(
                    String.format(
                        Locale.ENGLISH,
                        "الرصيد: %.2f",
                        totalCredit - totalDebit
                    )
                )
                totalTransactionsText!!.setText(String.format(Locale.ENGLISH, "%d", count))
                averageTransactionText!!.setText(
                    if (count > 0) String.format(
                        Locale.ENGLISH,
                        "%.2f",
                        sum / count
                    ) else "0"
                )
            })
    }
}