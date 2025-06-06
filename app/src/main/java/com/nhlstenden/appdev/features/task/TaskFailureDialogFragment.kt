package com.nhlstenden.appdev.features.task

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.main.MainActivity

class TaskFailureDialogFragment : DialogFragment() {
    
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
        
        val buttonContainer = view.findViewById<LinearLayout>(R.id.buttonContainer)
        buttonContainer.removeAllViews()
        
        val tryAgainButton = MaterialButton(requireContext()).apply {
            text = "Try Again"
            setOnClickListener {
                // Get the activity and reset the questions
                (activity as? com.nhlstenden.appdev.features.task.screens.TaskActivity)?.let { taskActivity ->
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