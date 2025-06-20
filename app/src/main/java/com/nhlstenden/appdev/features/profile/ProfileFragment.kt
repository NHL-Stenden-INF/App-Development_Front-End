package com.nhlstenden.appdev.features.profile

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
import androidx.recyclerview.widget.GridLayoutManager
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
import com.nhlstenden.appdev.core.repositories.AuthRepository
import com.nhlstenden.appdev.core.ui.base.BaseFragment
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
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import com.nhlstenden.appdev.core.repositories.SettingsRepository
import com.nhlstenden.appdev.features.profile.repositories.SettingsRepositoryImpl.SettingsConstants
import com.nhlstenden.appdev.shared.components.CameraActivity
import com.nhlstenden.appdev.utils.LevelCalculator
import com.nhlstenden.appdev.utils.RewardChecker
import com.nhlstenden.appdev.core.repositories.AchievementRepository
import com.nhlstenden.appdev.supabase.SupabaseClient
import com.nhlstenden.appdev.supabase.updateUserFriendMask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ProfileFragment : BaseFragment(), SensorEventListener {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var achievementAdapter: AchievementAdapter
    private lateinit var musicLobbySwitch: SwitchMaterial
    private val MUSIC_LOBBY_REWARD_ID = 11
    private val PREFS_NAME = "reward_settings"
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var rewardChecker: RewardChecker
    
    @Inject
    lateinit var achievementRepository: AchievementRepository

    @Inject
    lateinit var supabaseClient: SupabaseClient

    private val PROFILE_IMAGE_SIZE = 120
    private val MAX_BIO_LENGTH = 128
    private val MAX_NAME_LENGTH = 32
    
    private lateinit var xpCircularProgress: CircularProgressIndicator
    private lateinit var levelBadge: TextView
    private lateinit var profileCardUsername: TextView
    private lateinit var profileCardBio: TextView
    private var sensorManager: SensorManager? = null
    private var gyroSensor: Sensor? = null
    private var profileCard: View? = null

    // Gyro smoothing and animation parameters
    private val maxShift = 60f // px, tweak for more/less movement
    private val maxTilt = 15f // degrees, tweak for more/less tilt
    private val cardTiltFactor = 0.4f // how much the card tilts compared to bg
    private val smoothing = 0.15f // 0..1, higher = snappier, lower = smoother
    private var lastShiftX = 0f
    private var lastShiftY = 0f
    private var lastPitch = 0f
    private var lastRoll = 0f

    private fun setupViews() {
        binding.editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        binding.logoutButton.setOnClickListener {
            onLogoutClicked()
        }

        binding.settingsIcon.setOnClickListener {
            showSettingsDialog()
        }

        binding.achievementsRecyclerView.layoutManager = GridLayoutManager(context, 2)
        achievementAdapter = AchievementAdapter()
        binding.achievementsRecyclerView.adapter = achievementAdapter
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadProfile()
        }

        musicLobbySwitch = binding.root.findViewById(R.id.musicLobbySwitch)
        val sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isMusicLobbyEnabled = settingsRepository.hasValue(SettingsConstants.COURSE_LOBBY_MUSIC)
        musicLobbySwitch.isChecked = isMusicLobbyEnabled
        musicLobbySwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                val success = rewardChecker.setMusicLobbyEnabled(requireContext(), isChecked)
                if (success) {
                    if (isChecked) {
                        settingsRepository.addValue(SettingsConstants.COURSE_LOBBY_MUSIC)
                        Toast.makeText(requireContext(), "Course lobby music enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        settingsRepository.removeValue(SettingsConstants.COURSE_LOBBY_MUSIC)
                        Toast.makeText(requireContext(), "Course lobby music disabled", Toast.LENGTH_SHORT).show()
                    }
                } else if (isChecked) {
                    // If trying to enable but not unlocked, revert the switch
                    musicLobbySwitch.isChecked = false
                    Toast.makeText(requireContext(), "Music lobby reward not unlocked", Toast.LENGTH_SHORT).show()
                }
            }
        }
        musicLobbySwitch.isEnabled = false

        binding.root.findViewById<ImageView>(R.id.cameraOverlay).setOnClickListener {
            showImageSourceDialog()
        }

