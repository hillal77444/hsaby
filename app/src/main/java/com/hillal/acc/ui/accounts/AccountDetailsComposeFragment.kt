package com.hillal.acc.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hillal.acc.data.room.AppDatabase
import com.hillal.acc.ui.transactions.TransactionViewModelFactory

class AccountDetailsComposeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val accountId = arguments?.getLong("accountId") ?: -1L
        return ComposeView(requireContext()).apply {
            setContent {
                val context = requireContext().applicationContext
                val db = AppDatabase.getInstance(context)
                val accountRepository = com.hillal.acc.data.repository.AccountRepository(db.accountDao(), db)
                val accountViewModel: AccountViewModel = viewModel(factory = AccountViewModelFactory(accountRepository))
                val transactionViewModel: com.hillal.acc.ui.transactions.TransactionViewModel = viewModel(factory = TransactionViewModelFactory(accountRepository))
                com.hillal.acc.ui.accounts.AccountDetailsScreen(
                    accountId = accountId,
                    navController = findNavController(),
                    accountViewModel = accountViewModel,
                    transactionViewModel = transactionViewModel
                )
            }
        }
    }
} 