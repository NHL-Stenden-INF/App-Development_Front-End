package com.nhlstenden.appdev.core.utils

object ProgressCalculator {
    
    fun calculatePercentage(progress: Int, total: Int): Int {
        return if (total > 0) {
            ((progress.toFloat() / total.toFloat()) * 100).toInt()
        } else {
            0
        }
    }

    fun calculatePercentage(progress: Float, total: Float): Int {
        return if (total > 0) {
            ((progress / total) * 100).toInt()
        } else {
            0
        }
    }
} 