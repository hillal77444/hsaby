package com.hillal.hhhhhhh;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.hillal.hhhhhhh.databinding.FragmentHomeBinding;
import com.hillal.hhhhhhh.db.AppDatabase;
import com.hillal.hhhhhhh.db.Report;
import com.hillal.hhhhhhh.db.ReportDao;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private ReportAdapter adapter;
    private ReportDao reportDao;
    private List<Report> reports = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        try {
            AppDatabase db = AppDatabase.getDatabase(requireContext());
            reportDao = db.reportDao();
            Log.d(TAG, "Database initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database", e);
            Toast.makeText(requireContext(), "خطأ في تهيئة قاعدة البيانات", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started");
        try {
            binding = FragmentHomeBinding.inflate(inflater, container, false);
            View root = binding.getRoot();
            Log.d(TAG, "Layout inflated successfully");

            setupRecyclerView();
            setupAddButton();
            loadReports();
            Log.d(TAG, "onCreateView completed successfully");
            return root;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(requireContext(), "خطأ في تهيئة الواجهة", Toast.LENGTH_LONG).show();
            throw e;
        }
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView");
        try {
            adapter = new ReportAdapter(reports, report -> {
                Log.d(TAG, "Report clicked: " + report.id);
                Toast.makeText(requireContext(), "تم النقر على التقرير: " + report.title, Toast.LENGTH_SHORT).show();
            });
            binding.reportsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.reportsRecyclerView.setAdapter(adapter);
            Log.d(TAG, "RecyclerView setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            Toast.makeText(requireContext(), "خطأ في إعداد قائمة التقارير", Toast.LENGTH_LONG).show();
        }
    }

    private void setupAddButton() {
        Log.d(TAG, "Setting up Add Button");
        try {
            binding.addReportButton.setOnClickListener(v -> {
                Log.d(TAG, "Add button clicked");
                Toast.makeText(requireContext(), "تم النقر على زر الإضافة", Toast.LENGTH_SHORT).show();
                // TODO: Add new report dialog
            });
            Log.d(TAG, "Add Button setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Add Button", e);
            Toast.makeText(requireContext(), "خطأ في إعداد زر الإضافة", Toast.LENGTH_LONG).show();
        }
    }

    private void loadReports() {
        Log.d(TAG, "Loading reports");
        try {
            if (reportDao != null) {
                reports.clear();
                reports.addAll(reportDao.getAllReports());
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Reports loaded successfully. Count: " + reports.size());
            } else {
                Log.e(TAG, "ReportDao is null");
                Toast.makeText(requireContext(), "خطأ في تحميل التقارير", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading reports", e);
            Toast.makeText(requireContext(), "خطأ في تحميل التقارير", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        binding = null;
    }
} 