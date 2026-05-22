package com.diabeticcare.app.ui.nutrition

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diabeticcare.app.data.model.MealLog
import com.diabeticcare.app.databinding.ItemMealBinding
import java.text.SimpleDateFormat
import java.util.*

class MealAdapter(private val onDelete: (MealLog) -> Unit) :
    ListAdapter<MealLog, MealAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemMealBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(meal: MealLog) {
            b.tvMealType.text = meal.mealType.lowercase().replaceFirstChar { it.uppercase() }
            b.tvMealEmoji.text = when (meal.mealType) {
                "BREAKFAST" -> "🌅"
                "LUNCH" -> "☀️"
                "DINNER" -> "🌙"
                "SNACK" -> "🥨"
                else -> "🍽️"
            }
            val category = meal.foodCategory.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() }
            b.tvMealDetail.text = "${meal.portionSize.lowercase().replaceFirstChar { it.uppercase() }} portion · $category"
            if (meal.notes.isNotEmpty()) b.tvMealDetail.append(" · ${meal.notes}")
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            b.tvMealTime.text = sdf.format(Date(meal.timestamp))
            b.btnDeleteMeal.setOnClickListener { onDelete(meal) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMealBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MealLog>() {
            override fun areItemsTheSame(a: MealLog, b: MealLog) = a.id == b.id
            override fun areContentsTheSame(a: MealLog, b: MealLog) = a == b
        }
    }
}
