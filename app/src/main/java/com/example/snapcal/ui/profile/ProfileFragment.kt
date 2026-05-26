package com.example.snapcal.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.snapcal.R
import com.example.snapcal.data.repository.AuthRepository
import com.example.snapcal.data.repository.UserRepository
import com.example.snapcal.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(AuthRepository(), UserRepository(requireContext()))
    }

    private var selectedImageUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivProfilePhoto.imageTintList = null // remove tint for actual image
            Glide.with(this).load(uri).into(binding.ivProfilePhoto)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabEditPhoto.setOnClickListener {
            pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSave.setOnClickListener {
            val displayName = binding.etDisplayName.text.toString()
            if (displayName.isNotBlank()) {
                viewModel.updateProfile(displayName, selectedImageUri)
            } else {
                Toast.makeText(requireContext(), "Display name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            if (profile != null) {
                if (binding.etDisplayName.text.isNullOrBlank()) {
                    binding.etDisplayName.setText(profile.displayName)
                }
                if (profile.photoUrl.isNotBlank() && selectedImageUri == null) {
                    binding.ivProfilePhoto.imageTintList = null
                    Glide.with(this).load(profile.photoUrl).into(binding.ivProfilePhoto)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) "" else "Save Changes"
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.logoutSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                // Navigate to auth graph and clear backstack
                findNavController().navigate(R.id.action_global_authFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
