package com.hillal.acc.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.hillal.acc.App
import com.hillal.acc.data.model.Transaction
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
                transactions.setValue(value)
            })
    }

    fun loadTransactionsByAccount(accountName: String) {
        repository.getAllTransactions()
            .observeForever(Observer { transactionList: MutableList<Transaction>? ->
                if (transactionList != null) {
                    val filteredTransactions: MutableList<Transaction?> = ArrayList<Transaction?>()
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
                transactions.setValue(value)
            })
    }

    fun loadTransactionsByCurrency(currency: String) {
        repository.getAllTransactions()
            .observeForever(Observer { transactionList: MutableList<Transaction>? ->
                if (transactionList != null) {
                    val filteredTransactions: MutableList<Transaction?> = ArrayList<Transaction?>()
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
            transactions.setValue(filteredList)
        }
    }

    fun filterTransactionsByAccount(accountId: Long) {
        val currentList = transactions.getValue()
        if (currentList != null) {
            val filteredList = currentList.stream()
                .filter { t: Transaction? -> t!!.getAccountId() == accountId }
                .collect(Collectors.toList())
            transactions.setValue(filteredList)
        }
    }

    val accountBalancesMap: LiveData<MutableMap<Long?, MutableMap<String?, Double?>?>?>
        get() {
            val balancesLiveData =
                MutableLiveData<MutableMap<Long?, MutableMap<String?, Double?>?>?>()
            getTransactions().observeForever(Observer { transactionsList: MutableList<Transaction>? ->
                val balancesMap: MutableMap<Long?, MutableMap<String?, Double?>?> =
                    HashMap<Long?, MutableMap<String?, Double?>?>()
                if (transactionsList != null) {
                    for (t in transactionsList) {
                        val accountId = t.getAccountId()
                        val currency = t.getCurrency()
                        val amount = t.getAmount()
                        val type = t.getType()
                        if (!balancesMap.containsKey(accountId)) {
                            balancesMap.put(
                                accountId,
                                HashMap<String?, Double?>()
                            )
                        }
                        val currencyMap =
                            balancesMap.get(accountId)
                        var prev: Double = currencyMap!!.getOrDefault(currency, 0.0)!!
                        if (type == "عليه" || type.equals("debit", ignoreCase = true)) {
                            prev -= amount
                        } else {
                            prev += amount
                        }
                        currencyMap.put(currency, prev)
                    }
                }
                balancesLiveData.postValue(balancesMap)
            })
            return balancesLiveData
        }

    fun searchTransactionsByDescription(query: String?): LiveData<MutableList<Transaction?>?>? {
        return repository.searchTransactionsByDescription(query)
    }
}