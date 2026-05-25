package com.example.snapcal.ui.mymeals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.snapcal.R
import com.example.snapcal.data.model.Meal
import com.squareup.picasso.Picasso

class MyMealAdapter : ListAdapter<Meal, MyMealAdapter.MealViewHolder>(MealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageMeal: ImageView = itemView.findViewById(R.id.imageMeal)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)

        fun bind(meal: Meal) {
            tvDescription.text = meal.description
            tvCalories.text = itemView.context.getString(R.string.calories_value, meal.calories)
            if (meal.imageUrl.isNotBlank()) {
                Picasso.get().load(meal.imageUrl).into(imageMeal)
            } else {
                imageMeal.setImageDrawable(null)
            }
        }
    }

    private class MealDiffCallback : DiffUtil.ItemCallback<Meal>() {
        override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean {
            return oldItem == newItem
        }
    }
}
