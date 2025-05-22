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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.user = activity?.intent?.getParcelableExtra("USER_DATA", User::class.java)!!

        this.resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val scannedData = data?.getStringExtra("SCANNED_UUID").toString()

                GlobalScope.launch(Dispatchers.Main) {
                    val response = supabaseClient.addFriend(scannedData, user.authToken)
                    if (response.isSuccessful) {
                        Toast.makeText(activity, "Added a new friend!", Toast.LENGTH_LONG).show()
                        // Refresh friends list after adding a new friend
                        fetchFriends()
                    } else {
                        Toast.makeText(activity, "Failed to add new friend :<", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(activity, "Not a valid QR code", Toast.LENGTH_LONG).show()
            }
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