package com.diabeticcare.app.data.repository

import com.diabeticcare.app.data.database.dao.MedicationDao
import com.diabeticcare.app.data.model.Medication
import com.diabeticcare.app.data.model.MedicationLog
import java.util.concurrent.TimeUnit

class MedicationRepository(private val dao: MedicationDao) {

    val activeMedications = dao.getActiveMedications()
    val allLogs = dao.getAllLogs()

    suspend fun insertMedication(med: Medication) = dao.insertMedication(med)
    suspend fun updateMedication(med: Medication) = dao.updateMedication(med)
    suspend fun deleteMedication(med: Medication) = dao.deleteMedication(med)
    suspend fun getMedicationById(id: Int) = dao.getMedicationById(id)

    suspend fun logDose(log: MedicationLog) = dao.insertLog(log)

    suspend fun adherenceScoreLast(days: Int): Int {
        val from = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        val taken = dao.getTakenCountSince(from)
        val total = dao.getTotalLogCountSince(from)
        return if (total == 0) 100 else ((taken.toFloat() / total) * 100).toInt()
    }

    suspend fun takenCountToday(): Int {
        val startOfDay = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        return dao.getTakenCountSince(startOfDay)
    }

    suspend fun getActiveMedicationsSnapshot(): List<Medication> =
        dao.getActiveMedicationsSnapshot()
}
