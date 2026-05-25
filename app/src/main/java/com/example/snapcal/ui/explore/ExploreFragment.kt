package com.example.snapcal.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snapcal.data.repository.MealRepository
import com.example.snapcal.databinding.FragmentExploreBinding

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels {
        ExploreViewModelFactory(MealRepository(requireContext()))
    }

    private lateinit val mealAdapter: MealAdapter
    private lateinit val nutritionAdapter: NutritionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupSearchView()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        mealAdapter = MealAdapter()
        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mealAdapter
        }

        nutritionAdapter = NutritionAdapter()
        binding.rvNutrition.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = nutritionAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchNutrition(it) }
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
        viewModel.feedMeals.observe(viewLifecycleOwner) { meals ->
            mealAdapter.submitList(meals)
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            nutritionAdapter.submitList(results)
            if (results.isNotEmpty() || binding.searchView.query.isNotBlank()) {
                binding.rvFeed.visibility = View.GONE
                binding.rvNutrition.visibility = View.VISIBLE
            } else {
                binding.rvFeed.visibility = View.VISIBLE
                binding.rvNutrition.visibility = View.GONE
            }
        }

        viewModel.isSearching.observe(viewLifecycleOwner) { isSearching ->
            binding.progressBar.visibility = if (isSearching) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
