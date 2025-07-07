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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer
import com.hillal.acc.ui.auth.LoginScreen

class LoginFragment : Fragment() {
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
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LoginScreen(
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

    private fun handleLogin(phone: String, password: String) {
        if (phone.isEmpty()) {
            Toast.makeText(requireContext(), "الرجاء إدخال رقم الهاتف", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "الرجاء إدخال كلمة المرور", Toast.LENGTH_SHORT).show()
            return
        }
        if (authViewModel == null) {
            authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        }
        // تعطيل الزر أثناء عملية تسجيل الدخول (يمكنك تحسين ذلك بإضافة حالة loading)
        Log.d(TAG, "Attempting to login with phone: $phone")
        authViewModel!!.login(phone, password, object : AuthViewModel.AuthCallback {
            override fun onSuccess() {
                Log.d(TAG, "Login successful")
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
                    mainActivity.getAccountDao(),
                    mainActivity.getTransactionDao(),
                    mainActivity.getPendingOperationDao()
                )
                dataManager.fetchDataFromServer(object : DataManager.DataCallback {
                    override fun onSuccess() {
                        loadingDialog?.dismiss()
                        Log.d(TAG, "Data fetched successfully")
                        NavHostFragment.findNavController(this@LoginFragment)
                            .navigate(R.id.navigation_dashboard)
                        (requireActivity() as MainActivity).setBottomNavigationVisibility(true)
                    }
                    override fun onError(error: String) {
                        loadingDialog?.dismiss()
                        Log.e(TAG, "Failed to fetch data: $error")
                        copyToClipboard("تفاصيل الخطأ:\n$error")
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
                Log.e(TAG, "Login failed: $error")
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        })
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}