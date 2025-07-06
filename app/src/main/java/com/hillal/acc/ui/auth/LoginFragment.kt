package com.hillal.acc.ui.auth

import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.hillal.acc.MainActivity
import com.hillal.acc.R
import com.hillal.acc.data.preferences.UserPreferences
import com.hillal.acc.data.remote.DataManager
import com.hillal.acc.data.repository.AuthRepository
import com.hillal.acc.databinding.FragmentLoginBinding
import com.hillal.acc.viewmodel.AuthViewModel

class LoginFragment : Fragment() {
    private var binding: FragmentLoginBinding? = null
    private var authViewModel: AuthViewModel? = null
    private var loadingDialog: ProgressDialog? = null

    // دالة مساعدة لنسخ النص إلى الحافظة
    private fun copyToClipboard(text: String?) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Error Details", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(this).get<AuthViewModel>(AuthViewModel::class.java)

        // إضافة مستمع لرابط سياسة الخصوصية
        binding!!.buttonPrivacy.setOnClickListener(View.OnClickListener { v: View? ->
            val url = "https://malyp.com/api/privacy-policy"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        })

        binding!!.buttonLogin.setOnClickListener(View.OnClickListener { v: View? ->
            val phone = binding!!.editTextPhone.getText().toString().trim { it <= ' ' }
            val password = binding!!.editTextPassword.getText().toString()

            // التحقق من صحة المدخلات
            if (phone.isEmpty()) {
                binding!!.editTextPhone.setError("الرجاء إدخال رقم الهاتف")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding!!.editTextPassword.setError("الرجاء إدخال كلمة المرور")
                return@setOnClickListener
            }

            // تعطيل الزر أثناء عملية تسجيل الدخول
            binding!!.buttonLogin.setEnabled(false)

            Log.d(TAG, "Attempting to login with phone: " + phone)
            authViewModel!!.login(phone, password, object : AuthViewModel.AuthCallback {
                override fun onSuccess() {
                    Log.d(TAG, "Login successful")
                    Toast.makeText(getContext(), "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()

                    // حفظ اسم المستخدم في UserPreferences
                    val authRepository = AuthRepository(requireActivity().getApplication())
                    val userName =
                        if (authRepository.getCurrentUser() != null) authRepository.getCurrentUser()
                            .getUsername() else ""
                    val userPreferences = UserPreferences(requireContext())
                    userPreferences.saveUserName(userName)
                    // حفظ رقم الهاتف من الحقل
                    userPreferences.savePhoneNumber(phone)


                    // إظهار Dialog التحميل أثناء جلب البيانات من السيرفر
                    loadingDialog = ProgressDialog(getContext())
                    loadingDialog!!.setMessage("جاري تحميل البيانات...")
                    loadingDialog!!.setCancelable(false)
                    loadingDialog!!.show()


                    // جلب البيانات من السيرفر بعد تسجيل الدخول
                    val mainActivity = requireActivity() as MainActivity
                    val dataManager = DataManager(
                        requireContext(),
                        mainActivity.getAccountDao(),
                        mainActivity.getTransactionDao(),
                        mainActivity.getPendingOperationDao()
                    )

                    dataManager.fetchDataFromServer(object : DataManager.DataCallback {
                        override fun onSuccess() {
                            // إخفاء Dialog التحميل
                            if (loadingDialog != null && loadingDialog!!.isShowing()) {
                                loadingDialog!!.dismiss()
                            }
                            Log.d(TAG, "Data fetched successfully")
                            // التنقل إلى لوحة التحكم بعد جلب البيانات
                            NavHostFragment.findNavController(this@LoginFragment)
                                .navigate(R.id.navigation_dashboard)
                            (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
                        }

                        override fun onError(error: String) {
                            // إخفاء Dialog التحميل
                            if (loadingDialog != null && loadingDialog!!.isShowing()) {
                                loadingDialog!!.dismiss()
                            }
                            Log.e(TAG, "Failed to fetch data: " + error)


                            // تحضير رسالة الخطأ
                            val errorMessage: String?
                            if (error.contains("UNIQUE constraint failed")) {
                                errorMessage =
                                    "حدث خطأ في قاعدة البيانات المحلية. يرجى إعادة تشغيل التطبيق."
                            } else if (error.contains("Network error")) {
                                errorMessage =
                                    "فشل الاتصال بالإنترنت. يرجى التحقق من اتصالك وإعادة المحاولة."
                            } else if (error.contains("User not authenticated")) {
                                errorMessage = "انتهت صلاحية الجلسة. يرجى تسجيل الدخول مرة أخرى."
                            } else {
                                errorMessage = "فشل في جلب البيانات: " + error
                            }


                            // نسخ تفاصيل الخطأ الكاملة إلى الحافظة
                            copyToClipboard("تفاصيل الخطأ:\n" + error)


                            // عرض رسالة الخطأ
                            Toast.makeText(
                                getContext(),
                                errorMessage + "\nتم نسخ تفاصيل الخطأ إلى الحافظة",
                                Toast.LENGTH_LONG
                            ).show()


                            // التنقل إلى لوحة التحكم حتى في حالة فشل جلب البيانات
                            NavHostFragment.findNavController(this@LoginFragment)
                                .navigate(R.id.navigation_dashboard)
                        }
                    })
                }

                override fun onError(error: String?) {
                    Log.e(TAG, "Login failed: " + error)
                    binding!!.buttonLogin.setEnabled(true)


                    // عرض رسالة الخطأ مباشرة
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show()
                }
            })
        })

        binding!!.buttonRegister.setOnClickListener(View.OnClickListener { v: View? ->
            NavHostFragment.findNavController(this@LoginFragment)
                .navigate(R.id.action_loginFragment_to_registerFragment)
        }
        )

        // زر أرقام التواصل
        binding!!.buttonContact.setOnClickListener(View.OnClickListener { v: View? ->
            Toast.makeText(getContext(), "رقم التواصل: 774447251", Toast.LENGTH_LONG).show()
        })

        // زر عن التطبيق
        binding!!.buttonAbout.setOnClickListener(View.OnClickListener { v: View? ->
            val about =
                "تطبيق مالي برو هو رفيقك الذكي لإدارة الحسابات والمعاملات المالية بسهولة واحترافية.\n\n" +
                        "أهم المميزات:\n" +
                        "- العمل أونلاين أو أوفلاين بدون انقطاع.\n" +
                        "- استرداد قاعدة البيانات في أي وقت.\n" +
                        "- تقارير مالية دقيقة وشاملة.\n" +
                        "- إرسال إشعارات واتساب تلقائيًا (اختياري عند التفعيل)."
            AlertDialog.Builder(requireContext())
                .setTitle("عن تطبيق مالي برو")
                .setMessage(about)
                .setPositiveButton("حسناً", null)
                .show()
        })

        // إغلاق التطبيق عند الضغط على زر الرجوع في صفحة تسجيل الدخول
        requireActivity().getOnBackPressedDispatcher().addCallback(
            getViewLifecycleOwner(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finishAffinity()
                }
            }
        )

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}