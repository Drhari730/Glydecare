package com.diabeticcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glucose_readings")
data class GlucoseReading(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val value: Int,
    val readingType: String,   // FASTING, POST_MEAL, BEDTIME, RANDOM
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val status: GlucoseStatus get() = when {
        value < 70 -> GlucoseStatus.LOW
        value <= 140 -> GlucoseStatus.NORMAL
        value <= 200 -> GlucoseStatus.HIGH
        else -> GlucoseStatus.VERY_HIGH
    }
}

enum class GlucoseStatus { LOW, NORMAL, HIGH, VERY_HIGH }

enum class ReadingType { FASTING, POST_MEAL, BEDTIME, RANDOM }
