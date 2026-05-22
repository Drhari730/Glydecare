package com.diabeticcare.app.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.diabeticcare.app.data.model.UserProfile

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UserProfile)

    @Update
    suspend fun update(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): LiveData<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileSnapshot(): UserProfile?
}
