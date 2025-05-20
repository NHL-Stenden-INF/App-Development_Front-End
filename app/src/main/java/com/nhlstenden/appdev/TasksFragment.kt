package com.nhlstenden.appdev

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.nhlstenden.appdev.models.Course
import com.nhlstenden.appdev.models.CourseAdapter
import androidx.viewpager2.widget.ViewPager2
import android.widget.FrameLayout

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TasksFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TasksFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var tasksList: RecyclerView
    private lateinit var filterChipGroup: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_courses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tasksList = view.findViewById(R.id.coursesList)
        filterChipGroup = view.findViewById(R.id.filterChipGroup)

        setupTasksList()
        setupFilterChips()
    }

    private fun setupTasksList() {
        val courses = listOf(
            Course(
                "HTML",
                "Beginner",
                "Learn the fundamentals of HTML markup language",
                R.drawable.html_course
            ),
            Course(
                "CSS",
                "Intermediate",
                "Master CSS styling and layout techniques",
                R.drawable.css_course
            ),
            Course(
                "SQL",
                "Advanced",
                "Learn database management with SQL",
                R.drawable.sql_course
            )
        )

        tasksList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CourseAdapter(courses) { course ->
                // Show CourseTopicsFragment in fragment_container
                val fragment = CourseTopicsFragment().apply {
                    arguments = Bundle().apply {
                        putString("courseName", course.title)
                    }
                }
                requireActivity().findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
                requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.GONE
                parentFragmentManager.commit {
                    replace(R.id.fragment_container, fragment)
                    addToBackStack(null)
                }
            }
        }
    }

    private fun setupFilterChips() {
        filterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // TODO: Implement filtering when connected to backend
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TasksFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TasksFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}