package com.hillal.hhhhhhh.ui.summary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.databinding.FragmentAccountSummaryBinding;
import com.hillal.hhhhhhh.network.ApiService;
import com.hillal.hhhhhhh.network.RetrofitClient;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountSummaryFragment extends Fragment {
    private FragmentAccountSummaryBinding binding;
    private ApiService apiService;
    private NumberFormat numberFormat;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupNumberFormat();
        setupApiService();
        loadAccountSummary();
    }

    private void setupNumberFormat() {
        numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
    }

    private void setupApiService() {
        apiService = RetrofitClient.getInstance().getClient().create(ApiService.class);
    }

    private void loadAccountSummary() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String phoneNumber = getPhoneNumber();

        apiService.getAccountSummary(phoneNumber).enqueue(new Callback<AccountSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<AccountSummaryResponse> call, @NonNull Response<AccountSummaryResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    updateSummaryTable(response.body().getCurrencySummary());
                    updateDetailsTable(response.body().getAccounts());
                } else {
                    showError("فشل في تحميل البيانات");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AccountSummaryResponse> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                showError("حدث خطأ في الاتصال");
            }
        });
    }

    private void updateSummaryTable(List<CurrencySummary> summaries) {
        TableLayout table = binding.summaryTable;
        table.removeAllViews();

        // إضافة رأس الجدول
        addTableRow(table, new String[]{"العملة", "الرصيد", "المدين", "الدائن"}, true);

        // إضافة البيانات
        for (CurrencySummary summary : summaries) {
            addTableRow(table, new String[]{
                summary.getCurrency(),
                numberFormat.format(summary.getTotalBalance()),
                numberFormat.format(summary.getTotalDebits()),
                numberFormat.format(summary.getTotalCredits())
            }, false);
        }
    }

    private void updateDetailsTable(List<AccountSummary> accounts) {
        TableLayout table = binding.detailsTable;
        table.removeAllViews();

        // إضافة رأس الجدول
        addTableRow(table, new String[]{"ID", "الاسم", "العملة", "الرصيد", "المدين", "الدائن"}, true);

        // إضافة البيانات
        for (AccountSummary account : accounts) {
            addTableRow(table, new String[]{
                String.valueOf(account.getUserId()),
                account.getUserName(),
                account.getCurrency(),
                numberFormat.format(account.getBalance()),
                numberFormat.format(account.getTotalDebits()),
                numberFormat.format(account.getTotalCredits())
            }, false);
        }
    }

    private void addTableRow(TableLayout table, String[] values, boolean isHeader) {
        TableRow row = new TableRow(requireContext());
        row.setLayoutParams(new TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        ));

        for (String value : values) {
            TextView textView = new TextView(requireContext());
            textView.setText(value);
            textView.setPadding(8, 8, 8, 8);
            textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
            
            if (isHeader) {
                textView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray));
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            }
            
            row.addView(textView);
        }

        table.addView(row);
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private String getPhoneNumber() {
        // هنا يجب تنفيذ جلب رقم الهاتف من SharedPreferences أو من أي مصدر آخر
        return "715175085"; // مثال مؤقت
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 