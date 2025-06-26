package com.hillal.acc.ui.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.app.ProgressDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.activity.OnBackPressedCallback;

import com.hillal.acc.R;
import com.hillal.acc.databinding.FragmentLoginBinding;
import com.hillal.acc.viewmodel.AuthViewModel;
import com.hillal.acc.MainActivity;
import com.hillal.acc.data.remote.DataManager;
import com.hillal.acc.data.room.AccountDao;
import com.hillal.acc.data.room.TransactionDao;
import com.hillal.acc.data.room.PendingOperationDao;

public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;
    private AuthViewModel authViewModel;
    private ProgressDialog loadingDialog;

    // دالة مساعدة لنسخ النص إلى الحافظة
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Error Details", text);
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // إضافة مستمع لرابط سياسة الخصوصية
        binding.buttonPrivacy.setOnClickListener(v -> {
            String url = "https://malyp.com/api/privacy-policy";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

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

            Log.d(TAG, "Attempting to login with phone: " + phone);
            
            authViewModel.login(phone, password, new AuthViewModel.AuthCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Login successful");
                    Toast.makeText(getContext(), "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show();

                    // حفظ اسم المستخدم في UserPreferences
                    com.hillal.acc.data.repository.AuthRepository authRepository = new com.hillal.acc.data.repository.AuthRepository(requireActivity().getApplication());
                    String userName = authRepository.getCurrentUser() != null ? authRepository.getCurrentUser().getUsername() : "";
                    com.hillal.acc.data.preferences.UserPreferences userPreferences = new com.hillal.acc.data.preferences.UserPreferences(requireContext());
                    userPreferences.saveUserName(userName);
                    // حفظ رقم الهاتف من الحقل
                    userPreferences.savePhoneNumber(phone);
                    
                    // إظهار Dialog التحميل أثناء جلب البيانات من السيرفر
                    loadingDialog = new ProgressDialog(getContext());
                    loadingDialog.setMessage("جاري تحميل البيانات...");
                    loadingDialog.setCancelable(false);
                    loadingDialog.show();
                    
                    // جلب البيانات من السيرفر بعد تسجيل الدخول
                    MainActivity mainActivity = (MainActivity) requireActivity();
                    DataManager dataManager = new DataManager(requireContext(), 
                        mainActivity.getAccountDao(),
                        mainActivity.getTransactionDao(),
                        mainActivity.getPendingOperationDao());
                        
                    dataManager.fetchDataFromServer(new DataManager.DataCallback() {
                        @Override
                        public void onSuccess() {
                            // إخفاء Dialog التحميل
                            if (loadingDialog != null && loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            Log.d(TAG, "Data fetched successfully");
                            // التنقل إلى لوحة التحكم بعد جلب البيانات
                            NavHostFragment.findNavController(LoginFragment.this)
                                    .navigate(R.id.navigation_dashboard);
                            ((MainActivity) requireActivity()).setBottomNavigationVisibility(true);
                        }

                        @Override
                        public void onError(String error) {
                            // إخفاء Dialog التحميل
                            if (loadingDialog != null && loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            Log.e(TAG, "Failed to fetch data: " + error);
                            
                            // تحضير رسالة الخطأ
                            String errorMessage;
                            if (error.contains("UNIQUE constraint failed")) {
                                errorMessage = "حدث خطأ في قاعدة البيانات المحلية. يرجى إعادة تشغيل التطبيق.";
                            } else if (error.contains("Network error")) {
                                errorMessage = "فشل الاتصال بالإنترنت. يرجى التحقق من اتصالك وإعادة المحاولة.";
                            } else if (error.contains("User not authenticated")) {
                                errorMessage = "انتهت صلاحية الجلسة. يرجى تسجيل الدخول مرة أخرى.";
                            } else {
                                errorMessage = "فشل في جلب البيانات: " + error;
                            }
                            
                            // نسخ تفاصيل الخطأ الكاملة إلى الحافظة
                            copyToClipboard("تفاصيل الخطأ:\n" + error);
                            
                            // عرض رسالة الخطأ
                            Toast.makeText(getContext(), errorMessage + "\nتم نسخ تفاصيل الخطأ إلى الحافظة", Toast.LENGTH_LONG).show();
                            
                            // التنقل إلى لوحة التحكم حتى في حالة فشل جلب البيانات
                            NavHostFragment.findNavController(LoginFragment.this)
                                    .navigate(R.id.navigation_dashboard);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Login failed: " + error);
                    binding.buttonLogin.setEnabled(true);
                    
                    // عرض رسالة الخطأ مباشرة
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                }
            });
        });

        binding.buttonRegister.setOnClickListener(v -> 
            NavHostFragment.findNavController(LoginFragment.this)
                    .navigate(R.id.action_loginFragment_to_registerFragment)
        );

        // زر أرقام التواصل
        binding.buttonContact.setOnClickListener(v -> {
            Toast.makeText(getContext(), "رقم التواصل: 774447251", Toast.LENGTH_LONG).show();
        });

        // زر عن التطبيق
        binding.buttonAbout.setOnClickListener(v -> {
            String about = "تطبيق مالي برو هو رفيقك الذكي لإدارة الحسابات والمعاملات المالية بسهولة واحترافية.\n\n" +
                    "أهم المميزات:\n" +
                    "- العمل أونلاين أو أوفلاين بدون انقطاع.\n" +
                    "- استرداد قاعدة البيانات في أي وقت.\n" +
                    "- تقارير مالية دقيقة وشاملة.\n" +
                    "- إرسال إشعارات واتساب تلقائيًا (اختياري عند التفعيل).";
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("عن تطبيق مالي برو")
                    .setMessage(about)
                    .setPositiveButton("حسناً", null)
                    .show();
        });

        // إغلاق التطبيق عند الضغط على زر الرجوع في صفحة تسجيل الدخول
        requireActivity().getOnBackPressedDispatcher().addCallback(
            getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    requireActivity().finishAffinity();
                }
            }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 