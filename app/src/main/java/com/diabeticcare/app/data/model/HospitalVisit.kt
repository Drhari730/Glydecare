package com.diabeticcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hospital_visits")
data class HospitalVisit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val visitType: String,        // CLINIC, LAB_TEST, EYE_CHECK, FOOT_CHECK, OTHER
    val doctorName: String = "",
    val hospitalName: String = "",
    val scheduledDate: Long,      // epoch ms
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
