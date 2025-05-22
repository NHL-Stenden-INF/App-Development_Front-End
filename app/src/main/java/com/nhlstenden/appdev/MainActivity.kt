package com.nhlstenden.appdev

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import android.util.Log
import android.content.Intent
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private var userData: User? = null
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure window to handle system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Set up the window insets controller
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        windowInsetsController.isAppearanceLightNavigationBars = true
        
        setContentView(R.layout.activity_main)

        // Disable system back gesture
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        })

        userData = intent.getParcelableExtra("USER_DATA", User::class.java)

        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        // Set up ViewPager2
        viewPager.adapter = MainPagerAdapter(this)
        viewPager.offscreenPageLimit = 1
        viewPager.overScrollMode = View.OVER_SCROLL_NEVER
        viewPager.isUserInputEnabled = true

        // Set up page transformer for smoother transitions
        viewPager.setPageTransformer { page, position ->
            val scale = if (position < 0) 1f + position * 0.1f else 1f - position * 0.1f
            page.scaleX = scale
            page.scaleY = scale
        }

        // Set up page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigation.selectedItemId = when (position) {
                    0 -> R.id.nav_home
                    1 -> R.id.nav_courses
                    2 -> R.id.nav_rewards
                    3 -> R.id.nav_friends
                    4 -> R.id.nav_progress
                    else -> R.id.nav_home
                }
            }
        })

        // Set up bottom navigation
        bottomNavigation.setOnItemSelectedListener { item ->
            val position = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_courses -> 1
                R.id.nav_rewards -> 2
                R.id.nav_friends -> 3
                R.id.nav_progress -> 4
                else -> 0
            }
            
            // Only change page if we're not already on that page
            if (viewPager.currentItem != position) {
                viewPager.currentItem = position
            }
            
            // Clear the back stack and restore the main menu visibility
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
            findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
            true
        }
        
        // Check if we should navigate to a specific tab on startup/resume
        if (intent.hasExtra("NAVIGATE_TO_TAB")) {
            navigateToTab(intent.getStringExtra("NAVIGATE_TO_TAB") ?: "home")
        }
    }
    
    // Method to navigate to a specific tab
    fun navigateToTab(tabName: String) {
        val tabPosition = when (tabName.lowercase()) {
            "home" -> 0
            "courses" -> 1
            "rewards" -> 2
            "friends" -> 3
            "progress" -> 4
            else -> -1
        }
        
        if (tabPosition >= 0) {
            Log.d(TAG, "Navigating to tab: $tabName (position $tabPosition)")
            
            // Ensure we're in the right UI mode first
            val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
            val viewPagerView = findViewById<ViewPager2>(R.id.viewPager)
            
            // If we're showing a fragment in the container but not a specialized one (like profile),
            // go back to ViewPager mode
            if (fragmentContainer.visibility == View.VISIBLE) {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment == null || currentFragment is FriendsFragment) {
                    // Switch to ViewPager mode
                    Log.d(TAG, "Switching from fragment container to ViewPager for tab navigation")
                    viewPagerView.visibility = View.VISIBLE
                    fragmentContainer.visibility = View.GONE
                }
            }
            
            try {
                // Set ViewPager position
                viewPagerView.setCurrentItem(tabPosition, false)
                // Set bottom navigation selection
                bottomNavigation.selectedItemId = when (tabPosition) {
                    0 -> R.id.nav_home
                    1 -> R.id.nav_courses
                    2 -> R.id.nav_rewards
                    3 -> R.id.nav_friends
                    4 -> R.id.nav_progress
                    else -> R.id.nav_home
                }
                
                // Mark this tab in the intent for persistence
                intent.putExtra("CURRENT_TAB", tabName)
                
                // Clear any back stack if we're explicitly navigating
                supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to tab $tabName", e)
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        
        // Update our intent reference
        if (intent != null) {
            setIntent(intent)
            
            // Check if we should navigate to a specific tab
            if (intent.hasExtra("NAVIGATE_TO_TAB")) {
                navigateToTab(intent.getStringExtra("NAVIGATE_TO_TAB") ?: "home")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Check if we need to navigate to the Friends tab after returning from another activity
        if (resultCode == RESULT_OK && data?.getBooleanExtra("NAVIGATE_TO_FRIENDS", false) == true) {
            Log.d(TAG, "onActivityResult: Navigating to Friends tab")
            navigateToTab("friends")
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Check if we need to navigate to the Friends tab
        if (intent.getBooleanExtra("NAVIGATE_TO_FRIENDS", false)) {
            Log.d(TAG, "onResume: NAVIGATE_TO_FRIENDS flag found, navigating to Friends tab")
            // Small delay to ensure UI is ready
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToTab("friends")
                
                // Check again after a longer delay to ensure no other navigation overrides this
                Handler(Looper.getMainLooper()).postDelayed({
                    if (intent.getBooleanExtra("NAVIGATE_TO_FRIENDS", false)) {
                        Log.d(TAG, "Final check for Friends navigation")
                        
                        // Force correct visibility
                        findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
                        findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
                        
                        // Navigate to Friends one last time
                        bottomNavigation.selectedItemId = R.id.nav_friends
                        viewPager.setCurrentItem(3, false) // Direct ViewPager navigation
                    }
                }, 500) // 500ms later as final check
            }, 100)
        }
    }

    // Update method to more reliably refresh UI
    fun updateUserData(updatedUser: User?) {
        if (updatedUser != null) {
            // Update the stored user data
            userData = updatedUser
            Log.d(TAG, "User data updated, friends count: ${updatedUser.friends.size}")
            
            // Check which fragments are visible and update them
            val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
            val viewPagerView = findViewById<ViewPager2>(R.id.viewPager)
            
            // Handle profile screen if visible
            if (fragmentContainer.visibility == View.VISIBLE) {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment is ProfileFragment) {
                    Log.d(TAG, "Refreshing ProfileFragment")
                    currentFragment.refreshUI(userData)
                }
            }
            
            // Update the currently visible fragment in ViewPager if it's FriendsFragment
            if (viewPagerView.visibility == View.VISIBLE) {
                val currentItem = viewPagerView.currentItem
                if (currentItem == 3) { // Index 3 is the FriendsFragment
                    Log.d(TAG, "FriendsFragment is currently visible, refreshing it directly")
                    
                    // Try to find the FriendsFragment in the FragmentManager
                    val fragmentManager = supportFragmentManager
                    fragmentManager.fragments.forEach { fragment ->
                        if (fragment is FriendsFragment && fragment.isVisible) {
                            Log.d(TAG, "Found visible FriendsFragment, triggering refresh")
                            // Force refresh by calling fetchFriends
                            fragment.fetchFriendsNow()
                        }
                    }
                }
            }
            
            // Always recreate the ViewPager adapter to ensure fragments are refreshed when shown
            if (viewPagerView.adapter != null) {
                Log.d(TAG, "Recreating ViewPager adapter")
                // Create and set fresh adapter
                viewPagerView.adapter = null
                val newAdapter = MainPagerAdapter(this)
                viewPagerView.adapter = newAdapter
                
                // Preserve current position
                val currentPosition = viewPagerView.currentItem
                viewPagerView.setCurrentItem(currentPosition, false)
            }
            
            // Update the intent to keep the User data in sync for future activities
            intent.putExtra("USER_DATA", updatedUser)
        }
    }

    private inner class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        // Remove fragments map as it's not necessary with our new approach
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("USER_DATA", userData)
                    }
                }
                1 -> CoursesFragment()
                2 -> RewardsFragment()
                3 -> FriendsFragment()
                4 -> ProgressFragment()
                else -> HomeFragment()
            }
        }
    }
} 