package com.nhlstenden.appdev

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val tasksFragment = TasksFragment()
    private val rewardsFragment = RewardsFragment()
    private val friendsFragment = FriendsFragment()
    private val progressFragment = ProgressFragment()

    private val profileFragment = ProfileFragment()

    private val PREFS_NAME = "bottom_nav_prefs"
    private val SELECTED_ITEM_KEY = "selected_item_id"

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val fragmentMap  = mapOf(
            R.id.nav_home to profileFragment,
            R.id.nav_tasks to tasksFragment,
            R.id.nav_rewards to rewardsFragment,
            R.id.nav_friends to friendsFragment,
            R.id.nav_progress to progressFragment
        )

        bottomNav.setOnItemSelectedListener { item ->
            fragmentMap[item.itemId]?.let {
                replaceFragment(it)

                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                prefs.edit().putInt(SELECTED_ITEM_KEY, item.itemId).apply()

                true
            } ?: false
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedItemId = prefs.getInt(SELECTED_ITEM_KEY, R.id.nav_home)
        bottomNav.selectedItemId = savedItemId

        val user: UserResponse? = intent.getParcelableExtra("USER_DATA", UserResponse::class.java)

        val bundle = Bundle().apply {
            putParcelable("user", user)
        }
        profileFragment.arguments = bundle

    }
}