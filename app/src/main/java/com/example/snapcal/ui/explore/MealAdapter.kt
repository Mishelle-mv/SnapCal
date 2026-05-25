package com.example.snapcal.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.snapcal.data.model.Meal
import com.example.snapcal.databinding.ItemMealBinding

class MealAdapter : ListAdapter<Meal, MealAdapter.MealViewHolder>(MealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MealViewHolder(private val binding: ItemMealBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(meal: Meal) {
            binding.tvUserName.text = meal.userDisplayName
            binding.tvDescription.text = meal.description
            binding.tvCalories.text = "${meal.calories} kcal"

            // Since we don't have user avatars yet, leave blank or load placeholder
            
            Glide.with(binding.root)
                .load(meal.imageUrl)
                .into(binding.ivMealImage)
        }
    }
}

class MealDiffCallback : DiffUtil.ItemCallback<Meal>() {
    override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean = oldItem == newItem
}
