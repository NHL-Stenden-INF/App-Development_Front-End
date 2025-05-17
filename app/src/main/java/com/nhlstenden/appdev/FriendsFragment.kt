package com.nhlstenden.appdev

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
import androidx.recyclerview.widget.LinearLayoutManager

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FriendsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FriendsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var friendslist: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_friends, container, false)

        friendslist = view.findViewById(R.id.friendList)

        val friends = listOf(
            Friend("John Doe", 30, "/9j//gARTGF2YzU4LjEzNC4xMDAA/9sAQwAIBgYHBgcICAgICAgJCQkKCgoJCQkJCgoKCgoKDAwMCgoKCgoKCgwMDAwNDg0NDQwNDg4PDw8SEhERFRUVGRkf/8QAkgAAAgMBAQEAAAAAAAAAAAAABgcFAgQBAAgBAAMBAQEAAAAAAAAAAAAAAAADAgEEBRAAAgECBQIDBQMNAQAAAAAAAQIDBBExIQASBRMGB0FRYRSRInGCMghDI5OysTSBcjN0UiSz8BEAAQMCBAUEAgMBAAAAAAAAAQIAEQNBMSESBHGxE4EiYTIzUcHRkXKCBf/AABEIACgAKAMBEgACEgADEgD/2gAMAwEAAhEDEQA/APn/AF7QxjHqoqVqkvtXdttics/M/DUrwISOKVmIDs4FjiVAx+JOtAl3Tu8Jh4p1ShS2YGXsA1sqUEZ3Ddn5W1gS6OT2XgcTNHtYjHVqhze1rX9dQQwl6x4JUO6/rot7b7H5Dn5VZnhpILgs80i9Yr59KmB6pNsGYKnt1L56+7RRyAK1fQGQ4nAc3roJJbB5Tsngfd6qgp6GCB4/eg1RGN0sb00SSxTGSWS4Ug7HXcEYte2oLxC7uoKanq+H4p2llqpmatqy+9ijHd0Q31yIGCix0hNerrHlMWsc2zb0ZVqI8RmJxJ/QfpHaUlIMJgRM3H8te73I0mmk5nIxgkfs8mIcPxddWq5o4xMI2AkkVl2qxGQLE2wGWjrwdoBXcRzQx/2YB8Ynz06tuqO2jqLCScBmSewBfJ/0Nt110/Seb4U01L9omGygvQCx0cIwsKyrRJZMoqeOxZze12dhtVb5YG+t3d/G+5Vn51jHLEYrNmQYje23LG+Rzx16dApro1zAOAuWmgtFMIQVQUpGXC7SpOkxdtUhSgVAZTjxfOK4Ono5qkVZlikQ76arhhacGwt03Rk3rnmL7bg61UPKPUN0+lIqhB85QrvI+vprpCdNsb4uOumofEgtTZ0ykeSSOIcP3HBUUlRS8jHvhdgNrrZJEdP8trGxIzxOWR1zurkRUPHApBEQJb+Y+X8BqK6AqzFqkwwPWA69oY1Mb6/D+iycZzSkEk1dNb9E+rfh6/cOZ/u6f/k+uTdrIUgD3EGPru83fy0naMCxGBZp3L2LFzTBlmaBtrI6lFdZFzIuDgwOBvgdGU33v/eh0jr9NY1IE2M5+rRuflHfk2hZA0gmDiLd3IadrPDbuCJH6FfBUx2+QTtJG6nAgWUx+tj9LjTZqP6H2h+truRuqVMEpSkariM/y/OHxU/7B2oqXGpZMWJcnE8Gi5PCPk5lutfRgt8zdSOcP7QQENrZ+tzpw/lW+1+0670buTJBvg+VGI/1zdFOWWH26Hsf/9k="),
            Friend("Jane Doe", 900, "/9j//gARTGF2YzU4LjEzNC4xMDAA/9sAQwAIBgYHBgcICAgICAgJCQkKCgoJCQkJCgoKCgoKDAwMCgoKCgoKCgwMDAwNDg0NDQwNDg4PDw8SEhERFRUVGRkf/8QAkgAAAgMBAQEAAAAAAAAAAAAABgcFAgQBAAgBAAMBAQEAAAAAAAAAAAAAAAADAgEEBRAAAgECBQIDBQMNAQAAAAAAAQIDBBExIQASBRMGB0FRYRSRInGCMghDI5OysTSBcjN0UiSz8BEAAQMCBAUEAgMBAAAAAAAAAQIAEQNBMSESBHGxE4EiYTIzUcHRkXKCBf/AABEIACgAKAMBEgACEgADEgD/2gAMAwEAAhEDEQA/APn/AF7QxjHqoqVqkvtXdttics/M/DUrwISOKVmIDs4FjiVAx+JOtAl3Tu8Jh4p1ShS2YGXsA1sqUEZ3Ddn5W1gS6OT2XgcTNHtYjHVqhze1rX9dQQwl6x4JUO6/rot7b7H5Dn5VZnhpILgs80i9Yr59KmB6pNsGYKnt1L56+7RRyAK1fQGQ4nAc3roJJbB5Tsngfd6qgp6GCB4/eg1RGN0sb00SSxTGSWS4Ug7HXcEYte2oLxC7uoKanq+H4p2llqpmatqy+9ijHd0Q31yIGCix0hNerrHlMWsc2zb0ZVqI8RmJxJ/QfpHaUlIMJgRM3H8te73I0mmk5nIxgkfs8mIcPxddWq5o4xMI2AkkVl2qxGQLE2wGWjrwdoBXcRzQx/2YB8Ynz06tuqO2jqLCScBmSewBfJ/0Nt110/Seb4U01L9omGygvQCx0cIwsKyrRJZMoqeOxZze12dhtVb5YG+t3d/G+5Vn51jHLEYrNmQYje23LG+Rzx16dApro1zAOAuWmgtFMIQVQUpGXC7SpOkxdtUhSgVAZTjxfOK4Ono5qkVZlikQ76arhhacGwt03Rk3rnmL7bg61UPKPUN0+lIqhB85QrvI+vprpCdNsb4uOumofEgtTZ0ykeSSOIcP3HBUUlRS8jHvhdgNrrZJEdP8trGxIzxOWR1zurkRUPHApBEQJb+Y+X8BqK6AqzFqkwwPWA69oY1Mb6/D+iycZzSkEk1dNb9E+rfh6/cOZ/u6f/k+uTdrIUgD3EGPru83fy0naMCxGBZp3L2LFzTBlmaBtrI6lFdZFzIuDgwOBvgdGU33v/eh0jr9NY1IE2M5+rRuflHfk2hZA0gmDiLd3IadrPDbuCJH6FfBUx2+QTtJG6nAgWUx+tj9LjTZqP6H2h+truRuqVMEpSkariM/y/OHxU/7B2oqXGpZMWJcnE8Gi5PCPk5lutfRgt8zdSOcP7QQENrZ+tzpw/lW+1+0670buTJBvg+VGI/1zdFOWWH26Hsf/9k="),
            Friend("Johan Doe", 3000, "/9j//gARTGF2YzU4LjEzNC4xMDAA/9sAQwAIBgYHBgcICAgICAgJCQkKCgoJCQkJCgoKCgoKDAwMCgoKCgoKCgwMDAwNDg0NDQwNDg4PDw8SEhERFRUVGRkf/8QAkgAAAgMBAQEAAAAAAAAAAAAABgcFAgQBAAgBAAMBAQEAAAAAAAAAAAAAAAADAgEEBRAAAgECBQIDBQMNAQAAAAAAAQIDBBExIQASBRMGB0FRYRSRInGCMghDI5OysTSBcjN0UiSz8BEAAQMCBAUEAgMBAAAAAAAAAQIAEQNBMSESBHGxE4EiYTIzUcHRkXKCBf/AABEIACgAKAMBEgACEgADEgD/2gAMAwEAAhEDEQA/APn/AF7QxjHqoqVqkvtXdttics/M/DUrwISOKVmIDs4FjiVAx+JOtAl3Tu8Jh4p1ShS2YGXsA1sqUEZ3Ddn5W1gS6OT2XgcTNHtYjHVqhze1rX9dQQwl6x4JUO6/rot7b7H5Dn5VZnhpILgs80i9Yr59KmB6pNsGYKnt1L56+7RRyAK1fQGQ4nAc3roJJbB5Tsngfd6qgp6GCB4/eg1RGN0sb00SSxTGSWS4Ug7HXcEYte2oLxC7uoKanq+H4p2llqpmatqy+9ijHd0Q31yIGCix0hNerrHlMWsc2zb0ZVqI8RmJxJ/QfpHaUlIMJgRM3H8te73I0mmk5nIxgkfs8mIcPxddWq5o4xMI2AkkVl2qxGQLE2wGWjrwdoBXcRzQx/2YB8Ynz06tuqO2jqLCScBmSewBfJ/0Nt110/Seb4U01L9omGygvQCx0cIwsKyrRJZMoqeOxZze12dhtVb5YG+t3d/G+5Vn51jHLEYrNmQYje23LG+Rzx16dApro1zAOAuWmgtFMIQVQUpGXC7SpOkxdtUhSgVAZTjxfOK4Ono5qkVZlikQ76arhhacGwt03Rk3rnmL7bg61UPKPUN0+lIqhB85QrvI+vprpCdNsb4uOumofEgtTZ0ykeSSOIcP3HBUUlRS8jHvhdgNrrZJEdP8trGxIzxOWR1zurkRUPHApBEQJb+Y+X8BqK6AqzFqkwwPWA69oY1Mb6/D+iycZzSkEk1dNb9E+rfh6/cOZ/u6f/k+uTdrIUgD3EGPru83fy0naMCxGBZp3L2LFzTBlmaBtrI6lFdZFzIuDgwOBvgdGU33v/eh0jr9NY1IE2M5+rRuflHfk2hZA0gmDiLd3IadrPDbuCJH6FfBUx2+QTtJG6nAgWUx+tj9LjTZqP6H2h+truRuqVMEpSkariM/y/OHxU/7B2oqXGpZMWJcnE8Gi5PCPk5lutfRgt8zdSOcP7QQENrZ+tzpw/lW+1+0670buTJBvg+VGI/1zdFOWWH26Hsf/9k=")
        )

        friendslist.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = FriendAdapter(friends)
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FriendsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FriendsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

data class Friend(
    val name: String,
    val points: Int,
    val profilePicture: String
)

class FriendAdapter(private val friends: List<Friend>) :
    RecyclerView.Adapter<FriendAdapter.ViewHolder>(){

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
            val imageData = Base64.decode(friend.profilePicture, Base64.DEFAULT)
            friendName.text = friend.name
            friendPoints.text = friend.points.toString()
            friendProfilePicture.setImageBitmap(
                BitmapFactory.decodeByteArray(
                    imageData,
                    0,
                    imageData.size
                )
            )
        }
    }

    override fun getItemCount() = friends.size
}