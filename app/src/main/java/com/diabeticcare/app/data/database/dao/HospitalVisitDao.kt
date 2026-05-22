package com.diabeticcare.app.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.diabeticcare.app.data.model.HospitalVisit

@Dao
interface HospitalVisitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: HospitalVisit): Long

    @Update
    suspend fun update(visit: HospitalVisit)

    @Delete
    suspend fun delete(visit: HospitalVisit)

    @Query("SELECT * FROM hospital_visits ORDER BY scheduledDate ASC")
    fun getAllVisits(): LiveData<List<HospitalVisit>>

    @Query("SELECT * FROM hospital_visits WHERE completed = 0 ORDER BY scheduledDate ASC LIMIT 1")
    suspend fun getNextUpcoming(): HospitalVisit?

    @Query("SELECT * FROM hospital_visits WHERE completed = 0 ORDER BY scheduledDate ASC")
    fun getUpcomingVisits(): LiveData<List<HospitalVisit>>

    @Query("SELECT * FROM hospital_visits WHERE completed = 0 ORDER BY scheduledDate ASC LIMIT 1")
    fun getNextUpcomingLive(): LiveData<HospitalVisit?>
}
