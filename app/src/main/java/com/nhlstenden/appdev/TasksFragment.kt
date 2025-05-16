package com.nhlstenden.appdev

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TasksFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TasksFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var tasksList: RecyclerView
    private lateinit var filterChipGroup: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)
        
        // Initialize views
        tasksList = view.findViewById(R.id.tasksList)
        filterChipGroup = view.findViewById(R.id.filterChipGroup)
        
        setupTasksList()
        setupFilterChips()
        
        return view
    }

    private fun setupTasksList() {
        // Sample data - replace with actual data from your backend
        val tasks = listOf(
            Task("HTML Basics", "Beginner", "Learn the fundamentals of HTML markup language", R.drawable.html_course),
            Task("CSS Styling", "Intermediate", "Master CSS styling and layout techniques", R.drawable.css_course),
            Task("SQL Database", "Advanced", "Learn database management with SQL", R.drawable.sql_course)
        )

        tasksList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = TaskAdapter(tasks)
        }
    }

    private fun setupFilterChips() {
        filterChipGroup.setOnCheckedChangeListener { group, checkedId ->
            // Handle filter changes
            val selectedChips = group.checkedChipIds.map { id ->
                group.findViewById<Chip>(id).text.toString()
            }
            // Update the task list based on selected filters
            // This will be implemented when connecting to the backend
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TasksFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TasksFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

data class Task(
    val title: String,
    val difficulty: String,
    val description: String,
    val imageResId: Int
)

class TaskAdapter(private val tasks: List<Task>) : 
    RecyclerView.Adapter<TaskAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseImage: ImageView = view.findViewById(R.id.courseImage)
        val courseTitle: TextView = view.findViewById(R.id.courseTitle)
        val difficultyLevel: TextView = view.findViewById(R.id.difficultyLevel)
        val courseDescription: TextView = view.findViewById(R.id.courseDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        holder.apply {
            courseImage.setImageResource(task.imageResId)
            courseImage.contentDescription = "${task.title} course icon"
            courseTitle.text = task.title
            difficultyLevel.text = task.difficulty
            courseDescription.text = task.description
        }
    }

    override fun getItemCount() = tasks.size
}