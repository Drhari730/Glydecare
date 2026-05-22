package com.diabeticcare.app.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.diabeticcare.app.data.model.VitalsRecord

@Dao
interface VitalsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: VitalsRecord): Long

    @Query("SELECT * FROM vitals_records ORDER BY timestamp DESC")
    fun getAllRecords(): LiveData<List<VitalsRecord>>

    @Query("SELECT * FROM vitals_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecord(): VitalsRecord?

    @Query("SELECT * FROM vitals_records WHERE timestamp >= :fromTime ORDER BY timestamp ASC")
    suspend fun getRecordsSince(fromTime: Long): List<VitalsRecord>
}
