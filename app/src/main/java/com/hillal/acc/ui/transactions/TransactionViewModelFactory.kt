package com.hillal.acc.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hillal.acc.data.repository.AccountRepository

class TransactionViewModelFactory(private val accountRepository: AccountRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            return TransactionViewModel(accountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName())
    }
}