package com.hsaby.accounting.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.hsaby.accounting.AccountingApp
import com.hsaby.accounting.databinding.FragmentTransactionsBinding
import com.hsaby.accounting.util.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TransactionsFragment : Fragment() {
    
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var transactionsAdapter: TransactionsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupViews()
        observeData()
    }
    
    private fun setupViews() {
        transactionsAdapter = TransactionsAdapter()
        binding.rvTransactions.adapter = transactionsAdapter
        
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = preferencesManager.getUserId()
            if (userId != null) {
                (requireActivity().application as AccountingApp)
                    .transactionRepository.getTransactionsByUserId(userId)
                    .collect { transactions ->
                        transactionsAdapter.submitList(transactions)
                    }
            }
        }
    }
    
    private fun refreshData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userId = preferencesManager.getUserId()
                if (userId != null) {
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