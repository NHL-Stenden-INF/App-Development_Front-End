package com.nhlstenden.appdev.shared.navigation

import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.widget.ViewPager2
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.courses.ui.CourseFragment
import android.os.Bundle

object NavigationManager {
    fun navigateToFragment(
        activity: FragmentActivity,
        fragment: Fragment,
        addToBackStack: Boolean = true,
        tag: String? = null
    ) {
        showFragmentContainer(activity)
        activity.supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            if (addToBackStack) {
                addToBackStack(tag)
            }
            commit()
        }
    }

    fun navigateBack(activity: FragmentActivity) {
        val fragmentManager = activity.supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
            activity.findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
            activity.findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
        } else {
            showViewPager(activity)
        }
    }

    fun showFragmentContainer(activity: FragmentActivity) {
        activity.findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        activity.findViewById<ViewPager2>(R.id.viewPager).visibility = View.GONE
    }

    fun showViewPager(activity: FragmentActivity) {
        activity.findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
        activity.findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
    }

    fun isFragmentContainerVisible(activity: FragmentActivity): Boolean {
        return activity.findViewById<FrameLayout>(R.id.fragment_container).visibility == View.VISIBLE
    }

    fun getCurrentFragment(activity: FragmentActivity): Fragment? {
        return activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
    }

    fun clearBackStack(activity: FragmentActivity) {
        activity.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    fun navigateToTab(activity: FragmentActivity, tabPosition: Int) {
        activity.findViewById<ViewPager2>(R.id.viewPager).apply {
            visibility = View.VISIBLE
            currentItem = tabPosition
        }
        activity.findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
    }

    fun navigateToCourseTopics(activity: FragmentActivity, courseId: String) {
        val fragment = CourseFragment().apply {
            arguments = Bundle().apply {
                putString("courseId", courseId)
            }
        }
        navigateToFragment(activity, fragment)
    }
} 