package com.hillal.hhhhhhh.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.ui.AccountStatementActivity;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;
import com.hillal.hhhhhhh.databinding.FragmentAccountDetailsBinding;
import androidx.navigation.fragment.NavHostFragment;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AccountDetailsFragment extends Fragment {
    private FragmentAccountDetailsBinding binding;
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private TextView accountName;
    private TextView accountBalance;
    private TextView accountPhone;
    private TextView accountNotes;
    private long accountId;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountId = getArguments() != null ? getArguments().getLong("accountId") : -1;
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        loadAccountDetails();

        // PieChart: عرض الدائن والمدين
        PieChart pieChart = view.findViewById(R.id.pie_chart);
        transactionViewModel.getTotalCredit(accountId).observe(getViewLifecycleOwner(), credit -> {
            transactionViewModel.getTotalDebit(accountId).observe(getViewLifecycleOwner(), debit -> {
                float totalCredit = credit != null ? credit.floatValue() : 0f;
                float totalDebit = debit != null ? debit.floatValue() : 0f;
                ArrayList<PieEntry> entries = new ArrayList<>();
                if (totalCredit > 0) entries.add(new PieEntry(totalCredit, "له"));
                if (totalDebit > 0) entries.add(new PieEntry(totalDebit, "عليه"));
                PieDataSet dataSet = new PieDataSet(entries, "");
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                PieData data = new PieData(dataSet);
                data.setValueTextSize(14f);
                pieChart.setData(data);
                pieChart.setUsePercentValues(true);
                pieChart.getDescription().setEnabled(false);
                pieChart.getLegend().setEnabled(true);
                pieChart.invalidate();
            });
        });
    }

    private void setupViews() {
        accountName = binding.accountName;
        accountBalance = binding.accountBalance;
        accountPhone = binding.accountPhone;
        accountNotes = binding.accountNotes;

        // إضافة زر تعديل الحساب
        binding.editAccountButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("accountId", accountId);
            Navigation.findNavController(v).navigate(R.id.editAccountFragment, args);
        });

        // إضافة زر حذف الحساب
        binding.deleteAccountButton.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });

        FloatingActionButton fab = binding.fabAddTransaction;
        fab.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putLong("accountId", accountId);
            Navigation.findNavController(v).navigate(R.id.nav_add_transaction, bundle);
        });

        // إضافة زر كشف الحساب التفصيلي
        binding.viewAccountStatementButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AccountStatementActivity.class);
            intent.putExtra("account_id", accountId);
            startActivity(intent);
        });
    }

    private void loadAccountDetails() {
        accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                updateAccountDetails(account);
            }
        });
    }

    private void updateAccountDetails(Account account) {
        accountName.setText(account.getName());
        accountPhone.setText(account.getPhoneNumber());
        accountNotes.setText(account.getNotes());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_account)
            .setMessage(R.string.delete_account_confirmation)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
                    if (account != null) {
                        accountViewModel.deleteAccount(account);
                        Toast.makeText(getContext(), R.string.account_deleted, Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 