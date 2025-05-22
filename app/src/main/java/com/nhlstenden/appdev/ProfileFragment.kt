package com.nhlstenden.appdev

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class ProfileFragment : Fragment() {
    private lateinit var profilePictureView: ShapeableImageView
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var changePhotoButton: Button
    private var userData: User? = null
    private val supabaseClient = SupabaseClient()
    
    // Final image resolution for the profile picture
    private val PROFILE_IMAGE_SIZE = 120
    
    // Register for image picker result (first step: select image)
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { 
            // Launch the custom cropper with the selected image
            val intent = Intent(requireContext(), ImageCropActivity::class.java)
            intent.putExtra(ImageCropActivity.EXTRA_IMAGE_URI, uri)
            cropImageLauncher.launch(intent)
        }
    }
    
    // Register for crop result (second step: crop image)
    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                // Get the URI of the cropped image from result
                val croppedImageUriString = result.data?.getStringExtra(ImageCropActivity.RESULT_CROPPED_IMAGE_URI)
                if (croppedImageUriString != null) {
                    val croppedImageUri = Uri.parse(croppedImageUriString)
                    
                    // Load bitmap from the URI
                    val inputStream = requireContext().contentResolver.openInputStream(croppedImageUri)
                    val croppedBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    
                    if (croppedBitmap != null) {
                        // Resize if necessary to final profile resolution
                        val finalBitmap = if (croppedBitmap.width != PROFILE_IMAGE_SIZE) {
                            Bitmap.createScaledBitmap(croppedBitmap, PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE, true)
                        } else {
                            croppedBitmap
                        }
                        
                        // Convert to base64
                        val base64Image = bitmapToBase64(finalBitmap)
                        
                        // Check if the base64 string is not too large
                        if (base64Image.length > 100000) {
                            Toast.makeText(context, "Image is too large. Please select a smaller image.", Toast.LENGTH_LONG).show()
                            return@registerForActivityResult
                        }
                        
                        // Update UI
                        profilePictureView.setImageBitmap(finalBitmap)
                        
                        // Update in database
                        updateProfilePicture(base64Image)
                        
                        // Clean up the temporary file
                        try {
                            val file = File(croppedImageUri.path)
                            if (file.exists()) {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            // Non-critical error, just log it
                            android.util.Log.w("ProfileFragment", "Could not delete temp image file", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("ProfileFragment", "Error processing cropped image", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userData = arguments?.getParcelable("USER_DATA")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profilePictureView = view.findViewById(R.id.imageView2)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        changePhotoButton = view.findViewById(R.id.changePhotoButton)

        // Apply card styling
        val cardView = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardViewInputs)
        cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.cardBackground))

        // Setup UI with current data
        refreshUI(userData)

        // Setup change photo button
        changePhotoButton.setOnClickListener {
            // Launch image picker
            pickImageLauncher.launch("image/*")
        }
    }

    // Method to refresh the UI with updated user data
    fun refreshUI(updatedUserData: User?) {
        userData = updatedUserData
        updatedUserData?.let { user ->
            usernameTextView.text = user.username
            emailTextView.text = "Email: ${user.email}"
            
            // Load profile picture
            if (user.profilePicture.isNotEmpty()) {
                try {
                    val imageData = Base64.decode(user.profilePicture, Base64.DEFAULT)
                    profilePictureView.setImageBitmap(
                        BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                    )
                } catch (e: Exception) {
                    // If there's an error, keep the default image
                }
            }
        }
    }
    
    

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Compress with much lower quality to reduce size
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun updateProfilePicture(base64Image: String) {
        userData?.let { user ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = supabaseClient.updateProfilePicture(
                        user.id.toString(),
                        base64Image,
                        user.authToken
                    )
                    
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                            
                            // Update the User object with the new profile picture
                            userData = user.copy(profilePicture = base64Image)
                            
                            // Notify the parent activity/fragment that the user data has changed
                            (activity as? MainActivity)?.updateUserData(userData)
                        } else {
                            val errorBody = response.body?.string() ?: "Unknown error"
                            val errorCode = response.code
                            Toast.makeText(context, "Failed to update profile picture (Error $errorCode)", Toast.LENGTH_SHORT).show()
                            android.util.Log.e("ProfileFragment", "Error response: $errorCode - $errorBody")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        android.util.Log.e("ProfileFragment", "Exception updating profile picture", e)
                    }
                }
            }
        }
    }
}