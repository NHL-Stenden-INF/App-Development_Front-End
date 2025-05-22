package com.nhlstenden.appdev

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.progressindicator.LinearProgressIndicator

class ProgressFragment : Fragment() {
    private lateinit var pieChart: PieChart
    private lateinit var courseProgressList: RecyclerView
    private lateinit var overallProgressTitle: TextView
    private lateinit var overallProgressPercentage: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress, container, false)
        
        // Initialize views
        pieChart = view.findViewById(R.id.tasksPieChart)
        courseProgressList = view.findViewById(R.id.courseProgressList)
        overallProgressTitle = view.findViewById(R.id.overallProgressTitle)
        overallProgressPercentage = view.findViewById(R.id.overallProgressPercentage)
        
        // Set accessibility content descriptions
        view.findViewById<ImageView>(R.id.settingsIcon).contentDescription = "Settings"
        courseProgressList.contentDescription = "List of course progress"
        
        // Set text from resources
        overallProgressTitle.text = getString(R.string.overall_progress_title)
        
        setupPieChart()
        setupCourseList()
        
        return view
    }

    private fun setupPieChart() {
        // Mock data for task completion
        val totalTasks = 30
        val completedTasks = 18
        val remainingTasks = totalTasks - completedTasks
        val completionPercentage = (completedTasks.toFloat() / totalTasks.toFloat() * 100).toInt()
        
        // Update overall progress percentage
        overallProgressPercentage.text = getString(R.string.overall_progress_percentage, completionPercentage)
        
        val entries = listOf(
            PieEntry(completedTasks.toFloat(), "Completed"),
            PieEntry(remainingTasks.toFloat(), "Remaining")
        )

        val dataSet = PieDataSet(entries, "Task Progress")
        dataSet.colors = listOf(
            Color.rgb(76, 175, 80), // Green for completed
            Color.rgb(158, 158, 158) // Gray for remaining
        )
        dataSet.sliceSpace = 0f // Remove gap between slices
        dataSet.selectionShift = 0f // Remove selection shift effect
        dataSet.valueLinePart1Length = 0.1f // Make the value lines shorter
        dataSet.valueLinePart2Length = 0.1f
        dataSet.yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE // Move values inside slices
        dataSet.valueLineWidth = 0.2f // Make the value lines very thin
        dataSet.valueLineColor = Color.TRANSPARENT // Hide the value lines completely

        val data = PieData(dataSet)
        data.setValueTextSize(10f) // Make the text slightly smaller
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
            setCenterTextSize(14f) // Make center text slightly smaller
            setCenterTextColor(Color.BLACK)
            setDrawCenterText(true)
            setHoleRadius(50f) // Make the center hole larger
            setDrawSliceText(false) // Hide slice labels
            setRotationEnabled(false) // Disable rotation
            setHighlightPerTapEnabled(false) // Disable highlight on tap
            contentDescription = "Task completion chart showing $completedTasks completed and $remainingTasks remaining tasks"
            invalidate()
        }
    }

    private fun setupCourseList() {
        // Sample data - replace with actual data from your backend
        val courses = listOf(
            CourseProgress("HTML", "5/10", 50, R.drawable.html_course),
            CourseProgress("CSS", "8/12", 67, R.drawable.css_course),
            CourseProgress("SQL", "3/8", 38, R.drawable.sql_course)
        )

        courseProgressList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CourseProgressAdapter(courses)
        }
    }
}

data class CourseProgress(
    val title: String,
    val completionStatus: String,
    val progressPercentage: Int,
    val imageResId: Int
)

class CourseProgressAdapter(private val courses: List<CourseProgress>) : 
    RecyclerView.Adapter<CourseProgressAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseImage: ImageView = view.findViewById(R.id.courseImage)
        val courseTitle: TextView = view.findViewById(R.id.courseTitle)
        val completionStatus: TextView = view.findViewById(R.id.completionStatus)
        val progressBar: LinearProgressIndicator = view.findViewById(R.id.progressBar)
        val progressPercentage: TextView = view.findViewById(R.id.progressPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_progress, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]
        holder.apply {
            courseImage.setImageResource(course.imageResId)
            courseImage.contentDescription = "${course.title} course icon"
            courseTitle.text = course.title
            completionStatus.text = course.completionStatus
            progressBar.max = 100
            progressBar.setProgress(course.progressPercentage, false)
            progressBar.setIndicatorColor(Color.rgb(76, 175, 80)) // Set progress color to green
            progressBar.setTrackColor(Color.rgb(158, 158, 158)) // Set track color to gray
            progressPercentage.text = "${course.progressPercentage}%"
        }
    }

    override fun getItemCount() = courses.size
}