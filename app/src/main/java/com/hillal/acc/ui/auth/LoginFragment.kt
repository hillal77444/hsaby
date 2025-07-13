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
import androidx.activity.addCallback
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
import com.hillal.acc.viewmodel.AuthViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer
import com.hillal.acc.ui.auth.LoginScreen
import com.hillal.acc.data.room.AccountDao
import com.hillal.acc.data.room.TransactionDao
import com.hillal.acc.data.room.PendingOperationDao
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

class LoginFragment : Fragment() {
    private lateinit var authViewModel: AuthViewModel
    private var loadingDialog: ProgressDialog? = null
    private var isLoading: Boolean = false
    private var errorMessage by mutableStateOf<String?>(null)

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
    ): View {
        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        return ComposeView(requireContext()).apply {
            setContent {
                LoginScreen(
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onLoginClick = { phone, password ->
                        handleLogin(phone, password)
                    },
                    onRegisterClick = {
                        NavHostFragment.findNavController(this@LoginFragment)
                            .navigate(R.id.action_loginFragment_to_registerFragment)
                    },
                    onPrivacyClick = {
                        val url = "https://malyp.com/api/privacy-policy"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    },
                    onContactClick = {
                        Toast.makeText(requireContext(), "رقم التواصل: 774447251", Toast.LENGTH_LONG).show()
                    },
                    onAboutClick = {
                        val about = "تطبيق مالي برو هو رفيقك الذكي لإدارة الحسابات والمعاملات المالية بسهولة واحترافية.\n\n" +
                                "أهم المميزات:\n" +
                                "- العمل أونلاين أو أوفلاين بدون انقطاع.\n" +
                                "- استرداد قاعدة البيانات في أي وقت.\n" +
                                "- تقارير مالية دقيقة وشاملة.\n" +
                                "- إرسال إشعارات واتساب تلقائيًا (اختياري عند التفعيل)."
                        AlertDialog.Builder(requireContext())
                            .setTitle("حول التطبيق")
                            .setMessage(about)
                            .setPositiveButton("حسناً", null)
                            .show()
                    },
                    onForgotPasswordClick = {
                        Toast.makeText(requireContext(), "يرجى التواصل مع الدعم لاستعادة كلمة المرور", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.getAuthState().observe(viewLifecycleOwner) { state ->
            when (state) {
                AuthViewModel.AuthState.LOADING -> {
                    isLoading = true
                    errorMessage = null
                }
                AuthViewModel.AuthState.SUCCESS -> {
                    isLoading = false
                    errorMessage = null
                }
                AuthViewModel.AuthState.ERROR -> {
                    isLoading = false
                    errorMessage = "فشل تسجيل الدخول. تحقق من البيانات أو الاتصال."
                }
                else -> {
                    isLoading = false
                    errorMessage = null
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("تأكيد الخروج")
                    .setMessage("هل تريد الخروج من التطبيق؟")
                    .setPositiveButton("نعم") { _, _ ->
                        requireActivity().finish()
                    }
                    .setNegativeButton("لا", null)
                    .show()
            }
        })
    }

    private fun handleLogin(phone: String, password: String) {
        if (phone.isEmpty()) {
            errorMessage = "الرجاء إدخال رقم الهاتف"
            isLoading = false
            return
        }
        if (password.isEmpty()) {
            errorMessage = "الرجاء إدخال كلمة المرور"
            isLoading = false
            return
        }
        isLoading = true // <-- تفعيل التحميل فور الضغط
        errorMessage = null
        authViewModel.login(phone, password, object : AuthViewModel.AuthCallback {
            override fun onSuccess() {
                isLoading = false
                errorMessage = null
                Toast.makeText(requireContext(), "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
                val authRepository = AuthRepository(requireActivity().application)
                val userName = authRepository.getCurrentUser()?.getUsername() ?: ""
                val userPreferences = UserPreferences(requireContext())
                userPreferences.saveUserName(userName)
                userPreferences.savePhoneNumber(phone)
                loadingDialog = ProgressDialog(requireContext())
                loadingDialog!!.setMessage("جاري تحميل البيانات...")
                loadingDialog!!.setCancelable(false)
                loadingDialog!!.show()
                val mainActivity = requireActivity() as MainActivity
                val dataManager = DataManager(
                    requireContext(),
                    mainActivity.accountDao,
                    mainActivity.transactionDao,
                    mainActivity.pendingOperationDao
                )
                dataManager.fetchDataFromServer(object : DataManager.DataCallback {
                    override fun onSuccess() {
                        loadingDialog?.dismiss()
                        NavHostFragment.findNavController(this@LoginFragment)
                            .navigate(R.id.navigation_dashboard)
                        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
                    }
                    override fun onError(error: String) {
                        loadingDialog?.dismiss()
                        isLoading = false
                        errorMessage = "فشل في جلب البيانات: $error"
                        Toast.makeText(
                            requireContext(),
                            "فشل في جلب البيانات: $error\nتم نسخ تفاصيل الخطأ إلى الحافظة",
                            Toast.LENGTH_LONG
                        ).show()
                        NavHostFragment.findNavController(this@LoginFragment)
                            .navigate(R.id.navigation_dashboard)
                    }
                })
            }
            override fun onError(error: String?) {
                isLoading = false
                errorMessage = error ?: "فشل تسجيل الدخول. تحقق من البيانات أو الاتصال."
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        })
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}