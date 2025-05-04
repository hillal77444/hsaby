package com.hillal.hhhhhhh;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hillal.hhhhhhh.databinding.FragmentReportsBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReportsFragment extends Fragment {
    private FragmentReportsBinding binding;
    private Spinner reportTypeSpinner;
    private Button generateReportButton;
    private TextView reportTitleTextView;
    private RecyclerView reportRecyclerView;
    private AppDatabase database;
    private TransactionAdapter transactionAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // تهيئة قاعدة البيانات
        database = AppDatabase.getInstance(requireContext());

        // تهيئة العناصر
        reportTypeSpinner = binding.reportTypeSpinner;
        generateReportButton = binding.generateReportButton;
        reportTitleTextView = binding.reportTitleTextView;
        reportRecyclerView = binding.reportRecyclerView;

        // إعداد Spinner لأنواع التقارير
        String[] reportTypes = {
            getString(R.string.general_report),
            getString(R.string.account_report),
            getString(R.string.type_report)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, reportTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(adapter);

        // إعداد RecyclerView
        transactionAdapter = new TransactionAdapter();
        reportRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        reportRecyclerView.setAdapter(transactionAdapter);

        // إعداد زر إنشاء التقرير
        generateReportButton.setOnClickListener(v -> generateReport());

        return root;
    }

    private void generateReport() {
        String reportType = reportTypeSpinner.getSelectedItem().toString();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (reportType.equals(getString(R.string.general_report))) {
            // تقرير عام يظهر جميع العمليات
            database.transactionDao().getAllTransactions()
                    .observe(getViewLifecycleOwner(), transactions -> {
                        reportTitleTextView.setText(getString(R.string.general_report));
                        transactionAdapter.setTransactions(transactions);
                    });
        } else if (reportType.equals(getString(R.string.account_report))) {
            // تقرير حسب الحساب
            database.transactionDao().getTransactionsByAccount(0) // يمكنك تعديل معرف الحساب حسب الحاجة
                    .observe(getViewLifecycleOwner(), transactions -> {
                        reportTitleTextView.setText(getString(R.string.account_report));
                        transactionAdapter.setTransactions(transactions);
                    });
        } else if (reportType.equals(getString(R.string.type_report))) {
            // تقرير حسب النوع (مدين/دائن)
            database.transactionDao().getTransactionsByType(true) // true للدائن، false للمدين
                    .observe(getViewLifecycleOwner(), transactions -> {
                        reportTitleTextView.setText(getString(R.string.type_report));
                        transactionAdapter.setTransactions(transactions);
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 