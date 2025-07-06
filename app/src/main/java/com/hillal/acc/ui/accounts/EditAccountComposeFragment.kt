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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator

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
                val accountState = viewModel.getAccountById(accountId).observeAsState()
                val account = accountState.value

                when {
                    account == null && accountState.value == null -> {
                        // عرض مؤشر تحميل أثناء انتظار البيانات
                        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    account != null -> {
                        AddAccountComposeScreen(
                            viewModel = viewModel,
                            onNavigateBack = { findNavController().navigateUp() },
                            initialAccount = account,
                            onSave = { updatedAccount ->
                                updatedAccount.updatedAt = System.currentTimeMillis()
                                updatedAccount.syncStatus = 0
                                viewModel.updateAccount(updatedAccount)
                            }
                        )
                    }
                    else -> {
                        // عرض رسالة خطأ فقط إذا تأكدنا أن الحساب غير موجود بعد الانتظار
                        Toast.makeText(context, "تعذر تحميل بيانات الحساب", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }
} 