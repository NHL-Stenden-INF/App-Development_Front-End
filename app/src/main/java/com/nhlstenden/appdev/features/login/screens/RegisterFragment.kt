package com.nhlstenden.appdev.features.login.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.FragmentRegisterBinding
import com.nhlstenden.appdev.features.login.viewmodels.RegisterViewModel
import com.nhlstenden.appdev.core.utils.UserManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Intent
import com.nhlstenden.appdev.MainActivity
import android.util.Patterns
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.ViewTreeObserver
import android.widget.ScrollView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
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
        observeRegisterState()
        startArrowAnimation()
    }

    private fun setupViews() {
        binding.registerButton.setOnClickListener { view ->
            view.isEnabled = false // Disable button immediately to prevent double clicks
            val email = binding.emailEditText.text?.toString()?.trim() ?: ""
            val password = binding.passwordEditText.text?.toString() ?: ""
            val confirmPassword = binding.confirmPasswordEditText.text?.toString() ?: ""
            
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                view.isEnabled = true
                return@setOnClickListener
            }
            
            if (password != confirmPassword) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                view.isEnabled = true
                return@setOnClickListener
            }
            
            if (!isValidEmail(email)) {
                binding.emailEditText.error = "Please enter a valid email address"
                view.isEnabled = true
                return@setOnClickListener
            }
            
            viewModel.register(email, password)
        }
    }

    private fun observeRegisterState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registerState.collectLatest { state ->
                when (state) {
                    is RegisterViewModel.RegisterState.Loading -> {
                        binding.registerButton.isEnabled = false
                    }
                    is RegisterViewModel.RegisterState.Success -> {
                        binding.registerButton.isEnabled = true
                        // User is automatically stored in AuthRepository, no need for UserManager
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    is RegisterViewModel.RegisterState.Error -> {
                        binding.registerButton.isEnabled = true
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is RegisterViewModel.RegisterState.Initial -> {
                        binding.registerButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registration Successful")
            .setMessage("Your account has been created successfully. Please log in to continue.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // Navigate to login fragment using the activity's navigation method
                (requireActivity() as? com.nhlstenden.appdev.features.login.screens.LoginActivity)?.navigateToPage(com.nhlstenden.appdev.features.login.screens.LoginActivity.PAGE_LOGIN)
            }
            .setCancelable(false)
            .show()
    }

    private fun startArrowAnimation() {
        binding.arrowLeft.animate()
            .setDuration(1000)
            .translationX(-8f)
            .alpha(0.6f)
            .withEndAction {
                if (isAdded && !isDetached) {
                    binding.arrowLeft.animate()
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

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 