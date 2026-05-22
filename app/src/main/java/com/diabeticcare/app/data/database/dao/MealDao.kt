package com.diabeticcare.app.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.diabeticcare.app.data.model.MealLog

@Dao
interface MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealLog): Long

    @Delete
    suspend fun delete(meal: MealLog)

    @Query("SELECT * FROM meal_logs ORDER BY timestamp DESC")
    fun getAllMeals(): LiveData<List<MealLog>>

    @Query("SELECT * FROM meal_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMeal(): MealLog?

    @Query("SELECT * FROM meal_logs WHERE timestamp >= :fromTime ORDER BY timestamp DESC")
    suspend fun getMealsSince(fromTime: Long): List<MealLog>

    @Query("SELECT COUNT(*) FROM meal_logs WHERE timestamp >= :fromTime")
    suspend fun getMealCountSince(fromTime: Long): Int
}
