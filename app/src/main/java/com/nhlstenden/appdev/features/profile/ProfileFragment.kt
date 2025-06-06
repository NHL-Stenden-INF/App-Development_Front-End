package com.nhlstenden.appdev.features.profile.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.databinding.FragmentProfileBinding
import com.nhlstenden.appdev.features.login.screens.LoginActivity
import com.nhlstenden.appdev.features.profile.adapters.AchievementAdapter
import com.nhlstenden.appdev.core.models.Profile
import com.nhlstenden.appdev.features.profile.repositories.ProfileRepositoryImpl
import com.nhlstenden.appdev.features.profile.viewmodels.ProfileViewModel
import com.nhlstenden.appdev.features.profile.viewmodels.ProfileViewModel.ProfileState
import com.nhlstenden.appdev.shared.components.ImageCropActivity
import com.nhlstenden.appdev.core.utils.UserManager
import com.nhlstenden.appdev.shared.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.app.AlertDialog
import android.widget.EditText
import com.yalantis.ucrop.UCrop
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import javax.inject.Inject
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.TextView
import android.widget.ProgressBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.widget.ImageView
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import com.nhlstenden.appdev.shared.components.CameraActivity

@AndroidEntryPoint
class ProfileFragment : BaseFragment(), SensorEventListener {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var achievementAdapter: AchievementAdapter
    private lateinit var musicLobbySwitch: SwitchMaterial
    private val MUSIC_LOBBY_REWARD_ID = 11
    private val PREFS_NAME = "reward_settings"
    private val MUSIC_LOBBY_KEY = "music_lobby_enabled"
    
    private val PROFILE_IMAGE_SIZE = 120
    
    private lateinit var xpCircularProgress: CircularProgressIndicator
    private lateinit var levelBadge: TextView
    private lateinit var profileCardUsername: TextView
    private lateinit var profileCardBio: TextView
    private var sensorManager: SensorManager? = null
    private var gyroSensor: Sensor? = null
    private var profileCard: View? = null
    
    private fun setupViews() {
        binding.editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
        }

        binding.settingsIcon.setOnClickListener {
            showSettingsDialog()
        }

