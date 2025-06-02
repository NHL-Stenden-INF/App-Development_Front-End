package com.nhlstenden.appdev.core.models

data class Reward(
    val id: Int,
    val title: String,
    val description: String,
    val pointsCost: Int,
    val iconResId: Int,
    val unlocked: Boolean
) 