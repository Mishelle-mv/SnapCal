package com.example.snapcal.ui.mealform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.snapcal.R
import com.example.snapcal.util.Resource
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso

class MealFormFragment : Fragment() {

    private val args: MealFormFragmentArgs by navArgs()
    private val viewModel: MealFormViewModel by viewModels()

    private lateinit var imagePreview: ImageView
    private lateinit var btnSelectPhoto: MaterialButton
    private lateinit var etDescription: TextInputEditText
    private lateinit var etCalories: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setSelectedImageUri(uri)
            imagePreview.setImageURI(uri)
            hideError()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_meal_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.resetSaveState()
        viewModel.init(args.mode, args.mealId)

        imagePreview = view.findViewById(R.id.imagePreview)
        btnSelectPhoto = view.findViewById(R.id.btnSelectPhoto)
        etDescription = view.findViewById(R.id.etDescription)
        etCalories = view.findViewById(R.id.etCalories)
        btnSave = view.findViewById(R.id.btnSave)
        btnDelete = view.findViewById(R.id.btnDelete)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)

        val isEditMode = args.mode == MealFormViewModel.MODE_EDIT
        btnDelete.visibility = if (isEditMode) View.VISIBLE else View.GONE

        btnSelectPhoto.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        btnSave.setOnClickListener {
            hideError()
            viewModel.saveMeal(
                description = etDescription.text?.toString().orEmpty(),
                caloriesText = etCalories.text?.toString().orEmpty()
            )
        }

        btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_meal_title)
                .setMessage(R.string.delete_meal_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    hideError()
                    viewModel.deleteMeal()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        viewModel.selectedImageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                imagePreview.setImageURI(uri)
            }
        }

        viewModel.loadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    val meal = state.data
                    etDescription.setText(meal.description)
                    etCalories.setText(meal.calories.toString())
                    if (meal.imageUrl.isNotBlank()) {
                        Picasso.get().load(meal.imageUrl).into(imagePreview)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
                null -> showLoading(false)
            }
        }

        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    findNavController().navigateUp()
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
                null -> {
                    if (viewModel.loadState.value !is Resource.Loading) {
                        showLoading(false)
                    }
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !loading
        btnDelete.isEnabled = !loading
        btnSelectPhoto.isEnabled = !loading
        etDescription.isEnabled = !loading
        etCalories.isEnabled = !loading
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}
