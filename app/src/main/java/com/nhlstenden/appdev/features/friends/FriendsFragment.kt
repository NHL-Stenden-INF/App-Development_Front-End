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
import java.util.UUID
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.viewpager2.widget.ViewPager2
import android.widget.FrameLayout
import android.os.Handler
import android.os.Looper
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import okhttp3.Request
import okhttp3.OkHttpClient
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.EncodeHintType
import com.nhlstenden.appdev.main.MainActivity
import com.nhlstenden.appdev.friends.ui.QRScannerActivity
import com.nhlstenden.appdev.core.models.User
import com.nhlstenden.appdev.supabase.SupabaseClient

class FriendsFragment : Fragment() {
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var user: User
    private lateinit var friendsList: RecyclerView
    private lateinit var friendAdapter: FriendAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var qrCodeImage: ImageView
    private var qrCodeBitmap: Bitmap? = null
    private val supabaseClient = SupabaseClient()
    private val TAG = "FriendsFragment"
    
    fun fetchFriendsNow() {
        if (this::user.isInitialized && view != null && isAdded) {
            Log.d(TAG, "External call to fetch friends, refreshing list")
            
            activity?.let {
                if (it is MainActivity && it.intent.hasExtra("USER_DATA")) {
                    val updatedUser = it.intent.getParcelableExtra("USER_DATA", User::class.java)
                    if (updatedUser != null) {
                        Log.d(TAG, "Got updated user data with ${updatedUser.friends.size} friends")
                        user = updatedUser
                    }
                }
            }
            
            if (this::swipeRefreshLayout.isInitialized) {
                swipeRefreshLayout.isRefreshing = true
            }
            
            fetchFriends()
        } else {
            Log.d(TAG, "Fragment not ready for refresh")
        }
    }
    
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
                
                val outsideScan = data?.getBooleanExtra("OUTSIDE_SCAN", false) ?: false

