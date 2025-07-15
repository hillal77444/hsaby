package com.hillal.acc.ui.transactions

import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hillal.acc.data.preferences.UserPreferences
import com.hillal.acc.data.repository.TransactionRepository
import com.hillal.acc.ui.transactions.AddTransactionScreen
import com.hillal.acc.ui.transactions.TransactionsViewModel
import com.hillal.acc.App
import androidx.lifecycle.viewmodel.compose.viewModel

class AddTransactionFragment : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val app = requireActivity().application as App
        val transactionRepository = TransactionRepository(app.getDatabase())
        val userPreferences = UserPreferences(requireContext())
        return ComposeView(requireContext()).apply {
            setContent {
                AddTransactionScreen(
                    navController = findNavController(),
                    transactionsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    accountViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    cashboxViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    transactionRepository = transactionRepository,
                    userPreferences = userPreferences
                )
            }
        }
    }
}