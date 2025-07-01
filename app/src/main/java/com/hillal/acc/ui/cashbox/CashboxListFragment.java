package com.hillal.acc.ui.cashbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;

import com.hillal.acc.R;
import com.hillal.acc.data.entities.Cashbox;
import com.hillal.acc.viewmodel.CashboxViewModel;

import java.util.List;

public class CashboxListFragment extends Fragment {
    private CashboxViewModel cashboxViewModel;
    private RecyclerView recyclerView;
    private CashboxListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cashbox_list, container, false);
        recyclerView = view.findViewById(R.id.recycler_cashboxes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CashboxListAdapter();
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cashboxViewModel = new ViewModelProvider(this).get(CashboxViewModel.class);
        cashboxViewModel.getAllCashboxes().observe(getViewLifecycleOwner(), this::updateList);
        // لا داعي لاستدعاء fetchCashboxesFromApi هنا، المزامنة تتم تلقائياً
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

    private void updateList(List<Cashbox> cashboxes) {
        adapter.submitList(cashboxes);
    }
} 