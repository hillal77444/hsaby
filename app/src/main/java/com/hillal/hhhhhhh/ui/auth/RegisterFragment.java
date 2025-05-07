package com.hillal.hhhhhhh.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.databinding.FragmentRegisterBinding;
import com.hillal.hhhhhhh.viewmodel.AuthViewModel;

public class RegisterFragment extends Fragment {
    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRegisterButton();
        observeAuthState();

        return root;
    }

    private void setupRegisterButton() {
        binding.buttonRegister.setOnClickListener(v -> {
            String fullName = binding.editTextFullName.getText().toString();
            String displayName = binding.editTextDisplayName.getText().toString();
            String phone = binding.editTextPhone.getText().toString();
            String password = binding.editTextPassword.getText().toString();
            String confirmPassword = binding.editTextConfirmPassword.getText().toString();

            // التحقق من صحة المدخلات
            if (fullName.isEmpty() || displayName.isEmpty() || phone.isEmpty() || 
                password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(getContext(), "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(getContext(), "كلمات المرور غير متطابقة", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(getContext(), "يجب أن تكون كلمة المرور 6 أحرف على الأقل", Toast.LENGTH_SHORT).show();
                return;
            }

            // دمج الاسم الكامل واسم العرض في حقل واحد
            String username = fullName + " (" + displayName + ")";
            authViewModel.register(username, phone, password);
        });
    }

    private void observeAuthState() {
        authViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case LOADING:
                    binding.buttonRegister.setEnabled(false);
                    break;
                case SUCCESS:
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_register_to_home);
                    break;
                case ERROR:
                    binding.buttonRegister.setEnabled(true);
                    Toast.makeText(getContext(), "فشل إنشاء الحساب", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    binding.buttonRegister.setEnabled(true);
                    break;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 