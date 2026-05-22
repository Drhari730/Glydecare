package com.diabeticcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dose: String,
    val frequency: String,     // ONCE_DAILY, TWICE_DAILY, THREE_TIMES, WITH_MEALS
    val reminderTimes: String, // comma-separated HH:mm strings e.g. "08:00,14:00,20:00"
    val medicationType: String = "Tablet",
    val startDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val medicationName: String,
    val status: String,   // TAKEN, MISSED
    val scheduledTime: String,
    val timestamp: Long = System.currentTimeMillis()
)
