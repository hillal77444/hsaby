package com.hillal.hhhhhhh.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputEditText;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.databinding.FragmentEditProfileBinding;
import com.hillal.hhhhhhh.data.preferences.UserPreferences;
import com.hillal.hhhhhhh.data.repository.AuthRepository;
import com.hillal.hhhhhhh.data.model.User;

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