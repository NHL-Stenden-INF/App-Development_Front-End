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
import com.nhlstenden.appdev.shared.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var achievementAdapter: AchievementAdapter
    
    private val PROFILE_IMAGE_SIZE = 120
    
    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            viewModel.updateProfilePicture(uri.toString())
        }
    }
    
    private val cropImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            viewModel.updateProfilePicture(uri.toString())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeProfileState()
        viewModel.loadProfile()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupViews() {
        binding.cardViewInputs.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), com.nhlstenden.appdev.R.color.cardBackground)
        )

        binding.changePhotoButton.setOnClickListener {
            viewModel.onChangePhotoClicked()
        }

        binding.editProfileButton.setOnClickListener {
            viewModel.onEditProfileClicked()
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
                        
                        // Update profile image
                        Glide.with(this@ProfileFragment)
                            .load(state.profile.profilePictureResId)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(binding.profileImageView)

                        // Update user info
                        binding.usernameTextView.text = state.profile.username
                        binding.emailTextView.text = state.profile.email
                    }
                    is ProfileState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.cardViewInputs.visibility = View.VISIBLE
                        binding.swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    is ProfileState.LoggedOut -> {
                        // Handle logout state
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

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImage.launch(intent)
    }

    private fun cropImage(uri: Uri) {
        val intent = Intent("com.android.camera.action.CROP")
        intent.setDataAndType(uri, "image/*")
        intent.putExtra("crop", "true")
        intent.putExtra("aspectX", 1)
        intent.putExtra("aspectY", 1)
        intent.putExtra("outputX", 200)
        intent.putExtra("outputY", 200)
        intent.putExtra("return-data", true)
        cropImage.launch(intent)
    }

    private fun onEditProfileClicked() {
        // Handle edit profile click
    }

    private fun onLogoutClicked() {
        viewModel.logout()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }
} 