package com.nhlstenden.appdev.shared.ui.components

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import com.nhlstenden.appdev.R

class BaseCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {
    
    init {
        // Common card styling
        radius = resources.getDimension(R.dimen.card_corner_radius)
        cardElevation = resources.getDimension(R.dimen.card_elevation)
        useCompatPadding = true
    }
    
    fun setLoading(loading: Boolean) {
        isEnabled = !loading
        alpha = if (loading) 0.5f else 1.0f
    }
} 