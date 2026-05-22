package com.diabeticcare.app.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.diabeticcare.app.data.model.Medication
import com.diabeticcare.app.data.model.MedicationLog

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveMedications(): LiveData<List<Medication>>

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getActiveMedicationsSnapshot(): List<Medication>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Int): Medication?

    @Query("SELECT COUNT(*) FROM medications WHERE isActive = 1 AND name = :name")
    suspend fun countActiveByName(name: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLog): Long

    @Query("SELECT * FROM medication_logs ORDER BY timestamp DESC")
    fun getAllLogs(): LiveData<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE timestamp >= :fromTime ORDER BY timestamp DESC")
    suspend fun getLogsSince(fromTime: Long): List<MedicationLog>

    @Query("SELECT COUNT(*) FROM medication_logs WHERE status = 'TAKEN' AND timestamp >= :fromTime")
    suspend fun getTakenCountSince(fromTime: Long): Int

    @Query("SELECT COUNT(*) FROM medication_logs WHERE timestamp >= :fromTime")
    suspend fun getTotalLogCountSince(fromTime: Long): Int
}
