package com.nhlstenden.appdev.features.login.screens

import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.ActivityRegisterBinding
import com.nhlstenden.appdev.features.login.viewmodels.RegisterViewModel
import com.nhlstenden.appdev.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    private lateinit var gestureDetector: GestureDetectorCompat
    private val sleepHandler = Handler(Looper.getMainLooper())
    private var isMascotSleeping = false
    private val SLEEP_DELAY = 60000L // 1 minute in milliseconds

    private val sleepRunnable = Runnable {
        if (!isFinishing) {
            isMascotSleeping = true
            Glide.with(this@RegisterActivity)
                .asGif()
                .load(R.drawable.mascot_sleep)
                .into(binding.imageViewLogo)
        }
    }

    private fun resetSleepTimer() {
        sleepHandler.removeCallbacks(sleepRunnable)
        
        if (isMascotSleeping) {
            isMascotSleeping = false
            Glide.with(this)
                .asGif()
                .load(R.drawable.mascot)
                .into(binding.imageViewLogo)
        }
        
        sleepHandler.postDelayed(sleepRunnable, SLEEP_DELAY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        setupGestureDetector()
        observeRegisterState()
        
        resetSleepTimer()
    }

    private fun setupViews() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.mascot)
            .into(binding.imageViewLogo)
            
        binding.imageViewArrow.let { arrow ->
            val animator = AnimatorInflater.loadAnimator(this, R.animator.arrow_animation)
            animator.setTarget(arrow)
            animator.start()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            
            if (password == confirmPassword) {
                viewModel.register(email, password)
            } else {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
        }

        binding.root.setOnTouchListener { _, event ->
            resetSleepTimer()
            false
        }

        binding.emailEditText.setOnTouchListener { _, _ ->
            resetSleepTimer()
            false
        }
        
        binding.passwordEditText.setOnTouchListener { _, _ ->
            resetSleepTimer()
            false
        }

        binding.confirmPasswordEditText.setOnTouchListener { _, _ ->
            resetSleepTimer()
            false
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            // No need to override onFling for this activity
        })
    }

    private fun observeRegisterState() {
        lifecycleScope.launch {
            viewModel.registerState.collect { state ->
                when (state) {
                    is RegisterViewModel.RegisterState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.registerButton.isEnabled = false
                    }
                    is RegisterViewModel.RegisterState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.registerButton.isEnabled = true
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                        finish()
                    }
                    is RegisterViewModel.RegisterState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.registerButton.isEnabled = true
                        Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is RegisterViewModel.RegisterState.Initial -> {
                        binding.progressBar.visibility = View.GONE
                        binding.registerButton.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        resetSleepTimer()
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        sleepHandler.removeCallbacks(sleepRunnable)
    }

    override fun onResume() {
        super.onResume()
        resetSleepTimer()
    }

    private fun onSwipeRight() {
        // Handle swipe right
    }

    private fun onSwipeLeft() {
        // Handle swipe left
    }
} 