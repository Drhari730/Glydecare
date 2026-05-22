package com.diabeticcare.app.ui.vitals

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ojas-style rPPG processor adapted for Glydecare without native/TFLite assets.
 * It mirrors the upstream Ojas flow: normalize, Hamming window, frequency-domain
 * peak search, SNR validation, previous-HR lock, and exponential smoothing.
 */
class OjasRppgProcessor(
    private val bufferSize: Int = 300,
    private val samplingRate: Double = 30.0
) {
    private val buffer = ArrayDeque<Double>()
    private var previousHeartRate = 0.0

    fun reset() {
        buffer.clear()
        previousHeartRate = 0.0
    }

    fun sampleCount(): Int = buffer.size

    fun addSample(value: Double): OjasResult {
        if (buffer.size >= bufferSize) buffer.removeFirst()
        buffer.addLast(value)
        return computeHeartRate()
    }

    private fun computeHeartRate(): OjasResult {
        val sampleCount = buffer.size
        if (sampleCount < MIN_REQUIRED_SAMPLES) {
            return OjasResult(null, 0.0, "Collecting Ojas signal")
        }

        val values = buffer.toList()
        val mean = values.average()
        val normalized = DoubleArray(bufferSize)
        values.forEachIndexed { index, value ->
            val centered = value - mean
            val window = 0.54 - 0.46 * cos((2.0 * PI * index) / (sampleCount - 1).coerceAtLeast(1))
            normalized[index] = centered * window
        }

        var minFrequency = MIN_HZ
        var maxFrequency = MAX_HZ
        if (previousHeartRate > 0.0) {
            val previousFrequency = previousHeartRate / 60.0
            val lockWindow = 15.0 / 60.0
            minFrequency = max(MIN_HZ, previousFrequency - lockWindow)
            maxFrequency = min(MAX_HZ, previousFrequency + lockWindow)
        }

        var peakMagnitude = 0.0
        var peakIndex = -1
        var magnitudeSum = 0.0
        var magnitudeCount = 0

        for (index in 1 until bufferSize / 2) {
            val frequency = (index * samplingRate) / bufferSize
            val magnitude = magnitudeAt(normalized, index)

            if (frequency in MIN_HZ..MAX_HZ) {
                magnitudeSum += magnitude
                magnitudeCount++
            }

            if (frequency in minFrequency..maxFrequency && magnitude > peakMagnitude) {
                peakMagnitude = magnitude
                peakIndex = index
            }
        }

        val averageMagnitude = if (magnitudeCount == 0) 0.0 else magnitudeSum / magnitudeCount
        val confidence = if (averageMagnitude <= 0.0) 0.0 else (peakMagnitude / (averageMagnitude * 3.0)).coerceIn(0.0, 1.0)

        if (peakIndex < 0 || peakMagnitude < averageMagnitude * 1.6) {
            val locked = previousHeartRate.takeIf { it > 0.0 }?.toInt()
            return OjasResult(locked, confidence, "Ojas signal weak")
        }

        val currentBpm = ((peakIndex * samplingRate) / bufferSize) * 60.0
        previousHeartRate = if (previousHeartRate > 0.0) {
            previousHeartRate * 0.7 + currentBpm * 0.3
        } else {
            currentBpm
        }

        return OjasResult(previousHeartRate.toInt().coerceIn(40, 200), confidence, "Ojas FFT locked")
    }

    private fun magnitudeAt(values: DoubleArray, frequencyIndex: Int): Double {
        var real = 0.0
        var imaginary = 0.0
        for (index in values.indices) {
            val angle = -2.0 * PI * frequencyIndex * index / values.size
            real += values[index] * cos(angle)
            imaginary += values[index] * sin(angle)
        }
        return sqrt(real * real + imaginary * imaginary)
    }

    companion object {
        private const val MIN_REQUIRED_SAMPLES = 90
        private const val MIN_HZ = 0.75
        private const val MAX_HZ = 3.33
    }
}

data class OjasResult(
    val bpm: Int?,
    val confidence: Double,
    val message: String
)
