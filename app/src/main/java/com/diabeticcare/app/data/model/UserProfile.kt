package com.diabeticcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_DOCTOR_ID = "MCH GM 001"

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val phone: String,
    val age: Int,
    val gender: String,
    val diabetesType: String,     // TYPE1, TYPE2, GESTATIONAL, PREDIABETES
    val diagnosisYear: Int,
    val doctorName: String = "",
    val doctorId: String = DEFAULT_DOCTOR_ID,
    val lowGlucoseThreshold: Int = 70,
    val highGlucoseThreshold: Int = 180,
    val createdAt: Long = System.currentTimeMillis()
)
