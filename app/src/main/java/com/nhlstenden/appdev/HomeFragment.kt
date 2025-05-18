package com.nhlstenden.appdev

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    private lateinit var usernameView: TextView
    private lateinit var emailView: TextView
    private lateinit var pointsView: TextView
    private lateinit var userIdView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usernameView = view.findViewById(R.id.Usernameview)
        emailView = view.findViewById(R.id.Emailview)
        pointsView = view.findViewById(R.id.Pointsview)
        userIdView = view.findViewById(R.id.Useridview)

        // Get user data from arguments
        val userData = arguments?.getParcelable<User>("USER_DATA")
        userData?.let { user ->
            usernameView.text = "Username: ${user.username}"
            emailView.text = "Email: ${user.email}"
            pointsView.text = "Points: ${user.points}"
            userIdView.text = "User ID: ${user.id}"
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}