package com.nhlstenden.appdev

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView

class AnimatedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val animationView: LottieAnimationView
    private val textView: TextView
    private var clickListener: OnClickListener? = null
    private var buttonText: String = "Button"
    private var animateOnLoop = false

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.animated_button, this, true)
        animationView = view.findViewById(R.id.buttonAnimation)
        textView = view.findViewById(R.id.buttonText)
        
        // Set up default animation file
        animationView.setAnimation(R.raw.button_animation)
        
        // Handle touch events for animation
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Load press animation
                    animationView.setAnimation(R.raw.button_press)
                    animationView.playAnimation()
                    false
                }

                MotionEvent.ACTION_UP -> {
                    // If pressed within bounds, trigger click
                    if (event.x >= 0 && event.x <= width &&
                        event.y >= 0 && event.y <= height) {
                        
                        // Pass the click event to the original onClick implementation
                        performClick()
                    }
                    
                    // Reset animation back to idle state but don't play it
                    animationView.setAnimation(R.raw.button_animation)
                    animationView.progress = 0f // Reset to first frame
                    animationView.pauseAnimation() // Make sure it's not playing
                    
                    false
                }

                MotionEvent.ACTION_CANCEL -> {
                    // Reset animation back to idle state
                    animationView.setAnimation(R.raw.button_animation)
                    animationView.progress = 0f
                    animationView.pauseAnimation()
                    false
                }

                else -> false
            }
        }
        
        // Extract custom attributes if available
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AnimatedButton)
            try {
                buttonText = typedArray.getString(R.styleable.AnimatedButton_buttonText) ?: buttonText
                animateOnLoop = typedArray.getBoolean(R.styleable.AnimatedButton_animateOnLoop, false)
            } finally {
                typedArray.recycle()
            }
        }
        
        // Set button text
        textView.text = buttonText
    }
    
    // Set button text programmatically
    fun setButtonText(text: String) {
        buttonText = text
        textView.text = text
    }
    
    fun startIdleAnimation() {
        // Only start animation if animateOnLoop is true
        if (animateOnLoop) {
            animationView.setAnimation(R.raw.button_animation)
            animationView.playAnimation()
            animationView.repeatCount = -1 // Infinite loop
        } else {
            // Just set the animation but don't play it
            animationView.setAnimation(R.raw.button_animation)
            animationView.progress = 0f // Set to first frame
            animationView.pauseAnimation() // Make sure it's stopped
        }
    }
    
    override fun setOnClickListener(listener: OnClickListener?) {
        this.clickListener = listener
        // Don't call super here, we handle the click in performClick
    }
    
    override fun performClick(): Boolean {
        super.performClick() // This calls accessibility functions
        clickListener?.onClick(this) // Manually call the click listener
        return true
    }
    
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.5f
    }
} 