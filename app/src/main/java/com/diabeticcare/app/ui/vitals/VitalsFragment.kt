package com.diabeticcare.app.ui.vitals

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.model.VitalsRecord
import com.diabeticcare.app.data.repository.VitalsRepository
import com.diabeticcare.app.databinding.FragmentVitalsBinding

class VitalsFragment : Fragment() {

    private var _binding: FragmentVitalsBinding? = null
    private val binding get() = _binding!!
    private lateinit var vitalsAdapter: VitalsAdapter

    private val viewModel: VitalsViewModel by viewModels {
        VitalsViewModelFactory(
            VitalsRepository(AppDatabase.getInstance(requireContext()).vitalsDao())
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVitalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vitalsAdapter = VitalsAdapter()
        binding.rvVitals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVitals.adapter = vitalsAdapter

        binding.btnFaceScan.setOnClickListener {
            startActivity(Intent(requireContext(), PulseMonitorActivity::class.java))
        }

        binding.btnLogVitals.setOnClickListener { logVitals() }
        val bmiWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateBmi()
            override fun afterTextChanged(s: Editable?) = Unit
        }
        binding.etHeightCm.addTextChangedListener(bmiWatcher)
        binding.etWeightKg.addTextChangedListener(bmiWatcher)

        viewModel.records.observe(viewLifecycleOwner) { records ->
            vitalsAdapter.submitList(records)
            updatePulseTrend(records)
        }
    }

    private fun updatePulseTrend(records: List<VitalsRecord>) {
        val pulseRecords = records.filter { it.heartRate != null }
        if (pulseRecords.isEmpty()) {
            binding.tvPulseLatest.text = "Latest\n-- bpm"
            binding.tvPulseAverage.text = "7-read avg\n-- bpm"
            binding.tvPulseTrend.text = "Trend\nNo data"
            binding.tvPulseTrendNote.text = "Auto-saved pulse readings will appear here after a camera scan."
            binding.chartVitals.setPoints(emptyList(), "No vitals trend yet")
            return
        }

        val latest = pulseRecords.first().heartRate ?: 0
        val recent = pulseRecords.take(7).mapNotNull { it.heartRate }
        val average = recent.average().toInt()
        val previous = pulseRecords.drop(1).firstOrNull()?.heartRate
        val trend = when {
            previous == null -> "New baseline"
            latest - previous > 5 -> "Rising"
            previous - latest > 5 -> "Lower"
            else -> "Stable"
        }

        binding.tvPulseLatest.text = "Latest\n$latest bpm"
        binding.tvPulseAverage.text = "7-read avg\n$average bpm"
        binding.tvPulseTrend.text = "Trend\n$trend"
        binding.tvPulseTrendNote.text = "Based on the latest ${recent.size} pulse reading(s). Camera readings save automatically after the signal is stable."
        binding.chartVitals.setPoints(pulseRecords.take(14).reversed().mapNotNull { it.heartRate?.toFloat() }, "No pulse trend yet")
    }

    private fun updateBmi() {
        val heightCm = binding.etHeightCm.text?.toString()?.toFloatOrNull()
        val weightKg = binding.etWeightKg.text?.toString()?.toFloatOrNull()
        if (heightCm == null || weightKg == null || heightCm <= 0f || weightKg <= 0f) {
            binding.tvBmiResult.text = "Enter height and weight to calculate BMI."
            return
        }
        val heightM = heightCm / 100f
        val bmi = weightKg / (heightM * heightM)
        val label = when {
            bmi < 18.5f -> "Underweight"
            bmi < 23f -> "Healthy range for many Indian adults"
            bmi < 25f -> "Overweight risk range"
            else -> "High risk range"
        }
        binding.tvBmiResult.text = "BMI: %.1f - %s".format(bmi, label)
    }

    private fun logVitals() {
        val systolicStr = binding.etSystolic.text?.toString()?.trim()
        val diastolicStr = binding.etDiastolic.text?.toString()?.trim()
        val heartRateStr = binding.etHeartRate.text?.toString()?.trim()
        val spo2Str = binding.etSpo2.text?.toString()?.trim()
        val rrStr = binding.etRr.text?.toString()?.trim()

        if (systolicStr.isNullOrEmpty() && diastolicStr.isNullOrEmpty() &&
            heartRateStr.isNullOrEmpty() && spo2Str.isNullOrEmpty() && rrStr.isNullOrEmpty()
        ) {
            Toast.makeText(context, "Enter at least one vital value", Toast.LENGTH_SHORT).show()
            return
        }

        val record = VitalsRecord(
            systolic = systolicStr?.toIntOrNull(),
            diastolic = diastolicStr?.toIntOrNull(),
            heartRate = heartRateStr?.toIntOrNull(),
            spo2 = spo2Str?.toFloatOrNull(),
            respiratoryRate = rrStr?.toIntOrNull(),
            source = "MANUAL"
        )

        viewModel.logVitals(record)

        // Clear inputs
        listOf(
            binding.etSystolic, binding.etDiastolic, binding.etHeartRate,
            binding.etSpo2, binding.etRr
        ).forEach { it.setText("") }

        Toast.makeText(context, "Vitals saved!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