        binding.achievementsRecyclerView.layoutManager = LinearLayoutManager(context)
        achievementAdapter = AchievementAdapter()
        binding.achievementsRecyclerView.adapter = achievementAdapter
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadProfile()
        }

        musicLobbySwitch = binding.root.findViewById(R.id.musicLobbySwitch)
        val sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val isMusicLobbyEnabled = sharedPrefs.getBoolean(MUSIC_LOBBY_KEY, true)
        musicLobbySwitch.isChecked = isMusicLobbyEnabled
        musicLobbySwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean(MUSIC_LOBBY_KEY, isChecked).apply()
        }
        musicLobbySwitch.isEnabled = false

        binding.root.findViewById<ImageView>(R.id.cameraOverlay).setOnClickListener {
            showImageSourceDialog()
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get user data from UserManager
        val userData = UserManager.getCurrentUser()
        
        if (userData != null && userData.authToken.isNotEmpty()) {
            android.util.Log.d("ProfileFragment", "user.id=${userData.id}, user.authToken=${userData.authToken}")
            viewModel.setUserData(userData)
            viewModel.loadProfile()
        } else {
            android.util.Log.e("ProfileFragment", "No valid user data or auth token available")
        }
        
        xpCircularProgress = binding.root.findViewById(R.id.xpCircularProgress)
        levelBadge = binding.root.findViewById(R.id.levelBadge)
        profileCardUsername = binding.root.findViewById(R.id.profileCardUsername)
        profileCardBio = binding.root.findViewById(R.id.profileCardBio)
        profileCard = binding.root.findViewById(R.id.profileCard)
        
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        
        setupViews()
        observeProfileState()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Ensure ViewPager is visible and fragment_container is hidden when leaving profile
        activity?.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)?.visibility = View.VISIBLE
        activity?.findViewById<android.widget.FrameLayout>(R.id.fragment_container)?.visibility = View.GONE
    }
    
    override fun onResume() {
        super.onResume()
        gyroSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()   // sideways
            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat() // up/down
            val maxShift = 60f // px, more movement for larger bg
            val maxTilt = 15f
            val clampedRoll = roll.coerceIn(-maxTilt, maxTilt)
            val clampedPitch = pitch.coerceIn(-maxTilt, maxTilt)
            val shiftX = -clampedRoll / maxTilt * maxShift
            val shiftY = clampedPitch / maxTilt * maxShift
            val bgView = view?.findViewById<View>(R.id.holoCardBg)
            val cardView = profileCard
            if (bgView != null && cardView != null) {
                val maxX = ((bgView.width - cardView.width) / 2).toFloat().coerceAtLeast(0f)
                val maxY = ((bgView.height - cardView.height) / 2).toFloat().coerceAtLeast(0f)
                bgView.translationX = shiftX.coerceIn(-maxX, maxX)
                bgView.translationY = shiftY.coerceIn(-maxY, maxY)
                // 3D tilt for extra holo effect
                bgView.rotationX = clampedPitch
                bgView.rotationY = -clampedRoll
                // Debug log
                android.util.Log.d("HoloGyro", "shiftX=$shiftX shiftY=$shiftY rotX=$clampedPitch rotY=$clampedRoll maxX=$maxX maxY=$maxY")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    
    private fun observeProfileState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileState.collectLatest { state ->
                when (state) {
                    is ProfileState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.swipeRefreshLayout.isRefreshing = true
                    }
                    is ProfileState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false

                        // Display profile image (URL or file path)
                        val profilePic = state.profile.profilePicture
                        val invalidPics = listOf(null, "", "null")
                        if (profilePic !in invalidPics) {
                            if (profilePic!!.startsWith("http")) {
                                Glide.with(this@ProfileFragment)
                                    .load(profilePic)
                                    .placeholder(R.drawable.zorotlpf)
                                    .error(R.drawable.zorotlpf)
                                    .circleCrop()
                                    .into(binding.profileImageView)
                            } else {
                                // Try to load as base64
                                try {
                                    val imageBytes = android.util.Base64.decode(profilePic, android.util.Base64.DEFAULT)
                                    Glide.with(this@ProfileFragment)
                                        .load(imageBytes)
                                        .placeholder(R.drawable.zorotlpf)
                                        .error(R.drawable.zorotlpf)
                                        .circleCrop()
                                        .into(binding.profileImageView)
                                } catch (e: Exception) {
                                    binding.profileImageView.setImageResource(R.drawable.zorotlpf)
                                }
                            }
                        } else {
                            binding.profileImageView.setImageResource(R.drawable.zorotlpf)
                        }

                        // Notify HomeFragment of profile picture update
                        parentFragmentManager.setFragmentResult(
                            "profile_picture_updated",
                            Bundle().apply {
                                putBoolean("updated", true)
                            }
                        )

                        val bio = state.profile.bio
                        profileCardBio.text = if (bio.isNullOrEmpty() || bio == "null") "No bio set yet" else bio
                        profileCardUsername.text = state.profile.displayName

                        // Check unlocked rewards and update toggle
                        val unlockedRewardIds = state.profile.unlockedRewardIds ?: emptyList()
                        updateRewardSettingsSection(unlockedRewardIds)

                        // Set level and XP bar
                        val level = state.profile.level
                        val xp = state.profile.experience
                        levelBadge.text = "Lv. $level"
                        // Calculate XP needed for next level
                        var requiredXp = 100.0
                        var totalXp = 0.0
                        for (i in 1 until level) {
                            totalXp += requiredXp
                            requiredXp *= 1.1
                        }
                        val xpForCurrentLevel = xp - totalXp.toInt()
                        val xpForNextLevel = requiredXp.toInt()
                        xpCircularProgress.max = xpForNextLevel
                        xpCircularProgress.progress = xpForCurrentLevel.coerceAtLeast(0)
                    }
                    is ProfileState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    is ProfileState.LoggedOut -> {
                        requireActivity().finish()
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.achievements.collect { achievements ->
                    achievementAdapter.submitList(achievements)
                }
            }
        }
    }
    
    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val usernameEdit = dialogView.findViewById<EditText>(R.id.editUsername)
        val bioEdit = dialogView.findViewById<EditText>(R.id.editBio)
        usernameEdit.setText(profileCardUsername.text)
        bioEdit.setText(if (profileCardBio.text.toString() == "No bio set yet") "" else profileCardBio.text)
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, which ->
                val newName = usernameEdit.text.toString()
                val newBio = bioEdit.text.toString()
                viewModel.updateProfile(newName, newBio, null)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Choose from Gallery", "Take Photo")
        AlertDialog.Builder(requireContext())
            .setTitle("Change Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openImagePicker()
                    1 -> checkCameraPermissionAndOpenCamera()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let { startCrop(it) }
        }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoUri = result.data?.getStringExtra("photo_uri")
            if (photoUri != null) {
                startCrop(Uri.parse(photoUri))
            }
        }
    }

    private var tempCameraUri: Uri? = null

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun openCamera() {
        try {
            val intent = Intent(requireContext(), CameraActivity::class.java)
            takePicture.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCrop(sourceUri: Uri) {
        try {
            // Create a temporary file for the cropped image
            val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
            
            // Configure UCrop with smaller max size and simpler options
            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(256, 256)
                .withOptions(UCrop.Options().apply {
                    setCompressionQuality(60)
                    setHideBottomControls(true)
                    setFreeStyleCropEnabled(false)
                    setShowCropGrid(false)
                    setShowCropFrame(false)
                })
            
            // Launch UCrop activity
            cropImage.launch(uCrop.getIntent(requireContext()))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to start image cropping: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val cropImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                try {
                    // Convert image to base64
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    
                    // Update profile with base64 image
                    viewModel.updateProfile(
                        profileCardUsername.text.toString(),
                        profileCardBio.text.toString(),
                        base64Image
                    )
                    
                    // Notify HomeFragment of profile picture update
                    parentFragmentManager.setFragmentResult(
                        "profile_picture_updated",
                        Bundle().apply {
                            putBoolean("updated", true)
                        }
                    )
                    
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onEditProfileClicked() {
        // Handle edit profile click
    }

    private fun onLogoutClicked() {
        viewModel.logout()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    private fun updateRewardSettingsSection(unlockedRewardIds: List<Int>) {
        val isUnlocked = unlockedRewardIds.contains(MUSIC_LOBBY_REWARD_ID)
        musicLobbySwitch.isEnabled = isUnlocked
        musicLobbySwitch.alpha = if (isUnlocked) 1.0f else 0.5f
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()

        // Initialize settings switches
        val biometricSwitch = dialogView.findViewById<SwitchMaterial>(R.id.biometricSwitch)
        val achievementNotificationsSwitch = dialogView.findViewById<SwitchMaterial>(R.id.achievementNotificationsSwitch)
        val progressNotificationsSwitch = dialogView.findViewById<SwitchMaterial>(R.id.progressNotificationsSwitch)
        val friendActivitySwitch = dialogView.findViewById<SwitchMaterial>(R.id.friendActivitySwitch)
        val exportDataButton = dialogView.findViewById<MaterialButton>(R.id.exportDataButton)
        val deleteAccountButton = dialogView.findViewById<MaterialButton>(R.id.deleteAccountButton)

        // Load saved settings
        val settingsPrefs = requireContext().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        biometricSwitch.isChecked = settingsPrefs.getBoolean("biometric_enabled", false)
        achievementNotificationsSwitch.isChecked = settingsPrefs.getBoolean("achievement_notifications", true)
        progressNotificationsSwitch.isChecked = settingsPrefs.getBoolean("progress_notifications", true)
        friendActivitySwitch.isChecked = settingsPrefs.getBoolean("friend_activity", true)

        // Set up switch listeners
        biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit().putBoolean("biometric_enabled", isChecked).apply()
        }

        achievementNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit().putBoolean("achievement_notifications", isChecked).apply()
        }

        progressNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit().putBoolean("progress_notifications", isChecked).apply()
        }

        friendActivitySwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit().putBoolean("friend_activity", isChecked).apply()
        }

        // Set up button listeners
        exportDataButton.setOnClickListener {
            // TODO: Implement data export functionality
            Toast.makeText(requireContext(), "Data export functionality coming soon!", Toast.LENGTH_SHORT).show()
        }

        deleteAccountButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    // TODO: Implement account deletion
                    Toast.makeText(requireContext(), "Account deletion functionality coming soon!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
} 