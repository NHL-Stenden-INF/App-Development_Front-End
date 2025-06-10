package com.nhlstenden.appdev.features.login.screens

import android.animation.AnimatorInflater
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.ActivityLoginBinding
import com.nhlstenden.appdev.features.login.adapters.LoginPagerAdapter
import com.nhlstenden.appdev.features.login.viewmodels.LoginViewModel
import com.nhlstenden.appdev.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import android.graphics.Rect
import android.graphics.drawable.AnimationDrawable

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private val sleepHandler = Handler(Looper.getMainLooper())
    private var isMascotSleeping = false
    private val SLEEP_DELAY = 30000L // 30 seconds in milliseconds

    companion object {
        const val PAGE_LOGIN = 0
        const val PAGE_REGISTER = 1
    }

    private val sleepRunnable = Runnable {
        if (!isFinishing) {
            isMascotSleeping = true
            // First play the closed eye animation
            binding.imageViewLogo.setImageResource(R.drawable.mascot_closed_eye_animation)
            val closedEyeAnimation = binding.imageViewLogo.drawable as? AnimationDrawable
            closedEyeAnimation?.start()
            
            // After closed eye animation completes (200ms), start sleep animation
            sleepHandler.postDelayed({
                binding.imageViewLogo.setImageResource(R.drawable.mascot_sleep_animation)
                (binding.imageViewLogo.drawable as? AnimationDrawable)?.start()
            }, 200) // 4 frames * 50ms = 200ms
        }
    }

    private fun resetSleepTimer() {
        sleepHandler.removeCallbacks(sleepRunnable)
        
        if (isMascotSleeping) {
            isMascotSleeping = false
            binding.imageViewLogo.setImageResource(R.drawable.mascot_wakeup_animation)
            val wakeupAnimation = binding.imageViewLogo.drawable as? AnimationDrawable
            wakeupAnimation?.start()
            
            // After wake-up animation completes (750ms), start normal animation
            sleepHandler.postDelayed({
                binding.imageViewLogo.setImageResource(R.drawable.mascot_animation)
                (binding.imageViewLogo.drawable as? AnimationDrawable)?.start()
            }, 750) // 5 frames * 150ms = 750ms
        }
        
        sleepHandler.postDelayed(sleepRunnable, SLEEP_DELAY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        observeLoginState()
        resetSleepTimer()

        // Add touch listener to root view
        binding.root.setOnTouchListener { _, _ ->
            resetSleepTimer()
            false // Return false to allow other touch events to be processed
        }
    }

    private fun setupViews() {
        binding.imageViewLogo.setImageResource(R.drawable.mascot_animation)
        (binding.imageViewLogo.drawable as? AnimationDrawable)?.start()
            
        binding.imageViewArrow.let { arrow ->
            val animator = AnimatorInflater.loadAnimator(this, R.animator.arrow_animation)
            animator.setTarget(arrow)
            animator.start()
        }

        // Set up ViewPager2
        binding.viewPager.adapter = LoginPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.isUserInputEnabled = true

        // Set up TabLayout
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                PAGE_LOGIN -> "Login"
                PAGE_REGISTER -> "Register"
                else -> null
            }
        }.attach()

        // Set up page transformer for smoother transitions
        binding.viewPager.setPageTransformer { page, position ->
            val scale = if (position < 0) 1f + position * 0.1f else 1f - position * 0.1f
            page.scaleX = scale
            page.scaleY = scale
        }
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginViewModel.LoginState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is LoginViewModel.LoginState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is LoginViewModel.LoginState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is LoginViewModel.LoginState.Initial -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sleepHandler.removeCallbacks(sleepRunnable)
    }

    override fun onResume() {
        super.onResume()
        resetSleepTimer()
    }

    fun navigateToPage(page: Int) {
        if (!isFinishing && !isDestroyed) {
            try {
                binding.viewPager.setCurrentItem(page, true)
            } catch (e: Exception) {
                // Log the error but don't crash
                e.printStackTrace()
            }
        }
    }
} 