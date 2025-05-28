package com.hillal.hhhhhhh.ui.summary;

import android.content.Context;
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
        apiService = RetrofitClient.getInstance().getApiService();
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

        // تسجيل معلومات الطلب بشكل مفصل
        Log.d("AccountSummary", "=== بداية طلب ملخص الحسابات ===");
        Log.d("AccountSummary", "رقم الهاتف: " + phoneNumber);
        Log.d("AccountSummary", "رابط الطلب: http://212.224.88.122:5007/api/accounts/summary/" + phoneNumber);
        Log.d("AccountSummary", "وقت الطلب: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date()));

        try {
            apiService.getAccountSummary(phoneNumber).enqueue(new Callback<AccountSummaryResponse>() {
                @Override
                public void onResponse(@NonNull Call<AccountSummaryResponse> call, @NonNull Response<AccountSummaryResponse> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    
                    // تسجيل معلومات الاستجابة بشكل مفصل
                    Log.d("AccountSummary", "=== استلام استجابة من الخادم ===");
                    Log.d("AccountSummary", "رمز الاستجابة: " + response.code());
                    Log.d("AccountSummary", "نوع الاستجابة: " + response.headers().get("Content-Type"));
                    Log.d("AccountSummary", "حجم الاستجابة: " + (response.body() != null ? response.body().toString().length() : 0) + " bytes");
                    
                    if (response.isSuccessful()) {
                        try {
                            AccountSummaryResponse summaryResponse = response.body();
                            if (summaryResponse == null) {
                                Log.e("AccountSummary", "جسم الاستجابة فارغ");
                                showError("لم يتم استلام أي بيانات من الخادم", "");
                                return;
                            }

                            // تسجيل محتوى الاستجابة
                            Log.d("AccountSummary", "=== محتوى الاستجابة ===");
                            Log.d("AccountSummary", "عدد الحسابات: " + (summaryResponse.getAccounts() != null ? summaryResponse.getAccounts().size() : 0));
                            Log.d("AccountSummary", "عدد العملات: " + (summaryResponse.getCurrencySummary() != null ? summaryResponse.getCurrencySummary().size() : 0));
                            
                            if (summaryResponse.getAccounts() != null) {
                                for (AccountSummary account : summaryResponse.getAccounts()) {
                                    Log.d("AccountSummary", "حساب: " + account.toString());
                                }
                            }
                            
                            if (summaryResponse.getCurrencySummary() != null) {
                                for (CurrencySummary currency : summaryResponse.getCurrencySummary()) {
                                    Log.d("AccountSummary", "عملة: " + currency.toString());
                                }
                            }

                            // التحقق من البيانات المستلمة
                            if (summaryResponse.getCurrencySummary() == null || summaryResponse.getCurrencySummary().isEmpty()) {
                                Log.e("AccountSummary", "ملخص العملات فارغ");
                                showError("لا توجد بيانات ملخص العملات", "");
                                return;
                            }

                            if (summaryResponse.getAccounts() == null || summaryResponse.getAccounts().isEmpty()) {
                                Log.e("AccountSummary", "قائمة الحسابات فارغة");
                                showError("لا توجد بيانات الحسابات", "");
                                return;
                            }

                            // تحديث الجداول في الـ UI thread
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    try {
                                        updateSummaryTable(summaryResponse.getCurrencySummary());
                                        updateDetailsTable(summaryResponse.getAccounts());
                                        Log.d("AccountSummary", "تم تحديث الجداول بنجاح");
                                    } catch (Exception e) {
                                        Log.e("AccountSummary", "خطأ في تحديث الجداول", e);
                                        showError("خطأ في تحديث الجداول: " + e.getMessage(), "");
                                    }
                                });
                            }
                        } catch (Exception e) {
                            Log.e("AccountSummary", "خطأ في معالجة الاستجابة", e);
                            showError("خطأ في معالجة البيانات: " + e.getMessage(), "");
                        }
                    } else {
                        String errorMessage = "فشل في تحميل البيانات";
                        String errorBody = "";
                        try {
                            if (response.errorBody() != null) {
                                errorBody = response.errorBody().string();
                                Log.e("AccountSummary", "جسم الخطأ: " + errorBody);
                                errorMessage += ": " + errorBody;
                            } else {
                                errorMessage += " (رمز الخطأ: " + response.code() + ")";
                            }
                        } catch (IOException e) {
                            Log.e("AccountSummary", "خطأ في قراءة جسم الخطأ", e);
                            errorMessage += " (رمز الخطأ: " + response.code() + ")";
                        }
                        showError(errorMessage, errorBody);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AccountSummaryResponse> call, @NonNull Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("AccountSummary", "=== فشل في الاتصال ===");
                    Log.e("AccountSummary", "نوع الخطأ: " + t.getClass().getName());
                    Log.e("AccountSummary", "رسالة الخطأ: " + t.getMessage());
                    if (t.getCause() != null) {
                        Log.e("AccountSummary", "سبب الخطأ: " + t.getCause().getMessage());
                    }
                    String errorMessage = "حدث خطأ في الاتصال: " + t.getMessage();
                    showError(errorMessage, "");
                }
            });
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            Log.e("AccountSummary", "=== خطأ في إرسال الطلب ===");
            Log.e("AccountSummary", "نوع الخطأ: " + e.getClass().getName());
            Log.e("AccountSummary", "رسالة الخطأ: " + e.getMessage());
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
            showError("خطأ في إضافة صف للجدول: " + e.getMessage(), "");
        }
    }

    private void showError(String message, String responseData) {
        try {
            // طباعة رسالة الخطأ في السجل
            Log.e("AccountSummary", "Error: " + message);
            Log.e("AccountSummary", "Response Data: " + responseData);
            
            // الحصول على رقم الهاتف
            String phoneNumber = getPhoneNumber();
            
            // الحصول على رابط الطلب مباشرة من ApiService
            String endpoint = apiService.getAccountSummary(phoneNumber).request().url().toString();
            
            // إضافة معلومات إضافية للرسالة
            String detailedMessage = "تفاصيل الخطأ:\n" +
                    "الوقت: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date()) + "\n" +
                    "الرسالة: " + message + "\n" +
                    "نوع الخطأ: " + (message.contains("null") ? "خطأ في البيانات" : "خطأ في المعالجة") + "\n" +
                    "سبب الخطأ: " + (message.contains("ClassCastException") ? 
                        "خطأ في تحويل نوع البيانات - تأكد من تطابق أنواع البيانات مع الخادم" : 
                        "خطأ غير معروف") + "\n" +
                    "رابط الطلب: " + endpoint + "\n" +
                    "رقم الهاتف: " + (phoneNumber != null ? phoneNumber : "غير متوفر") + "\n" +
                    "البيانات المستلمة من السيرفر:\n" + (responseData != null && !responseData.isEmpty() ? responseData : "لا توجد بيانات متاحة") + "\n" +
                    "تفاصيل إضافية:\n" +
                    "- تأكد من أن السيرفر يعمل بشكل صحيح\n" +
                    "- تأكد من صحة رقم الهاتف\n" +
                    "- تأكد من وجود اتصال بالإنترنت";
            
            // نسخ رسالة الخطأ إلى الحافظة
            if (getContext() != null) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Error Details", detailedMessage);
                clipboard.setPrimaryClip(clip);
                
                // عرض رسالة الخطأ في Toast مع إشعار بنسخ التفاصيل
                Toast.makeText(getContext(), "تم نسخ تفاصيل الخطأ إلى الحافظة\n" + message, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("AccountSummary", "Error showing error message", e);
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