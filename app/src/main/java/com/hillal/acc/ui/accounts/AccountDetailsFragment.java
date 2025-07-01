package com.hillal.acc.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.Toast;
import android.widget.DatePicker;
import android.widget.Button;
import android.app.DatePickerDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.card.MaterialCardView;
import com.hillal.acc.R;
import com.hillal.acc.data.model.Account;
import com.hillal.acc.data.model.Transaction;
import com.hillal.acc.ui.AccountStatementActivity;
import com.hillal.acc.viewmodel.AccountViewModel;
import com.hillal.acc.viewmodel.TransactionViewModel;
import com.hillal.acc.databinding.FragmentAccountDetailsBinding;
import androidx.navigation.fragment.NavHostFragment;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AccountDetailsFragment extends Fragment {
    private FragmentAccountDetailsBinding binding;
    private AccountViewModel accountViewModel;
    private TransactionViewModel transactionViewModel;
    private TextView accountName;
    private TextView accountBalance;
    private TextView accountPhone;
    private TextView accountNotes;
    private long accountId;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    private Date startDate;
    private Date endDate;
    private Button dateRangeButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountId = getArguments() != null ? getArguments().getLong("accountId") : -1;
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        
        // Set default date range to last 30 days
        Calendar calendar = Calendar.getInstance();
        endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        startDate = calendar.getTime();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        loadAccountDetails();
        setupDateRangePicker();
        updateStatistics();
        // ضبط insets للجذر لرفع المحتوى مع الكيبورد وأزرار النظام
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
            if (bottom == 0) {
                bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
            }
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return insets;
        });
    }

    private void setupViews() {
        accountName = binding.accountName;
        accountBalance = binding.accountBalance;
        accountPhone = binding.accountPhone;
        accountNotes = binding.accountNotes;
        dateRangeButton = binding.dateRangeButton;

        // إضافة زر تعديل الحساب
        binding.editAccountButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("accountId", accountId);
            Navigation.findNavController(v).navigate(R.id.editAccountFragment, args);
        });

        // إضافة زر كشف الحساب التفصيلي
        binding.viewAccountStatementButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AccountStatementActivity.class);
            intent.putExtra("account_id", accountId);
            intent.putExtra("start_date", startDate.getTime());
            intent.putExtra("end_date", endDate.getTime());
            startActivity(intent);
        });
    }

    private void setupDateRangePicker() {
        dateRangeButton.setOnClickListener(v -> {
            showDateRangePickerDialog();
        });
        updateDateRangeButtonText();
    }

    private void showDateRangePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog startDatePicker = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, dayOfMonth);
                startDate = cal.getTime();
                
                // Show end date picker after start date is selected
                showEndDatePicker();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        startDatePicker.show();
    }

    private void showEndDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog endDatePicker = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, dayOfMonth);
                endDate = cal.getTime();
                
                if (endDate.before(startDate)) {
                    Toast.makeText(requireContext(), "تاريخ البداية يجب أن يكون قبل تاريخ النهاية", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                updateDateRangeButtonText();
                updateStatistics();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        endDatePicker.show();
    }

    private void updateDateRangeButtonText() {
        String dateRange = String.format(Locale.US, "%s - %s",
            DATE_FORMAT.format(startDate),
            DATE_FORMAT.format(endDate));
        dateRangeButton.setText(dateRange);
    }

    private void updateStatistics() {
        PieChart pieChart = binding.pieChart;
        
        transactionViewModel.getTotalCreditForDateRange(accountId, startDate, endDate).observe(getViewLifecycleOwner(), credit -> {
            transactionViewModel.getTotalDebitForDateRange(accountId, startDate, endDate).observe(getViewLifecycleOwner(), debit -> {
                float totalCredit = credit != null ? credit.floatValue() : 0f;
                float totalDebit = debit != null ? debit.floatValue() : 0f;
                
                ArrayList<PieEntry> entries = new ArrayList<>();
                entries.add(new PieEntry(totalCredit, "له"));
                entries.add(new PieEntry(totalDebit, "عليه"));
                
                PieDataSet dataSet = new PieDataSet(entries, "");
                dataSet.setColors(new int[] {
                    getResources().getColor(R.color.credit_green),
                    getResources().getColor(R.color.debit_red)
                });
                
                PieData data = new PieData(dataSet);
                data.setValueTextSize(14f);
                data.setValueTextColor(getResources().getColor(R.color.white));
                
                pieChart.setData(data);
                pieChart.setUsePercentValues(true);
                pieChart.getDescription().setEnabled(false);
                pieChart.getLegend().setEnabled(true);
                pieChart.setEntryLabelTextSize(14f);
                pieChart.setEntryLabelColor(getResources().getColor(R.color.text_primary));
                pieChart.setHoleRadius(50f);
                pieChart.setTransparentCircleRadius(55f);
                pieChart.setDrawHoleEnabled(true);
                pieChart.setHoleColor(getResources().getColor(R.color.background));
                pieChart.invalidate();
            });
        });
    }

    private void loadAccountDetails() {
        accountViewModel.getAccountById(accountId).observe(getViewLifecycleOwner(), account -> {
            if (account != null) {
                updateAccountDetails(account);
            }
        });
    }

    private void updateAccountDetails(Account account) {
        accountName.setText(account.getName());
        accountPhone.setText(account.getPhoneNumber());
        accountNotes.setText(account.getNotes());
        
        // Update balance with proper formatting
        transactionViewModel.getAccountBalance(accountId).observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                String formattedBalance = String.format(Locale.US, "%,.2f ريال يمني", balance);
                accountBalance.setText(formattedBalance);
                accountBalance.setTextColor(balance >= 0 ? 
                    getResources().getColor(R.color.credit_green) : 
                    getResources().getColor(R.color.debit_red));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 