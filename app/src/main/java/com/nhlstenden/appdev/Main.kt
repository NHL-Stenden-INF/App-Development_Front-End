package com.nhlstenden.appdev

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView

    private val homeFragment = HomeFragment()
    private val tasksFragment = TasksFragment()
    private val rewardsFragment = RewardsFragment()
    private val friendsFragment = FriendsFragment()
    private val progressFragment = ProgressFragment()

    private val fragments = listOf(
        homeFragment,
        tasksFragment,
        rewardsFragment,
        friendsFragment,
        progressFragment
    )

    private val PREFS_NAME = "bottom_nav_prefs"
    private val SELECTED_ITEM_KEY = "selected_item_id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_navigation)

        // Set up ViewPager2 adapter
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        // Disable ViewPager2 swipe when in nested scrollable views
        viewPager.isUserInputEnabled = true

        // Set up bottom navigation listener
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> viewPager.setCurrentItem(0, true)
                R.id.nav_tasks -> viewPager.setCurrentItem(1, true)
                R.id.nav_rewards -> viewPager.setCurrentItem(2, true)
                R.id.nav_friends -> viewPager.setCurrentItem(3, true)
                R.id.nav_progress -> viewPager.setCurrentItem(4, true)
            }
            true
        }

        // Set up ViewPager2 page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNav.menu.getItem(position).isChecked = true
                
                // Save the selected position
                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                prefs.edit().putInt(SELECTED_ITEM_KEY, position).apply()
            }
        })

        // Restore the last selected position
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedPosition = prefs.getInt(SELECTED_ITEM_KEY, 0)
        viewPager.setCurrentItem(savedPosition, false)

        // Pass user data to HomeFragment
        val user: User? = intent.getParcelableExtra("USER_DATA", User::class.java)
        user?.let {
            homeFragment.setUserData(it)
        }
    }
}