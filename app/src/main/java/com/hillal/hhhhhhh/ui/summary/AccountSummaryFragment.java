package com.hillal.hhhhhhh.ui.summary;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
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
    private float scale = 1.0f;
    private ScaleGestureDetector scaleGestureDetector;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 2.0f;

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
        setupScaleGestureDetector();
        loadAccountSummary();
    }

    private void setupNumberFormat() {
        numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);
    }

    private void setupApiService() {
        apiService = RetrofitClient.getInstance().getApiService();
    }

    private void setupScaleGestureDetector() {
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleListener());
        
        // إضافة معالج اللمس للجداول
        binding.summaryTable.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
        
        binding.detailsTable.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            try {
                scale *= detector.getScaleFactor();
                // تحديد حدود التكبير والتصغير
                scale = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
                
                // تطبيق التكبير على الجداول
                binding.summaryTable.setScaleX(scale);
                binding.summaryTable.setScaleY(scale);
                binding.detailsTable.setScaleX(scale);
                binding.detailsTable.setScaleY(scale);
                
                return true;
            } catch (Exception e) {
                Log.e("AccountSummary", "خطأ في التكبير والتصغير", e);
                return false;
            }
        }
    }

    private void loadAccountSummary() {
        String phoneNumber = getPhoneNumber();
        
        if (phoneNumber == null) {
            binding.progressBar.setVisibility(View.GONE);
            showError("لم يتم العثور على رقم الهاتف", "");
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        updateTitle(phoneNumber);

        try {
            apiService.getAccountSummary(phoneNumber).enqueue(new Callback<AccountSummaryResponse>() {
                @Override
                public void onResponse(@NonNull Call<AccountSummaryResponse> call, @NonNull Response<AccountSummaryResponse> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    
                    if (response.isSuccessful()) {
                        try {
                            AccountSummaryResponse summaryResponse = response.body();
                            if (summaryResponse == null) {
                                showError("لم يتم استلام أي بيانات من الخادم", "");
                                return;
                            }

                            // التحقق من البيانات المستلمة
                            if (summaryResponse.getCurrencySummary() == null || summaryResponse.getCurrencySummary().isEmpty()) {
                                showError("لا توجد بيانات ملخص العملات", "");
                                return;
                            }

                            if (summaryResponse.getAccounts() == null || summaryResponse.getAccounts().isEmpty()) {
                                showError("لا توجد بيانات الحسابات", "");
                                return;
                            }

                            // تحديث الجداول في الـ UI thread
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    try {
                                        updateSummaryTable(summaryResponse.getCurrencySummary());
                                        updateDetailsTable(summaryResponse.getAccounts());
                                    } catch (Exception e) {
                                        showError("خطأ في تحديث الجداول: " + e.getMessage(), "");
                                    }
                                });
                            }
                        } catch (Exception e) {
                            showError("خطأ في معالجة البيانات: " + e.getMessage(), "");
                        }
                    } else {
                        String errorMessage = "فشل في تحميل البيانات";
                        try {
                            if (response.errorBody() != null) {
                                errorMessage += ": " + response.errorBody().string();
                            } else {
                                errorMessage += " (رمز الخطأ: " + response.code() + ")";
                            }
                        } catch (IOException e) {
                            errorMessage += " (رمز الخطأ: " + response.code() + ")";
                        }
                        showError(errorMessage, "");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AccountSummaryResponse> call, @NonNull Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    showError("حدث خطأ في الاتصال: " + t.getMessage(), "");
                }
            });
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            showError("خطأ في إرسال الطلب: " + e.getMessage(), "");
        }
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
            showError("خطأ في عرض ملخص العملات: " + e.getMessage(), "");
        }
    }

    private void updateDetailsTable(List<AccountSummary> accounts) {
        try {
            TableLayout table = binding.detailsTable;
            table.removeAllViews();
            // إعادة تعيين مقياس التكبير عند تحديث الجدول
            scale = 1.0f;
            table.setScaleX(scale);
            table.setScaleY(scale);

            // إضافة رأس الجدول
            addTableRow(table, new String[]{"الاسم", "العملة", "لك", "عليك", "الرصيد", "ID"}, true);

            // إضافة البيانات
            if (accounts != null && !accounts.isEmpty()) {
                for (AccountSummary account : accounts) {
                    if (account != null) {
                        addTableRow(table, new String[]{
                            account.getUserName() != null ? account.getUserName() : "-",
                            account.getCurrency() != null ? account.getCurrency() : "-",
                            numberFormat.format(account.getTotalCredits()),
                            numberFormat.format(account.getTotalDebits()),
                            numberFormat.format(account.getBalance()),
                            String.valueOf(account.getUserId())
                        }, false);
                    }
                }
            } else {
                addTableRow(table, new String[]{"لا توجد بيانات", "-", "-", "-", "-", "-"}, false);
            }
        } catch (Exception e) {
            showError("خطأ في عرض تفاصيل الحسابات: " + e.getMessage(), "");
        }
    }

    private void addTableRow(TableLayout table, String[] values, boolean isHeader) {
        try {
            TableRow row = new TableRow(requireContext());
            row.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            ));

            // تحديد الأوزان النسبية للأعمدة
            float[] weights = {0.25f, 0.15f, 0.15f, 0.15f, 0.15f, 0.15f}; // مجموع الأوزان = 1

            for (int i = 0; i < values.length; i++) {
                TextView textView = new TextView(requireContext());
                textView.setText(values[i] != null ? values[i] : "-");
                textView.setPadding(4, 4, 4, 4);
                textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                
                // السماح بعرض النص في عدة أسطر
                textView.setSingleLine(false);
                textView.setMaxLines(Integer.MAX_VALUE);
                
                // تعيين الوزن النسبي للعمود
                TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT);
                params.weight = weights[i];
                textView.setLayoutParams(params);
                
                if (isHeader) {
                    textView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_gray));
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                    textView.setTextSize(14);
                    textView.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    textView.setTextSize(12);
                }
                
                row.addView(textView);
            }

            table.addView(row);
        } catch (Exception e) {
            showError("خطأ في إضافة صف للجدول: " + e.getMessage(), "");
        }
    }

    private void showError(String message, String responseData) {
        try {
            // طباعة رسالة الخطأ في السجل
            Log.e("AccountSummary", "Error: " + message);
            
            // عرض رسالة Toast بسيطة للمستخدم
            if (getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("AccountSummary", "خطأ في عرض رسالة الخطأ", e);
        }
    }

    private String getAppVersion() {
        try {
            return getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "غير معروف";
        }
    }

    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private String getNetworkType() {
        try {
            android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                if (activeNetworkInfo.getType() == android.net.ConnectivityManager.TYPE_WIFI) {
                    return "WiFi";
                } else if (activeNetworkInfo.getType() == android.net.ConnectivityManager.TYPE_MOBILE) {
                    return "بيانات الجوال";
                }
            }
            return "غير معروف";
        } catch (Exception e) {
            return "غير معروف";
        }
    }

    private String getSignalStrength() {
        try {
            android.telephony.TelephonyManager telephonyManager = (android.telephony.TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            android.telephony.SignalStrength signalStrength = telephonyManager.getSignalStrength();
            if (signalStrength != null) {
                return signalStrength.getLevel() + "/4";
            }
            return "غير معروف";
        } catch (Exception e) {
            return "غير معروف";
        }
    }

    private String getPhoneNumber() {
        try {
            UserPreferences userPreferences = new UserPreferences(requireContext());
            String phoneNumber = userPreferences.getPhoneNumber();
            
            // التحقق من صحة الرقم
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                showError("لم يتم العثور على رقم الهاتف", "");
                return null;
            }
            
            // إزالة المسافات فقط
            phoneNumber = phoneNumber.trim();
            
            return phoneNumber;
        } catch (Exception e) {
            showError("حدث خطأ في جلب رقم الهاتف", "");
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 