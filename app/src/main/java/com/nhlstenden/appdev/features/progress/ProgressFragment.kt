package com.nhlstenden.appdev.features.progress

import android.app.Application
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.features.courses.CourseFragment
import com.nhlstenden.appdev.features.courses.CourseParser
import com.nhlstenden.appdev.features.courses.model.Course
import com.nhlstenden.appdev.features.courses.repositories.CourseRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import com.daimajia.numberprogressbar.NumberProgressBar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nhlstenden.appdev.core.utils.NavigationManager
import javax.inject.Inject

@AndroidEntryPoint
class ProgressFragment : Fragment() {
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var courseRepositoryImpl: CourseRepositoryImpl
    private lateinit var pieChart: PieChart
    private lateinit var courseProgressList: RecyclerView
    private lateinit var overallProgressTitle: TextView
    private lateinit var overallProgressPercentage: TextView
    private var courses: List<Course>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress, container, false)

        pieChart = view.findViewById(R.id.tasksPieChart)
        courseProgressList = view.findViewById(R.id.courseProgressList)
        overallProgressTitle = view.findViewById(R.id.overallProgressTitle)
        overallProgressPercentage = view.findViewById(R.id.overallProgressPercentage)
        
        courseProgressList.contentDescription = "List of course progress"
        overallProgressTitle.text = getString(R.string.overall_progress_title)
        
        loadData()
        
        return view
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    // Fetch course data from the repository in background thread
    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentUser = authRepository.getCurrentUserSync()
                if (currentUser != null) {
                    courses = courseRepositoryImpl.getCourses(currentUser)
                    withContext(Dispatchers.Main) {
                        setupPieChart()
                        setupCourseList()
                    }
                }
            } catch (e: RuntimeException) {
                Log.e("ProgressFragment", "Error loading data", e)
            }
        }
    }

    // Create and configure the pie chart showing overall task completion
    private fun setupPieChart() {
        var totalTasks = 0
        var completedTasks = 0

        courses?.forEach { course ->
            if (course.progress != 0) {
                totalTasks += course.totalTasks
                completedTasks += course.progress
            }
        }

        val remainingTasks = totalTasks - completedTasks

        val completionPercentage = if (totalTasks > 0) {
            (completedTasks.toFloat() / totalTasks.toFloat() * 100).toInt()
        } else {
            0
        }

        overallProgressPercentage.text = getString(R.string.overall_progress_percentage, completionPercentage)

        val entries = listOf(
            PieEntry(completedTasks.toFloat(), "Completed"),
            PieEntry(remainingTasks.toFloat(), "Remaining")
        )

        val dataSet = PieDataSet(entries, "Task Progress")
        dataSet.colors = listOf(
            Color.rgb(76, 175, 80),
            Color.rgb(158, 158, 158)
        )
        dataSet.sliceSpace = 0f
        dataSet.selectionShift = 0f
        dataSet.valueLinePart1Length = 0.1f
        dataSet.valueLinePart2Length = 0.1f
        dataSet.yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
        dataSet.valueLineWidth = 0.2f
        dataSet.valueLineColor = Color.TRANSPARENT

        val data = PieData(dataSet)
        data.setValueTextSize(10f)
        data.setValueTextColor(Color.WHITE)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val percentage = (value / totalTasks * 100).toInt()
                return "$percentage%"
            }
        })

        pieChart.apply {
            this.data = data
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleRadius(30f)
            setDrawEntryLabels(false)
            legend.isEnabled = false
            animateY(1000)
            centerText = "$completedTasks/$totalTasks\ntasks"
            setCenterTextSize(14f)
            setCenterTextColor(Color.BLACK)
            setDrawCenterText(true)
            setHoleRadius(50f)
            setDrawSliceText(false)
            setRotationEnabled(false)
            setHighlightPerTapEnabled(false)
            contentDescription = "Task completion chart showing $completedTasks completed and $remainingTasks remaining tasks"
            invalidate()
        }
    }

    // Create and populate the recyclerview showing individual course progress
    private fun setupCourseList() {
        val progressCourses: List<CourseProgress> = courses?.mapNotNull{ course ->
            if (course.progress == 0) {
                Log.d("ProgressFragment", "Not adding course: ${course.title}")
                return@mapNotNull null
            }
            Log.d("ProgressFragment", "Adding course: ${course.title}")

            CourseProgress(
                course.id,
                course.title,
                "${course.progress}/${course.totalTasks}",
                ((course.progress.toFloat() / course.totalTasks.toFloat()) * 100).toInt(),
                course.imageResId,
            )
        } ?: emptyList()

        courseProgressList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CourseProgressAdapter(progressCourses) { courseId ->
                NavigationManager.navigateToCourseTasks(requireActivity(), courseId)
            }
        }
    }
    
    // Legacy navigation method - kept for potential future use
    private fun navigateToCourse(courseName: String) {
        val fragment = CourseFragment().apply {
            arguments = Bundle().apply {
                putString("courseName", courseName)
            }
        }

        requireActivity().findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
        requireActivity().findViewById<ViewPager2>(R.id.viewPager).visibility = View.GONE
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}

data class CourseProgress(
    val id: String,
    val title: String,
    val completionStatus: String,
    val progressPercentage: Int,
    val imageResId: Int
)

class CourseProgressAdapter(
    private val courses: List<CourseProgress>,
    private val onCourseClick: (String) -> Unit
) : RecyclerView.Adapter<CourseProgressAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseImage: ImageView = view.findViewById(R.id.courseImage)
        val courseTitle: TextView = view.findViewById(R.id.courseTitle)
        val difficultyLevel: TextView = view.findViewById(R.id.difficultyLevel)
        val progressBar: NumberProgressBar = view.findViewById(R.id.progressBar)
        val root: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]
        holder.courseTitle.text = course.title
        holder.difficultyLevel.text = course.completionStatus
        holder.progressBar.progress = course.progressPercentage
        holder.courseImage.setImageResource(course.imageResId)

        holder.root.setOnClickListener {
            onCourseClick(course.id)
        }
    }

    override fun getItemCount() = courses.size
}