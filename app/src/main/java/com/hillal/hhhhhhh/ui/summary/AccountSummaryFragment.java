package com.hillal.hhhhhhh.ui.summary;

import android.os.Bundle;
import android.util.Log;
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
import com.hillal.hhhhhhh.models.AccountSummary;
import com.hillal.hhhhhhh.models.AccountSummaryResponse;
import com.hillal.hhhhhhh.models.CurrencySummary;
import com.hillal.hhhhhhh.network.ApiService;
import com.hillal.hhhhhhh.network.RetrofitClient;
import com.hillal.hhhhhhh.data.preferences.UserPreferences;

import java.io.IOException;
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
        String phoneNumber = getPhoneNumber();
        
        if (phoneNumber == null) {
            binding.progressBar.setVisibility(View.GONE);
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        updateTitle(phoneNumber);

        apiService.getAccountSummary(phoneNumber).enqueue(new Callback<AccountSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<AccountSummaryResponse> call, @NonNull Response<AccountSummaryResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                try {
                    if (response.isSuccessful()) {
                        AccountSummaryResponse summaryResponse = response.body();
                        if (summaryResponse == null) {
                            showError("لم يتم استلام أي بيانات من الخادم");
                            return;
                        }

                        // طباعة البيانات المستلمة للتأكد من صحتها
                        Log.d("AccountSummary", "Response: " + summaryResponse.toString());

                        // التحقق من البيانات المستلمة
                        if (summaryResponse.getCurrencySummary() == null) {
                            showError("بيانات العملات فارغة");
                            return;
                        }
                        
                        if (summaryResponse.getCurrencySummary().isEmpty()) {
                            showError("لا توجد بيانات ملخص العملات");
                            return;
                        }

                        if (summaryResponse.getAccounts() == null) {
                            showError("بيانات الحسابات فارغة");
                            return;
                        }
                        
                        if (summaryResponse.getAccounts().isEmpty()) {
                            showError("لا توجد بيانات الحسابات");
                            return;
                        }

                        try {
                            updateSummaryTable(summaryResponse.getCurrencySummary());
                        } catch (Exception e) {
                            showError("خطأ في تحديث جدول العملات: " + e.getMessage());
                            e.printStackTrace();
                            return;
                        }

                        try {
                            updateDetailsTable(summaryResponse.getAccounts());
                        } catch (Exception e) {
                            showError("خطأ في تحديث جدول الحسابات: " + e.getMessage());
                            e.printStackTrace();
                            return;
                        }
                    } else {
                        String errorMessage = "فشل في تحميل البيانات";
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e("AccountSummary", "Error response: " + errorBody);
                                if (errorBody.contains("error")) {
                                    errorMessage = errorBody;
                                } else {
                                    errorMessage += ": " + errorBody;
                                }
                            } else {
                                errorMessage += " (رمز الخطأ: " + response.code() + ")";
                            }
                        } catch (IOException e) {
                            errorMessage += " (رمز الخطأ: " + response.code() + ")";
                            Log.e("AccountSummary", "Error reading error body", e);
                        }
                        showError(errorMessage);
                    }
                } catch (Exception e) {
                    String errorMessage = "حدث خطأ في معالجة البيانات: " + e.getMessage();
                    Log.e("AccountSummary", "Error processing response", e);
                    showError(errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AccountSummaryResponse> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                String errorMessage = "حدث خطأ في الاتصال: " + t.getMessage();
                Log.e("AccountSummary", "Network error", t);
                showError(errorMessage);
            }
        });
    }

    private void updateTitle(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            binding.summaryTitleTextView.setText("ملخص الحسابات لرقم: " + phoneNumber);
        } else {
            binding.summaryTitleTextView.setText("ملخص الحسابات");
        }
    }

    private void updateSummaryTable(List<CurrencySummary> summaries) {
        try {
            TableLayout table = binding.summaryTable;
            table.removeAllViews();

            // إضافة رأس الجدول
            addTableRow(table, new String[]{"العملة", "الرصيد", "المدين", "الدائن"}, true);

            // إضافة البيانات
            if (summaries != null && !summaries.isEmpty()) {
                for (CurrencySummary summary : summaries) {
                    if (summary != null) {
                        addTableRow(table, new String[]{
                            summary.getCurrency() != null ? summary.getCurrency() : "-",
                            numberFormat.format(summary.getTotalBalance()),
                            numberFormat.format(summary.getTotalDebits()),
                            numberFormat.format(summary.getTotalCredits())
                        }, false);
                    }
                }
            } else {
                addTableRow(table, new String[]{"لا توجد بيانات", "-", "-", "-"}, false);
            }
        } catch (Exception e) {
            showError("خطأ في عرض ملخص العملات: " + e.getMessage());
        }
    }

    private void updateDetailsTable(List<AccountSummary> accounts) {
        try {
            TableLayout table = binding.detailsTable;
            table.removeAllViews();

            // إضافة رأس الجدول
            addTableRow(table, new String[]{"ID", "الاسم", "العملة", "الرصيد", "المدين", "الدائن"}, true);

            // إضافة البيانات
            if (accounts != null && !accounts.isEmpty()) {
                for (AccountSummary account : accounts) {
                    if (account != null) {
                        addTableRow(table, new String[]{
                            String.valueOf(account.getUserId()),
                            account.getUserName() != null ? account.getUserName() : "-",
                            account.getCurrency() != null ? account.getCurrency() : "-",
                            numberFormat.format(account.getBalance()),
                            numberFormat.format(account.getTotalDebits()),
                            numberFormat.format(account.getTotalCredits())
                        }, false);
                    }
                }
            } else {
                addTableRow(table, new String[]{"لا توجد بيانات", "-", "-", "-", "-", "-"}, false);
            }
        } catch (Exception e) {
            showError("خطأ في عرض تفاصيل الحسابات: " + e.getMessage());
        }
    }

    private void addTableRow(TableLayout table, String[] values, boolean isHeader) {
        try {
            TableRow row = new TableRow(requireContext());
            row.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            ));

            for (String value : values) {
                TextView textView = new TextView(requireContext());
                textView.setText(value != null ? value : "-");
                textView.setPadding(16, 12, 16, 12);
                textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                
                if (isHeader) {
                    textView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray));
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                    textView.setTextSize(16);
                    textView.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    textView.setTextSize(14);
                }
                
                row.addView(textView);
            }

            table.addView(row);
        } catch (Exception e) {
            showError("خطأ في إضافة صف للجدول: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private String getPhoneNumber() {
        try {
            UserPreferences userPreferences = new UserPreferences(requireContext());
            String phoneNumber = userPreferences.getPhoneNumber();
            
            // التحقق من صحة الرقم
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                showError("لم يتم العثور على رقم الهاتف");
                return null;
            }
            
            // إزالة المسافات فقط
            phoneNumber = phoneNumber.trim();
            
            return phoneNumber;
        } catch (Exception e) {
            showError("حدث خطأ في جلب رقم الهاتف");
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 