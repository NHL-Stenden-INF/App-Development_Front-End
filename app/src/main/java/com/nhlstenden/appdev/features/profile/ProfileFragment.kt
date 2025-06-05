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

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var achievementAdapter: AchievementAdapter
    private lateinit var musicLobbySwitch: SwitchMaterial
    private val MUSIC_LOBBY_REWARD_ID = 11
    private val PREFS_NAME = "reward_settings"
    private val MUSIC_LOBBY_KEY = "music_lobby_enabled"
    
    private val PROFILE_IMAGE_SIZE = 120
    
    private lateinit var levelTextView: TextView
    private lateinit var xpProgressBar: ProgressBar
    private lateinit var xpLabelTextView: TextView
    
    private fun setupViews() {
        binding.cardViewInputs.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), com.nhlstenden.appdev.R.color.cardBackground)
        )

        binding.changePhotoButton.setOnClickListener {
            showImageSourceDialog()
        }

        binding.editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
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
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get user data from arguments or UserManager
        val userData = arguments?.getParcelable<com.nhlstenden.appdev.core.models.User>("USER_DATA") 
            ?: UserManager.getCurrentUser()
        
        userData?.let { user ->
            android.util.Log.d("ProfileFragment", "user.id=${user.id}, user.authToken=${user.authToken}")
            viewModel.setUserData(user)
        }
        
        levelTextView = binding.root.findViewById(R.id.levelTextView)
        xpProgressBar = binding.root.findViewById(R.id.xpProgressBar)
        xpLabelTextView = binding.root.findViewById(R.id.xpLabelTextView)
        
        setupViews()
        observeProfileState()
        viewModel.loadProfile()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Ensure ViewPager is visible and fragment_container is hidden when leaving profile
        activity?.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)?.visibility = View.VISIBLE
        activity?.findViewById<android.widget.FrameLayout>(R.id.fragment_container)?.visibility = View.GONE
    }
    
    private fun observeProfileState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileState.collectLatest { state ->
                when (state) {
                    is ProfileState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.cardViewInputs.visibility = View.GONE
                        binding.swipeRefreshLayout.isRefreshing = true
                    }
                    is ProfileState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.cardViewInputs.visibility = View.VISIBLE
                        binding.swipeRefreshLayout.isRefreshing = false

                        // Display profile image (URL or base64)
                        val profilePic = state.profile.profilePicture
                        val invalidPics = listOf(null, "", "null")
                        if (profilePic !in invalidPics) {
                            if (profilePic!!.startsWith("http")) {
                                Glide.with(this@ProfileFragment)
                                    .load(profilePic)
                                    .placeholder(R.drawable.zorotlpf)
                                    .error(R.drawable.zorotlpf)
                                    .into(binding.profileImageView)
                            } else {
                                // Assume base64
                                val imageBytes = android.util.Base64.decode(profilePic, android.util.Base64.DEFAULT)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                binding.profileImageView.setImageBitmap(bitmap)
                            }
                        } else {
                            binding.profileImageView.setImageResource(R.drawable.zorotlpf)
                        }

                        // Notify HomeFragment of profile picture update
                        parentFragmentManager.setFragmentResult(
                            "profile_picture_updated",
                            android.os.Bundle().apply {
                                putString("profile_picture", profilePic)
                            }
                        )

                        val bio = state.profile.bio
                        binding.bioTextView.text = if (bio.isNullOrEmpty() || bio == "null") "No bio set yet" else bio
                        binding.usernameTextView.text = state.profile.displayName
                        binding.emailTextView.text = state.profile.email

                        // Check unlocked rewards and update toggle
                        val unlockedRewardIds = state.profile.unlockedRewardIds ?: emptyList()
                        updateRewardSettingsSection(unlockedRewardIds)

                        // Set level and XP bar
                        val level = state.profile.level
                        val xp = state.profile.experience
                        levelTextView.text = getString(R.string.level_format, level)
                        // Calculate XP needed for next level
                        var requiredXp = 100.0
                        var totalXp = 0.0
                        for (i in 1 until level) {
                            totalXp += requiredXp
                            requiredXp *= 1.1
                        }
                        val xpForCurrentLevel = xp - totalXp.toInt()
                        val xpForNextLevel = requiredXp.toInt()
                        xpProgressBar.max = xpForNextLevel
                        xpProgressBar.progress = xpForCurrentLevel.coerceAtLeast(0)
                        xpProgressBar.contentDescription = getString(R.string.experience_format, xpForCurrentLevel, xpForNextLevel)
                        xpLabelTextView.text = getString(R.string.experience_format, xpForCurrentLevel, xpForNextLevel)
                    }
                    is ProfileState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.cardViewInputs.visibility = View.VISIBLE
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
        usernameEdit.setText(binding.usernameTextView.text)
        bioEdit.setText(if (binding.bioTextView.text.toString() == "No bio set yet") "" else binding.bioTextView.text)
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

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            startCrop(tempCameraUri!!)
        }
    }

    private var tempCameraUri: Uri? = null

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun openCamera() {
        val photoFile = File.createTempFile("profile_photo", ".jpg", requireContext().cacheDir)
        tempCameraUri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".provider",
            photoFile
        )
        takePicture.launch(tempCameraUri)
    }

    private val cropImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                viewModel.updateProfile(
                    binding.usernameTextView.text.toString(),
                    binding.bioTextView.text.toString(),
                    base64String
                )
            }
        }
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(512, 512)
        cropImage.launch(uCrop.getIntent(requireContext()))
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
} 