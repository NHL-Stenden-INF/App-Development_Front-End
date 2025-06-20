package com.nhlstenden.appdev.friends.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhlstenden.appdev.databinding.FragmentFriendsBinding
import com.nhlstenden.appdev.friends.ui.QRScannerActivity
import com.nhlstenden.appdev.features.friends.adapters.FriendAdapter
import com.nhlstenden.appdev.features.friends.viewmodels.FriendsViewModel
import com.nhlstenden.appdev.features.friends.dialogs.FriendDetailsDialog
import com.nhlstenden.appdev.features.friends.models.Friend
import com.nhlstenden.appdev.core.ui.base.BaseFragment
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FriendsFragment : BaseFragment() {
    private val viewModel: FriendsViewModel by viewModels()
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FriendAdapter
    
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scannedUuid = result.data?.getStringExtra("SCANNED_UUID")
            scannedUuid?.let { uuid ->
                viewModel.addFriend(uuid)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
        
        viewModel.loadFriends()
        viewModel.generateQRCode()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun setupUI() {
        binding.friendsList.layoutManager = LinearLayoutManager(context)
        adapter = FriendAdapter { friend ->
            // Show friend details dialog when friend is clicked
            showFriendDetailsDialog(friend)
        }
        binding.friendsList.adapter = adapter
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadFriends()
        }
        
        binding.scanButton.setOnClickListener {
            val intent = Intent(requireContext(), QRScannerActivity::class.java)
            qrScannerLauncher.launch(intent)
        }
    }
    
    private fun shareQRCode(bitmap: Bitmap) {
        try {
            // Create a temporary file to store the QR code
            val imagesFolder = File(requireContext().cacheDir, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "qr_code.png")
            
            // Save the bitmap to the file
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // Get the URI for the file using FileProvider
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            
            // Create and start the share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                requireContext(),
                "Failed to share QR code: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.friends.collect { friends ->
                    adapter.submitList(friends)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.qrCode.collect { qrCode ->
                    qrCode?.let { bitmap ->
                        binding.qrCodeImage.setImageBitmap(bitmap)
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.swipeRefreshLayout.isRefreshing = isLoading
                    showLoading(isLoading)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { error ->
                    error?.let { showError(it) }
                }
            }
        }
    }
    
    fun fetchFriendsNow() {
        viewModel.loadFriends()
    }
    
    private fun showLoading(isLoading: Boolean) {
        // Optionally show/hide a loading indicator. Here, just use swipeRefreshLayout.
        binding.swipeRefreshLayout.isRefreshing = isLoading
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showFriendDetailsDialog(friend: Friend) {
        val dialog = FriendDetailsDialog.newInstance(friend)
        dialog.show(parentFragmentManager, "FriendDetailsDialog")
    }
} 