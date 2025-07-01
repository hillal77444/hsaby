package com.hillal.acc.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.hillal.acc.BuildConfig;
import com.hillal.acc.R;

public class AboutFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_about, container, false);

        // تعيين رقم الإصدار
        TextView versionTextView = root.findViewById(R.id.textViewVersion);
        versionTextView.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));

        // إعداد زر سياسة الخصوصية
        root.findViewById(R.id.buttonPrivacyPolicy).setOnClickListener(v -> {
            String privacyPolicyUrl = "https://hillal.github.io/privacy-policy/";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
            startActivity(intent);
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
} 