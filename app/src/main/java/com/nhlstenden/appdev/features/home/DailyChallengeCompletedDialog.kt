package com.nhlstenden.appdev.features.home

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.nhlstenden.appdev.R

class DailyChallengeCompletedDialog(val isSuccessful: Boolean = true) : DialogFragment() {
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
        Log.d("DailyChallengeCompletedDialog", isSuccessful.toString())
        val view = inflater.inflate(R.layout.dialog_daily_challenge_completed, container, false)

        val image = view.findViewById<ImageView>(R.id.imageWarning)
        val title = view.findViewById<TextView>(R.id.textTitle)
        val subtitle = view.findViewById<TextView>(R.id.textMessage)
        view.findViewById<Button>(R.id.homeButton).setOnClickListener {
            setFragmentResult("dialog_action", bundleOf("action" to "home"))
            dismiss()
        }
        view.findViewById<Button>(R.id.playGameButton).setOnClickListener {
            setFragmentResult("dialog_action", bundleOf("action" to "casino"))
            dismiss()
        }

        if (isSuccessful) {
            title.text = title.text.toString().format("Completed!")
            subtitle.text = subtitle.text.toString().format("completed")
        } else {
            title.text = title.text.toString().format("Failed!")
            subtitle.text = subtitle.text.toString().format("failed")
            image.setImageResource(R.drawable.mascot_angry_animation)
            view.findViewById<Button>(R.id.playGameButton).visibility = View.GONE
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.finish()
    }
}