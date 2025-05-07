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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.hillal.hhhhhhh.R;
import com.hillal.hhhhhhh.databinding.FragmentLoginBinding;
import com.hillal.hhhhhhh.viewmodel.AuthViewModel;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupLoginButton();
        setupRegisterButton();
        observeAuthState();

        return root;
    }

    private void setupLoginButton() {
        binding.buttonLogin.setOnClickListener(v -> {
            String phone = binding.editTextPhone.getText().toString();
            String password = binding.editTextPassword.getText().toString();

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.login(phone, password);
        });
    }

    private void setupRegisterButton() {
        binding.buttonRegister.setOnClickListener(v -> {
            String username = binding.editTextUsername.getText().toString();
            String phone = binding.editTextPhone.getText().toString();
            String password = binding.editTextPassword.getText().toString();

            if (username.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.register(username, phone, password);
        });
    }

    private void observeAuthState() {
        authViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case LOADING:
                    binding.buttonLogin.setEnabled(false);
                    binding.buttonRegister.setEnabled(false);
                    break;
                case SUCCESS:
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_login_to_home);
                    break;
                case ERROR:
                    binding.buttonLogin.setEnabled(true);
                    binding.buttonRegister.setEnabled(true);
                    Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    binding.buttonLogin.setEnabled(true);
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