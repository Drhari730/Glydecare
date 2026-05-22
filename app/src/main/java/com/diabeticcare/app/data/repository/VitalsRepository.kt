package com.diabeticcare.app.data.repository

import com.diabeticcare.app.data.database.dao.VitalsDao
import com.diabeticcare.app.data.model.VitalsRecord

class VitalsRepository(private val dao: VitalsDao) {

    val allRecords = dao.getAllRecords()

    suspend fun insert(record: VitalsRecord) = dao.insert(record)
    suspend fun getLatest() = dao.getLatestRecord()
}
