package com.example.snapcal.ui.mymeals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.snapcal.R
import com.example.snapcal.ui.mealform.MealFormViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MyMealsFragment : Fragment() {

    private val viewModel: MyMealsViewModel by viewModels()
    private lateinit var adapter: MyMealAdapter

    private lateinit var recyclerMyMeals: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabMealForm: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_meals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerMyMeals = view.findViewById(R.id.recyclerMyMeals)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        progressBar = view.findViewById(R.id.progressBar)
        fabMealForm = view.findViewById(R.id.fabMealForm)

        adapter = MyMealAdapter { meal ->
            val action = MyMealsFragmentDirections.actionMyMealsFragmentToMealFormFragment(
                mode = MealFormViewModel.MODE_EDIT,
                mealId = meal.id
            )
            findNavController().navigate(action)
        }

        recyclerMyMeals.layoutManager = LinearLayoutManager(requireContext())
        recyclerMyMeals.adapter = adapter

        fabMealForm.setOnClickListener {
            val action = MyMealsFragmentDirections.actionMyMealsFragmentToMealFormFragment(
                mode = MealFormViewModel.MODE_ADD,
                mealId = null
            )
            findNavController().navigate(action)
        }

        viewModel.meals.observe(viewLifecycleOwner) { meals ->
            adapter.submitList(meals)
        }

        viewModel.screenState.observe(viewLifecycleOwner) { state ->
            renderScreenState(state)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMeals()
    }

    private fun renderScreenState(state: MyMealsScreenState?) {
        when (state) {
            MyMealsScreenState.Loading -> {
                progressBar.visibility = View.VISIBLE
                recyclerMyMeals.visibility = View.GONE
                tvEmptyState.visibility = View.GONE
            }
            MyMealsScreenState.FirebaseNotConfigured -> {
                progressBar.visibility = View.GONE
                recyclerMyMeals.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.setText(R.string.my_meals_firebase_not_configured)
            }
            MyMealsScreenState.NotLoggedIn -> {
                progressBar.visibility = View.GONE
                recyclerMyMeals.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.setText(R.string.my_meals_not_logged_in)
            }
            is MyMealsScreenState.Ready -> {
                progressBar.visibility = View.GONE
                if (state.isEmpty) {
                    recyclerMyMeals.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.setText(R.string.my_meals_empty)
                } else {
                    recyclerMyMeals.visibility = View.VISIBLE
                    tvEmptyState.visibility = View.GONE
                }
            }
            is MyMealsScreenState.Error -> {
                progressBar.visibility = View.GONE
                recyclerMyMeals.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = state.message
            }
            null -> {
                progressBar.visibility = View.GONE
            }
        }
    }
}
