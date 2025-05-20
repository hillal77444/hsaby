package com.hsaby.accounting.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hsaby.accounting.AccountingApp
import com.hsaby.accounting.databinding.FragmentDashboardBinding
import com.hsaby.accounting.ui.transaction.AddTransactionBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    
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
        observeViewModel()
    }
    
    private fun setupViews() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }

        binding.addTransactionFab.setOnClickListener {
            showAddTransactionDialog()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is DashboardUiState.Loading -> {
                        binding.swipeRefresh.isRefreshing = true
                    }
                    is DashboardUiState.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        updateUI(state)
                    }
                    is DashboardUiState.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        // عرض رسالة الخطأ
                    }
                }
            }
        }
    }
    
    private fun updateUI(state: DashboardUiState.Success) {
        binding.totalBalanceText.text = state.totalBalance.toString()
        binding.totalDebtorsText.text = state.totalDebtors.toString()
        binding.activeAccountsText.text = state.activeAccounts.toString()
        
        // تحديث قائمة المعاملات الأخيرة
        // تحديث قائمة الحسابات النشطة
    }
    
    private fun showAddTransactionDialog() {
        AddTransactionBottomSheet().show(
            childFragmentManager,
            "add_transaction"
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 