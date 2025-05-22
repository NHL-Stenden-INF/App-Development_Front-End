package com.nhlstenden.appdev

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.nhlstenden.appdev.models.UserManager

class EndTaskDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater;

            builder.setView(inflater.inflate(R.layout.item_end_task_popup, null))
                .setPositiveButton("Confirm",
                    DialogInterface.OnClickListener { dialog, id ->
                        val context = requireContext()
                        val intent = Intent(context, MainActivity::class.java)
                        
                        // Get current user from UserManager singleton
                        val currentUser = UserManager.getCurrentUser()
                        if (currentUser != null) {
                            intent.putExtra("USER_DATA", currentUser)
                        }
                        
                        context.startActivity(intent)

                        if (context is Activity)
                        {
                            context.finish()
                        }
                    })
                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()?.cancel()
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}