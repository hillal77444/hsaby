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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
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
        binding.apply {
            // Setup RecyclerViews
            accountsRecyclerView.layoutManager = LinearLayoutManager(context)
            accountsAdapter = AccountsAdapter { account ->
                findNavController().navigate(
                    DashboardFragmentDirections.actionDashboardToAccountDetails(account.id)
                )
            }
            accountsRecyclerView.adapter = accountsAdapter

            transactionsRecyclerView.layoutManager = LinearLayoutManager(context)
            transactionsAdapter = TransactionsAdapter { transaction ->
                findNavController().navigate(
                    DashboardFragmentDirections.actionDashboardToTransactionDetails(transaction.id)
                )
            }
            transactionsRecyclerView.adapter = transactionsAdapter

            // Setup FABs
            addAccountFab.setOnClickListener {
                findNavController().navigate(
                    DashboardFragmentDirections.actionDashboardToAddAccount()
                )
            }

            addTransactionFab.setOnClickListener {
                findNavController().navigate(
                    DashboardFragmentDirections.actionDashboardToAddTransaction()
                )
            }

            // Setup sync button
            syncButton.setOnClickListener {
                scope.launch {
                    viewModel.syncNow()
                }
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.accounts.observe(viewLifecycleOwner) { accounts ->
            accountsAdapter.submitList(accounts)
            binding.emptyAccountsView.visibility = if (accounts.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionsAdapter.submitList(transactions)
            binding.emptyTransactionsView.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SyncState.Syncing -> {
                    binding.syncButton.isEnabled = false
                    binding.syncProgress.visibility = View.VISIBLE
                }
                is SyncState.Success -> {
                    binding.syncButton.isEnabled = true
                    binding.syncProgress.visibility = View.GONE
                    Snackbar.make(binding.root, "تمت المزامنة بنجاح", Snackbar.LENGTH_SHORT).show()
                }
                is SyncState.Error -> {
                    binding.syncButton.isEnabled = true
                    binding.syncProgress.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    binding.syncButton.isEnabled = true
                    binding.syncProgress.visibility = View.GONE
                }
            }
        }
    }
    
    private fun showAddTransactionDialog() {
        AddTransactionBottomSheet().show(
            childFragmentManager,
            "add_transaction"
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }
} 