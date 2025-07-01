package com.hillal.acc.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.core.view.ViewCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.hillal.acc.R;
import com.hillal.acc.databinding.FragmentEditProfileBinding;
import com.hillal.acc.data.preferences.UserPreferences;
import com.hillal.acc.data.repository.AuthRepository;
import com.hillal.acc.data.model.User;

public class EditProfileFragment extends Fragment {
    private FragmentEditProfileBinding binding;
    private UserPreferences userPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userPreferences = new UserPreferences(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // تعبئة حقل الاسم الحالي
        String currentName = userPreferences.getUserName();
        binding.userNameInput.setText(currentName);

        // إعداد زر الحفظ
        binding.saveButton.setOnClickListener(v -> saveUserName());

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

    private void saveUserName() {
        String newName = binding.userNameInput.getText().toString().trim();
        
        if (newName.isEmpty()) {
            Toast.makeText(requireContext(), "الرجاء إدخال اسم المستخدم", Toast.LENGTH_SHORT).show();
            return;
        }

        // إرسال الاسم الجديد للسيرفر
        AuthRepository authRepository = new AuthRepository(requireActivity().getApplication());
        authRepository.updateUserNameOnServer(newName, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                // حفظ الاسم الجديد محليًا فقط إذا نجح في السيرفر
                userPreferences.saveUserName(newName);
                Toast.makeText(requireContext(), "تم حفظ الاسم بنجاح", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "فشل تحديث الاسم: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 