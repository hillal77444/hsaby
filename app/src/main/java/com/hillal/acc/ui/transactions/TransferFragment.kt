package com.hillal.acc.ui.transactions

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.hillal.acc.App
import com.hillal.acc.R
import com.hillal.acc.data.entities.Cashbox
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.repository.TransactionRepository
import com.hillal.acc.ui.common.AccountPickerBottomSheet
import com.hillal.acc.viewmodel.AccountViewModel
import com.hillal.acc.viewmodel.CashboxViewModel

class TransferFragment : Fragment() {
    private var fromAccountAutoComplete: TextInputEditText? = null
    private var toAccountAutoComplete: TextInputEditText? = null
    private var fromAccountBalanceText: TextView? = null
    private var toAccountBalanceText: TextView? = null
    private var cashboxSpinner: Spinner? = null
    private var currencySpinner: Spinner? = null
    private var amountEditText: EditText? = null
    private var notesEditText: EditText? = null
    private var accounts: MutableList<Account?>? = null
    private var cashboxes: MutableList<Cashbox> = ArrayList<Cashbox>()
    private var fromAccount: Account? = null
    private var toAccount: Account? = null
    private var selectedCashboxId: Long = -1
    private var currencies: Array<String?>
    private var currencyAdapter: ArrayAdapter<String?>? = null
    private var cashboxAdapter: ArrayAdapter<String?>? = null
    private var accountBalancesMap: MutableMap<Long?, MutableMap<String?, Double?>?> =
        HashMap<Long?, MutableMap<String?, Double?>?>()
    private var cashboxViewModel: CashboxViewModel? = null
    private var accountViewModel: AccountViewModel? = null
    private var transactionRepository: TransactionRepository? = null
    private var transactionsViewModel: TransactionsViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transfer, container, false)
        fromAccountAutoComplete = view.findViewById<TextInputEditText>(R.id.fromAccountAutoComplete)
        toAccountAutoComplete = view.findViewById<TextInputEditText>(R.id.toAccountAutoComplete)
        fromAccountBalanceText = view.findViewById<TextView>(R.id.fromAccountBalanceText)
        toAccountBalanceText = view.findViewById<TextView>(R.id.toAccountBalanceText)
        cashboxSpinner = view.findViewById<Spinner>(R.id.cashboxSpinner)
        currencySpinner = view.findViewById<Spinner>(R.id.currencySpinner)
        amountEditText = view.findViewById<EditText>(R.id.amountEditText)
        notesEditText = view.findViewById<EditText>(R.id.notesEditText)
        fromAccountAutoComplete!!.setFocusable(false)
        fromAccountAutoComplete!!.setOnClickListener(View.OnClickListener { v: View? ->
            openAccountPicker(
                true
            )
        })
        toAccountAutoComplete!!.setFocusable(false)
        toAccountAutoComplete!!.setOnClickListener(View.OnClickListener { v: View? ->
            openAccountPicker(
                false
            )
        })
        accountViewModel =
            ViewModelProvider(this).get<AccountViewModel>(AccountViewModel::class.java)
        cashboxViewModel =
            ViewModelProvider(this).get<CashboxViewModel>(CashboxViewModel::class.java)
        val accountRepository = (requireActivity().getApplication() as App).getAccountRepository()
        transactionRepository =
            (requireActivity().getApplication() as App).getTransactionRepository()
        currencies = getResources().getStringArray(R.array.currencies_array)
        currencyAdapter = ArrayAdapter<String?>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            currencies
        )
        currencyAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner!!.setAdapter(currencyAdapter)
        accountViewModel!!.getAllAccounts()
            .observe(getViewLifecycleOwner(), Observer { accs: MutableList<Account?>? ->
                accounts = accs
            })
        cashboxViewModel!!.getAllCashboxes()
            .observe(getViewLifecycleOwner(), Observer { cbList: MutableList<Cashbox?>? ->
                cashboxes = if (cbList != null) cbList.filterNotNull().toMutableList() else ArrayList<Cashbox>()
                val names: MutableList<String?> = ArrayList<String?>()
                for (cb in cashboxes) names.add(cb.getName())
                cashboxAdapter = ArrayAdapter<String?>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    names
                )
                cashboxAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                cashboxSpinner!!.setAdapter(cashboxAdapter)
                if (!cashboxes.isEmpty()) {
                    cashboxSpinner!!.setSelection(0)
                    selectedCashboxId = cashboxes.get(0).getId()
                }
            })
        cashboxSpinner!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view1: View?,
                position: Int,
                id: Long
            ) {
                if (position >= 0 && position < cashboxes.size) {
                    selectedCashboxId = cashboxes.get(position).getId()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
        currencySpinner!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view1: View?,
                position: Int,
                id: Long
            ) {
                updateBalances()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
        amountEditText!!.addTextChangedListener(SimpleTextWatcher(Runnable { this.updateBalances() }))
        transactionsViewModel =
            ViewModelProvider(this).get(TransactionsViewModel::class.java)
        transactionsViewModel!!.getAccountBalancesMap().observe(
            getViewLifecycleOwner(),
            Observer { balancesMap: MutableMap<Long?, MutableMap<String?, Double?>?>? ->
                accountBalancesMap =
                    if (balancesMap != null) balancesMap else HashMap<Long?, MutableMap<String?, Double?>?>()
                updateBalances()
            })
        val transferButton = view.findViewById<Button>(R.id.transferButton)
        transferButton.setOnClickListener(View.OnClickListener { v: View? -> performTransfer() })
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun openAccountPicker(isFrom: Boolean) {
        val picker = AccountPickerBottomSheet(
            accounts,
            ArrayList<Transaction?>(),
            accountBalancesMap,
            AccountPickerBottomSheet.OnAccountSelectedListener { account: Account? ->
                if (isFrom) {
                    fromAccount = account
                    fromAccountAutoComplete!!.setText(account!!.getName())
                } else {
                    toAccount = account
                    toAccountAutoComplete!!.setText(account!!.getName())
                }
                updateBalances()
            })
        picker.show(
            getParentFragmentManager(),
            if (isFrom) "from_account_picker" else "to_account_picker"
        )
    }

    private fun updateBalances() {
        val currency = this.selectedCurrency
        if (fromAccount != null) {
            val accountId = fromAccount!!.getId()
            var bal = 0.0
            if (accountBalancesMap.containsKey(accountId) && accountBalancesMap.get(accountId)!!
                    .containsKey(currency)
            ) {
                bal = accountBalancesMap.get(accountId)!!.get(currency)!!
            }
            fromAccountBalanceText!!.setText("الرصيد: " + bal + " " + currency)
        } else {
            fromAccountBalanceText!!.setText("الرصيد: -")
        }
        if (toAccount != null) {
            val accountId = toAccount!!.getId()
            var bal = 0.0
            if (accountBalancesMap.containsKey(accountId) && accountBalancesMap.get(accountId)!!
                    .containsKey(currency)
            ) {
                bal = accountBalancesMap.get(accountId)!!.get(currency)!!
            }
            toAccountBalanceText!!.setText("الرصيد: " + bal + " " + currency)
        } else {
            toAccountBalanceText!!.setText("الرصيد: -")
        }
    }

    private val selectedCurrency: String
        get() = if (currencySpinner!!.getSelectedItem() != null) currencySpinner!!.getSelectedItem()
            .toString() else ""

    private fun performTransfer() {
        if (fromAccount == null || toAccount == null) {
            Toast.makeText(getContext(), "يرجى اختيار الحسابين", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCashboxId == -1L) {
            Toast.makeText(getContext(), "يرجى اختيار الصندوق", Toast.LENGTH_SHORT).show()
            return
        }
        val currency = this.selectedCurrency
        val amountStr = amountEditText!!.getText().toString().trim { it <= ' ' }
        val notes = notesEditText!!.getText().toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(getContext(), "يرجى إدخال المبلغ", Toast.LENGTH_SHORT).show()
            return
        }
        val amount = amountStr.toDouble()
        val desc =
            "تحويل من " + fromAccount!!.getName() + " إلى " + toAccount!!.getName() + (if (TextUtils.isEmpty(
                    notes
                )
            ) "" else (" - " + notes))
        // معاملة الخصم
        val debitTx = Transaction()
        debitTx.setAccountId(fromAccount!!.getId())
        debitTx.setAmount(amount)
        debitTx.setCurrency(currency)
        debitTx.setType("debit")
        debitTx.setDescription(desc)
        debitTx.setWhatsappEnabled(fromAccount!!.isWhatsappEnabled())
        debitTx.setCashboxId(selectedCashboxId)
        // معاملة الإضافة
        val creditTx = Transaction()
        creditTx.setAccountId(toAccount!!.getId())
        creditTx.setAmount(amount)
        creditTx.setCurrency(currency)
        creditTx.setType("credit")
        creditTx.setDescription(desc)
        creditTx.setWhatsappEnabled(toAccount!!.isWhatsappEnabled())
        creditTx.setCashboxId(selectedCashboxId)
        transactionRepository!!.insert(debitTx)
        transactionRepository!!.insert(creditTx)
        Toast.makeText(getContext(), "تمت عملية التحويل بنجاح", Toast.LENGTH_LONG).show()
        amountEditText!!.setText("")
        notesEditText!!.setText("")
        updateBalances()
    }

    // أداة مساعدة لمراقبة النصوص
    private class SimpleTextWatcher(private val callback: Runnable) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
        override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
            callback.run()
        }

        override fun afterTextChanged(s: Editable?) {}
    }
}