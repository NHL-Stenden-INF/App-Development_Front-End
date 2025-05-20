package com.nhlstenden.appdev

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val emailTextView = view.findViewById<TextView>(R.id.emailTextView)

        val userData = arguments?.getParcelable<User>("USER_DATA")
        userData?.let { user ->
            usernameTextView.text = user.username
            emailTextView.text = "Email: ${user.email}"
        }
    }
}