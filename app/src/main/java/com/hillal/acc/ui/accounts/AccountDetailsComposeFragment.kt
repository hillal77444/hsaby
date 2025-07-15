package com.hillal.acc.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class AccountDetailsComposeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val accountId = arguments?.getLong("accountId") ?: -1L
        return ComposeView(requireContext()).apply {
            setContent {
                com.hillal.acc.ui.accounts.AccountDetailsScreen(
                    accountId = accountId,
                    navController = findNavController()
                )
            }
        }
    }
} 