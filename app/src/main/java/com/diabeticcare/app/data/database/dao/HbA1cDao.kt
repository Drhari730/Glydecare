package com.diabeticcare.app.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.diabeticcare.app.data.model.HbA1cRecord

@Dao
interface HbA1cDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HbA1cRecord): Long

    @Delete
    suspend fun delete(record: HbA1cRecord)

    @Query("SELECT * FROM hba1c_records ORDER BY timestamp DESC")
    fun getAllRecords(): LiveData<List<HbA1cRecord>>

    @Query("SELECT * FROM hba1c_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): HbA1cRecord?

    @Query("SELECT * FROM hba1c_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLive(): LiveData<HbA1cRecord?>
}
