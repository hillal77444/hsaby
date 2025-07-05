package com.hillal.acc.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.repository.AccountRepository
import com.hillal.acc.data.repository.TransactionRepository

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val accounts = MutableLiveData<MutableList<Account?>?>()
    private val transactions = MutableLiveData<MutableList<Transaction?>?>()
    private val recentAccounts = MutableLiveData<MutableList<Account?>?>()
    private val totalDebtors = MutableLiveData<Double?>(0.0)
    private val totalCreditors = MutableLiveData<Double?>(0.0)
    private val netBalance = MutableLiveData<Double?>(0.0)

    init {
        loadData()
    }

    private fun loadData() {
        // جلب الحسابات
        accountRepository.getAllAccounts()
            .observeForever(Observer { accountsList: MutableList<Account?>? ->
                accounts.setValue(accountsList)
            })

        // جلب المعاملات
        transactionRepository.getAllTransactions()
            .observeForever(Observer { transactionsList: MutableList<Transaction?>? ->
                transactions.setValue(transactionsList)
            })

        // Load recent accounts
        accountRepository.getRecentAccounts()
            .observeForever(Observer { accounts: MutableList<Account?>? ->
                recentAccounts.setValue(accounts)
            })

        // Load totals from transactions
        transactionRepository.getTotalDebtors().observeForever(Observer { debtors: Double? ->
            totalDebtors.setValue(if (debtors != null) debtors else 0.0)
            updateNetBalance()
        })

        transactionRepository.getTotalCreditors().observeForever(Observer { creditors: Double? ->
            totalCreditors.setValue(if (creditors != null) creditors else 0.0)
            updateNetBalance()
        })
    }

    private fun updateNetBalance() {
        val debtors = totalDebtors.getValue()
        val creditors = totalCreditors.getValue()
        if (debtors != null && creditors != null) {
            netBalance.setValue(creditors - debtors)
        }
    }

    fun getAccounts(): LiveData<MutableList<Account?>?> {
        return accounts
    }

    fun getTransactions(): LiveData<MutableList<Transaction?>?> {
        return transactions
    }

    fun getRecentAccounts(): LiveData<MutableList<Account?>?> {
        return recentAccounts
    }

    fun getTotalDebtors(): LiveData<Double?> {
        return totalDebtors
    }

    fun getTotalCreditors(): LiveData<Double?> {
        return totalCreditors
    }

    fun getNetBalance(): LiveData<Double?> {
        return netBalance
    }

    override fun onCleared() {
        super.onCleared()
        // إزالة المراقبين عند تدمير ViewModel
        accountRepository.getAllAccounts()
            .removeObserver(Observer { value: MutableList<Account?>? -> accounts.setValue(value) })
        transactionRepository.getAllTransactions()
            .removeObserver(Observer { value: MutableList<Transaction?>? ->
                transactions.setValue(value)
            })
    }
}