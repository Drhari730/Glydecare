package com.diabeticcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_logs")
data class MealLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mealType: String,      // BREAKFAST, LUNCH, DINNER, SNACK
    val portionSize: String,   // SMALL, MEDIUM, LARGE
    val foodCategory: String,  // RICE_BASED, WHEAT_BASED, FRIED, FRUITS, VEGETABLES, MIXED, OTHER
    val notes: String = "",
    val aiSuggestion: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }
enum class PortionSize { SMALL, MEDIUM, LARGE }
enum class FoodCategory {
    RICE_BASED, WHEAT_BASED, FRIED, FRUITS, VEGETABLES, PULSES, DAIRY, MIXED, OTHER
}
