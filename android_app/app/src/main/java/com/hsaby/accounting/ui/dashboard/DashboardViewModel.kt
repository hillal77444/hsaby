package com.hsaby.accounting.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsaby.accounting.data.model.Account
import com.hsaby.accounting.data.model.Transaction
import com.hsaby.accounting.data.repository.AccountRepository
import com.hsaby.accounting.data.repository.TransactionRepository
import com.hsaby.accounting.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                combine(
                    accountRepository.getAllAccounts(),
                    transactionRepository.getAllTransactions()
                ) { accounts, transactions ->
                    val totalBalance = accounts.sumOf { it.balance }
                    val totalDebtors = accounts.count { it.isDebtor }
                    val activeAccounts = accounts.count { it.isActive }
                    
                    DashboardUiState.Success(
                        totalBalance = totalBalance,
                        totalDebtors = totalDebtors,
                        activeAccounts = activeAccounts,
                        recentTransactions = transactions.take(5)
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "حدث خطأ غير معروف")
            }
        }
    }

    fun refresh() {
        loadDashboardData()
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val totalBalance: Double,
        val totalDebtors: Int,
        val activeAccounts: Int,
        val recentTransactions: List<Transaction>
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
} 