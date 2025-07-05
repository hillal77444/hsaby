package com.hillal.acc.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.hillal.acc.R
import com.hillal.acc.data.repository.AccountRepository
import com.hillal.acc.data.room.AppDatabase

class AddAccountComposeFragment : Fragment() {

    private lateinit var viewModel: AccountViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize Repository and ViewModel
        val database = AppDatabase.getInstance(requireContext())
        val accountRepository = AccountRepository(database.accountDao(), database)
        val viewModelFactory = AccountViewModelFactory(accountRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[AccountViewModel::class.java]

        return ComposeView(requireContext()).apply {
            setContent {
                AddAccountComposeScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        Navigation.findNavController(requireView()).navigateUp()
                    }
                )
            }
        }
    }
} 