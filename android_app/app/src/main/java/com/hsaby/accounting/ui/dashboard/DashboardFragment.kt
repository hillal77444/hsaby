package com.hsaby.accounting.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.hsaby.accounting.AccountingApp
import com.hsaby.accounting.databinding.FragmentDashboardBinding
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupViews()
        observeData()
    }
    
    private fun setupViews() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = preferencesManager.userId.first()
            if (userId != null) {
                // Observe accounts
                (requireActivity().application as AccountingApp)
                    .accountRepository.getAccountsByUserId(userId)
                    .collect { accounts ->
                        // Update accounts UI
                        updateAccountsUI(accounts)
                    }
                
                // Observe transactions
                (requireActivity().application as AccountingApp)
                    .transactionRepository.getTransactionsByUserId(userId)
                    .collect { transactions ->
                        // Update transactions UI
                        updateTransactionsUI(transactions)
                    }
            }
        }
    }
    
    private fun updateAccountsUI(accounts: List<com.hsaby.accounting.data.local.entity.AccountEntity>) {
        // TODO: Update accounts UI
    }
    
    private fun updateTransactionsUI(transactions: List<com.hsaby.accounting.data.local.entity.TransactionEntity>) {
        // TODO: Update transactions UI
    }
    
    private fun refreshData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = preferencesManager.userId.first()
                if (userId != null) {
                    // Sync accounts
                    (requireActivity().application as AccountingApp)
                        .accountRepository.syncAccounts(userId)
                    
                    // Sync transactions
                    (requireActivity().application as AccountingApp)
                        .transactionRepository.syncTransactions(userId)
                }
            } catch (e: Exception) {
                // Handle sync error
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 