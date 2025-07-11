package com.nhlstenden.appdev.features.login.screens

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.FragmentLoginBinding
import com.nhlstenden.appdev.features.login.viewmodels.LoginViewModel
import com.nhlstenden.appdev.MainActivity
import com.nhlstenden.appdev.core.utils.UserManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

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
        val scrollView = view.findViewById<ScrollView>(R.id.scrollView)
        view.viewTreeObserver.addOnGlobalLayoutListener {
            val focused = activity?.currentFocus
            if (focused != null && scrollView != null) {
                scrollView.post {
                    scrollView.smoothScrollTo(0, focused.bottom)
                }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, 0, 0, imeInsets.bottom)
            insets
        }
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
                        binding.loginButton.text = getString(R.string.loading)
                    }
                    is LoginViewModel.LoginState.Success -> {
                        binding.loginButton.isEnabled = true
                        binding.loginButton.text = getString(R.string.login)
                        // User is automatically stored in AuthRepository, no need for UserManager
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    is LoginViewModel.LoginState.Error -> {
                        binding.loginButton.isEnabled = true
                        binding.loginButton.text = getString(R.string.login)
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is LoginViewModel.LoginState.Initial -> {
                        binding.loginButton.isEnabled = true
                        binding.loginButton.text = getString(R.string.login)
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