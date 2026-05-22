package com.diabeticcare.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vitals_records")
data class VitalsRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val heartRate: Int? = null,
    val spo2: Float? = null,
    val respiratoryRate: Int? = null,
    val source: String = "MANUAL",   // MANUAL, CAMERA_PPG, or OJAS_FFT_PPG
    val timestamp: Long = System.currentTimeMillis()
) {
    val bpStatus: BPStatus? get() {
        val s = systolic ?: return null
        val d = diastolic ?: return null
        return when {
            s < 120 && d < 80 -> BPStatus.NORMAL
            s < 130 && d < 80 -> BPStatus.ELEVATED
            s < 140 || d < 90 -> BPStatus.HIGH_STAGE1
            else -> BPStatus.HIGH_STAGE2
        }
    }
}

enum class BPStatus { NORMAL, ELEVATED, HIGH_STAGE1, HIGH_STAGE2 }