//        4 is the ID of the profile frames
        lifecycleScope.launch {
            val canChangeProfileMask = withContext(Dispatchers.IO) {
                rewardChecker.isRewardUnlocked(4)
            }
            val profileMaskSelector = binding.profileMaskSelector
            if (canChangeProfileMask) {
                ArrayAdapter.createFromResource(
                    requireContext(),
                    R.array.mask_types,
                    android.R.layout.simple_spinner_item
                ).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    profileMaskSelector.adapter = adapter
                }

                profileMaskSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    var isSelected = false

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (!isSelected) {
                            isSelected = true

                            return
                        }
                        val selectedItem = parent?.getItemAtPosition(position).toString()
                        Log.d("ProfileFragment", "Selected: $selectedItem")
                        val user = authRepository.getCurrentUserSync()!!
                        lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                supabaseClient.updateUserFriendMask(user.id, selectedItem, user.authToken)
                            }
                            result.onFailure { result ->
                                Log.d("ProfileFragment", result.message.toString(), result)
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        return
                    }
                }
            } else {
                profileMaskSelector.isEnabled = false
            }
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check authentication using AuthRepository instead of UserManager
        if (authRepository.isLoggedIn()) {
            val currentUser = authRepository.getCurrentUserSync()
            if (currentUser != null) {
                Log.d("ProfileFragment", "user.id=${currentUser.id}, authenticated")
                viewModel.loadProfile()
            } else {
                Log.e("ProfileFragment", "User is logged in but current user is null")
                viewModel.loadProfile() // Try loading anyway, repository will handle authentication
            }
        } else {
            Log.e("ProfileFragment", "User is not logged in")
            // Redirect to login or show login prompt
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
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
        activity?.findViewById<ViewPager2>(R.id.viewPager)?.visibility = View.VISIBLE
        activity?.findViewById<FrameLayout>(R.id.fragment_container)?.visibility = View.GONE
    }
    
    override fun onResume() {
        super.onResume()
        gyroSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        // Refresh achievements in case user completed tasks
        refreshAchievements()
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
            val clampedRoll = roll.coerceIn(-maxTilt, maxTilt)
            val clampedPitch = pitch.coerceIn(-maxTilt, maxTilt)
            val shiftX = -clampedRoll / maxTilt * maxShift
            val shiftY = clampedPitch / maxTilt * maxShift

            // Low-pass filter for smoothing
            lastShiftX += (shiftX - lastShiftX) * smoothing
            lastShiftY += (shiftY - lastShiftY) * smoothing
            lastPitch += (clampedPitch - lastPitch) * smoothing
            lastRoll += (clampedRoll - lastRoll) * smoothing

            val bgView = view?.findViewById<View>(R.id.holoCardBg)
            val cardView = profileCard
            if (bgView != null && cardView != null) {
                val maxX = ((bgView.width - cardView.width) / 2).toFloat().coerceAtLeast(0f)
                val maxY = ((bgView.height - cardView.height) / 2).toFloat().coerceAtLeast(0f)
                // Animate background
                bgView.animate()
                    .translationX(lastShiftX.coerceIn(-maxX, maxX))
                    .translationY(lastShiftY.coerceIn(-maxY, maxY))
                    .rotationX(lastPitch)
                    .rotationY(-lastRoll)
                    .setDuration(80)
                    .start()
                // Animate card for subtle tilt
                cardView.animate()
                    .rotationX(lastPitch * cardTiltFactor)
                    .rotationY(-lastRoll * cardTiltFactor)
                    .setDuration(80)
                    .start()
                // Debug log
                Log.d("HoloGyro", "shiftX=$lastShiftX shiftY=$lastShiftY rotX=$lastPitch rotY=$lastRoll maxX=$maxX maxY=$maxY")
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
                                    val imageBytes = Base64.decode(profilePic, Base64.DEFAULT)
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
                        val displayBio = if (bio.isNullOrEmpty() || bio == "null") "No bio set yet" else bio
                        profileCardBio.text = displayBio
                        profileCardUsername.text = state.profile.displayName
                        
                        android.util.Log.d("ProfileFragment", "Profile UI updated: displayName='${state.profile.displayName}', bio='$displayBio'")
                        
                        // Add click listener to show full bio in toast
                        profileCardBio.setOnClickListener {
                            if (!bio.isNullOrEmpty() && bio != "null" && bio != "No bio set yet") {
                                Toast.makeText(requireContext(), bio, Toast.LENGTH_LONG).show()
                            }
                        }

                        // Check unlocked rewards and update toggle
                        val unlockedRewardIds = state.profile.unlockedRewardIds ?: emptyList()
                        updateRewardSettingsSection(unlockedRewardIds)

                        // Set level and XP bar
                        val level = state.profile.level
                        val xp = state.profile.experience
                        levelBadge.text = "Lv. $level"
                        // Calculate XP needed for next level using centralized calculator
                        val (currentLevelProgress, currentLevelMax) = LevelCalculator.calculateLevelProgress(xp.toLong())
                        xpCircularProgress.max = currentLevelMax
                        xpCircularProgress.progress = currentLevelProgress
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
        
        // Load real achievements from repository
        loadAchievements()
    }
    
    private fun loadAchievements() {
        lifecycleScope.launch {
            try {
                val currentUser = authRepository.getCurrentUserSync()
                if (currentUser != null) {
                    // Get all achievements and user's unlocked ones
                    val allAchievements = achievementRepository.getAllAchievements()
                    val unlockedResult = achievementRepository.getUserUnlockedAchievements(currentUser.id.toString())
                    val unlockedIds = unlockedResult.getOrElse { emptyList() }.toSet()
                    
                    // Filter to only show unlocked achievements
                    val unlockedAchievements = allAchievements.filter { achievement ->
                        unlockedIds.contains(achievement.id.toInt())
                    }.map { achievement ->
                        achievement.copy(unlocked = true)
                    }
                    
                    achievementAdapter.submitList(unlockedAchievements)
                    updateAchievementsVisibility(unlockedAchievements.isNotEmpty())
                    Log.d("ProfileFragment", "Loaded ${unlockedAchievements.size} unlocked achievements")
                } else {
                    // Show empty list if no user
                    achievementAdapter.submitList(emptyList())
                    updateAchievementsVisibility(false)
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error loading achievements", e)
                // Fallback: show empty list
                achievementAdapter.submitList(emptyList())
                updateAchievementsVisibility(false)
            }
        }
    }
    
    fun refreshAchievements() {
        loadAchievements()
    }
    
    private fun updateAchievementsVisibility(hasAchievements: Boolean) {
        val emptyState = binding.root.findViewById<View>(R.id.achievementsEmptyState)
        val recyclerView = binding.achievementsRecyclerView
        
        if (hasAchievements) {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        } else {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
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
                if (newBio.length > MAX_BIO_LENGTH) {
                    Log.d("ProfileFragment", "Bio too long")
                    Toast.makeText(context, "Bio cannot be longer than $MAX_BIO_LENGTH characters", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                if (newName.length > MAX_NAME_LENGTH) {
                    Log.d("ProfileFragment", "Name too long")
                    Toast.makeText(context, "Name cannot be longer than $MAX_NAME_LENGTH characters", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                Log.d("ProfileFragment", "Updated displayname/ bio")
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
                    val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                    
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
        
        // Navigate to LoginActivity and clear the entire task stack
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
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
        biometricSwitch.isChecked = settingsRepository.hasValue("biometric_enabled")
        achievementNotificationsSwitch.isChecked = settingsRepository.hasValue("achievement_notifications")
        progressNotificationsSwitch.isChecked = settingsRepository.hasValue("progress_notifications")
        friendActivitySwitch.isChecked = settingsRepository.hasValue("friend_activity")

        // Set up switch listeners
        biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                settingsRepository.toggleValue(SettingsConstants.BIOMETRICS)
            }
        }

        achievementNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                settingsRepository.toggleValue(SettingsConstants.ACHIEVEMENTS_NOTIFICATIONS)
            }
        }

        progressNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                settingsRepository.toggleValue(SettingsConstants.PROGRESS_NOTIFICATIONS)
            }
        }

        friendActivitySwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                settingsRepository.toggleValue(SettingsConstants.FRIENDS_ACTIVITY)
            }
        }

        // Set up button listeners
        exportDataButton.setOnClickListener {
            Toast.makeText(requireContext(), "Data export functionality coming soon!", Toast.LENGTH_SHORT).show()
        }

        deleteAccountButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
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