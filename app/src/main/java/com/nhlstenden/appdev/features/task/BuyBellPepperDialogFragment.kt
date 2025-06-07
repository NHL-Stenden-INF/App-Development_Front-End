package com.nhlstenden.appdev.features.task

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.utils.UserManager
import com.nhlstenden.appdev.main.MainActivity
import com.nhlstenden.appdev.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BuyBellPepperDialogFragment : DialogFragment() {
    private val supabaseClient = SupabaseClient()
    private val BELL_PEPPER_COST = 300
    private val TAG = "TASK_BELL_PEPPER_DIALOG"

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

        val currentUser = UserManager.getCurrentUser()
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.i(TAG, "Fetching user attributes...")
                    val profile = supabaseClient.fetchUserAttributes(currentUser.authToken)
                    val currentPoints = profile.optInt("points", 0)
                    val currentBellPeppers = profile.optInt("bell_peppers", 0)
                    Log.i(TAG, "Current points: $currentPoints, Current bell peppers: $currentBellPeppers")

                    withContext(Dispatchers.Main) {
                        if (currentBellPeppers >= 3) {
                            Log.i(TAG, "Purchase rejected: Too many bell peppers ($currentBellPeppers)")
                            Toast.makeText(context, "You already have the maximum number of bell peppers!", Toast.LENGTH_SHORT).show()
                            dismiss()
                            return@withContext
                        }

                        val buyButton = MaterialButton(requireContext()).apply {
                            text = "Buy Bell Pepper (300 points)"
                            isEnabled = currentPoints >= BELL_PEPPER_COST
                            setOnClickListener {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        Log.i(TAG, "Starting purchase process...")
                                        val newPoints = currentPoints - BELL_PEPPER_COST
                                        val newBellPeppers = currentBellPeppers + 1
                                        
                                        Log.i(TAG, "Updating points...")
                                        val pointsResponse = supabaseClient.updateUserPoints(currentUser.id, newPoints, currentUser.authToken)
                                        Log.i(TAG, "Points update response code: ${pointsResponse.code}")
                                        
                                        Log.i(TAG, "Updating bell peppers...")
                                        val bellPeppersResponse = supabaseClient.updateUserBellPeppers(currentUser.id, newBellPeppers, currentUser.authToken)
                                        Log.i(TAG, "Bell peppers update response code: ${bellPeppersResponse.code}")
                                        
                                        if ((pointsResponse.code == 200 || pointsResponse.code == 204) && 
                                            (bellPeppersResponse.code == 200 || bellPeppersResponse.code == 204)) {
                                            Log.i(TAG, "Purchase completed successfully")
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Bell pepper purchased!", Toast.LENGTH_SHORT).show()
                                                // Get the activity and reset the questions
                                                (activity as? com.nhlstenden.appdev.features.task.screens.TaskActivity)?.let { taskActivity ->
                                                    taskActivity.resetForWrongQuestions()
                                                }
                                                // Reload the profile to update the UI
                                                (activity as? com.nhlstenden.appdev.main.MainActivity)?.let { mainActivity ->
                                                    // Find the HomeFragment and reload its data
                                                    mainActivity.supportFragmentManager.fragments.forEach { fragment ->
                                                        if (fragment is com.nhlstenden.appdev.features.home.HomeFragment) {
                                                            fragment.setupUI(fragment.requireView())
                                                        }
                                                    }
                                                }
                                                dismiss()
                                            }
                                        } else {
                                            Log.e(TAG, "Purchase failed - Points response: ${pointsResponse.code}, Bell peppers response: ${bellPeppersResponse.code}")
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Failed to purchase bell pepper. Please try again.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error during purchase: ${e.message}", e)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error during purchase. Please try again.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
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
                                val context = requireContext()
                                val intent = Intent(context, MainActivity::class.java)
                                context.startActivity(intent)
                                
                                if (context is android.app.Activity) {
                                    context.finish()
                                }
                                dismiss()
                            }
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                                marginStart = 8
                            }
                            stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(
                                context, R.animator.button_state_animator
                            )
                        }

                        buttonContainer.addView(buyButton)
                        buttonContainer.addView(cancelButton)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking user data: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error checking points. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
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