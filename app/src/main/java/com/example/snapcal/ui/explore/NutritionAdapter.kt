package com.example.snapcal.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.snapcal.data.model.NutritionProduct
import com.example.snapcal.databinding.ItemNutritionBinding

class NutritionAdapter : ListAdapter<NutritionProduct, NutritionAdapter.NutritionViewHolder>(NutritionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NutritionViewHolder {
        val binding = ItemNutritionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NutritionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NutritionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NutritionViewHolder(private val binding: ItemNutritionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: NutritionProduct) {
            binding.tvProductName.text = product.productName ?: "Unknown Product"
            binding.tvBrand.text = product.brands ?: "Unknown Brand"
            
            val calories = product.nutriments?.energyKcal100g?.toInt() ?: 0
            binding.tvCalories.text = "$calories kcal / 100g"

            if (!product.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root)
                    .load(product.imageUrl)
                    .into(binding.ivProduct)
            } else {
                binding.ivProduct.setImageDrawable(null)
            }
        }
    }
}

class NutritionDiffCallback : DiffUtil.ItemCallback<NutritionProduct>() {
    override fun areItemsTheSame(oldItem: NutritionProduct, newItem: NutritionProduct): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: NutritionProduct, newItem: NutritionProduct): Boolean = oldItem == newItem
}
