package com.nhlstenden.appdev.core.models

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconResId: Int,
    val unlocked: Boolean = false
) 