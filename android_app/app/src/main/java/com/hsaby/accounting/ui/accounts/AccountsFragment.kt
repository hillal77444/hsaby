package com.hsaby.accounting.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.hsaby.accounting.AccountingApp
import com.hsaby.accounting.databinding.FragmentAccountsBinding
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AccountsFragment : Fragment() {
    
    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var accountsAdapter: AccountsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupViews()
        observeData()
    }
    
    private fun setupViews() {
        accountsAdapter = AccountsAdapter()
        binding.rvAccounts.adapter = accountsAdapter
        
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = preferencesManager.userId.first()
            if (userId != null) {
                (requireActivity().application as AccountingApp)
                    .accountRepository.getAccountsByUserId(userId)
                    .collect { accounts ->
                        accountsAdapter.submitList(accounts)
                    }
            }
        }
    }
    
    private fun refreshData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = preferencesManager.userId.first()
                if (userId != null) {
                    (requireActivity().application as AccountingApp)
                        .accountRepository.syncAccounts(userId)
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