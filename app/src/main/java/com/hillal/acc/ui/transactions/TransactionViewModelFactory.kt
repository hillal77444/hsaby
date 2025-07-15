package com.hillal.acc.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hillal.acc.data.repository.AccountRepository
import com.hillal.acc.data.repository.TransactionRepository

class TransactionViewModelFactory(private val accountRepository: AccountRepository, private val transactionRepository: TransactionRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            return TransactionViewModel(accountRepository, transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName())
    }
}