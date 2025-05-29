package com.hillal.acc.ui.reports;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.viewmodel.TransactionViewModel;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AccountStatementFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private TextInputEditText fromDateInput;
    private TextInputEditText toDateInput;
    private RecyclerView transactionsRecyclerView;
    private TransactionsAdapter transactionsAdapter;
    private TextView accountNameText;
    private TextView accountBalanceText;
    private TextView totalDebitText;
    private TextView totalCreditText;
    private long accountId;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        if (getArguments() != null) {
            accountId = getArguments().getLong("accountId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_statement, container, false);
        
        fromDateInput = view.findViewById(R.id.fromDateInput);
        toDateInput = view.findViewById(R.id.toDateInput);
        transactionsRecyclerView = view.findViewById(R.id.transactionsRecyclerView);
        accountNameText = view.findViewById(R.id.accountNameText);
        accountBalanceText = view.findViewById(R.id.accountBalanceText);
        totalDebitText = view.findViewById(R.id.totalDebitText);
        totalCreditText = view.findViewById(R.id.totalCreditText);
        
        transactionsAdapter = new TransactionsAdapter();
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionsRecyclerView.setAdapter(transactionsAdapter);

        view.findViewById(R.id.generateStatementButton).setOnClickListener(v -> generateStatement());
        view.findViewById(R.id.exportPdfButton).setOnClickListener(v -> exportToPdf());

        loadAccountData();
        return view;
    }

    private void loadAccountData() {
        accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                accountNameText.setText(account.getName());
                accountBalanceText.setText(String.format("الرصيد: %.2f", account.getBalance()));
                loadTransactions();
            }
        });
    }

    private void loadTransactions() {
        String fromDateStr = fromDateInput.getText() != null ? fromDateInput.getText().toString() : "";
        String toDateStr = toDateInput.getText() != null ? toDateInput.getText().toString() : "";
        
        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);
        
        if (fromDate != null && toDate != null) {
            transactionViewModel.getTransactionsByAccountAndDateRange(accountId, fromDate.getTime(), toDate.getTime())
                .observe(getViewLifecycleOwner(), this::updateTransactions);
        } else {
            transactionViewModel.getTransactionsByAccount(accountId)
                .observe(getViewLifecycleOwner(), this::updateTransactions);
        }
    }

    private void updateTransactions(List<Transaction> transactions) {
        transactionsAdapter.submitList(transactions);
        
        double totalDebit = 0;
        double totalCredit = 0;
        
        for (Transaction transaction : transactions) {
            if (transaction.getType().equals("debit")) {
                totalDebit += transaction.getAmount();
            } else {
                totalCredit += transaction.getAmount();
            }
        }
        
        totalDebitText.setText(String.format("إجمالي المدين: %.2f", totalDebit));
        totalCreditText.setText(String.format("إجمالي الدائن: %.2f", totalCredit));
    }

    private void generateStatement() {
        String fromDateStr = fromDateInput.getText().toString();
        String toDateStr = toDateInput.getText().toString();
        
        if (fromDateStr.isEmpty() || toDateStr.isEmpty()) {
            return;
        }

        Date fromDate = parseDate(fromDateStr);
        Date toDate = parseDate(toDateStr);

        if (fromDate != null && toDate != null) {
            transactionViewModel.getTransactionsByDateRange(fromDate.getTime(), toDate.getTime())
                .observe(getViewLifecycleOwner(), transactions -> {
                    transactionsAdapter.submitList(transactions);
                    updateSummary(transactions);
                });
        }
    }

    private void updateSummary(List<Transaction> transactions) {
        double totalDebit = 0;
        double totalCredit = 0;

        for (Transaction transaction : transactions) {
            if ("debit".equals(transaction.getType())) {
                totalDebit += transaction.getAmount();
            } else {
                totalCredit += transaction.getAmount();
            }
        }

        totalDebitText.setText(String.format("إجمالي المدين: %.2f", totalDebit));
        totalCreditText.setText(String.format("إجمالي الدائن: %.2f", totalCredit));
    }

    private void exportToPdf() {
        // TODO: Implement PDF export functionality
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        
        try {
            return dateFormat.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }
} 