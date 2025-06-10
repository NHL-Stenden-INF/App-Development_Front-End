package com.nhlstenden.appdev.core.utils

import android.app.Activity
import com.nhlstenden.appdev.features.courses.CourseFragment
import com.nhlstenden.appdev.MainActivity

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
        
        // Hide profile header when navigating to course details
        if (activity is MainActivity) {
            activity.hideProfileHeader()
        }
    }

    fun navigateBack(activity: Activity) {
        val fragmentManager = (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
            // If we're going back to the main view, show the ViewPager and hide the fragment container
            if (fragmentManager.backStackEntryCount == 0) {
                activity.findViewById<androidx.viewpager2.widget.ViewPager2>(com.nhlstenden.appdev.R.id.viewPager)?.visibility = android.view.View.VISIBLE
                activity.findViewById<android.widget.FrameLayout>(com.nhlstenden.appdev.R.id.fragment_container)?.visibility = android.view.View.GONE
                
                // Show profile header if needed when returning to main tabs
                if (activity is MainActivity) {
                    activity.showProfileHeaderIfNeeded()
                }
            }
        }
    }
} 