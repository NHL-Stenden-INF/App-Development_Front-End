package com.nhlstenden.appdev

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
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
import com.nhlstenden.appdev.models.UserManager
import com.google.android.material.button.MaterialButton

class EndTaskDialogFragment : DialogFragment() {
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            // Make dialog background transparent to use our custom background
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.item_end_task_popup, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Add buttons programmatically to the LinearLayout
        val buttonContainer = view.findViewById<LinearLayout>(R.id.buttonContainer)
        
        // Clear any existing views first
        buttonContainer.removeAllViews()
        
        // Add cancel button
        val cancelButton = MaterialButton(requireContext()).apply {
            text = "Cancel"
            setOnClickListener {
                dismiss()
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                marginEnd = 8
            }
            stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(
                context, R.animator.button_state_animator
            )
        }
        
        // Add confirm button
        val confirmButton = MaterialButton(requireContext()).apply {
            text = "Confirm"
            setOnClickListener {
                // Exit to main activity
                val context = requireContext()
                val intent = Intent(context, MainActivity::class.java)
                
                // Get current user from UserManager singleton
                val currentUser = UserManager.getCurrentUser()
                if (currentUser != null) {
                    intent.putExtra("USER_DATA", currentUser)
                }
                
                context.startActivity(intent)
                
                if (context is Activity) {
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
        
        // Add buttons to container
        buttonContainer.addView(cancelButton)
        buttonContainer.addView(confirmButton)
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