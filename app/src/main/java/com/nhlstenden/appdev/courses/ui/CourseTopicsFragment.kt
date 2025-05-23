package com.nhlstenden.appdev.courses.ui

import android.media.MediaPlayer
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import androidx.viewpager2.widget.ViewPager2
import android.widget.FrameLayout
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.supabase.SupabaseClient
import com.nhlstenden.appdev.task.ui.TaskActivity
import com.nhlstenden.appdev.supabase.User
import com.nhlstenden.appdev.courses.parser.CourseParser
import java.io.Serializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class CourseTopicsFragment : Fragment() {
    private lateinit var topicsList: RecyclerView
    private lateinit var courseTitle: TextView
    private lateinit var courseDescription: TextView
    private lateinit var backButton: ImageButton
    private lateinit var user: User
    private lateinit var gestureDetector: GestureDetectorCompat
    private var mediaPlayer: MediaPlayer? = null
    private var courseName: String = ""
    private var courseData: CourseParser.Course? = null

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                    if (diffX > 0) {
                        findNavController().navigateUp()
                        return true
                    }
                }
            }
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.user = activity?.intent?.getParcelableExtra("USER_DATA", User::class.java)!!
        this.courseName = arguments?.getString("courseName") ?: ""
        
        val courseParser = CourseParser(requireContext())
        this.courseData = courseParser.loadCourseByTitle(courseName)

        return inflater.inflate(R.layout.fragment_course_topics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        topicsList = view.findViewById(R.id.topicsList)
        courseTitle = view.findViewById(R.id.courseTitle)
        courseDescription = view.findViewById(R.id.courseDescription)
        backButton = view.findViewById(R.id.backButton)

        gestureDetector = GestureDetectorCompat(requireContext(), SwipeGestureListener())

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }

        setupCourseInfo()
        setupTopicsList()
        setupBackButton()
        fetchUnlockedRewardsAndPlayMusic()
    }

    private fun setupCourseInfo() {
        courseTitle.text = courseName
        courseDescription.text = courseData?.description ?: when (courseName) {
            "HTML" -> "Learn the fundamentals of HTML markup language and web structure"
            "CSS" -> "Master CSS styling, layout techniques, and responsive design"
            "SQL" -> "Learn database management, queries, and data manipulation"
            else -> ""
        }
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()

            requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
            requireActivity().findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
        }
    }

    private fun setupTopicsList() {
        val topics = courseData?.topics ?: when (courseName) {
            "HTML" -> listOf(
                Topic("HTML Basics", "Beginner", "Learn the fundamentals of HTML markup language", 75),
                Topic("HTML Structure", "Beginner", "Learn about the basic structure of HTML documents", 50),
                Topic("HTML Forms", "Intermediate", "Create interactive forms with HTML", 25),
                Topic("HTML Tables", "Intermediate", "Organize data with HTML tables", 0)
            )
            "CSS" -> listOf(
                Topic("CSS Basics", "Beginner", "Learn the fundamentals of CSS styling", 100),
                Topic("CSS Layout", "Intermediate", "Master CSS layout techniques", 75),
                Topic("CSS Flexbox", "Advanced", "Create flexible layouts with CSS Flexbox", 50),
                Topic("CSS Grid", "Advanced", "Build complex layouts with CSS Grid", 25)
            )
            "SQL" -> listOf(
                Topic("SQL Basics", "Beginner", "Learn the fundamentals of SQL", 100),
                Topic("SQL Queries", "Intermediate", "Write complex SQL queries", 75),
                Topic("SQL Joins", "Advanced", "Combine data from multiple tables", 50),
                Topic("SQL Optimization", "Advanced", "Optimize your SQL queries", 25)
            )
            else -> emptyList()
        }

        topicsList.adapter = TopicAdapter(requireContext(), topics, user)
    }

    private fun fetchUnlockedRewardsAndPlayMusic() {
        val supabaseClient = SupabaseClient()
        CoroutineScope(Dispatchers.IO).launch {
            val response = supabaseClient.getUserUnlockedRewards(user.id.toString(), user.authToken)
            val unlockedRewards = mutableListOf<String>()
            if (response.code == 200) {
                val responseBody = response.body?.string()
                val rewardsArray = JSONArray(responseBody ?: "[]")
                for (i in 0 until rewardsArray.length()) {
                    val rewardId = rewardsArray.getJSONObject(i).getString("reward_id")
                    unlockedRewards.add(rewardId)
                }
            }
            withContext(Dispatchers.Main) {
                playMusicWithUnlockedRewards(unlockedRewards)
            }
        }
    }

    private fun playMusicWithUnlockedRewards(unlockedRewards: List<String>) {
        val hasMusicReward = unlockedRewards.any { it.equals("Course Lobby Music", ignoreCase = true) }
        android.util.Log.d("CourseTopicsFragment", "Unlocked rewards: $unlockedRewards, Has music reward: $hasMusicReward")
        if (hasMusicReward) {
            try {
                mediaPlayer = MediaPlayer.create(context,
                    when(courseName) {
                        "HTML" -> R.raw.html_themesong
                        "CSS" -> R.raw.css_themesong
                        "SQL" -> R.raw.sql_themesong
                        else -> R.raw.default_themesong
                    })
                mediaPlayer?.start()
                android.util.Log.d("CourseTopicsFragment", "Started playing music for course: $courseName")
            } catch (e: Exception) {
                android.util.Log.e("CourseTopicsFragment", "Error playing music: ${e.message}")
            }
        } else {
            android.util.Log.d("CourseTopicsFragment", "Music reward not unlocked")
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    data class Topic(
        val title: String,
        val difficulty: String,
        val description: String,
        val progress: Int
    ) : Serializable

    class TopicAdapter(
        private val context: Context,
        private val topics: List<Topic>,
        private val user: User
    ) : RecyclerView.Adapter<TopicAdapter.TopicViewHolder>() {
        class TopicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view as MaterialCardView
            val title: TextView = view.findViewById(R.id.topicTitle)
            val difficulty: TextView = view.findViewById(R.id.difficultyLevel)
            val description: TextView = view.findViewById(R.id.topicDescription)
            val progressBar: ProgressBar = view.findViewById(R.id.topicProgress)
            val progressText: TextView = view.findViewById(R.id.progressText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_topic, parent, false)
            return TopicViewHolder(view)
        }

        override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
            val topic = topics[position]
            holder.card.setOnClickListener {
                val intent = Intent(context, TaskActivity::class.java)
                intent.putExtra("TOPIC_DATA", topic)
                intent.putExtra("USER_DATA", user)
                context.startActivity(intent)

                if (context is Activity)
                {
                    context.finish()
                }
            }
            holder.title.text = topic.title
            holder.difficulty.text = topic.difficulty
            holder.description.text = topic.description
            holder.progressBar.progress = topic.progress
            holder.progressText.text = "${topic.progress}% Complete"
        }

        override fun getItemCount() = topics.size
    }
}