                activity?.let { mainActivity ->
                    if (mainActivity is MainActivity) {
                        Log.d(TAG, "QR scan successful, immediately navigating to Friends tab")
                        
                        mainActivity.intent.putExtra("NAVIGATE_TO_FRIENDS", true)
                        
                        mainActivity.runOnUiThread {
                            try {
                                mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_friends
                                mainActivity.navigateToTab("friends")
                                mainActivity.findViewById<ViewPager2>(R.id.viewPager)?.visibility = View.VISIBLE
                                mainActivity.findViewById<FrameLayout>(R.id.fragment_container)?.visibility = View.GONE
                            } catch (e: Exception) {
                                Log.e(TAG, "Error navigating to Friends tab", e)
                            }
                        }
                        
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                if (isAdded && !isDetached) {
                                    Log.d(TAG, "Delayed navigation to Friends tab")
                                    mainActivity.navigateToTab("friends")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in delayed navigation", e)
                            }
                        }, 300) 
                    }
                }
                
                if (isValidUUID(scannedData)) {
                    val friendUuid = UUID.fromString(scannedData)
                    if (user.friends.contains(friendUuid)) {
                        Toast.makeText(activity, "This friend is already in your list!", Toast.LENGTH_LONG).show()
                        return@registerForActivityResult
                    }
                    
                    if (outsideScan) {
                        Log.d(TAG, "Handling scan from outside FriendsFragment")
                        
                        activity?.let { mainActivity ->
                            if (mainActivity is MainActivity) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    try {
                                        if (isAdded && !isDetached) {
                                            Log.d(TAG, "Delayed navigation to Friends tab")
                                            mainActivity.navigateToTab("friends")
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error in delayed navigation", e)
                                    }
                                }, 300) 
                            }
                        }
                    }

                    GlobalScope.launch(Dispatchers.Main) {
                        val response = supabaseClient.addFriend(scannedData, user.authToken)
                        if (response.isSuccessful) {
                            Toast.makeText(activity, "Added a new friend!", Toast.LENGTH_LONG).show()
                            
                            updateUserWithNewFriend(scannedData)
                            
                            fetchFriends()
                            
                            activity?.let { mainActivity ->
                                val bottomNav = mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                                Log.d(TAG, "Setting selected nav item to Friends")
                                bottomNav?.selectedItemId = R.id.nav_friends
                                
                                if (mainActivity is MainActivity) {
                                    Log.d(TAG, "Explicitly navigating to Friends tab via MainActivity")
                                    mainActivity.navigateToTab("friends")
                                    
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

        friendsList = view.findViewById(R.id.friendsList)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        qrCodeImage = view.findViewById(R.id.qrCodeImage)

        // Generate QR code immediately
        generateQRCode()

        // Set up the SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            // Refresh friends list when user swipes down
            Log.d(TAG, "Pull-to-refresh triggered, refreshing friends list")
            fetchFriends()
        }
        
        // Customize the refresh indicator colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.primary_dark
        )

        // Initialize with empty list, will be populated in onViewCreated
        val emptyFriends = listOf<Friend>()
        friendAdapter = FriendAdapter(emptyFriends.toMutableList())
        
        friendsList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendAdapter
        }

        val shareButton: Button = view.findViewById(R.id.shareButton)
        shareButton.setOnClickListener {
            shareQRCode()
        }

        val scanButton: Button = view.findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            val intent = Intent(activity, QRScannerActivity::class.java)
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
        activity?.let { mainActivity ->
            if (mainActivity is MainActivity) {
                // Only navigate if NAVIGATE_TO_FRIENDS flag is set
                val shouldNavigate = mainActivity.intent.getBooleanExtra("NAVIGATE_TO_FRIENDS", false)
                if (shouldNavigate) {
                    view?.post {
                        Log.d(TAG, "onStart: Conditional navigation to Friends tab")
                        mainActivity.navigateToTab("friends")
                        // Clear the flag so it doesn't trigger again
                        mainActivity.intent.putExtra("NAVIGATE_TO_FRIENDS", false)
                    }
                }
            }
        }
    }
    
    // Add a method to refresh data when fragment becomes visible again
    override fun onResume() {
        super.onResume()
        // Refresh the friends list in case it was updated elsewhere
        fetchFriends()
        
        activity?.let { mainActivity ->
            val shouldNavigate = mainActivity.intent.getBooleanExtra("NAVIGATE_TO_FRIENDS", false)
            if (shouldNavigate) {
                Log.d(TAG, "onResume: Conditional navigation to Friends tab")
                val bottomNav = mainActivity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNav?.selectedItemId = R.id.nav_friends
                if (mainActivity is MainActivity) {
                    mainActivity.navigateToTab("friends")
                    // Clear the flag so it doesn't trigger again
                    mainActivity.intent.putExtra("NAVIGATE_TO_FRIENDS", false)
                }
            }
        }
    }
    
    private fun fetchFriends() {
        // Log user info to debug
        Log.d(TAG, "User ID: ${user.id}, Friends count: ${user.friends.size}")
        Log.d(TAG, "Auth token: ${user.authToken.take(15)}...")
        
        // Set refreshing indicator
        if (::swipeRefreshLayout.isInitialized && !swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = true
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Always get fresh data from the database when pull-to-refresh is triggered
                if (::swipeRefreshLayout.isInitialized && swipeRefreshLayout.isRefreshing) {
                    Log.d(TAG, "Pull-to-refresh triggered, getting completely fresh data from database")
                    
                    try {
                        // Get fresh list of friend IDs using the RPC function
                        val friendIdsResponse = supabaseClient.getUserFriendIds(user.authToken)
                        
                        if (friendIdsResponse.isSuccessful) {
                            val responseBody = friendIdsResponse.body?.string()
                            Log.d(TAG, "Fresh friend IDs response: $responseBody")
                            
                            // Parse the response as a JSON array of UUIDs
                            val jsonArray = JSONArray(responseBody ?: "[]")
                            val freshFriends = ArrayList<UUID>()
                            for (i in 0 until jsonArray.length()) {
                                try {
                                    val friendObject = jsonArray.getJSONObject(i)
                                    val friendId = friendObject.getString("friend_id")
                                    freshFriends.add(UUID.fromString(friendId))
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error during friend IDs refresh: ${e.message}", e)
                                }
                            }
                            Log.d(TAG, "Retrieved ${freshFriends.size} friend IDs from database")
                            
                            if (freshFriends.size > 0 || jsonArray.length() == 0) {
                                // Update the user with the completely fresh friends list
                                // We update even if the list is empty (jsonArray.length==0) as that's valid - no friends
                                user.friends.clear()
                                user.friends.addAll(freshFriends)
                                
                                // Update MainActivity
                                activity?.let {
                                    if (it is MainActivity) {
                                        it.updateUserData(user)
                                        Log.d(TAG, "Updated user with ${freshFriends.size} fresh friend IDs")
                                    }
                                }
                            } else {
                                Log.w(TAG, "Friend IDs parsing issue, keeping current list.")
                            }
                        } else {
                            // Get the error body for more information
                            val errorBody = friendIdsResponse.body?.string() ?: "No error body"
                            Log.e(TAG, "Failed to refresh friend IDs: ${friendIdsResponse.code}, Error: $errorBody")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during friend IDs refresh: ${e.message}", e)
                    }
                }
                
                // Get friend IDs from the (possibly updated) user object
                val friendIds = user.friends.map { it.toString() }
                Log.d(TAG, "Using these friend IDs: $friendIds")
                
                // Update UI based on whether we have friends or not
                if (friendIds.isEmpty()) {
                    // No friends found, show empty message
                    Log.d(TAG, "No friends found")
                    friendsList.visibility = View.GONE
                    
                    // Stop refresh animation
                    if (::swipeRefreshLayout.isInitialized) {
                        swipeRefreshLayout.isRefreshing = false
                    }
                    return@launch
                }
                
                // We have friends, hide empty message and show list
                Log.d(TAG, "Found ${friendIds.size} friends")
                friendsList.visibility = View.VISIBLE
                
                // Create a new list to store friend details
                val friendDetails = mutableListOf<Friend>()
                
                // For each friend ID, fetch the latest details
                withContext(Dispatchers.IO) {
                    for (friendId in friendIds) {
                        Log.d(TAG, "Fetching fresh details for friend ID: $friendId")
                        
                        // Get fresh details for each friend using existing get_friend_details function
                        val attributesResponse = supabaseClient.getOrCreateFriendAttributes(friendId, user.authToken)
                        
                        if (attributesResponse.isSuccessful) {
                            val attrResponseBody = attributesResponse.body?.string()
                            
                            try {
                                val jsonArray = JSONArray(attrResponseBody ?: "[]")
                                if (jsonArray.length() > 0) {
                                    val friendData = jsonArray.getJSONObject(0)
                                    val points = friendData.getInt("points")
                                    val profilePicture = friendData.getString("profile_picture")
                                    val displayName = friendData.getString("display_name")
                                    val bio = friendData.optString("bio", null)
                                    
                                    // Add friend details
                                    friendDetails.add(Friend(displayName, points, profilePicture, bio))
                                    Log.d(TAG, "Added friend details: $displayName")
                                } else {
                                    val shortId = friendId.replace("-", "").take(8)
                                    friendDetails.add(Friend("User ($shortId)", 0, "", ""))
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing friend data: ${e.message}")
                                val shortId = friendId.replace("-", "").take(8)
                                friendDetails.add(Friend("User ($shortId)", 0, "", ""))
                            }
                        } else {
                            Log.e(TAG, "Error getting friend details: ${attributesResponse.code}")
                            val shortId = friendId.replace("-", "").take(8)
                            friendDetails.add(Friend("User ($shortId)", 0, "", ""))
                        }
                    }
                }
                
                // Update the UI with the fresh data
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Updating adapter with ${friendDetails.size} fresh friends")
                    
                    // Clear and update adapter with completely fresh data
                    friendAdapter.updateFriends(friendDetails)
                    
                    // Stop refresh animation
                    if (::swipeRefreshLayout.isInitialized) {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing friends: ${e.message}", e)
                e.printStackTrace()
                
                // Update UI on error
                withContext(Dispatchers.Main) {
                    friendsList.visibility = View.GONE
                    
                    // Stop refresh animation
                    if (::swipeRefreshLayout.isInitialized) {
                        swipeRefreshLayout.isRefreshing = false
                    }
                    
                    // Show error toast
                    Toast.makeText(context, "Failed to refresh friends list", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateQRCode() {
        val uuid = this.user.id
        qrCodeBitmap = generateQRCodeBitmap(uuid.toString())
        qrCodeImage.setImageBitmap(qrCodeBitmap)
    }

    private fun shareQRCode() {
        qrCodeBitmap?.let { bitmap ->
            try {
                // Create a temporary file to store the QR code image
                val imagesFolder = File(requireContext().cacheDir, "images")
                imagesFolder.mkdirs()
                val imageFile = File(imagesFolder, "qr_code.png")
                
                // Save the bitmap to the file
                val stream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
                stream.close()
                
                // Create the share intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        imageFile
                    ))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // Start the share activity
                startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing QR code: ${e.message}")
                Toast.makeText(context, "Failed to share QR code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateQRCodeBitmap(content: String, size: Int = 512): Bitmap? {
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
        )
        return try {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }
}

data class Friend(
    val name: String,
    val points: Int,
    val profilePicture: String,
    val bio: String?
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
        val friendName: TextView = view.findViewById(R.id.friendUsername)
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