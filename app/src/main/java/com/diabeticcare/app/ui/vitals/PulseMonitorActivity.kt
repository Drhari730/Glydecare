package com.diabeticcare.app.ui.vitals

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.diabeticcare.app.DiabeticCareApp
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.model.VitalsRecord
import com.diabeticcare.app.databinding.ActivityPulseMonitorBinding
import com.diabeticcare.app.ui.widgets.LineChartView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
class PulseMonitorActivity : AppCompatActivity(), SurfaceHolder.Callback, Camera.PreviewCallback {

    private lateinit var binding: ActivityPulseMonitorBinding
    private var camera: Camera? = null
    private var isMeasuring = false
    private var latestBpm: Int? = null
    private var latestPeakBpm: Int? = null
    private var latestOjasBpm: Int? = null
    private var stableChecks = 0
    private var isAutoSaved = false
    private val samples = ArrayDeque<Sample>()
    private val ojasProcessor = OjasRppgProcessor()

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else showPermissionDenied()
        }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("diabeticcare_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        super.attachBaseContext(DiabeticCareApp.applyLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPulseMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.surfacePreview.holder.addCallback(this)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnStartStop.setOnClickListener { toggleMeasurement() }
        binding.btnSave.visibility = View.GONE
        binding.btnSave.setOnClickListener { saveHeartRate() }
        showRppgTourIfNeeded()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (hasCameraPermission()) startCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopCamera()
    }

    override fun onPause() {
        super.onPause()
        stopMeasurement()
        stopCamera()
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        if (!isMeasuring) return
        val size = camera.parameters.previewSize ?: return
        val channels = averageChannels(data, size.width, size.height)
        val now = System.currentTimeMillis()
        samples.addLast(Sample(now, channels.red))
        while (samples.isNotEmpty() && now - samples.first().timeMs > WINDOW_MS) {
            samples.removeFirst()
        }

        val ojasResult = ojasProcessor.addSample(channels.red)
        latestOjasBpm = ojasResult.bpm

        if (samples.size % 5 == 0) {
            updatePulse(ojasResult)
        }
    }

    private fun toggleMeasurement() {
        if (isMeasuring) {
            stopMeasurement()
        } else {
            if (camera == null) {
                if (hasCameraPermission()) startCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
            }
            startMeasurement()
        }
    }

    private fun startMeasurement() {
        samples.clear()
        ojasProcessor.reset()
        latestBpm = null
        latestPeakBpm = null
        latestOjasBpm = null
        stableChecks = 0
        isAutoSaved = false
        isMeasuring = true
        binding.btnSave.visibility = View.GONE
        binding.btnSave.isEnabled = false
        binding.tvBpm.text = "-- bpm"
        binding.tvPeakBpm.text = ""
        binding.tvOjasBpm.text = ""
        binding.tvComparison.text = ""
        binding.tvSignal.text = "Collecting signal"
        binding.tvStatus.text = "Cover camera and flash fully. Keep still; the final reading saves automatically when the signal is clear."
        binding.btnStartStop.text = "Stop Scan"
    }

    private fun stopMeasurement() {
        isMeasuring = false
        binding.btnStartStop.text = "Start Pulse Scan"
        binding.tvSignal.text = if (isAutoSaved) "Final reading saved" else if (latestBpm == null) "Scan paused" else "Pulse estimate ready"
    }

    private fun updatePulse(ojasResult: OjasResult) {
        if (samples.size < MIN_SAMPLES) {
            binding.tvSignal.text = "Stabilizing signal"
            binding.tvStatus.text = "Keep your fingertip steady. Glydecare is collecting the first few seconds of signal."
            return
        }

        val values = samples.map { it.red }
        val mean = values.average()
        val threshold = mean + (values.maxOrNull()!! - values.minOrNull()!!) * 0.18
        val peaks = mutableListOf<Long>()

        for (index in 1 until samples.size - 1) {
            val prev = samples[index - 1]
            val current = samples[index]
            val next = samples[index + 1]
            if (current.red > prev.red && current.red > next.red && current.red > threshold) {
                if (peaks.isEmpty() || current.timeMs - peaks.last() > MIN_PEAK_DISTANCE_MS) {
                    peaks.add(current.timeMs)
                }
            }
        }

        if (peaks.size >= 3) {
            val durationMinutes = (peaks.last() - peaks.first()) / 60000.0
            val bpm = ((peaks.size - 1) / durationMinutes).roundToInt().coerceIn(40, 180)
            latestPeakBpm = bpm
            updatePreferredResult(ojasResult)
        } else {
            binding.tvSignal.text = "Searching for pulse"
            binding.tvStatus.text = "Make sure the camera and flash are fully covered. Do not press too hard."
            updatePreferredResult(ojasResult)
        }
    }

    private fun updatePreferredResult(ojasResult: OjasResult) {
        val peak = latestPeakBpm
        val ojas = latestOjasBpm
        val preferred = ojas ?: peak
        latestBpm = preferred

        if (preferred != null) {
            binding.tvBpm.text = "$preferred bpm"
            binding.tvSignal.text = if (ojas != null) "Signal ready" else "Estimating pulse"
            binding.tvStatus.text = "Keep still. Glydecare will save one final pulse reading automatically."
            binding.btnSave.visibility = View.GONE
        }

        evaluateAutoSave(ojasResult)

        binding.tvComparison.text = when {
            ojas != null -> "Signal quality ${(ojasResult.confidence * 100).toInt()}%"
            peak != null -> "Signal detected"
            else -> ojasResult.message
        }
    }

    private fun evaluateAutoSave(ojasResult: OjasResult) {
        if (isAutoSaved || !isMeasuring) return
        if (ojasProcessor.sampleCount() < AUTO_SAVE_MIN_SAMPLES) {
            stableChecks = 0
            return
        }

        val peak = latestPeakBpm
        val ojas = latestOjasBpm
        val useOjas = ojas != null && ojasResult.confidence >= AUTO_SAVE_MIN_CONFIDENCE
        val useFallbackPeak = ojas == null && peak != null && ojasProcessor.sampleCount() >= AUTO_SAVE_FALLBACK_SAMPLES
        val bpm = when {
            useOjas -> ojas
            useFallbackPeak -> peak
            else -> null
        } ?: run {
            stableChecks = 0
            return
        }
        val stable = useOjas || useFallbackPeak

        if (stable) {
            stableChecks += 1
            val remaining = (AUTO_SAVE_STABLE_CHECKS - stableChecks).coerceAtLeast(0)
            binding.tvStatus.text = if (remaining == 0) {
                "Final pulse detected. Saving automatically..."
            } else {
                "Good signal. Hold still for $remaining more checks."
            }
        } else {
            stableChecks = 0
        }

        if (stableChecks >= AUTO_SAVE_STABLE_CHECKS) {
            autoSaveHeartRate(bpm, if (useOjas) "OJAS_FFT_PPG" else "CAMERA_PPG")
        }
    }

    private fun averageChannels(data: ByteArray, width: Int, height: Int): Channels {
        val frameSize = width * height
        var redTotal = 0.0
        var greenTotal = 0.0
        var count = 0
        var y = height / 4
        while (y < height * 3 / 4) {
            var x = width / 4
            while (x < width * 3 / 4) {
                val yValue = data[y * width + x].toInt() and 0xff
                val uvIndex = frameSize + (y shr 1) * width + (x and 1.inv())
                if (uvIndex + 1 < data.size) {
                    val u = (data[uvIndex + 1].toInt() and 0xff) - 128
                    val v = (data[uvIndex].toInt() and 0xff) - 128
                    val red = yValue + 1.402 * v
                    val green = yValue - 0.344136 * u - 0.714136 * v
                    redTotal += red.coerceIn(0.0, 255.0)
                    greenTotal += green.coerceIn(0.0, 255.0)
                    count++
                }
                x += 12
            }
            y += 12
        }
        if (count == 0) return Channels(0.0, 0.0)
        return Channels(redTotal / count, greenTotal / count)
    }

    private fun saveHeartRate() {
        val bpm = latestBpm ?: return
        lifecycleScope.launch {
            AppDatabase.getInstance(this@PulseMonitorActivity)
                .vitalsDao()
                .insert(VitalsRecord(heartRate = bpm, source = if (latestOjasBpm != null) "OJAS_FFT_PPG" else "CAMERA_PPG"))
            runOnUiThread {
                Toast.makeText(this@PulseMonitorActivity, "Heart rate saved: $bpm bpm", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun autoSaveHeartRate(bpm: Int, source: String) {
        if (isAutoSaved) return
        isAutoSaved = true
        isMeasuring = false
        latestBpm = bpm
        binding.tvBpm.text = "$bpm bpm"
        binding.tvSignal.text = "Final reading saved"
        binding.tvStatus.text = "Saved automatically to pulse history. You can measure again or go back to Vitals."
        binding.btnStartStop.text = "Measure Again"
        binding.btnSave.visibility = View.GONE

        lifecycleScope.launch {
            AppDatabase.getInstance(this@PulseMonitorActivity)
                .vitalsDao()
                .insert(VitalsRecord(heartRate = bpm, source = source))
            runOnUiThread {
                Toast.makeText(this@PulseMonitorActivity, "Pulse saved automatically: $bpm bpm", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        if (camera != null || !hasCameraPermission()) return
        try {
            camera = Camera.open().apply {
                val params = parameters
                params.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                parameters = params
                setPreviewDisplay(binding.surfacePreview.holder)
                setPreviewCallback(this@PulseMonitorActivity)
                startPreview()
            }
        } catch (error: Exception) {
            binding.tvStatus.text = "Camera pulse scan is unavailable on this device."
            Toast.makeText(this, "Unable to open camera: ${error.message}", Toast.LENGTH_LONG).show()
            stopCamera()
        }
    }

    private fun stopCamera() {
        camera?.setPreviewCallback(null)
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionDenied() {
        binding.tvStatus.text = "Camera permission is needed to estimate pulse."
        Toast.makeText(this, "Camera permission is required for pulse scan", Toast.LENGTH_LONG).show()
    }

    private fun showRppgTourIfNeeded() {
        val prefs = getSharedPreferences("diabeticcare_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("rppg_tour_seen", false)) return

        val tour = listOf(
            TourStep(
                "Measure your pulse with your camera",
                "No wearable needed. The rear camera and flashlight detect tiny fingertip color changes caused by blood flow.\n\n1. Flash shines through the finger.\n2. Camera captures color changes.\n3. Glydecare calculates the final BPM."
            ),
            TourStep(
                "Flash and camera turn on automatically",
                "Allow camera permission when asked. Keep the phone case away from the lens and flash. The app turns torch mode on so capillaries are visible."
            ),
            TourStep(
                "Place your finger correctly",
                "Cover both the rear camera lens and flashlight fully with the soft pad of your index finger. Do not use the nail or side of the finger."
            ),
            TourStep(
                "Use gentle pressure and hold still",
                "Too light causes light leaks. Too hard blocks blood flow. Use gentle, firm contact and stay still for 15-20 seconds while the signal stabilizes."
            ),
            TourStep(
                "Watch the waveform and final BPM",
                "When the signal is stable, Glydecare saves one final pulse reading automatically to Vitals history. You do not need to press Save.",
                true
            )
        )

        val view = layoutInflater.inflate(com.diabeticcare.app.R.layout.dialog_rppg_tour, null)
        var index = 0
        val dots = view.findViewById<TextView>(com.diabeticcare.app.R.id.tv_tour_dots)
        val title = view.findViewById<TextView>(com.diabeticcare.app.R.id.tv_tour_title)
        val body = view.findViewById<TextView>(com.diabeticcare.app.R.id.tv_tour_body)
        val image = view.findViewById<View>(com.diabeticcare.app.R.id.iv_tour_image)
        val wave = view.findViewById<LineChartView>(com.diabeticcare.app.R.id.chart_tour_wave)
        val back = view.findViewById<View>(com.diabeticcare.app.R.id.btn_tour_back)
        val skip = view.findViewById<View>(com.diabeticcare.app.R.id.btn_tour_skip)
        val next = view.findViewById<TextView>(com.diabeticcare.app.R.id.btn_tour_next)

        fun render() {
            val step = tour[index]
            dots.text = (tour.indices).joinToString(" ") { if (it == index) "●" else "○" }
            title.text = step.title
            body.text = step.body
            image.visibility = if (step.showWave) View.GONE else View.VISIBLE
            wave.visibility = if (step.showWave) View.VISIBLE else View.GONE
            if (step.showWave) wave.setPoints(listOf(68f, 72f, 70f, 76f, 73f, 78f, 74f), "")
            back.visibility = if (index == 0) View.INVISIBLE else View.VISIBLE
            next.text = if (index == tour.lastIndex) "Start" else "Next"
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        back.setOnClickListener {
            if (index > 0) {
                index -= 1
                render()
            }
        }
        skip.setOnClickListener {
            prefs.edit().putBoolean("rppg_tour_seen", true).apply()
            dialog.dismiss()
        }
        next.setOnClickListener {
            if (index == tour.lastIndex) {
                prefs.edit().putBoolean("rppg_tour_seen", true).apply()
                dialog.dismiss()
            } else {
                index += 1
                render()
            }
        }
        render()
        dialog.show()
    }

    private data class Sample(val timeMs: Long, val red: Double)
    private data class Channels(val red: Double, val green: Double)
    private data class TourStep(val title: String, val body: String, val showWave: Boolean = false)

    companion object {
        private const val WINDOW_MS = 15_000L
        private const val MIN_SAMPLES = 45
        private const val MIN_PEAK_DISTANCE_MS = 320L
        private const val AUTO_SAVE_MIN_SAMPLES = 180
        private const val AUTO_SAVE_FALLBACK_SAMPLES = 300
        private const val AUTO_SAVE_STABLE_CHECKS = 2
        private const val AUTO_SAVE_MIN_CONFIDENCE = 0.55
    }
}
