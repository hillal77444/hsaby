package com.hillal.acc.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.hillal.acc.R
import com.hillal.acc.data.model.Account
import com.hillal.acc.databinding.FragmentAddAccountBinding
import com.hillal.acc.viewmodel.AccountViewModel

class EditAccountFragment : Fragment() {
    private var _binding: FragmentAddAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var accountViewModel: AccountViewModel
    private var accountId: Long = -1
    private var oldAccount: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountViewModel = ViewModelProvider(this)[AccountViewModel::class.java]
        arguments?.let {
            accountId = it.getLong("accountId", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        loadAccount()
        // ضبط insets للجذر لرفع المحتوى مع الكيبورد وأزرار النظام
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            var bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
            if (bottom == 0) {
                bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom
            }
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottom)
            insets
        }
    }

    private fun setupViews() {
        binding.whatsappSwitch.text = getString(R.string.whatsapp_enabled)
        binding.saveButton.setOnClickListener { updateAccount() }
        binding.cancelButton.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadAccount() {
        if (accountId != -1L) {
            accountViewModel.getAccountById(accountId).observe(viewLifecycleOwner) { account ->
                account?.let {
                    oldAccount = it
                    binding.nameEditText.setText(it.name)
                    binding.phoneEditText.setText(it.phoneNumber)
                    binding.openingBalanceEditText.setText(it.balance.toString())
                    binding.notesEditText.setText(it.notes)
                    binding.whatsappSwitch.isChecked = it.isWhatsappEnabled
                }
            }
        }
    }

    private fun updateAccount() {
        val name = binding.nameEditText.text.toString()
        val phone = binding.phoneEditText.text.toString()
        val notes = binding.notesEditText.text.toString()
        val balanceStr = binding.openingBalanceEditText.text.toString()
        val whatsappEnabled = binding.whatsappSwitch.isChecked

        if (name.isEmpty()) {
            binding.nameEditText.error = "الرجاء إدخال اسم الحساب"
            return
        }

        val balance = balanceStr.toDoubleOrNull()
        if (balance == null) {
            binding.openingBalanceEditText.error = "الرجاء إدخال رصيد صحيح"
            return
        }

        accountViewModel.getAccountById(accountId).observe(viewLifecycleOwner) { account ->
            if (account != null) {
                if (oldAccount == null) {
                    Toast.makeText(context, "حدث خطأ في تحميل بيانات الحساب الأصلية", Toast.LENGTH_SHORT).show()
                    return@observe
                }
                account.name = name
                account.phoneNumber = phone
                account.balance = balance
                account.notes = notes
                account.isWhatsappEnabled = whatsappEnabled
                account.updatedAt = System.currentTimeMillis()
                account.serverId = oldAccount!!.serverId
                account.syncStatus = 0
                accountViewModel.updateAccount(account)
                Toast.makeText(context, getString(R.string.account_saved), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 