package com.hillal.acc.ui.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.hillal.acc.data.model.Transaction
import com.hillal.acc.data.repository.AccountRepository
import com.hillal.acc.data.repository.TransactionRepository

class TransactionViewModel(private val accountRepository: AccountRepository, private val transactionRepository: TransactionRepository) : ViewModel() {
    fun getTransactionsForAccount(accountId: Long): LiveData<MutableList<Transaction?>?>? {
        return accountRepository.getTransactionsForAccount(accountId)
    }

    fun getAccountBalance(accountId: Long): LiveData<Double?>? {
        return accountRepository.getAccountBalance(accountId)
    }

    fun getBalanceUntilTransaction(accountId: String, transactionId: String, currency: String): Double? {
        return transactionRepository.getBalanceUntilTransaction(accountId, transactionId, currency)
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