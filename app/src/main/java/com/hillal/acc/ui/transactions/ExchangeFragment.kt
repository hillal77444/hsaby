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
import com.hillal.acc.databinding.FragmentExchangeBinding
import com.hillal.acc.ui.common.AccountPickerBottomSheet
import com.hillal.acc.viewmodel.AccountViewModel
import com.hillal.acc.viewmodel.CashboxViewModel
import java.math.BigDecimal
import java.math.RoundingMode

class ExchangeFragment : Fragment() {
    private var fromCurrencySpinner: Spinner? = null
    private var toCurrencySpinner: Spinner? = null
    private var operationTypeSpinner: Spinner? = null
    private var cashboxSpinner: Spinner? = null
    private var accountBalanceText: TextView? = null
    private var exchangeAmountText: TextView? = null
    private var amountEditText: EditText? = null
    private var rateEditText: EditText? = null
    private var notesEditText: EditText? = null
    private var accountAutoComplete: TextInputEditText? = null
    private var accounts: MutableList<Account?>? = null
    private var cashboxes: MutableList<Cashbox> = ArrayList<Cashbox>()
    private var selectedAccount: Account? = null
    private var selectedCashboxId: Long = -1
    private var currencies: Array<String> = arrayOf()
    private var currencyAdapter: ArrayAdapter<String?>? = null
    private var cashboxAdapter: ArrayAdapter<String?>? = null
    private var opTypeAdapter: ArrayAdapter<CharSequence?>? = null
    private var accountBalancesMap: MutableMap<Long?, MutableMap<String?, Double?>?> =
        HashMap<Long?, MutableMap<String?, Double?>?>()
    private var cashboxViewModel: CashboxViewModel? = null
    private var accountViewModel: AccountViewModel? = null
    private var transactionViewModel: TransactionViewModel? = null
    private var transactionRepository: TransactionRepository? = null
    private var transactionsViewModel: TransactionsViewModel? = null
    private var binding: FragmentExchangeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentExchangeBinding.inflate(inflater, container, false)
        fromCurrencySpinner = binding!!.fromCurrencySpinner
        toCurrencySpinner = binding!!.toCurrencySpinner
        operationTypeSpinner = binding!!.operationTypeSpinner
        cashboxSpinner = binding!!.cashboxSpinner
        accountBalanceText = binding!!.accountBalanceText
        exchangeAmountText = binding!!.exchangeAmountText
        amountEditText = binding!!.amountEditText
        rateEditText = binding!!.rateEditText
        notesEditText = binding!!.notesEditText
        accountAutoComplete = binding!!.accountAutoComplete
        accountAutoComplete!!.setFocusable(false)
        accountAutoComplete!!.setOnClickListener(View.OnClickListener { v: View? -> openAccountPicker() })
        accountViewModel =
            ViewModelProvider(this).get<AccountViewModel>(AccountViewModel::class.java)
        cashboxViewModel =
            ViewModelProvider(this).get<CashboxViewModel>(CashboxViewModel::class.java)
        val accountRepository = (requireActivity().getApplication() as App).getAccountRepository()
        transactionRepository = (requireActivity().getApplication() as App).getTransactionRepository()
        val transactionFactory = TransactionViewModelFactory(accountRepository, transactionRepository!!)
        transactionViewModel =
            ViewModelProvider(this, transactionFactory).get<TransactionViewModel>(
                TransactionViewModel::class.java
            )
        currencies = getResources().getStringArray(R.array.currencies_array)
        currencyAdapter = ArrayAdapter<String?>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            currencies
        )
        currencyAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fromCurrencySpinner!!.setAdapter(currencyAdapter)
        toCurrencySpinner!!.setAdapter(currencyAdapter)
        opTypeAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.exchange_operation_types,
            android.R.layout.simple_spinner_item
        )
        opTypeAdapter!!.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        operationTypeSpinner!!.setAdapter(opTypeAdapter)
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
        fromCurrencySpinner!!.setOnItemSelectedListener(object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view1: View?,
                position: Int,
                id: Long
            ) {
                updateToCurrencyOptions()
                updateBalanceText()
                updateExchangeAmount()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
        toCurrencySpinner!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view1: View?,
                position: Int,
                id: Long
            ) {
                updateExchangeAmount()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
        operationTypeSpinner!!.setOnItemSelectedListener(object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view1: View?,
                position: Int,
                id: Long
            ) {
                updateExchangeAmount()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
        amountEditText!!.addTextChangedListener(SimpleTextWatcher(Runnable { this.updateExchangeAmount() }))
        rateEditText!!.addTextChangedListener(SimpleTextWatcher(Runnable { this.updateExchangeAmount() }))
        transactionsViewModel =
            ViewModelProvider(this).get(TransactionsViewModel::class.java)
        transactionsViewModel!!.accountBalancesMap.observe(
            getViewLifecycleOwner(),
            Observer { balancesMap: MutableMap<Long?, MutableMap<String?, Double?>?>? ->
                accountBalancesMap =
                    if (balancesMap != null) balancesMap else HashMap<Long?, MutableMap<String?, Double?>?>()
            })
        val exchangeButton = binding!!.exchangeButton
        exchangeButton.setOnClickListener(View.OnClickListener { v: View? -> performExchange() })
        return binding!!.getRoot()
    }

    private fun openAccountPicker() {
        val picker = AccountPickerBottomSheet(
            accounts,
            ArrayList<Transaction?>(),
            accountBalancesMap,
            AccountPickerBottomSheet.OnAccountSelectedListener { account: Account? ->
                selectedAccount = account
                accountAutoComplete!!.setText(account!!.getName())
                updateBalanceText()
            })
        picker.show(getParentFragmentManager(), "account_picker")
    }

    private fun updateToCurrencyOptions() {
        val fromCurrency =
            if (fromCurrencySpinner!!.getSelectedItem() != null) fromCurrencySpinner!!.getSelectedItem()
                .toString() else ""
        val toList: MutableList<String?> = ArrayList<String?>()
        for (c in currencies) {
            if (c != fromCurrency) toList.add(c)
        }
        val toAdapter =
            ArrayAdapter<String?>(requireContext(), android.R.layout.simple_spinner_item, toList)
        toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        toCurrencySpinner!!.setAdapter(toAdapter)
        if (!toList.isEmpty()) toCurrencySpinner!!.setSelection(0)
    }

    private val selectedFromCurrency: String
        get() = if (fromCurrencySpinner!!.getSelectedItem() != null) fromCurrencySpinner!!.getSelectedItem()
            .toString() else ""

    private val selectedToCurrency: String
        get() = if (toCurrencySpinner!!.getSelectedItem() != null) toCurrencySpinner!!.getSelectedItem()
            .toString() else ""

    private val selectedOperationType: String
        get() = if (operationTypeSpinner!!.getSelectedItem() != null) operationTypeSpinner!!.getSelectedItem()
            .toString() else ""

    private fun updateBalanceText() {
        if (selectedAccount == null) {
            accountBalanceText!!.setText("الرصيد: -")
            return
        }
        val currency = this.selectedFromCurrency
        val accountId = selectedAccount!!.getId()
        transactionsViewModel?.getTransactionsByAccount(accountId)?.observe(getViewLifecycleOwner()) { transactions ->
            val lastTx = transactions?.filter { it.getCurrency() == currency }?.maxWithOrNull(compareBy<Transaction>({ it.getTransactionDate() }, { it.getId() }))
            if (lastTx != null) {
                transactionRepository!!.getBalanceUntilTransaction(accountId, lastTx.getTransactionDate(), lastTx.getId(), currency)
                    .observe(getViewLifecycleOwner(), Observer { balance: Double? ->
                        val bal = balance ?: 0.0
                        accountBalanceText!!.setText("الرصيد: " + bal + " " + currency)
                    })
            } else {
                transactionRepository!!.getBalanceUntilTransaction(accountId, System.currentTimeMillis(), -1, currency)
                    .observe(getViewLifecycleOwner(), Observer { balance: Double? ->
                        val bal = balance ?: 0.0
                        accountBalanceText!!.setText("الرصيد: " + bal + " " + currency)
                    })
            }
        }
    }

    private fun updateExchangeAmount() {
        val amountStr = amountEditText!!.getText().toString().trim { it <= ' ' }
        val rateStr = rateEditText!!.getText().toString().trim { it <= ' ' }
        val opType = this.selectedOperationType
        val fromCurrency = this.selectedFromCurrency
        val toCurrency = this.selectedToCurrency
        if (amountStr.isEmpty() || rateStr.isEmpty() || fromCurrency == toCurrency) {
            exchangeAmountText!!.setText("المبلغ بعد الصرف: -")
            return
        }
        try {
            val amount = amountStr.toDouble()
            val rate = rateStr.toDouble()
            val toAmount = if (opType == "بيع") amount * rate else amount / rate
            val rounded = BigDecimal(toAmount).setScale(2, RoundingMode.HALF_UP)
            exchangeAmountText!!.setText("المبلغ بعد الصرف: " + rounded.toPlainString() + " " + toCurrency)
        } catch (e: Exception) {
            exchangeAmountText!!.setText("المبلغ بعد الصرف: -")
        }
    }

    private fun performExchange() {
        if (selectedAccount == null) {
            Toast.makeText(getContext(), "يرجى اختيار الحساب", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCashboxId == -1L) {
            Toast.makeText(getContext(), "يرجى اختيار الصندوق", Toast.LENGTH_SHORT).show()
            return
        }
        val fromCurrency = this.selectedFromCurrency
        val toCurrency = this.selectedToCurrency
        val opType = this.selectedOperationType
        val amountStr = amountEditText!!.getText().toString().trim { it <= ' ' }
        val rateStr = rateEditText!!.getText().toString().trim { it <= ' ' }
        val notes = notesEditText!!.getText().toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(rateStr)) {
            Toast.makeText(getContext(), "يرجى إدخال المبلغ وسعر الصرف", Toast.LENGTH_SHORT).show()
            return
        }
        if (fromCurrency == toCurrency) {
            Toast.makeText(getContext(), "العملتان متطابقتان!", Toast.LENGTH_SHORT).show()
            return
        }
        val amount = amountStr.toDouble()
        val rate = rateStr.toDouble()
        val toAmount = if (opType == "بيع") amount * rate else amount / rate
        val rounded = BigDecimal(toAmount).setScale(2, RoundingMode.HALF_UP)
        var desc = opType + " عملة من " + fromCurrency + " إلى " + toCurrency + " بسعر صرف " + rate
        if (!TextUtils.isEmpty(notes)) desc += " - " + notes
        val whatsappEnabled = selectedAccount != null && selectedAccount!!.isWhatsappEnabled()
        // معاملة الخصم
        val debitTx = Transaction()
        debitTx.id = System.currentTimeMillis() * 1000000 + System.nanoTime() % 1000000
        debitTx.setAccountId(selectedAccount!!.getId())
        debitTx.setAmount(amount)
        debitTx.setCurrency(fromCurrency)
        debitTx.setType("debit")
        debitTx.setDescription(desc)
        debitTx.setWhatsappEnabled(whatsappEnabled)
        debitTx.setCashboxId(selectedCashboxId)
        // معاملة الإضافة
        val creditTx = Transaction()
        creditTx.id = System.currentTimeMillis() * 1000000 + (System.nanoTime() + 1) % 1000000
        creditTx.setAccountId(selectedAccount!!.getId())
        creditTx.setAmount(rounded.toDouble())
        creditTx.setCurrency(toCurrency)
        creditTx.setType("credit")
        creditTx.setDescription(desc)
        creditTx.setWhatsappEnabled(whatsappEnabled)
        creditTx.setCashboxId(selectedCashboxId)
        transactionViewModel!!.insertTransaction(debitTx)
        transactionViewModel!!.insertTransaction(creditTx)
        Toast.makeText(getContext(), "تمت عملية الصرف بنجاح", Toast.LENGTH_LONG).show()
        amountEditText!!.setText("")
        rateEditText!!.setText("")
        notesEditText!!.setText("")
        exchangeAmountText!!.setText("المبلغ بعد الصرف: -")
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