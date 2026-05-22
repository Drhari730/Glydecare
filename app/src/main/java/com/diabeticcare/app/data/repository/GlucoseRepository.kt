package com.diabeticcare.app.data.repository

import com.diabeticcare.app.data.database.dao.GlucoseDao
import com.diabeticcare.app.data.model.GlucoseReading
import java.util.concurrent.TimeUnit

class GlucoseRepository(private val dao: GlucoseDao) {

    val allReadings = dao.getAllReadings()
    val latestLive = dao.getLatestReadingLive()

    fun readingsSince(days: Int) = dao.getReadingsSince(
        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
    )

    suspend fun insert(reading: GlucoseReading) = dao.insert(reading)

    suspend fun delete(reading: GlucoseReading) = dao.delete(reading)

    suspend fun getLatest() = dao.getLatestReading()

    suspend fun averageLast(days: Int) = dao.getAverageValueSince(
        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
    )

    suspend fun outOfRangeCountLast(days: Int) = dao.getOutOfRangeCountSince(
        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
    )

    suspend fun snapshotLast(days: Int) = dao.getReadingsSinceSnapshot(
        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
    )
}
