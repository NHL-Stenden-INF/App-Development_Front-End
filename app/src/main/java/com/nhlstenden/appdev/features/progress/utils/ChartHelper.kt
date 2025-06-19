package com.nhlstenden.appdev.features.progress.utils

import android.graphics.Color
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nhlstenden.appdev.features.progress.viewmodels.OverallProgress

object ChartHelper {
    
    fun setupPieChart(pieChart: PieChart, overallProgress: OverallProgress) {
        val entries = listOf(
            PieEntry(overallProgress.completedTasks.toFloat(), "Completed"),
            PieEntry(overallProgress.remainingTasks.toFloat(), "Remaining")
        )

        val dataSet = createPieDataSet(entries)
        val data = createPieData(dataSet, overallProgress.totalTasks)
        
        configurePieChart(pieChart, data, overallProgress)
    }

    private fun createPieDataSet(entries: List<PieEntry>): PieDataSet {
        return PieDataSet(entries, "Task Progress").apply {
            colors = listOf(
                Color.rgb(76, 175, 80),  // Green for completed
                Color.rgb(158, 158, 158) // Gray for remaining
            )
            sliceSpace = 0f
            selectionShift = 0f
            valueLinePart1Length = 0.1f
            valueLinePart2Length = 0.1f
            yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
            valueLineWidth = 0.2f
            valueLineColor = Color.TRANSPARENT
        }
    }

    private fun createPieData(dataSet: PieDataSet, totalTasks: Int): PieData {
        return PieData(dataSet).apply {
            setValueTextSize(10f)
            setValueTextColor(Color.WHITE)
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val percentage = if (totalTasks > 0) (value / totalTasks * 100).toInt() else 0
                    return "$percentage%"
                }
            })
        }
    }

    private fun configurePieChart(pieChart: PieChart, data: PieData, overallProgress: OverallProgress) {
        pieChart.apply {
            this.data = data
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleRadius(30f)
            setDrawEntryLabels(false)
            legend.isEnabled = false
            animateY(1000)
            centerText = "${overallProgress.completedTasks}/${overallProgress.totalTasks}\ntasks"
            setCenterTextSize(14f)
            setCenterTextColor(Color.BLACK)
            setDrawCenterText(true)
            setHoleRadius(50f)
            setDrawSliceText(false)
            setRotationEnabled(false)
            setHighlightPerTapEnabled(false)
            contentDescription = "Task completion chart showing ${overallProgress.completedTasks} completed and ${overallProgress.remainingTasks} remaining tasks"
            invalidate()
        }
    }
} 