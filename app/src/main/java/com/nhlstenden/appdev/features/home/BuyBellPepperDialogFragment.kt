package com.nhlstenden.appdev.features.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import com.nhlstenden.appdev.supabase.*
import com.nhlstenden.appdev.databinding.DialogBuyBellPepperBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BuyBellPepperDialogFragment : DialogFragment() {

    private lateinit var binding: DialogBuyBellPepperBinding
    private lateinit var supabaseClient: SupabaseClient
    private val BELL_PEPPER_COST = 300
    private val TAG = "BELL_PEPPER_DIALOG"
    
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogBuyBellPepperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        supabaseClient = SupabaseClient()
        setupBuyButton()
    }

    private fun setupBuyButton() {
        binding.btnBuy.setOnClickListener {
            val currentUser = authRepository.getCurrentUserSync()
            if (currentUser != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.i(TAG, "Starting bell pepper purchase process...")
                        val attributesResult = userRepository.getUserAttributes(currentUser.id)
                        if (attributesResult.isSuccess) {
                            val profile = attributesResult.getOrThrow()
                            val currentPoints = profile.optInt("points", 0)
                            val currentBellPeppers = profile.optInt("bell_peppers", 0)
                            Log.i(TAG, "Current points: $currentPoints, Current bell peppers: $currentBellPeppers")

                            withContext(Dispatchers.Main) {
                                if (currentBellPeppers >= 3) {
                                    Log.i(TAG, "Purchase rejected: Too many bell peppers ($currentBellPeppers)")
                                    Toast.makeText(requireContext(), "You already have the maximum number of bell peppers!", Toast.LENGTH_SHORT).show()
                                    return@withContext
                                }
                                
                                if (currentPoints < BELL_PEPPER_COST) {
                                    Log.i(TAG, "Purchase rejected: Not enough points ($currentPoints < $BELL_PEPPER_COST)")
                                    Toast.makeText(requireContext(), "Not enough points to purchase a bell pepper!", Toast.LENGTH_SHORT).show()
                                    return@withContext
                                }

                                // If we get here, we can proceed with the purchase
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        Log.i(TAG, "Updating points...")
                                        // First update points
                                        val pointsUpdateResult = userRepository.updateUserPoints(currentUser.id, currentPoints - BELL_PEPPER_COST)
                                        if (pointsUpdateResult.isSuccess) {
                                            Log.i(TAG, "Points updated successfully")
                                            
                                            Log.i(TAG, "Updating bell peppers...")
                                            // Then update bell peppers
                                            val bellPeppersUpdateResult = userRepository.updateUserBellPeppers(currentUser.id, currentBellPeppers + 1)
                                            if (bellPeppersUpdateResult.isSuccess) {
                                                Log.i(TAG, "Bell peppers updated successfully")
                                                withContext(Dispatchers.Main) {
                                                    Log.i(TAG, "Purchase completed successfully")
                                                    Toast.makeText(requireContext(), "Successfully purchased a bell pepper!", Toast.LENGTH_SHORT).show()
                                                    dismiss()
                                                }
                                            } else {
                                                Log.e(TAG, "Error updating bell peppers: ${bellPeppersUpdateResult.exceptionOrNull()?.message}")
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(requireContext(), "Error updating bell peppers. Please try again.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            Log.e(TAG, "Error updating points: ${pointsUpdateResult.exceptionOrNull()?.message}")
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(requireContext(), "Error updating points. Please try again.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error during purchase: ${e.message}", e)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(requireContext(), "Error updating user data. Please try again.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.e(TAG, "Error fetching user attributes: ${attributesResult.exceptionOrNull()?.message}")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Error checking user data. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching user data: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error checking user data. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
} 