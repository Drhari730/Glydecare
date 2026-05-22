package com.diabeticcare.app.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.diabeticcare.app.data.model.GlucoseReading

@Dao
interface GlucoseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: GlucoseReading): Long

    @Delete
    suspend fun delete(reading: GlucoseReading)

    @Query("SELECT * FROM glucose_readings ORDER BY timestamp DESC")
    fun getAllReadings(): LiveData<List<GlucoseReading>>

    @Query("SELECT * FROM glucose_readings ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestReading(): GlucoseReading?

    @Query("SELECT * FROM glucose_readings WHERE timestamp >= :fromTime ORDER BY timestamp ASC")
    fun getReadingsSince(fromTime: Long): LiveData<List<GlucoseReading>>

    @Query("SELECT AVG(value) FROM glucose_readings WHERE timestamp >= :fromTime")
    suspend fun getAverageValueSince(fromTime: Long): Float?

    @Query("SELECT COUNT(*) FROM glucose_readings WHERE timestamp >= :fromTime AND (value < 70 OR value > 180)")
    suspend fun getOutOfRangeCountSince(fromTime: Long): Int

    @Query("SELECT * FROM glucose_readings WHERE timestamp >= :fromTime ORDER BY timestamp DESC")
    suspend fun getReadingsSinceSnapshot(fromTime: Long): List<GlucoseReading>

    @Query("SELECT * FROM glucose_readings ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReadingLive(): LiveData<GlucoseReading?>
}
