package com.hillal.acc.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.hillal.acc.data.repository.AccountRepository
import com.hillal.acc.data.room.AppDatabase

class EditAccountComposeFragment : Fragment() {
    private lateinit var viewModel: AccountViewModel
    private var accountId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getInstance(requireContext())
        val accountRepository = AccountRepository(database.accountDao(), database)
        val viewModelFactory = AccountViewModelFactory(accountRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[AccountViewModel::class.java]
        arguments?.let {
            accountId = it.getLong("accountId", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val account = viewModel.getAccountById(accountId).observeAsState(null).value
                if (account != null) {
                    AddAccountComposeScreen(
                        viewModel = viewModel,
                        onNavigateBack = { findNavController().navigateUp() },
                        initialAccount = account,
                        onSave = { updatedAccount ->
                            viewModel.updateAccount(updatedAccount)
                        }
                    )
                } else {
                    // عرض رسالة خطأ أو الرجوع للخلف إذا لم يوجد الحساب
                    Toast.makeText(context, "تعذر تحميل بيانات الحساب", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }
} 