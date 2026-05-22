package com.diabeticcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hba1c_records")
data class HbA1cRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val value: Float,          // e.g. 7.2
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val status: HbA1cStatus get() = when {
        value < 5.7f -> HbA1cStatus.NORMAL
        value < 6.5f -> HbA1cStatus.PREDIABETES
        value < 8.0f -> HbA1cStatus.CONTROLLED
        else -> HbA1cStatus.UNCONTROLLED
    }
}

enum class HbA1cStatus { NORMAL, PREDIABETES, CONTROLLED, UNCONTROLLED }
