package com.nhlstenden.appdev

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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import androidx.viewpager2.widget.ViewPager2
import android.widget.FrameLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import java.io.Serializable

class CourseTopicsFragment : Fragment() {
    private lateinit var topicsList: RecyclerView
    private lateinit var courseTitle: TextView
    private lateinit var courseDescription: TextView
    private lateinit var backButton: ImageButton
    private lateinit var user: User
    private val args: CourseTopicsFragmentArgs by navArgs()
    private lateinit var gestureDetector: GestureDetectorCompat

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
                        // Swipe right - go back
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

        return inflater.inflate(R.layout.fragment_course_topics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        topicsList = view.findViewById(R.id.topicsList)
        courseTitle = view.findViewById(R.id.courseTitle)
        courseDescription = view.findViewById(R.id.courseDescription)
        backButton = view.findViewById(R.id.backButton)

        // Set up gesture detector
        gestureDetector = GestureDetectorCompat(requireContext(), SwipeGestureListener())

        // Set up touch listener for the root view
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }

        setupCourseInfo()
        setupTopicsList()
        setupBackButton()
    }

    private fun setupCourseInfo() {
        courseTitle.text = args.courseName
        courseDescription.text = when (args.courseName) {
            "HTML" -> "Learn the fundamentals of HTML markup language and web structure"
            "CSS" -> "Master CSS styling, layout techniques, and responsive design"
            "SQL" -> "Learn database management, queries, and data manipulation"
            else -> ""
        }
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            // Pop the back stack to return to the previous fragment
            parentFragmentManager.popBackStack()
            // Restore the main menu visibility
            requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.VISIBLE
            requireActivity().findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
        }
    }

    private fun setupTopicsList() {
        val topics = when (args.courseName) {
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