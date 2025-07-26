package com.hillal.acc.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.hillal.acc.App
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.room.AccountTransactionCount
import com.hillal.acc.data.room.AccountBalanceByCurrency
import com.hillal.acc.data.repository.TransactionRepository
import java.util.stream.Collectors

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    private val transactions = MutableLiveData<MutableList<Transaction>?>()
    private var startDate: Long = 0
    private var endDate: Long = 0

    init {
        repository = (application as App).getTransactionRepository()
        loadAllTransactions()
    }

    fun getTransactions(): LiveData<MutableList<Transaction>?> {
        return transactions
    }

    fun loadAllTransactions() {
        repository.getAllTransactions()
            .observeForever(Observer { value: MutableList<Transaction?>? ->
                transactions.setValue(value?.filterNotNull()?.toMutableList())
            })
    }

    fun loadTransactionsByAccount(accountName: String) {
        repository.getAllTransactions()
            .observeForever(Observer { transactionList: MutableList<Transaction>? ->
                if (transactionList != null) {
                    val filteredTransactions: MutableList<Transaction> = ArrayList<Transaction>()
                    for (t in transactionList) {
                        if (accountName == t.getAccountId().toString()) {
                            filteredTransactions.add(t)
                        }
                    }
                    transactions.setValue(filteredTransactions)
                }
            })
    }

    fun loadTransactionsByDateRange(startDate: Long, endDate: Long) {
        this.startDate = startDate
        this.endDate = endDate
        repository.getTransactionsByDateRange(startDate, endDate)
            .observeForever(Observer { value: MutableList<Transaction?>? ->
                transactions.setValue(value?.filterNotNull()?.toMutableList())
            })
    }

    fun loadTransactionsByCurrency(currency: String) {
        repository.getAllTransactions()
            .observeForever(Observer { transactionList: MutableList<Transaction>? ->
                if (transactionList != null) {
                    val filteredTransactions: MutableList<Transaction> = ArrayList<Transaction>()
                    for (t in transactionList) {
                        if (currency == t.getCurrency()) {
                            filteredTransactions.add(t)
                        }
                    }
                    transactions.setValue(filteredTransactions)
                }
            })
    }

    fun deleteTransaction(transaction: Transaction?) {
        repository.delete(transaction)
        loadTransactionsByDateRange(startDate, endDate)
    }

    fun updateTransaction(transaction: Transaction?) {
        repository.update(transaction)
    }

    fun insertTransaction(transaction: Transaction?) {
        repository.insert(transaction)
        loadTransactionsByDateRange(startDate, endDate)
    }

    fun getTransactionById(id: Long): LiveData<Transaction?>? {
        return repository.getTransactionById(id)
    }

    fun filterTransactionsByCurrency(currency: String) {
        val currentList = transactions.getValue()
        if (currentList != null) {
            val filteredList = currentList.stream()
                .filter { t: Transaction? ->
                    t!!.getCurrency() != null &&
                            t.getCurrency().trim { it <= ' ' }
                                .equals(currency.trim { it <= ' ' }, ignoreCase = true)
                }
                .collect(Collectors.toList())
            transactions.setValue(ArrayList(filteredList ?: emptyList()))
        }
    }

    fun filterTransactionsByAccount(accountId: Long) {
        val currentList = transactions.getValue()
        if (currentList != null) {
            val filteredList = currentList.stream()
                .filter { t: Transaction? -> t!!.getAccountId() == accountId }
                .collect(Collectors.toList())
            transactions.setValue(ArrayList(filteredList ?: emptyList()))
        }
    }

    // إضافة دوال محسنة
    fun getAccountsTransactionCount(): LiveData<List<AccountTransactionCount>> {
        return repository.getAccountsTransactionCount()
    }

    fun getAllAccountsBalancesByCurrency(): LiveData<List<AccountBalanceByCurrency>> {
        return repository.getAllAccountsBalancesByCurrency()
    }

    // تحسين حساب الأرصدة
    val accountBalancesMapOptimized: LiveData<Map<Long, Map<String, Double>>>
        get() {
            val balancesLiveData = MutableLiveData<Map<Long, Map<String, Double>>>()
            getAllAccountsBalancesByCurrency().observeForever { balancesList ->
                val balancesMap = mutableMapOf<Long, MutableMap<String, Double>>()
                balancesList?.forEach { balance ->
                    if (!balancesMap.containsKey(balance.accountId)) {
                        balancesMap[balance.accountId] = mutableMapOf()
                    }
                    balancesMap[balance.accountId]!![balance.currency] = balance.balance
                }
                balancesLiveData.postValue(balancesMap)
            }
            return balancesLiveData
        }

    // دالة التوافق مع الملفات القديمة
    val accountBalancesMap: LiveData<MutableMap<Long?, MutableMap<String?, Double?>?>?>
        get() {
            val balancesLiveData = MutableLiveData<MutableMap<Long?, MutableMap<String?, Double?>?>?>()
            getAllAccountsBalancesByCurrency().observeForever { balancesList ->
                val balancesMap: MutableMap<Long?, MutableMap<String?, Double?>?> = HashMap()
                balancesList?.forEach { balance ->
                    if (!balancesMap.containsKey(balance.accountId)) {
                        balancesMap[balance.accountId] = HashMap()
                    }
                    balancesMap[balance.accountId]!![balance.currency] = balance.balance
                }
                balancesLiveData.postValue(balancesMap)
            }
            return balancesLiveData
        }

    fun searchTransactionsByDescription(query: String?): LiveData<MutableList<Transaction?>?>? {
        return repository.searchTransactionsByDescription(query)
    }
}