package com.nhlstenden.appdev.profile.ui.screens

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
import com.nhlstenden.appdev.login.ui.screens.LoginActivity
import com.nhlstenden.appdev.profile.ui.adapters.AchievementAdapter
import com.nhlstenden.appdev.profile.ui.viewmodels.ProfileViewModel
import com.nhlstenden.appdev.profile.ui.viewmodels.ProfileViewModel.ProfileState
import com.nhlstenden.appdev.shared.components.ImageCropActivity
import com.nhlstenden.appdev.shared.components.UserManager
import com.nhlstenden.appdev.shared.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import android.app.AlertDialog
import android.widget.EditText
import com.nhlstenden.appdev.profile.data.repositories.ProfileRepositoryImpl
import javax.inject.Inject
import com.yalantis.ucrop.UCrop
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var achievementAdapter: AchievementAdapter
    
    @Inject
    lateinit var profileRepository: ProfileRepositoryImpl
    
    private val PROFILE_IMAGE_SIZE = 120
    
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
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get user data from arguments or UserManager
        val userData = arguments?.getParcelable<com.nhlstenden.appdev.supabase.User>("USER_DATA") 
            ?: UserManager.getCurrentUser()
        
        userData?.let { user ->
            profileRepository.setUserData(user)
        }
        
        setupViews()
        observeProfileState()
        viewModel.loadProfile()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

                        val bio = state.profile.bio
                        binding.bioTextView.text = if (bio.isNullOrEmpty() || bio == "null") "No bio set yet" else bio
                        binding.usernameTextView.text = state.profile.displayName
                        binding.emailTextView.text = state.profile.email
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
} 