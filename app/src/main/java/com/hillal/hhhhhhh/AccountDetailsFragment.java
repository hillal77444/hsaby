package com.hillal.hhhhhhh;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hillal.hhhhhhh.databinding.FragmentAccountDetailsBinding;

public class AccountDetailsFragment extends Fragment {
    private FragmentAccountDetailsBinding binding;
    private AccountViewModel viewModel;
    private Account currentAccount;
    private int accountId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountDetailsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // الحصول على معرف الحساب من الوسائط
        accountId = AccountDetailsFragmentArgs.fromBundle(getArguments()).getAccountId();

        // تهيئة ViewModel
        viewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        // مراقبة تغييرات الحساب
        viewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                currentAccount = account;
                updateUI(account);
            }
        });

        // إعداد زر إضافة عملية
        binding.addTransactionButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("accountId", accountId);
            Navigation.findNavController(v).navigate(R.id.addTransactionFragment, args);
        });

        return root;
    }

    private void updateUI(Account account) {
        binding.accountName.setText(account.getName());
        binding.accountPhone.setText(account.getPhone());
        binding.accountBalance.setText(String.format("%.2f", account.getOpeningBalance()));
        binding.accountBalance.setTextColor(account.isCreditor() ?
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_red_dark));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 