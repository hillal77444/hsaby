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
import com.hillal.acc.viewmodel.AuthViewModel
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

class RegisterFragment : Fragment() {
    private var authViewModel: AuthViewModel? = null
    private var isLoading = false
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        return ComposeView(requireContext()).apply {
            setContent {
                RegisterScreen(
                    onRegister = { displayName, phone, password, confirmPassword ->
                        isLoading = true
                        errorMessage = null
                        authViewModel!!.register(
                            displayName,
                            phone,
                            password,
                            object : AuthViewModel.AuthCallback {
                                override fun onSuccess() {
                                    isLoading = false
                                    errorMessage = null
                                    Toast.makeText(requireContext(), "تم إنشاء الحساب بنجاح", Toast.LENGTH_SHORT).show()
                                    NavHostFragment.findNavController(this@RegisterFragment)
                                        .navigate(R.id.action_registerFragment_to_loginFragment)
                                }
                                override fun onError(error: String) {
                                    isLoading = false
                                    errorMessage = error
                                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                                }
                            })
                    },
                    onBackToLogin = {
                        NavHostFragment.findNavController(this@RegisterFragment)
                            .navigate(R.id.action_registerFragment_to_loginFragment)
                    },
                    errorMessage = errorMessage
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // إخفاء شريط التنقل السفلي
        ViewCompat.setOnApplyWindowInsetsListener(
            view,
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

        authViewModel = null
    }

    companion object {
        private const val TAG = "RegisterFragment"
    }
}