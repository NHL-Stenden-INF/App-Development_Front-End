package com.nhlstenden.appdev.shared.ui.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.nhlstenden.appdev.shared.navigation.NavigationManager

abstract class BaseFragment : Fragment() {
    
    protected fun navigateToFragment(fragment: Fragment, addToBackStack: Boolean = true, tag: String? = null) {
        activity?.let { activity ->
            NavigationManager.navigateToFragment(activity, fragment, addToBackStack, tag)
        }
    }
    
    protected fun navigateBack() {
        activity?.let { activity ->
            NavigationManager.navigateBack(activity)
        }
    }
    
    protected fun showLoading(show: Boolean) {
        // TODO: Implement loading state
    }
    
    protected fun showError(message: String) {
        // TODO: Implement error state
    }
} 