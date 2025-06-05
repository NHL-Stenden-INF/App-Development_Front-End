package com.nhlstenden.appdev.core.utils

import android.app.Activity
import com.nhlstenden.appdev.features.courses.CourseFragment

object NavigationManager {
    fun navigateToCourseTasks(activity: Activity, courseId: String) {
        val fragment = CourseFragment()
        val args = android.os.Bundle()
        args.putString("COURSE_ID", courseId)
        fragment.arguments = args
        val fragmentManager = (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(com.nhlstenden.appdev.R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
        activity.findViewById<androidx.viewpager2.widget.ViewPager2>(com.nhlstenden.appdev.R.id.viewPager)?.visibility = android.view.View.GONE
        activity.findViewById<android.widget.FrameLayout>(com.nhlstenden.appdev.R.id.fragment_container)?.visibility = android.view.View.VISIBLE
    }

    fun navigateBack(activity: Activity) {
        val fragmentManager = (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
        fragmentManager.popBackStack()
    }
} 