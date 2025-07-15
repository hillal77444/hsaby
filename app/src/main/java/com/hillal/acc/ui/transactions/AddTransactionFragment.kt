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
import androidx.lifecycle.ViewModelProvider

class AddTransactionFragment : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val app = requireActivity().application as App
        val accountRepository = app.getAccountRepository()
        val transactionRepository = app.getTransactionRepository()
        val userPreferences = UserPreferences(requireContext())
        val transactionsViewModel = ViewModelProvider(this, TransactionViewModelFactory(accountRepository, transactionRepository)).get(TransactionsViewModel::class.java)
        // إذا كان لديك AccountViewModelFactory أو CashboxViewModelFactory، أنشئهم هنا بنفس الطريقة
        // مثال:
        // val accountViewModel = ViewModelProvider(this, AccountViewModelFactory(accountRepository)).get(AccountViewModel::class.java)
        // val cashboxViewModel = ViewModelProvider(this, CashboxViewModelFactory(...)).get(CashboxViewModel::class.java)
        return ComposeView(requireContext()).apply {
            setContent {
                AddTransactionScreen(
                    navController = findNavController(),
                    transactionsViewModel = transactionsViewModel,
                    // accountViewModel = accountViewModel, // مررهم إذا أنشأتهم
                    // cashboxViewModel = cashboxViewModel, // مررهم إذا أنشأتهم
                    transactionRepository = transactionRepository,
                    userPreferences = userPreferences
                )
            }
        }
    }
}