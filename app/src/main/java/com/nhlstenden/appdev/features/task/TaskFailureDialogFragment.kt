package com.nhlstenden.appdev.features.task

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.MainActivity
import com.nhlstenden.appdev.features.task.screens.TaskActivity
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.repositories.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class TaskFailureDialogFragment : DialogFragment() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var userRepository: UserRepository

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
        return inflater.inflate(R.layout.dialog_task_failure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mascotImageView = view.findViewById<ImageView>(R.id.imageWarning)
        (mascotImageView.drawable as? AnimationDrawable)?.start()
        val buttonContainer = view.findViewById<LinearLayout>(R.id.buttonContainer)
        buttonContainer.removeAllViews()
        val currentUser = authRepository.getCurrentUserSync()
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val attributesResult = userRepository.getUserAttributes(currentUser.id)
                    if (attributesResult.isSuccess) {
                        val profile = attributesResult.getOrThrow()
                        val currentBellPeppers = profile.optInt("bell_peppers", 0)
                        withContext(Dispatchers.Main) {
                            if (currentBellPeppers >= 1) {
                                val tryAgainButton = MaterialButton(requireContext()).apply {
                                    text = "Try Again (Costs 1 Bell Pepper)"
                                    setBackgroundColor(0xFF6200EE.toInt())
                                    setTextColor(0xFFFFFFFF.toInt())
                                    setOnClickListener {
                                        (activity as? TaskActivity)?.let { taskActivity ->
                                            taskActivity.resetForWrongQuestions()
                                        }
                                        dismiss()
                                    }
                                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                                        marginEnd = 8
                                    }
                                    stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(
                                        context, R.animator.button_state_animator
                                    )
                                }
                                val giveUpButton = MaterialButton(requireContext()).apply {
                                    text = "Give Up"
                                    setBackgroundColor(0xFFB00020.toInt())
                                    setTextColor(0xFFFFFFFF.toInt())
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
                                buttonContainer.addView(tryAgainButton)
                                buttonContainer.addView(giveUpButton)
                            } else {
                                val buyButton = MaterialButton(requireContext()).apply {
                                    text = "Buy Bell Pepper"
                                    setBackgroundColor(0xFF018786.toInt())
                                    setTextColor(0xFFFFFFFF.toInt())
                                    setOnClickListener {
                                        dismiss()
                                        BuyBellPepperDialogFragment().show(parentFragmentManager, "buy_bell_pepper")
                                    }
                                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                                        marginEnd = 8
                                    }
                                    stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(
                                        context, R.animator.button_state_animator
                                    )
                                }
                                val giveUpButton = MaterialButton(requireContext()).apply {
                                    text = "Give Up"
                                    setBackgroundColor(0xFFB00020.toInt())
                                    setTextColor(0xFFFFFFFF.toInt())
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
                                buttonContainer.addView(giveUpButton)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error checking bell peppers. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error checking bell peppers. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Fallback: show only Give Up button if user is not available
            val giveUpButton = MaterialButton(requireContext()).apply {
                text = "Give Up"
                setBackgroundColor(0xFFB00020.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setOnClickListener {
                    val context = requireContext()
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                    if (context is android.app.Activity) {
                        context.finish()
                    }
                    dismiss()
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(
                    context, R.animator.button_state_animator
                )
            }
            buttonContainer.addView(giveUpButton)
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