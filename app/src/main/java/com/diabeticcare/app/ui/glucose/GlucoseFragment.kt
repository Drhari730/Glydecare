package com.diabeticcare.app.ui.glucose

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.diabeticcare.app.R
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.model.GlucoseReading
import com.diabeticcare.app.data.remote.SyncRepository
import com.diabeticcare.app.data.repository.GlucoseRepository
import com.diabeticcare.app.databinding.FragmentGlucoseBinding
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GlucoseFragment : Fragment() {

    private var _binding: FragmentGlucoseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GlucoseViewModel by viewModels {
        GlucoseViewModelFactory(
            GlucoseRepository(AppDatabase.getInstance(requireContext()).glucoseDao())
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGlucoseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        observeData()

        binding.fabAddGlucose.setOnClickListener { showAddReadingDialog() }

        binding.tabPeriod.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                viewModel.setPeriodDays(if (tab?.position == 0) 7 else 30)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupChart() {
        binding.glucoseChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 40f
                axisMaximum = 300f
                setDrawGridLines(true)
                // Normal range bands
                addLimitLine(LimitLine(70f, "Low").apply {
                    lineColor = resources.getColor(R.color.glucose_low, null)
                    lineWidth = 1f
                    textColor = resources.getColor(R.color.glucose_low, null)
                })
                addLimitLine(LimitLine(180f, "High").apply {
                    lineColor = resources.getColor(R.color.glucose_high, null)
                    lineWidth = 1f
                    textColor = resources.getColor(R.color.glucose_high, null)
                })
            }
        }
    }

    private fun observeData() {
        viewModel.readings.observe(viewLifecycleOwner) { readings ->
            updateChart(readings)
            binding.tvReadingsCount.text = readings.size.toString()
        }

        viewModel.avgGlucose.observe(viewLifecycleOwner) { avg ->
            binding.tvAvgGlucose.text = if (avg != null) "%.0f".format(avg) else "--"
        }

        viewModel.outOfRange.observe(viewLifecycleOwner) { count ->
            binding.tvOutOfRange.text = count.toString()
        }
    }

    private fun updateChart(readings: List<GlucoseReading>) {
        if (readings.isEmpty()) {
            binding.glucoseChart.clear()
            return
        }
        val sorted = readings.sortedBy { it.timestamp }
        val entries = sorted.mapIndexed { i, r -> BarEntry(i.toFloat(), r.value.toFloat()) }
        val dataSet = BarDataSet(entries, "Glucose").apply {
            color = resources.getColor(R.color.health_teal, null)
            setDrawValues(false)
        }
        binding.glucoseChart.data = BarData(dataSet).apply {
            barWidth = 0.35f
        }
        binding.glucoseChart.setFitBars(true)
        binding.glucoseChart.invalidate()
    }

    private fun showAddReadingDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_glucose, null)
        val btnDateTime = dialogView.findViewById<MaterialButton>(R.id.btn_select_datetime)

        // Selected timestamp — default to now
        val selectedCal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        btnDateTime.text = "Select test date: ${sdf.format(selectedCal.time)}"

        btnDateTime.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedCal.set(Calendar.YEAR, year)
                    selectedCal.set(Calendar.MONTH, month)
                    selectedCal.set(Calendar.DAY_OF_MONTH, day)
                    // Then pick time
                    TimePickerDialog(requireContext(), { _, hour, minute ->
                        selectedCal.set(Calendar.HOUR_OF_DAY, hour)
                        selectedCal.set(Calendar.MINUTE, minute)
                        btnDateTime.text = "Test date: ${sdf.format(selectedCal.time)}"
                    }, selectedCal.get(Calendar.HOUR_OF_DAY), selectedCal.get(Calendar.MINUTE), false).show()
                },
                selectedCal.get(Calendar.YEAR),
                selectedCal.get(Calendar.MONTH),
                selectedCal.get(Calendar.DAY_OF_MONTH)
            ).also { it.datePicker.maxDate = System.currentTimeMillis() }.show()
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Glucose Reading")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val valueStr = dialogView.findViewById<TextInputEditText>(R.id.et_glucose_value)
                ?.text?.toString()?.trim()
            val value = valueStr?.toIntOrNull()
            if (value == null || value < 20 || value > 600) {
                Toast.makeText(context, "Enter a valid glucose value (20–600)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val readingType = when (dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_reading_type).checkedChipId) {
                R.id.chip_fasting -> "FASTING"
                R.id.chip_post_meal -> "POST_MEAL"
                R.id.chip_bedtime -> "BEDTIME"
                else -> "RANDOM"
            }
            val notes = dialogView.findViewById<TextInputEditText>(R.id.et_glucose_notes)
                ?.text?.toString()?.trim() ?: ""
            val reading = GlucoseReading(
                value = value,
                readingType = readingType,
                notes = notes,
                timestamp = selectedCal.timeInMillis
            )
            viewModel.addReading(reading)
            syncGlucoseReading(reading)
            Toast.makeText(context, "Reading saved!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    private fun syncGlucoseReading(reading: GlucoseReading) {
        val appContext = requireContext().applicationContext
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val profile = AppDatabase.getInstance(appContext)
                    .userProfileDao()
                    .getProfileSnapshot()
                if (profile != null) {
                    SyncRepository.syncGlucose(profile, reading)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
