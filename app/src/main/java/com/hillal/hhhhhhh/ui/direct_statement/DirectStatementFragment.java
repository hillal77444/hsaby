package com.hillal.hhhhhhh.ui.direct_statement;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.data.remote.RetrofitClient;
import com.hillal.hhhhhhh.databinding.FragmentDirectStatementBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DirectStatementFragment extends Fragment {
    private FragmentDirectStatementBinding binding;
    private ApiService apiService;
    private TextInputEditText fromDateInput;
    private TextInputEditText toDateInput;
    private RecyclerView accountsRecyclerView;
    private RecyclerView transactionsRecyclerView;
    private DirectStatementAdapter accountsAdapter;
    private TransactionAdapter transactionsAdapter;
    private TextView totalBalanceText;
    private TextView totalDebitText;
    private TextView totalCreditText;
    private String phoneNumber;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiService = RetrofitClient.getApiService();
        if (getArguments() != null) {
            phoneNumber = getArguments().getString("phoneNumber");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDirectStatementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // إخفاء حقول التاريخ من الشاشة الرئيسية
        binding.dateRangeLayout.setVisibility(View.GONE);
        binding.generateButton.setVisibility(View.GONE);
        
        setupRecyclerViews();
        initializeApiService();
        loadData();
    }

    private void setupRecyclerViews() {
        // إعداد قائمة الحسابات
        accountsAdapter = new DirectStatementAdapter();
        accountsAdapter.setOnAccountClickListener(account -> {
            // عند النقر على حساب، نعرض حقول التاريخ وزر إنشاء الكشف
            binding.dateRangeLayout.setVisibility(View.VISIBLE);
            binding.generateButton.setVisibility(View.VISIBLE);
            loadAccountTransactions(account.getUserId());
        });
        accountsRecyclerView = binding.accountsRecyclerView;
        accountsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        accountsRecyclerView.setAdapter(accountsAdapter);

        // إعداد قائمة المعاملات
        transactionsAdapter = new TransactionAdapter();
        transactionsRecyclerView = binding.transactionsRecyclerView;
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        transactionsRecyclerView.setAdapter(transactionsAdapter);

        // إعداد مستمع لزر إنشاء الكشف
        binding.generateButton.setOnClickListener(v -> {
            String fromDate = fromDateInput.getText().toString();
            String toDate = toDateInput.getText().toString();
            loadAccountTransactions(accountsAdapter.getSelectedAccountId(), fromDate, toDate);
        });
    }

    private void initializeApiService() {
        apiService = RetrofitClient.getApiService();
    }

    private void loadData() {
        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "لا يوجد اتصال بالإنترنت", Toast.LENGTH_LONG).show();
            return;
        }

        showLoadingDialog("جاري تحميل البيانات...");
        
        if (phoneNumber != null) {
            apiService.getAccountsSummary(phoneNumber).enqueue(new Callback<AccountSummaryResponse>() {
                @Override
                public void onResponse(@NonNull Call<AccountSummaryResponse> call, 
                                     @NonNull Response<AccountSummaryResponse> response) {
                    hideLoadingDialog();
                    
                    if (response.isSuccessful() && response.body() != null) {
                        AccountSummaryResponse summary = response.body();
                        accountsAdapter.setAccounts(summary.getAccounts());
                        
                        // تحديث الإجماليات
                        binding.totalBalanceText.setText(String.format("الرصيد الإجمالي: %.2f يمني", summary.getTotalBalance()));
                        binding.totalDebitText.setText(String.format("إجمالي الديون: %.2f يمني", summary.getTotalDebits()));
                        binding.totalCreditText.setText(String.format("إجمالي المدفوعات: %.2f يمني", summary.getTotalCredits()));
                    } else {
                        Toast.makeText(requireContext(), "فشل في جلب البيانات", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AccountSummaryResponse> call, 
                                    @NonNull Throwable t) {
                    hideLoadingDialog();
                    Toast.makeText(requireContext(), "فشل في جلب البيانات", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            hideLoadingDialog();
            Toast.makeText(requireContext(), "رقم الهاتف غير متوفر", Toast.LENGTH_LONG).show();
        }
    }

    private void loadAccountTransactions(int userId) {
        loadAccountTransactions(userId, null, null);
    }

    private void loadAccountTransactions(int userId, String fromDate, String toDate) {
        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "لا يوجد اتصال بالإنترنت", Toast.LENGTH_LONG).show();
            return;
        }

        showLoadingDialog("جاري تحميل المعاملات...");
        
        apiService.getDetailedTransactions(phoneNumber, userId, fromDate, toDate)
            .enqueue(new Callback<TransactionResponse>() {
                @Override
                public void onResponse(@NonNull Call<TransactionResponse> call, 
                                     @NonNull Response<TransactionResponse> response) {
                    hideLoadingDialog();
                    
                    if (response.isSuccessful() && response.body() != null) {
                        transactionsAdapter.setTransactions(response.body().getTransactions());
                    } else {
                        Toast.makeText(requireContext(), "فشل في جلب المعاملات", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<TransactionResponse> call, 
                                    @NonNull Throwable t) {
                    hideLoadingDialog();
                    Toast.makeText(requireContext(), "فشل في جلب المعاملات", Toast.LENGTH_LONG).show();
                }
            });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void showLoadingDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        binding = null;
    }
} 