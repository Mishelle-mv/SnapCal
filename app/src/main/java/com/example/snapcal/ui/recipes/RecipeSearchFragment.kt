package com.example.snapcal.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snapcal.databinding.FragmentRecipeSearchBinding

class RecipeSearchFragment : Fragment() {

    private var _binding: FragmentRecipeSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeSearchViewModel by viewModels()
    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter()
        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recipeAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchRecipes(it) }
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    viewModel.clearSearch()
                }
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewModel.recipes.observe(viewLifecycleOwner) { recipes ->
            recipeAdapter.submitList(recipes)
            if (recipes.isEmpty() && !binding.searchView.query.isNullOrBlank() && viewModel.isLoading.value == false) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.tvEmptyState.text = getString(com.example.snapcal.R.string.no_recipes_found, binding.searchView.query)
                binding.rvRecipes.visibility = View.GONE
            } else if (recipes.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.tvEmptyState.text = getString(com.example.snapcal.R.string.search_recipes_empty)
                binding.rvRecipes.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.rvRecipes.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isSearching ->
            binding.progressBar.visibility = if (isSearching) View.VISIBLE else View.GONE
            if (isSearching) {
                binding.emptyStateLayout.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.tvEmptyState.text = errorMsg
                binding.rvRecipes.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
