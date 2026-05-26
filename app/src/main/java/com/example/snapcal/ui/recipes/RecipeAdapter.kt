package com.example.snapcal.ui.recipes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.snapcal.data.model.Recipe
import com.example.snapcal.databinding.ItemRecipeBinding

class RecipeAdapter(private val onItemClick: (Recipe) -> Unit) : ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecipeViewHolder(private val binding: ItemRecipeBinding, private val onItemClick: (Recipe) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            binding.root.setOnClickListener { onItemClick(recipe) }
            binding.tvRecipeTitle.text = recipe.title
            binding.tvRecipeCategory.text = buildString {
                if (!recipe.category.isNullOrBlank()) append(recipe.category)
                if (!recipe.category.isNullOrBlank() && !recipe.area.isNullOrBlank()) append(" | ")
                if (!recipe.area.isNullOrBlank()) append(recipe.area)
            }
            binding.tvRecipeInstructions.text = recipe.instructions

            if (!recipe.imageUrl.isNullOrBlank()) {
                Glide.with(binding.root)
                    .load(recipe.imageUrl)
                    .into(binding.ivRecipeImage)
            } else {
                binding.ivRecipeImage.setImageDrawable(null)
            }
        }
    }
}

class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
    override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean = oldItem.idMeal == newItem.idMeal
    override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean = oldItem == newItem
}
