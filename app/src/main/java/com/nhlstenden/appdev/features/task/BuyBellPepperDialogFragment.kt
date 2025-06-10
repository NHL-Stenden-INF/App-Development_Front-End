package com.nhlstenden.appdev.features.task

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.MainActivity
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.shared.ProfileHeaderFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class BuyBellPepperDialogFragment : DialogFragment() {
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var userRepository: UserRepository
    
    private val BELL_PEPPER_COST = 300
    private val TAG = "BuyBellPepperDialog"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_buy_bell_pepper, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonContainer = view.findViewById<LinearLayout>(R.id.buttonContainer)
        buttonContainer.removeAllViews()

        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Fetching user attributes...")
                    val attributesResult = userRepository.getUserAttributes(currentUser.id)
                    
                    if (attributesResult.isFailure) {
                        val error = attributesResult.exceptionOrNull()
                        Log.e(TAG, "Failed to fetch user attributes: ${error?.message}")
                        withContext(Dispatchers.Main) {
                            if (error?.message?.contains("Session expired") == true) {
                                // JWT expired, dialog will close and app will redirect to login
                                dismiss()
                            } else {
                                Toast.makeText(context, "Error fetching user data. Please try again.", Toast.LENGTH_SHORT).show()
                                dismiss()
                            }
                        }
                        return@launch
                    }
                    
                    val profile = attributesResult.getOrNull()!!
                    val currentPoints = profile.optInt("points", 0)
                    val currentBellPeppers = profile.optInt("bell_peppers", 0)
                    Log.d(TAG, "Current points: $currentPoints, Current bell peppers: $currentBellPeppers")

                    withContext(Dispatchers.Main) {
                        if (currentBellPeppers >= 3) {
                            Log.d(TAG, "Purchase rejected: Already at maximum bell peppers ($currentBellPeppers)")
                            Toast.makeText(context, "You already have the maximum number of bell peppers!", Toast.LENGTH_SHORT).show()
                            dismiss()
                            return@withContext
                        }

                        val buyButton = MaterialButton(requireContext()).apply {
                            text = "Buy Bell Pepper ($BELL_PEPPER_COST points)"
                            isEnabled = currentPoints >= BELL_PEPPER_COST
                            setOnClickListener {
                                performPurchase(currentPoints, currentBellPeppers)
                            }
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                                marginEnd = 8
                            }
                            stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(
                                context, R.animator.button_state_animator
                            )
                        }

                        val cancelButton = MaterialButton(requireContext()).apply {
                            text = "Cancel"
                            setOnClickListener {
                                dismiss()
                            }
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                                marginStart = 8
                            }
                            stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(
                                context, R.animator.button_state_animator
                            )
                        }

                        if (currentPoints < BELL_PEPPER_COST) {
                            Toast.makeText(context, "Not enough points! You need $BELL_PEPPER_COST points.", Toast.LENGTH_SHORT).show()
                        }

                        buttonContainer.addView(buyButton)
                        buttonContainer.addView(cancelButton)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting up dialog: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error loading dialog. Please try again.", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
            }
        } else {
            Log.w(TAG, "No current user found")
            Toast.makeText(context, "Please log in again.", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
    
    private fun performPurchase(currentPoints: Int, currentBellPeppers: Int) {
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser == null) {
            Toast.makeText(context, "Please log in again.", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting purchase process...")
                val newPoints = currentPoints - BELL_PEPPER_COST
                val newBellPeppers = currentBellPeppers + 1
                
                // Update points
                Log.d(TAG, "Updating points to $newPoints...")
                val pointsResult = userRepository.updateUserPoints(currentUser.id, newPoints)
                if (pointsResult.isFailure) {
                    val error = pointsResult.exceptionOrNull()
                    Log.e(TAG, "Failed to update points: ${error?.message}")
                    withContext(Dispatchers.Main) {
                        if (error?.message?.contains("Session expired") == true) {
                            dismiss() // JWT expired, app will redirect to login
                        } else {
                            Toast.makeText(context, "Failed to update points. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@launch
                }
                
                // Update bell peppers
                Log.d(TAG, "Updating bell peppers to $newBellPeppers...")
                val bellPeppersResult = userRepository.updateUserBellPeppers(currentUser.id, newBellPeppers)
                if (bellPeppersResult.isFailure) {
                    val error = bellPeppersResult.exceptionOrNull()
                    Log.e(TAG, "Failed to update bell peppers: ${error?.message}")
                    withContext(Dispatchers.Main) {
                        if (error?.message?.contains("Session expired") == true) {
                            dismiss() // JWT expired, app will redirect to login
                        } else {
                            Toast.makeText(context, "Failed to update bell peppers. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@launch
                }
                
                Log.d(TAG, "Purchase completed successfully!")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Bell pepper purchased successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Reset task if called from TaskActivity
                    (activity as? com.nhlstenden.appdev.features.task.screens.TaskActivity)?.let { taskActivity ->
                        taskActivity.resetForWrongQuestions()
                    }
                    
                    // Trigger immediate refresh first
                    Log.d(TAG, "Triggering immediate profile data refresh...")
                    refreshProfileData()
                    
                    // Also add a delayed refresh as backup
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "Triggering delayed profile data refresh...")
                        refreshProfileData()
                    }, 1000) // 1 second delay
                    
                    dismiss()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during purchase: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Unexpected error during purchase. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun refreshProfileData() {
        Log.d(TAG, "refreshProfileData() called - checking activity...")
        // Use MainActivity's public method to refresh profile data
        (activity as? MainActivity)?.let { mainActivity ->
            Log.d(TAG, "MainActivity found, calling refreshProfileData()...")
            mainActivity.refreshProfileData()
            
            // Also try to directly refresh the ProfileHeaderFragment
            Log.d(TAG, "Searching for ProfileHeaderFragment by tag...")
            val profileHeaderFragment = mainActivity.supportFragmentManager.findFragmentByTag("profile_header") as? ProfileHeaderFragment
            profileHeaderFragment?.let { fragment ->
                Log.d(TAG, "Found ProfileHeaderFragment by tag, calling refreshProfile() directly...")
                fragment.refreshProfile()
                
                // Force an immediate view refresh as well
                mainActivity.runOnUiThread {
                    Log.d(TAG, "Forcing immediate UI refresh on main thread...")
                    fragment.forceRefreshUI()
                }
            } ?: Log.w(TAG, "ProfileHeaderFragment not found by tag 'profile_header'")
        } ?: Log.w(TAG, "MainActivity not found - cannot refresh profile data")
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }
} 