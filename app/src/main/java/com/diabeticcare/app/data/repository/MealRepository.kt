package com.diabeticcare.app.data.repository

import com.diabeticcare.app.data.database.dao.MealDao
import com.diabeticcare.app.data.model.MealLog
import java.util.concurrent.TimeUnit

class MealRepository(private val dao: MealDao) {

    val allMeals = dao.getAllMeals()

    suspend fun insert(meal: MealLog) = dao.insert(meal)
    suspend fun delete(meal: MealLog) = dao.delete(meal)
    suspend fun getLatest() = dao.getLatestMeal()

    suspend fun mealCountLast(days: Int) = dao.getMealCountSince(
        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
    )

    suspend fun snapshotLast(days: Int) = dao.getMealsSince(
        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
    )
}
