package com.hillal.hhhhhhh.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.databinding.FragmentRegisterBinding;
import com.hillal.hhhhhhh.viewmodel.AuthViewModel;
import com.hillal.hhhhhhh.ui.MainActivity;

public class RegisterFragment extends Fragment {
    private static final String TAG = "RegisterFragment";
    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // إخفاء شريط التنقل السفلي
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(false);
        }
        
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.buttonRegister.setOnClickListener(v -> {
            String fullName = binding.editTextFullName.getText().toString().trim();
            String displayName = binding.editTextDisplayName.getText().toString().trim();
            String phone = binding.editTextPhone.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString();
            String confirmPassword = binding.editTextConfirmPassword.getText().toString();

            // التحقق من صحة المدخلات
            if (fullName.isEmpty()) {
                binding.editTextFullName.setError("الرجاء إدخال الاسم الرباعي");
                return;
            }
            if (displayName.isEmpty()) {
                binding.editTextDisplayName.setError("الرجاء إدخال الاسم المستخدم في الإشعارات");
                return;
            }
            if (phone.isEmpty()) {
                binding.editTextPhone.setError("الرجاء إدخال رقم الهاتف");
                return;
            }
            if (password.isEmpty()) {
                binding.editTextPassword.setError("الرجاء إدخال كلمة المرور");
                return;
            }
            if (confirmPassword.isEmpty()) {
                binding.editTextConfirmPassword.setError("الرجاء تأكيد كلمة المرور");
                return;
            }
            if (!password.equals(confirmPassword)) {
                binding.editTextConfirmPassword.setError("كلمة المرور غير متطابقة");
                return;
            }

            // تعطيل الزر أثناء عملية التسجيل
            binding.buttonRegister.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);

            Log.d(TAG, "Attempting to register with phone: " + phone);
            
            // دمج الاسم الرباعي واسم العرض في اسم مستخدم واحد
            String username = fullName + " (" + displayName + ")";
            
            authViewModel.register(username, phone, password, new AuthViewModel.AuthCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Registration successful");
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "تم إنشاء الحساب بنجاح", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(RegisterFragment.this)
                            .navigate(R.id.action_registerFragment_to_loginFragment);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Registration failed: " + error);
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonRegister.setEnabled(true);
                    
                    // رسائل خطأ أكثر تفصيلاً
                    String errorMessage;
                    if (error.contains("UnknownHostException")) {
                        errorMessage = "لا يمكن الوصول إلى الخادم. يرجى التحقق من اتصال الإنترنت";
                    } else if (error.contains("SocketTimeoutException")) {
                        errorMessage = "انتهت مهلة الاتصال بالخادم. يرجى المحاولة مرة أخرى";
                    } else if (error.contains("500")) {
                        errorMessage = "خطأ في الخادم. يرجى المحاولة لاحقاً";
                    } else if (error.contains("400")) {
                        errorMessage = "بيانات غير صحيحة. يرجى التحقق من المدخلات";
                    } else if (error.contains("409")) {
                        errorMessage = "رقم الهاتف مسجل مسبقاً";
                    } else {
                        errorMessage = "فشل إنشاء الحساب: " + error;
                    }
                    
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        binding.buttonBackToLogin.setOnClickListener(v -> 
            NavHostFragment.findNavController(RegisterFragment.this)
                    .navigate(R.id.action_registerFragment_to_loginFragment)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // إظهار شريط التنقل السفلي عند مغادرة الصفحة
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(true);
        }
        binding = null;
    }
} 