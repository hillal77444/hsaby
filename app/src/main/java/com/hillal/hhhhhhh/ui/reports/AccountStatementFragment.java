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
import com.hillal.hhhhhhh.data.entities.Account;
import com.hillal.hhhhhhh.data.entities.Transaction;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AccountStatementFragment extends Fragment {
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private TextInputEditText fromDateEditText;
    private TextInputEditText toDateEditText;
    private RecyclerView transactionsRecyclerView;
    private TransactionsAdapter transactionsAdapter;
    private Date fromDate;
    private Date toDate;
    private long accountId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            accountId = getArguments().getLong("accountId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_statement, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        
        setupViews(view);
        setupDatePickers();
        setupClickListeners();
        loadAccountDetails();
    }

    private void setupViews(View view) {
        fromDateEditText = view.findViewById(R.id.from_date);
        toDateEditText = view.findViewById(R.id.to_date);
        transactionsRecyclerView = view.findViewById(R.id.transactions_recycler_view);
        
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionsAdapter = new TransactionsAdapter();
        transactionsRecyclerView.setAdapter(transactionsAdapter);
    }

    private void setupDatePickers() {
        Calendar calendar = Calendar.getInstance();
        
        fromDateEditText.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    fromDate = calendar.getTime();
                    fromDateEditText.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(fromDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        toDateEditText.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    toDate = calendar.getTime();
                    toDateEditText.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(toDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void setupClickListeners() {
        view.findViewById(R.id.generate_statement_button).setOnClickListener(v -> generateStatement());
        view.findViewById(R.id.export_pdf_button).setOnClickListener(v -> exportToPdf());
    }

    private void loadAccountDetails() {
        accountViewModel.getAccount(accountId).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                // Update UI with account details
                ((TextView) view.findViewById(R.id.account_name)).setText(account.getName());
                ((TextView) view.findViewById(R.id.account_balance)).setText(
                    String.format("%.2f", account.getBalance()));
            }
        });
    }

    private void generateStatement() {
        if (fromDate == null || toDate == null) {
            Toast.makeText(getContext(), "Please select date range", Toast.LENGTH_SHORT).show();
            return;
        }

        transactionViewModel.getTransactionsByAccountAndDateRange(accountId, fromDate, toDate)
                .observe(getViewLifecycleOwner(), transactions -> {
                    transactionsAdapter.setTransactions(transactions);
                    updateAccountBalance(transactions);
                });
    }

    private void updateAccountBalance(List<Transaction> transactions) {
        double totalDebit = 0;
        double totalCredit = 0;

        for (Transaction transaction : transactions) {
            if (transaction.getType().equals("debit")) {
                totalDebit += transaction.getAmount();
            } else {
                totalCredit += transaction.getAmount();
            }
        }

        ((TextView) view.findViewById(R.id.total_debit)).setText(
            String.format("%.2f", totalDebit));
        ((TextView) view.findViewById(R.id.total_credit)).setText(
            String.format("%.2f", totalCredit));
    }

    private void exportToPdf() {
        if (fromDate == null || toDate == null) {
            Toast.makeText(getContext(), "Please select date range", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File pdfFile = new File(requireContext().getExternalFilesDir(null), 
                "account_statement.pdf");
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Account Statement", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Add account details
            Account account = accountViewModel.getAccount(accountId).getValue();
            if (account != null) {
                document.add(new Paragraph("Account: " + account.getName()));
                document.add(new Paragraph("Balance: " + String.format("%.2f", account.getBalance())));
            }
            document.add(new Paragraph("\n"));

            // Add date range
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            document.add(new Paragraph("Period: " + dateFormat.format(fromDate) + 
                " to " + dateFormat.format(toDate)));
            document.add(new Paragraph("\n"));

            // Add transactions
            for (Transaction transaction : transactionsAdapter.getTransactions()) {
                document.add(new Paragraph(dateFormat.format(transaction.getDate()) + 
                    " - " + transaction.getType() + " - " + 
                    String.format("%.2f", transaction.getAmount()) + 
                    " - " + transaction.getDescription()));
            }

            document.close();
            Toast.makeText(getContext(), "PDF saved to: " + pdfFile.getAbsolutePath(), 
                Toast.LENGTH_LONG).show();
        } catch (DocumentException | IOException e) {
            Toast.makeText(getContext(), "Error creating PDF: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }
} 