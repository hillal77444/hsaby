package com.hillal.acc.ui.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.hillal.acc.ui.AccountStatementActivity
import com.hillal.acc.ui.accounts.ResponsiveAccountsTheme
import com.hillal.acc.ui.theme.ProvideResponsiveDimensions
import com.hillal.acc.viewmodel.TransactionViewModel

class ReportsComposeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val transactionViewModel: TransactionViewModel by viewModels()
        return ComposeView(requireContext()).apply {
            setContent {
                ProvideResponsiveDimensions {
                    ResponsiveAccountsTheme {
                        ReportsScreen(
                            transactionViewModel = transactionViewModel,
                            onAccountStatementClick = {
                                val intent = Intent(requireContext(), AccountStatementActivity::class.java)
                                startActivity(intent)
                            },
                            onAccountsSummaryClick = {
                                findNavController().navigate(com.hillal.acc.R.id.accountsSummaryReportFragment)
                            },
                            onCashboxStatementClick = {
                                findNavController().navigate(com.hillal.acc.R.id.cashboxStatementFragment)
                            }
                        )
                    }
                }
            }
        }
    }
} 