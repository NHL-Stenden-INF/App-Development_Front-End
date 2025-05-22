package com.nhlstenden.appdev

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageCropActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var cropOverlay: View
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button
    
    private var imageBitmap: Bitmap? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    // Current image matrix for tracking transforms
    private val imageMatrix = Matrix()
    private var imageMatrixValues = FloatArray(9)
    
    // Scale gesture detector for pinch-to-zoom
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private val minScale = 0.5f
    private val maxScale = 5.0f
    
    companion object {
        const val EXTRA_IMAGE_URI = "image_uri"
        const val RESULT_CROPPED_IMAGE_URI = "cropped_image_uri"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_crop)
        
        // Get views
        imageView = findViewById(R.id.cropImageView)
        cropOverlay = findViewById(R.id.cropOverlay)
        confirmButton = findViewById(R.id.confirmButton)
        cancelButton = findViewById(R.id.cancelButton)
        
        // Initialize scale gesture detector
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        
        // Get image URI from intent
        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
        if (imageUri == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        
        // Load the image
        try {
            imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            imageView.setImageBitmap(imageBitmap)
            
            // Center the image initially
            centerImage()
        } catch (e: Exception) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        
        // Set up touch listener for dragging the image
        imageView.setOnTouchListener { _, event ->
            // Handle both scaling and dragging
            scaleGestureDetector.onTouchEvent(event)
            
            // If we're not scaling, handle the drag events
            if (!scaleGestureDetector.isInProgress) {
                handleImageTouch(event)
            }
            true
        }
        
        // Set up confirm button
        confirmButton.setOnClickListener {
            // Get the cropped image
            val croppedBitmap = cropImage()
            
            // Save bitmap to a temporary file and return its URI instead of the bitmap itself
            croppedBitmap?.let {
                try {
                    val tempFile = File(cacheDir, "temp_cropped_image.jpg")
                    FileOutputStream(tempFile).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    
                    // Create URI from the saved file
                    val fileUri = Uri.fromFile(tempFile)
                    
                    // Return the URI instead of the bitmap
                    val resultIntent = Intent()
                    resultIntent.putExtra(RESULT_CROPPED_IMAGE_URI, fileUri.toString())
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } catch (e: Exception) {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            } ?: run {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
        
        // Set up cancel button
        cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
    
    // Scale listener for pinch-to-zoom
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Calculate the new scale factor
            scaleFactor *= detector.scaleFactor
            
            // Constrain the scale factor to avoid extreme zooming
            scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
            
            // Get the focus point of the scaling gesture
            val focusX = detector.focusX
            val focusY = detector.focusY
            
            // Apply the scale transformation centered on the focus point
            imageMatrix.getValues(imageMatrixValues)
            val currentScale = imageMatrixValues[Matrix.MSCALE_X]
            val newScale = currentScale * detector.scaleFactor
            
            // Apply the scale
            imageMatrix.postScale(
                detector.scaleFactor, detector.scaleFactor,
                focusX, focusY
            )
            
            // Apply the transformation
            imageView.imageMatrix = imageMatrix
            return true
        }
    }
    
    private fun centerImage() {
        imageBitmap?.let { bitmap ->
            // Get dimensions
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()
            
            // Wait for layout to be ready
            imageView.post {
                val viewWidth = imageView.width.toFloat()
                val viewHeight = imageView.height.toFloat()
                
                // Calculate scale to fill the view while maintaining aspect ratio
                val scale = Math.max(viewWidth / imageWidth, viewHeight / imageHeight)
                scaleFactor = scale  // Initialize the scale factor
                
                // Calculate translation to center
                val translateX = (viewWidth - imageWidth * scale) / 2f
                val translateY = (viewHeight - imageHeight * scale) / 2f
                
                // Apply transformation
                imageMatrix.reset()
                imageMatrix.postScale(scale, scale)
                imageMatrix.postTranslate(translateX, translateY)
                imageView.imageMatrix = imageMatrix
                imageView.scaleType = ImageView.ScaleType.MATRIX
            }
        }
    }
    
    private fun handleImageTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Record initial touch position
                lastTouchX = event.x
                lastTouchY = event.y
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Calculate distance moved
                val deltaX = event.x - lastTouchX
                val deltaY = event.y - lastTouchY
                
                // Update last touch position
                lastTouchX = event.x
                lastTouchY = event.y
                
                // Apply translation
                imageMatrix.postTranslate(deltaX, deltaY)
                imageView.imageMatrix = imageMatrix
            }
        }
    }
    
    private fun cropImage(): Bitmap? {
        imageBitmap?.let { bitmap ->
            // Get crop overlay dimensions relative to imageView
            val cropOverlayLocation = IntArray(2)
            cropOverlay.getLocationOnScreen(cropOverlayLocation)
            
            val imageViewLocation = IntArray(2)
            imageView.getLocationOnScreen(imageViewLocation)
            
            // Calculate crop area in view coordinates
            val cropX = cropOverlayLocation[0] - imageViewLocation[0].toFloat()
            val cropY = cropOverlayLocation[1] - imageViewLocation[1].toFloat()
            val cropWidth = cropOverlay.width.toFloat()
            val cropHeight = cropOverlay.height.toFloat()
            
            // Get current matrix values
            imageMatrix.getValues(imageMatrixValues)
            val scaleX = imageMatrixValues[Matrix.MSCALE_X]
            val scaleY = imageMatrixValues[Matrix.MSCALE_Y]
            val transX = imageMatrixValues[Matrix.MTRANS_X]
            val transY = imageMatrixValues[Matrix.MTRANS_Y]
            
            // Convert from view coordinates to bitmap coordinates
            val bitmapRect = RectF(
                (cropX - transX) / scaleX,
                (cropY - transY) / scaleY,
                (cropX + cropWidth - transX) / scaleX,
                (cropY + cropHeight - transY) / scaleY
            )
            
            // Ensure we're within bitmap bounds
            bitmapRect.left = bitmapRect.left.coerceIn(0f, bitmap.width.toFloat())
            bitmapRect.top = bitmapRect.top.coerceIn(0f, bitmap.height.toFloat())
            bitmapRect.right = bitmapRect.right.coerceIn(0f, bitmap.width.toFloat())
            bitmapRect.bottom = bitmapRect.bottom.coerceIn(0f, bitmap.height.toFloat())
            
            // Create cropped bitmap
            return try {
                Bitmap.createBitmap(
                    bitmap,
                    bitmapRect.left.toInt(),
                    bitmapRect.top.toInt(),
                    (bitmapRect.right - bitmapRect.left).toInt(),
                    (bitmapRect.bottom - bitmapRect.top).toInt()
                )
            } catch (e: Exception) {
                // If there's an error, return null
                null
            }
        }
        return null
    }
} 