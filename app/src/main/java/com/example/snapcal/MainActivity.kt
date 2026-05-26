package com.example.snapcal

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        val topLevelDestinations = setOf(
            R.id.exploreFragment,
            R.id.myMealsNavGraph,
            R.id.myMealsFragment,
            R.id.profileFragment,
            R.id.recipeSearchFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.visibility = if (destination.id in topLevelDestinations) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }
}
