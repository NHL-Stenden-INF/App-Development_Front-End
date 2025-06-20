package com.nhlstenden.appdev.features.rewards.dialogs

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.rewards.viewmodels.RewardsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ThemeCustomizationDialog : DialogFragment() {

    private var onThemeApplied: ((String) -> Unit)? = null
    private val rewardsViewModel: RewardsViewModel by activityViewModels()
    private lateinit var colorInput: TextInputEditText
    private lateinit var colorPreview: View

    fun setOnThemeAppliedListener(listener: (String) -> Unit) {
        onThemeApplied = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.Theme_AppdevNHL)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_theme_customization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val applyButton = view.findViewById<MaterialButton>(R.id.applyButton)
        val previewButton = view.findViewById<MaterialButton>(R.id.previewButton)
        colorInput = view.findViewById<TextInputEditText>(R.id.colorInput)
        colorPreview = view.findViewById<View>(R.id.colorPreview)

        // Update preview when text changes
        colorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateColorPreview(s?.toString() ?: "", colorPreview)
            }
        })

        // Preview button
        previewButton.setOnClickListener {
            previewTheme()
        }

        // Apply button
        applyButton.setOnClickListener {
            applyTheme()
        }

        // Initial preview
        updateColorPreview("#FF5733", colorPreview)
    }

    private fun updateColorPreview(colorValue: String, previewView: View) {
        try {
            val color = parseColor(colorValue)
            previewView.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        } catch (e: Exception) {
            // Keep default color if parsing fails
        }
    }

    private fun isValidColor(colorValue: String): Boolean {
        return try {
            parseColor(colorValue)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseColor(colorValue: String): Int {
        val trimmed = colorValue.trim()
        
        return when {
            trimmed.startsWith("#") -> {
                Color.parseColor(trimmed)
            }
            trimmed.startsWith("rgb(") && trimmed.endsWith(")") -> {
                val rgbValues = trimmed.substring(4, trimmed.length - 1)
                    .split(",")
                    .map { it.trim().toInt() }
                Color.rgb(rgbValues[0], rgbValues[1], rgbValues[2])
            }
            else -> {
                // Try as color name or hex without #
                Color.parseColor(trimmed)
            }
        }
    }

    private fun applyTheme() {
        val colorValue = colorInput.text.toString().trim()
        
        if (colorValue.isNotEmpty()) {
            try {
                // Validate color format
                Color.parseColor(colorValue)
                
                // Apply the theme
                rewardsViewModel.applyCustomTheme(colorValue)
                
                // Show success message and close dialog
                Toast.makeText(requireContext(), "Theme applied successfully! Restart app to see changes.", Toast.LENGTH_LONG).show()
                dismiss()
            } catch (e: IllegalArgumentException) {
                colorInput.error = "Invalid color format. Use #RRGGBB or rgb(r,g,b)"
            }
        } else {
            colorInput.error = "Please enter a color value"
        }
    }

    private fun previewTheme() {
        val colorValue = colorInput.text.toString().trim()
        
        if (colorValue.isNotEmpty()) {
            try {
                // Validate color format
                Color.parseColor(colorValue)
                
                // Apply the theme immediately for preview
                rewardsViewModel.applyCustomTheme(colorValue)
                
                // Show preview message
                Toast.makeText(requireContext(), "Theme preview applied! Use 'Apply' to save permanently.", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                colorInput.error = "Invalid color format. Use #RRGGBB or rgb(r,g,b)"
            }
        } else {
            colorInput.error = "Please enter a color value"
        }
    }

    companion object {
        fun newInstance(): ThemeCustomizationDialog {
            return ThemeCustomizationDialog()
        }
    }
} 