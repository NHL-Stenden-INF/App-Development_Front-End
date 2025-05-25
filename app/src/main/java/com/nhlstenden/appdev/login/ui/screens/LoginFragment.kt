package com.nhlstenden.appdev.login.ui.screens

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.FragmentLoginBinding
import com.nhlstenden.appdev.login.ui.viewmodels.LoginViewModel
import com.nhlstenden.appdev.main.MainActivity
import com.nhlstenden.appdev.shared.components.UserManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeLoginState()
        startArrowAnimation()
    }

    private fun setupViews() {
        binding.loginButton.setOnClickListener { view ->
            val email = binding.emailEditText.text?.toString()?.trim() ?: ""
            val password = binding.passwordEditText.text?.toString()?.trim() ?: ""
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            view.isEnabled = false
            viewModel.login(email, password)
        }
    }

    private fun observeLoginState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.collectLatest { state ->
                when (state) {
                    is LoginViewModel.LoginState.Loading -> {
                        binding.loginButton.isEnabled = false
                    }
                    is LoginViewModel.LoginState.Success -> {
                        binding.loginButton.isEnabled = true
                        UserManager.setCurrentUser(state.user)
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    }
                    is LoginViewModel.LoginState.Error -> {
                        binding.loginButton.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is LoginViewModel.LoginState.Initial -> {
                        binding.loginButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun startArrowAnimation() {
        binding.arrowRight.animate()
            .setDuration(1000)
            .translationX(8f)
            .alpha(0.6f)
            .withEndAction {
                if (isAdded && !isDetached) {
                    binding.arrowRight.animate()
                        .setDuration(1000)
                        .translationX(0f)
                        .alpha(1f)
                        .withEndAction {
                            if (isAdded && !isDetached) {
                                startArrowAnimation()
                            }
                        }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 