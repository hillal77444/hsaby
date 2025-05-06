package com.hillal.hhhhhhh.ui.reports;

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
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.Account;
import com.hillal.hhhhhhh.data.model.Transaction;
import com.hillal.hhhhhhh.viewmodel.AccountViewModel;
import com.hillal.hhhhhhh.viewmodel.TransactionViewModel;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AccountStatementFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private TextView accountNameText;
    private TextView accountBalanceText;
    private TextInputEditText fromDateInput;
    private TextInputEditText toDateInput;
    private RecyclerView transactionsRecyclerView;
    private TransactionsAdapter transactionsAdapter;
    private long accountId;
    private Date fromDate;
    private Date toDate;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountId = getArguments() != null ? getArguments().getLong("accountId") : -1;
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_statement, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        setupDatePickers();
        setupClickListeners();
        loadAccountDetails();
    }

    private void setupViews(View view) {
        accountNameText = view.findViewById(R.id.accountNameText);
        accountBalanceText = view.findViewById(R.id.accountBalanceText);
        fromDateInput = view.findViewById(R.id.fromDateInput);
        toDateInput = view.findViewById(R.id.toDateInput);
        transactionsRecyclerView = view.findViewById(R.id.transactionsRecyclerView);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        transactionsAdapter = new TransactionsAdapter();
        transactionsRecyclerView.setAdapter(transactionsAdapter);
    }

    private void setupDatePickers() {
        Calendar calendar = Calendar.getInstance();
        fromDateInput.setOnClickListener(v -> showDatePicker(fromDateInput, calendar));
        toDateInput.setOnClickListener(v -> showDatePicker(toDateInput, calendar));
    }

    private void showDatePicker(TextInputEditText input, Calendar calendar) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                Date date = calendar.getTime();
                input.setText(DATE_FORMAT.format(date));
                if (input == fromDateInput) {
                    fromDate = date;
                } else {
                    toDate = date;
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void setupClickListeners() {
        requireView().findViewById(R.id.generateStatementButton).setOnClickListener(v -> generateStatement());
        requireView().findViewById(R.id.exportPdfButton).setOnClickListener(v -> exportToPdf());
    }

    private void loadAccountDetails() {
        accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                accountNameText.setText(getString(R.string.account_statement_for, account.getName()));
                loadTransactions(account);
            }
        });
    }

    private void loadTransactions(Account account) {
        if (fromDate != null && toDate != null) {
            transactionViewModel.getTransactionsByDateRange(accountId, fromDate.getTime(), toDate.getTime())
                .observe(getViewLifecycleOwner(), transactions -> {
                    transactionsAdapter.setTransactions(transactions);
                    updateAccountBalance(account, transactions);
                });
        }
    }

    private void updateAccountBalance(Account account, List<Transaction> transactions) {
        double totalDebit = 0;
        double totalCredit = 0;
        for (Transaction transaction : transactions) {
            if ("مدين".equals(transaction.getType())) {
                totalDebit += transaction.getAmount();
            } else {
                totalCredit += transaction.getAmount();
            }
        }
        double balance = account.getOpeningBalance() + totalDebit - totalCredit;
        accountBalanceText.setText(getString(R.string.closing_balance, String.format("%.2f", balance)));
    }

    private void generateStatement() {
        if (fromDate == null || toDate == null) {
            Toast.makeText(requireContext(), "الرجاء اختيار نطاق التاريخ", Toast.LENGTH_SHORT).show();
            return;
        }
        loadAccountDetails();
    }

    private void exportToPdf() {
        if (fromDate == null || toDate == null) {
            Toast.makeText(requireContext(), "الرجاء اختيار نطاق التاريخ", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File pdfFile = new File(requireContext().getExternalFilesDir(null), "account_statement.pdf");
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph(accountNameText.getText().toString(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Add date range
            Paragraph dateRange = new Paragraph(getString(R.string.period, 
                DATE_FORMAT.format(fromDate), DATE_FORMAT.format(toDate)));
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);

            // Calculate totals
            double totalDebit = 0;
            double totalCredit = 0;
            List<Transaction> transactions = transactionsAdapter.getTransactions();
            for (Transaction transaction : transactions) {
                if ("مدين".equals(transaction.getType())) {
                    totalDebit += transaction.getAmount();
                } else {
                    totalCredit += transaction.getAmount();
                }
            }

            // Add summary
            Font summaryFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Paragraph summary = new Paragraph();
            summary.setSpacingBefore(20);
            summary.setSpacingAfter(20);
            
            // Add opening balance
            Account account = accountViewModel.getAccountById(accountId).getValue();
            if (account != null) {
                summary.add(new Paragraph(getString(R.string.opening_balance, 
                    String.format("%.2f", account.getOpeningBalance())), summaryFont));
                summary.add(new Paragraph("\n"));
            }

            // Add totals
            summary.add(new Paragraph(getString(R.string.total_debit, 
                String.format("%.2f", totalDebit)), summaryFont));
            summary.add(new Paragraph(getString(R.string.total_credit, 
                String.format("%.2f", totalCredit)), summaryFont));
            summary.add(new Paragraph(accountBalanceText.getText().toString(), summaryFont));
            document.add(summary);

            // Add transactions table header
            Paragraph tableHeader = new Paragraph("التاريخ | النوع | المبلغ | البيان");
            tableHeader.setAlignment(Element.ALIGN_RIGHT);
            tableHeader.setSpacingBefore(20);
            tableHeader.setSpacingAfter(10);
            document.add(tableHeader);

            // Add transactions
            Font transactionFont = new Font(Font.FontFamily.HELVETICA, 10);
            for (Transaction transaction : transactions) {
                Paragraph transactionText = new Paragraph(String.format("%s | %s | %s | %s",
                    DATE_FORMAT.format(transaction.getDate()),
                    transaction.getType(),
                    String.format("%.2f", transaction.getAmount()),
                    transaction.getDescription()), transactionFont);
                transactionText.setAlignment(Element.ALIGN_RIGHT);
                transactionText.setSpacingAfter(5);
                document.add(transactionText);
            }

            document.close();
            Toast.makeText(requireContext(), "تم حفظ كشف الحساب بنجاح", Toast.LENGTH_SHORT).show();
        } catch (DocumentException | java.io.IOException e) {
            Toast.makeText(requireContext(), "حدث خطأ أثناء حفظ الملف", Toast.LENGTH_SHORT).show();
        }
    }
} 