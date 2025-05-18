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
import com.hillal.hhhhhhh.databinding.FragmentLoginBinding;
import com.hillal.hhhhhhh.viewmodel.AuthViewModel;
import com.hillal.hhhhhhh.MainActivity;

public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
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

        binding.buttonLogin.setOnClickListener(v -> {
            String phone = binding.editTextPhone.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString();

            // التحقق من صحة المدخلات
            if (phone.isEmpty()) {
                binding.editTextPhone.setError("الرجاء إدخال رقم الهاتف");
                return;
            }
            if (password.isEmpty()) {
                binding.editTextPassword.setError("الرجاء إدخال كلمة المرور");
                return;
            }

            // تعطيل الزر أثناء عملية تسجيل الدخول
            binding.buttonLogin.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);

            Log.d(TAG, "Attempting to login with phone: " + phone);
            
            authViewModel.login(phone, password, new AuthViewModel.AuthCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Login successful");
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show();
                    
                    // التنقل إلى لوحة التحكم
                    NavHostFragment.findNavController(LoginFragment.this)
                            .navigate(R.id.navigation_dashboard);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Login failed: " + error);
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonLogin.setEnabled(true);
                    
                    // رسائل خطأ أكثر تفصيلاً
                    String errorMessage;
                    if (error.contains("UnknownHostException")) {
                        errorMessage = "لا يمكن الوصول إلى الخادم. يرجى التحقق من اتصال الإنترنت";
                    } else if (error.contains("SocketTimeoutException")) {
                        errorMessage = "انتهت مهلة الاتصال بالخادم. يرجى المحاولة مرة أخرى";
                    } else if (error.contains("401")) {
                        errorMessage = "رقم الهاتف أو كلمة المرور غير صحيحة";
                    } else {
                        errorMessage = "فشل تسجيل الدخول: " + error;
                    }
                    
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        binding.buttonRegister.setOnClickListener(v -> 
            NavHostFragment.findNavController(LoginFragment.this)
                    .navigate(R.id.action_loginFragment_to_registerFragment)
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