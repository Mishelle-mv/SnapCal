package com.example.snapcal.ui.mymeals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.snapcal.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MyMealsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_meals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<FloatingActionButton>(R.id.fabMealForm).setOnClickListener {
            findNavController().navigate(R.id.action_myMealsFragment_to_mealFormFragment)
        }
    }
}
