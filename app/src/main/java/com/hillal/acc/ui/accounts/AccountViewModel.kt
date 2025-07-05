package com.hillal.acc.ui.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.hillal.acc.data.model.Account
import com.hillal.acc.data.repository.AccountRepository

class AccountViewModel(private val accountRepository: AccountRepository) : ViewModel() {

    val allAccounts: LiveData<List<Account>>
        get() = accountRepository.getAllAccounts()

    fun getAccountById(accountId: Long): LiveData<Account> {
        return accountRepository.getAccountById(accountId)
    }

    fun searchAccounts(query: String): LiveData<List<Account>> {
        return accountRepository.searchAccounts(query)
    }

    fun insertAccount(account: Account) {
        accountRepository.insertAccount(account)
    }

    fun updateAccount(account: Account) {
        accountRepository.updateAccount(account)
    }

    fun deleteAccount(account: Account) {
        accountRepository.deleteAccount(account)
    }

    fun getAccountBalanceYemeni(accountId: Long): LiveData<Double?> {
        return accountRepository.getAccountBalanceYemeni(accountId)
    }

    fun getAccountByPhoneNumber(phoneNumber: String): Account? {
        return accountRepository.getAccountByPhoneNumber(phoneNumber)
    }

    fun generateUniqueAccountNumber(): String {
        return accountRepository.generateUniqueAccountNumber()
    }
} 