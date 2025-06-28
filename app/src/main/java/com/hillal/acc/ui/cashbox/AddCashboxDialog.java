package com.hillal.acc.ui.cashbox;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.hillal.acc.R;

public class AddCashboxDialog extends DialogFragment {
    public interface OnCashboxAddedListener {
        void onCashboxAdded(String name);
    }
    private OnCashboxAddedListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof OnCashboxAddedListener) {
            listener = (OnCashboxAddedListener) getParentFragment();
        } else if (context instanceof OnCashboxAddedListener) {
            listener = (OnCashboxAddedListener) context;
        }
        android.util.Log.d("AddCashboxDialog", "Listener set: " + (listener != null ? "YES" : "NO"));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_cashbox, null);
        EditText editName = view.findViewById(R.id.edit_cashbox_name);
        return new AlertDialog.Builder(requireContext())
                .setTitle("إضافة صندوق جديد")
                .setView(view)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    android.util.Log.d("AddCashboxDialog", "Save button clicked, name: '" + name + "', listener: " + (listener != null ? "YES" : "NO"));
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "يرجى إدخال اسم الصندوق", Toast.LENGTH_SHORT).show();
                    } else if (listener != null) {
                        android.util.Log.d("AddCashboxDialog", "Calling listener.onCashboxAdded");
                        listener.onCashboxAdded(name);
                    } else {
                        android.util.Log.e("AddCashboxDialog", "Listener is null! Cannot add cashbox");
                        Toast.makeText(getContext(), "خطأ في إضافة الصندوق", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .create();
    }
} 