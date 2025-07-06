package com.hillal.acc.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.hillal.acc.R
import com.hillal.acc.databinding.FragmentRegisterBinding
import com.hillal.acc.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {
    private var binding: FragmentRegisterBinding? = null
    private var authViewModel: AuthViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // إخفاء شريط التنقل السفلي
        authViewModel = ViewModelProvider(this).get<AuthViewModel>(AuthViewModel::class.java)

        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.getRoot(),
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                var bottom = insets!!.getInsets(WindowInsetsCompat.Type.ime()).bottom
                if (bottom == 0) {
                    bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                }
                v!!.setPadding(0, 0, 0, bottom)
                insets
            })

        binding!!.buttonRegister.setOnClickListener(View.OnClickListener { v: View? ->
            val displayName = binding!!.editTextDisplayName.getText().toString().trim { it <= ' ' }
            val phone = binding!!.editTextPhone.getText().toString().trim { it <= ' ' }
            val password = binding!!.editTextPassword.getText().toString()
            val confirmPassword = binding!!.editTextConfirmPassword.getText().toString()

            // التحقق من صحة المدخلات
            if (displayName.isEmpty()) {
                binding!!.editTextDisplayName.setError("الرجاء إدخال الاسم المستخدم في الإشعارات")
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                binding!!.editTextPhone.setError("الرجاء إدخال رقم الهاتف")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding!!.editTextPassword.setError("الرجاء إدخال كلمة المرور")
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                binding!!.editTextConfirmPassword.setError("الرجاء تأكيد كلمة المرور")
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                binding!!.editTextConfirmPassword.setError("كلمة المرور غير متطابقة")
                return@setOnClickListener
            }

            // تعطيل الزر أثناء عملية التسجيل
            binding!!.buttonRegister.setEnabled(false)
            binding!!.progressBar.setVisibility(View.VISIBLE)

            Log.d(TAG, "Attempting to register with phone: " + phone)

            val username = displayName
            authViewModel!!.register(
                username,
                phone,
                password,
                object : AuthViewModel.AuthCallback {
                    override fun onSuccess() {
                        Log.d(TAG, "Registration successful")
                        binding!!.progressBar.setVisibility(View.GONE)
                        Toast.makeText(getContext(), "تم إنشاء الحساب بنجاح", Toast.LENGTH_SHORT)
                            .show()
                        NavHostFragment.findNavController(this@RegisterFragment)
                            .navigate(R.id.action_registerFragment_to_loginFragment)
                    }

                    override fun onError(error: String) {
                        Log.e(TAG, "Registration failed: " + error)
                        binding!!.progressBar.setVisibility(View.GONE)
                        binding!!.buttonRegister.setEnabled(true)


                        // رسائل خطأ أكثر تفصيلاً
                        val errorMessage: String?
                        if (error.contains("UnknownHostException")) {
                            errorMessage =
                                "لا يمكن الوصول إلى الخادم. يرجى التحقق من اتصال الإنترنت"
                        } else if (error.contains("SocketTimeoutException")) {
                            errorMessage = "انتهت مهلة الاتصال بالخادم. يرجى المحاولة مرة أخرى"
                        } else if (error.contains("500")) {
                            errorMessage = "خطأ في الخادم. يرجى المحاولة لاحقاً"
                        } else if (error.contains("400")) {
                            errorMessage = "بيانات غير صحيحة. يرجى التحقق من المدخلات"
                        } else if (error.contains("409")) {
                            errorMessage = "رقم الهاتف مسجل مسبقاً"
                        } else {
                            errorMessage = "فشل إنشاء الحساب: " + error
                        }

                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                })
        })

        binding!!.buttonBackToLogin.setOnClickListener(View.OnClickListener { v: View? ->
            NavHostFragment.findNavController(this@RegisterFragment)
                .navigate(R.id.action_registerFragment_to_loginFragment)
        }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }

    companion object {
        private const val TAG = "RegisterFragment"
    }
}