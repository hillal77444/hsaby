package com.hillal.hhhhhhh.ui.direct_statement;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.data.model.AccountSummary;
import com.hillal.hhhhhhh.data.model.AccountSummaryResponse;
import com.hillal.hhhhhhh.data.model.TransactionResponse;
import com.hillal.hhhhhhh.data.remote.ApiClient;
import com.hillal.hhhhhhh.data.remote.ApiService;
import com.hillal.hhhhhhh.databinding.FragmentDirectStatementBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class DirectStatementFragment extends Fragment {
    private FragmentDirectStatementBinding binding;
    private DirectStatementAdapter accountsAdapter;
    private TransactionAdapter transactionsAdapter;
    private ApiService apiService;
    private ProgressDialog progressDialog;
    private String phoneNumber;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDirectStatementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = ApiClient.getClient().create(ApiService.class);
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("جاري تحميل البيانات...");
        
        setupRecyclerViews();
        hideDateFields();
        loadData();
    }

    private void setupRecyclerViews() {
        accountsAdapter = new DirectStatementAdapter(account -> {
            showDateFields();
            loadAccountTransactions(account.getUserId());
        });
        
        transactionsAdapter = new TransactionAdapter();
        
        binding.accountsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.accountsRecyclerView.setAdapter(accountsAdapter);
        
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsRecyclerView.setAdapter(transactionsAdapter);
    }

    private void hideDateFields() {
        binding.dateRangeLayout.setVisibility(View.GONE);
        binding.generateButton.setVisibility(View.GONE);
    }

    private void showDateFields() {
        binding.dateRangeLayout.setVisibility(View.VISIBLE);
        binding.generateButton.setVisibility(View.VISIBLE);
    }

    private void loadData() {
        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "لا يوجد اتصال بالإنترنت", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog.show();
        apiService.getAccountsSummary(phoneNumber).enqueue(new Callback<AccountSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<AccountSummaryResponse> call, @NonNull Response<AccountSummaryResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    AccountSummaryResponse summary = response.body();
                    accountsAdapter.setAccounts(summary.getAccounts());
                    updateSummaryTotals(summary);
                } else {
                    Toast.makeText(requireContext(), "فشل في تحميل البيانات", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AccountSummaryResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "حدث خطأ في الاتصال", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadAccountTransactions(long userId) {
        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "لا يوجد اتصال بالإنترنت", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog.show();
        String fromDate = binding.fromDateInput.getText().toString();
        String toDate = binding.toDateInput.getText().toString();

        apiService.getDetailedTransactions(phoneNumber, userId, fromDate, toDate)
                .enqueue(new Callback<TransactionResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TransactionResponse> call, @NonNull Response<TransactionResponse> response) {
                        progressDialog.dismiss();
                        if (response.isSuccessful() && response.body() != null) {
                            TransactionResponse transactionResponse = response.body();
                            transactionsAdapter.setTransactions(transactionResponse.getTransactions());
                            updateCurrentBalance(transactionResponse.getCurrentBalance());
                        } else {
                            Toast.makeText(requireContext(), "فشل في تحميل المعاملات", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TransactionResponse> call, @NonNull Throwable t) {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "حدث خطأ في الاتصال", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateSummaryTotals(AccountSummaryResponse summary) {
        binding.totalBalanceText.setText(String.format("إجمالي الرصيد: %.2f", summary.getTotalBalance()));
        binding.totalDebitsText.setText(String.format("إجمالي المدفوعات: %.2f", summary.getTotalDebits()));
        binding.totalCreditsText.setText(String.format("إجمالي الديون: %.2f", summary.getTotalCredits()));
    }

    private void updateCurrentBalance(double currentBalance) {
        binding.currentBalanceText.setText(String.format("الرصيد الحالي: %.2f", currentBalance));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 