package com.nhlstenden.appdev

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import qrcode.QRCodeBuilder
import qrcode.QRCodeShapesEnum
import qrcode.color.Colors
import qrcode.raw.ErrorCorrectionLevel
import java.util.UUID
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.viewpager2.widget.ViewPager2
import android.widget.FrameLayout
import android.os.Handler
import android.os.Looper

/**
 * A simple [Fragment] subclass.
 * Use the [FriendsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FriendsFragment : Fragment() {
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var user: User
    private lateinit var friendsList: RecyclerView
    private lateinit var friendAdapter: FriendAdapter
    private val supabaseClient = SupabaseClient()
    private val TAG = "FriendsFragment"
    
    // This method can be called from MainActivity to force a refresh
    fun fetchFriendsNow() {
        // Check if view and user are ready
        if (this::user.isInitialized && view != null && isAdded) {
            Log.d(TAG, "External call to fetch friends, refreshing list")
            
            // Make sure we have the latest user data from the activity
            activity?.let {
                if (it is MainActivity && it.intent.hasExtra("USER_DATA")) {
                    val updatedUser = it.intent.getParcelableExtra("USER_DATA", User::class.java)
                    if (updatedUser != null) {
                        Log.d(TAG, "Got updated user data with ${updatedUser.friends.size} friends")
                        user = updatedUser
                    }
                }
            }
            
            // Fetch the latest friend data
            fetchFriends()
        } else {
            Log.d(TAG, "Fragment not ready for refresh")
        }
    }
    
    // Utility function to validate UUID
    private fun isValidUUID(uuidString: String): Boolean {
        return try {
            UUID.fromString(uuidString)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.user = activity?.intent?.getParcelableExtra("USER_DATA", User::class.java)!!

        this.resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val scannedData = data?.getStringExtra("SCANNED_UUID").toString()
                
                // Check if the data is outside scan
                val outsideScan = data?.getBooleanExtra("OUTSIDE_SCAN", false) ?: false

                // Before any async work, immediately try to navigate to Friends tab
                activity?.let { mainActivity ->
                    if (mainActivity is MainActivity) {
                        Log.d(TAG, "QR scan successful, immediately navigating to Friends tab")
                        
                        // Add a flag to the MainActivity's intent to indicate we need to navigate to Friends tab
                        // This will be checked in MainActivity.onResume()
                        mainActivity.intent.putExtra("NAVIGATE_TO_FRIENDS", true)
                        
                        // Immediate navigation
                        mainActivity.runOnUiThread {
                            try {
                                // Navigate directly
                                mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_friends
                                // And use our helper method
                                mainActivity.navigateToTab("friends")
                                // Ensure proper visibility
                                mainActivity.findViewById<ViewPager2>(R.id.viewPager)?.visibility = View.VISIBLE
                                mainActivity.findViewById<FrameLayout>(R.id.fragment_container)?.visibility = View.GONE
                            } catch (e: Exception) {
                                Log.e(TAG, "Error navigating to Friends tab", e)
                            }
                        }
                        
                        // Schedule another navigation attempt after a short delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                if (isAdded && !isDetached) {
                                    Log.d(TAG, "Delayed navigation to Friends tab")
                                    mainActivity.navigateToTab("friends")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in delayed navigation", e)
                            }
                        }, 300) // Wait 300ms to ensure UI has stabilized
                    }
                }
                
                // Check if the scanned data is a valid UUID
                if (isValidUUID(scannedData)) {
                    // First, check if this friend is already in the user's friend list
                    val friendUuid = UUID.fromString(scannedData)
                    if (user.friends.contains(friendUuid)) {
                        Toast.makeText(activity, "This friend is already in your list!", Toast.LENGTH_LONG).show()
                        return@registerForActivityResult
                    }
                    
                    // Handle the case where the QR code was scanned from outside
                    if (outsideScan) {
                        Log.d(TAG, "Handling scan from outside FriendsFragment")
                        
                        // Make sure we're on the Friends tab
                        activity?.let { mainActivity ->
                            if (mainActivity is MainActivity) {
                                // Set up delayed execution to ensure UI is ready
                                Handler(Looper.getMainLooper()).postDelayed({
                                    try {
                                        // Check that we're still valid
                                        if (isAdded && !isDetached) {
                                            Log.d(TAG, "Delayed navigation to Friends tab")
                                            mainActivity.navigateToTab("friends")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error in delayed navigation", e)
                                    }
                                }, 300) // Wait 300ms to ensure UI has stabilized
                            }
                        }
                    }

                    GlobalScope.launch(Dispatchers.Main) {
                        val response = supabaseClient.addFriend(scannedData, user.authToken)
                        if (response.isSuccessful) {
                            Toast.makeText(activity, "Added a new friend!", Toast.LENGTH_LONG).show()
                            
                            // Update user object with the new friend
                            updateUserWithNewFriend(scannedData)
                            
                            // Refresh friends list after adding a new friend
                            fetchFriends()
                            
                            // Make sure we navigate to the Friends tab
                            activity?.let { mainActivity ->
                                // Force navigation to Friends tab
                                val bottomNav = mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                                Log.d(TAG, "Setting selected nav item to Friends")
                                bottomNav?.selectedItemId = R.id.nav_friends
                                
                                // Ensure ViewPager is set to Friends tab (position 3)
                                if (mainActivity is MainActivity) {
                                    Log.d(TAG, "Explicitly navigating to Friends tab via MainActivity")
                                    mainActivity.navigateToTab("friends")
                                    
                                    // Ensure fragment container is hidden and ViewPager is visible
                                    mainActivity.findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
                                    mainActivity.findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
                                }
                            }
                        } else {
                            Toast.makeText(activity, "Failed to add new friend :<", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(activity, "Not a valid QR code", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, "Not a valid QR code", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateUserWithNewFriend(friendId: String) {
        try {
            // Add the new friend UUID to the user's friends list if not already there
            val friendUuid = UUID.fromString(friendId)
            if (!user.friends.contains(friendUuid)) {
                user.friends.add(friendUuid)
                Log.d(TAG, "Added friend $friendId to user's friend list. New count: ${user.friends.size}")
                
                // If we're in an activity context, update the user data
                activity?.let {
                    if (it is MainActivity) {
                        it.updateUserData(user)
                        Log.d(TAG, "Updated MainActivity with new user data")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user with new friend: ${e.message}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friends, container, false)

        friendsList = view.findViewById(R.id.friendList)

        // Initialize with empty list, will be populated in onViewCreated
        val emptyFriends = listOf<Friend>()
        friendAdapter = FriendAdapter(emptyFriends.toMutableList())
        
        friendsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendAdapter
        }

        val shareButton: Button = view.findViewById(R.id.shareCodeButton)
        val uuid = this.user.id

        shareButton.setOnClickListener {
            val qrCode = QRCodeBuilder(QRCodeShapesEnum.SQUARE)
                .withErrorCorrectionLevel(ErrorCorrectionLevel.LOW)
                .withBackgroundColor(Colors.WHITE_SMOKE)
                .build(uuid.toString())
                .renderToBytes()
            val qrCodeImage: ImageView = view.findViewById(R.id.qrImage)

            qrCodeImage.setImageBitmap(
                BitmapFactory.decodeByteArray(
                    qrCode,
                    0,
                    qrCode.size
                )
            )
        }

        val scanButton: Button = view.findViewById(R.id.scanCodeButton)

        scanButton.setOnClickListener {
            val intent = Intent(activity, QRScannerActivity::class.java)
            // Add a flag to remember we should return to the Friends tab
            intent.putExtra("RETURN_TO", "friends")
            Log.d(TAG, "Launching QR scanner with RETURN_TO=friends flag")
            this.resultLauncher.launch(intent)
        }

        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add debug logging for user data
        Log.d(TAG, "User data loaded: ${user.username}, ID: ${user.id}, Friends count: ${user.friends.size}")
        fetchFriends()
    }
    
    override fun onStart() {
        super.onStart()
        
        // One more check to ensure we're on the Friends tab
        activity?.let { mainActivity ->
            if (mainActivity is MainActivity) {
                // Don't switch immediately, give the UI time to set up
                view?.post {
                    Log.d(TAG, "onStart: Post-delayed navigation to Friends tab")
                    mainActivity.navigateToTab("friends")
                }
            }
        }
    }
    
    // Add a method to refresh data when fragment becomes visible again
    override fun onResume() {
        super.onResume()
        // Refresh the friends list in case it was updated elsewhere
        fetchFriends()
        
        // Ensure we're on the Friends tab when this fragment is resumed
        activity?.let { mainActivity ->
            Log.d(TAG, "onResume: Setting selected nav item to Friends")
            val bottomNav = mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNav?.selectedItemId = R.id.nav_friends
            
            if (mainActivity is MainActivity) {
                Log.d(TAG, "onResume: Explicitly navigating to Friends tab")
                mainActivity.navigateToTab("friends")
                
                // If FriendsFragment is being displayed directly (not in ViewPager),
                // we need to make sure we switch back to ViewPager mode
                if (mainActivity.findViewById<FrameLayout>(R.id.fragment_container).visibility == View.VISIBLE) {
                    val currentFragment = mainActivity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                    // Only switch if the current fragment isn't something important like profile
                    if (currentFragment is FriendsFragment) {
                        Log.d(TAG, "onResume: Switching from direct fragment to ViewPager")
                        mainActivity.findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
                        mainActivity.findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
                    }
                }
            }
        }
    }
    
    private fun fetchFriends() {
        // Log user info to debug
        Log.d(TAG, "User ID: ${user.id}, Friends count: ${user.friends.size}")
        Log.d(TAG, "Auth token: ${user.authToken.take(15)}...")
        
        // Don't hide elements yet - wait until we know if we have friends from the user object
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convert the UUIDs to strings
                val friendIds = user.friends.map { it.toString() }
                
                Log.d(TAG, "Friend IDs from User object: $friendIds")
                
                // Update UI based on whether we have friends or not
                withContext(Dispatchers.Main) {
                    val emptyText: TextView? = view?.findViewById(R.id.emptyFriendsText)
                    
                    if (friendIds.isEmpty()) {
                        // No friends found, show empty message
                        Log.d(TAG, "No friends found in user object")
                        emptyText?.visibility = View.VISIBLE
                        friendsList.visibility = View.GONE
                        return@withContext
                    }
                    
                    // We have friends, hide empty message and show list
                    Log.d(TAG, "Found ${friendIds.size} friends in user object")
                    emptyText?.visibility = View.GONE
                    friendsList.visibility = View.VISIBLE
                }
                
                // Create a list to store friend details
                val friendDetails = mutableListOf<Friend>()
                
                // For each friend ID, fetch the details
                for (friendId in friendIds) {
                    Log.d(TAG, "Fetching details for friend ID: $friendId")
                    
                    // Use our new SQL function to get friend details
                    val attributesResponse = supabaseClient.getOrCreateFriendAttributes(friendId, user.authToken)
                    Log.d(TAG, "Friend details response code: ${attributesResponse.code}")
                    
                    if (attributesResponse.isSuccessful) {
                        val attrResponseBody = attributesResponse.body?.string()
                        Log.d(TAG, "Friend detailed response: $attrResponseBody")
                        
                        try {
                            val jsonArray = JSONArray(attrResponseBody ?: "[]")
                            if (jsonArray.length() > 0) {
                                val friendData = jsonArray.getJSONObject(0)
                                val points = friendData.getInt("points")
                                val profilePicture = friendData.getString("profile_picture")
                                
                                // Get the display name from our SQL function
                                val displayName = friendData.getString("display_name")
                                
                                // Add the friend to our list with all data from SQL function
                                friendDetails.add(Friend(displayName, points, profilePicture))
                                Log.d(TAG, "Added friend with full details: $displayName, points: $points")
                            } else {
                                Log.e(TAG, "No friend data in JSON response")
                                val shortId = friendId.replace("-", "").take(8)
                                friendDetails.add(Friend("User ($shortId)", 0, ""))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing friend data: ${e.message}")
                            e.printStackTrace()
                            val shortId = friendId.replace("-", "").take(8)
                            friendDetails.add(Friend("User ($shortId)", 0, ""))
                        }
                    } else {
                        Log.e(TAG, "Error getting friend details: ${attributesResponse.code}")
                        val shortId = friendId.replace("-", "").take(8)
                        friendDetails.add(Friend("User ($shortId)", 0, ""))
                    }
                }
                
                // Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Updating adapter with ${friendDetails.size} friends")
                    friendAdapter.updateFriends(friendDetails)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching friends: ${e.message}")
                e.printStackTrace()
                
                withContext(Dispatchers.Main) {
                    val emptyText: TextView? = view?.findViewById(R.id.emptyFriendsText)
                    emptyText?.visibility = View.VISIBLE
                    friendsList.visibility = View.GONE
                }
            }
        }
    }
}

data class Friend(
    val name: String,
    val points: Int,
    val profilePicture: String
)

class FriendAdapter(private val friends: MutableList<Friend>) :
    RecyclerView.Adapter<FriendAdapter.ViewHolder>(){

    fun updateFriends(newFriends: List<Friend>) {
        friends.clear()
        friends.addAll(newFriends)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val friendProfilePicture: ImageView = view.findViewById(R.id.friendProfilePicture)
        val friendName: TextView = view.findViewById(R.id.friendName)
        val friendPoints: TextView = view.findViewById(R.id.friendPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = friends[position]

        holder.apply {
            friendName.text = friend.name
            friendPoints.text = "${friend.points} pts"
            
            try {
                if (friend.profilePicture.isNotEmpty()) {
                    val imageData = Base64.decode(friend.profilePicture, Base64.DEFAULT)
                    friendProfilePicture.setImageBitmap(
                        BitmapFactory.decodeByteArray(
                            imageData,
                            0,
                            imageData.size
                        )
                    )
                } else {
                    // Set a default image if no profile picture is available
                    friendProfilePicture.setImageResource(R.drawable.zorotlpf)
                }
            } catch (e: Exception) {
                Log.e("FriendAdapter", "Error setting profile picture: ${e.message}")
                // Set a default image on error
                friendProfilePicture.setImageResource(R.drawable.zorotlpf)
            }
            
            // Update the scaleType and padding for the profile picture
            friendProfilePicture.scaleType = ImageView.ScaleType.FIT_CENTER
            friendProfilePicture.setPadding(8, 8, 8, 8)
        }
    }

    override fun getItemCount() = friends.size
} 