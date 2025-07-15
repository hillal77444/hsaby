package com.hillal.acc.ui.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.repository.AccountRepository

class TransactionViewModel(private val accountRepository: AccountRepository) : ViewModel() {
    fun getTransactionsForAccount(accountId: Long): LiveData<MutableList<Transaction?>?>? {
        return accountRepository.getTransactionsForAccount(accountId)
    }

    fun getAccountBalance(accountId: Long): LiveData<Double?>? {
        return accountRepository.getAccountBalance(accountId)
    }

    fun getBalanceUntilTransaction(accountId: Long, transactionDate: Long, transactionId: Long, currency: String): LiveData<Double> {
        return accountRepository.database.transactionDao().getBalanceUntilTransaction(accountId, transactionDate, transactionId, currency)
    }

    fun insertTransaction(transaction: Transaction?) {
        accountRepository.insertTransaction(transaction)
    }

    fun updateTransaction(transaction: Transaction?) {
        accountRepository.updateTransaction(transaction)
    }

    fun deleteTransaction(transaction: Transaction?) {
        accountRepository.deleteTransaction(transaction)
    }
}