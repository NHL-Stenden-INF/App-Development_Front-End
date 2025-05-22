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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.WindowManager

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private var userData: User? = null

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
    }

    // Update method to more reliably refresh UI
    fun updateUserData(updatedUser: User?) {
        if (updatedUser != null) {
            // Update the stored user data
            userData = updatedUser
            android.util.Log.d("MainActivity", "User data updated, profile pic: ${updatedUser.profilePicture.take(20)}...")
            
            // Check which fragments are visible and update them
            val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
            val viewPagerView = findViewById<ViewPager2>(R.id.viewPager)
            
            // Handle profile screen if visible
            if (fragmentContainer.visibility == View.VISIBLE) {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment is ProfileFragment) {
                    android.util.Log.d("MainActivity", "Refreshing ProfileFragment")
                    currentFragment.refreshUI(userData)
                }
            }
            
            // Always recreate the ViewPager adapter to ensure fragments are refreshed when shown
            if (viewPagerView.adapter != null) {
                android.util.Log.d("MainActivity", "Recreating ViewPager adapter")
                // Create and set fresh adapter
                viewPagerView.adapter = null
                val newAdapter = MainPagerAdapter(this)
                viewPagerView.adapter = newAdapter
                
                // Preserve current position
                val currentPosition = viewPagerView.currentItem
                viewPagerView.setCurrentItem(currentPosition, false)
            }
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