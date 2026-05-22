package com.diabeticcare.app.ui.hba1c

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.diabeticcare.app.R
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.model.HbA1cRecord
import com.diabeticcare.app.data.model.HbA1cStatus
import com.diabeticcare.app.databinding.FragmentHba1cBinding
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class HbA1cFragment : Fragment() {

    private var _binding: FragmentHba1cBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HbA1cViewModel
    private lateinit var adapter: HbA1cAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHba1cBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = HbA1cViewModel(AppDatabase.getInstance(requireContext()).hba1cDao())
        adapter = HbA1cAdapter { viewModel.delete(it) }

        binding.rvHba1c.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHba1c.adapter = adapter

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnAddHba1c.setOnClickListener { showAddDialog() }

        setupChart()

        viewModel.records.observe(viewLifecycleOwner) { records ->
            adapter.submitList(records)
            updateLatestDisplay(records)
            if (records.size >= 2) {
                updateChart(records)
                updatePrediction(records)
            }
        }
    }

    private fun setupChart() {
        binding.hba1cChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = false
            setScaleEnabled(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.setDrawLabels(true)
            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 4f
                axisMaximum = 14f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                // Target range line
                addLimitLine(LimitLine(7.0f, "Target").apply {
                    lineColor = Color.parseColor("#4CAF50")
                    lineWidth = 1.5f
                    enableDashedLine(10f, 5f, 0f)
                    textColor = Color.parseColor("#4CAF50")
                    textSize = 9f
                })
                addLimitLine(LimitLine(8.0f, "Uncontrolled").apply {
                    lineColor = Color.parseColor("#F44336")
                    lineWidth = 1f
                    enableDashedLine(8f, 4f, 0f)
                    textColor = Color.parseColor("#F44336")
                    textSize = 9f
                })
            }
        }
    }

    private fun updateLatestDisplay(records: List<HbA1cRecord>) {
        if (records.isNotEmpty()) {
            val latest = records.first()
            binding.tvHba1cValue.text = "%.1f".format(latest.value)
            binding.tvHba1cStatus.text = when (latest.status) {
                HbA1cStatus.NORMAL -> "Normal"
                HbA1cStatus.PREDIABETES -> "Pre-diabetes"
                HbA1cStatus.CONTROLLED -> "Controlled diabetes"
                HbA1cStatus.UNCONTROLLED -> "Uncontrolled — see doctor"
            }
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.tvHba1cDate.text = sdf.format(Date(latest.timestamp))
            val color = when (latest.status) {
                HbA1cStatus.NORMAL -> resources.getColor(R.color.success, null)
                HbA1cStatus.PREDIABETES -> resources.getColor(R.color.warning, null)
                HbA1cStatus.CONTROLLED -> resources.getColor(R.color.tile_hba1c, null)
                HbA1cStatus.UNCONTROLLED -> resources.getColor(R.color.error, null)
            }
            binding.tvHba1cValue.setTextColor(color)
        } else {
            binding.tvHba1cValue.text = "--"
            binding.tvHba1cStatus.text = "No reading yet"
            binding.tvHba1cDate.text = ""
        }
    }

    private fun updateChart(records: List<HbA1cRecord>) {
        val sorted = records.sortedBy { it.timestamp }
        val sdf = SimpleDateFormat("MMM yy", Locale.getDefault())

        val actualEntries = sorted.mapIndexed { i, r -> Entry(i.toFloat(), r.value) }

        val actualSet = LineDataSet(actualEntries, "HbA1c").apply {
            color = resources.getColor(R.color.tile_hba1c, null)
            setCircleColor(resources.getColor(R.color.tile_hba1c, null))
            lineWidth = 2.5f
            circleRadius = 5f
            setDrawValues(true)
            valueTextSize = 9f
            valueTextColor = resources.getColor(R.color.text_primary, null)
            mode = LineDataSet.Mode.LINEAR
            setDrawFilled(true)
            fillAlpha = 25
            fillColor = resources.getColor(R.color.tile_hba1c, null)
        }

        // X axis labels as months
        binding.hba1cChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx >= 0 && idx < sorted.size) sdf.format(Date(sorted[idx].timestamp)) else ""
            }
        }

        // If we have 3+ readings, add a projected point
        val dataSets = mutableListOf<ILineDataSet>(actualSet)
        if (sorted.size >= 2) {
            val projected = projectNextHba1c(sorted)
            val projEntries = listOf(
                Entry((sorted.size - 1).toFloat(), sorted.last().value),
                Entry(sorted.size.toFloat(), projected)
            )
            val projSet = LineDataSet(projEntries, "Projected").apply {
                color = Color.parseColor("#9E9E9E")
                setCircleColor(Color.parseColor("#9E9E9E"))
                lineWidth = 1.5f
                circleRadius = 4f
                enableDashedLine(10f, 5f, 0f)
                setDrawValues(true)
                valueTextSize = 9f
                valueTextColor = Color.parseColor("#9E9E9E")
                mode = LineDataSet.Mode.LINEAR
            }
            dataSets.add(projSet)
        }

        binding.hba1cChart.data = LineData(dataSets)
        binding.hba1cChart.invalidate()
        binding.cardChart.visibility = View.VISIBLE
    }

    private fun projectNextHba1c(sorted: List<HbA1cRecord>): Float {
        // Simple linear regression on last 3 readings (or all if fewer)
        val recent = if (sorted.size > 3) sorted.takeLast(3) else sorted
        val n = recent.size.toFloat()
        val sumX = (0 until recent.size).sumOf { it.toDouble() }.toFloat()
        val sumY = recent.sumOf { it.value.toDouble() }.toFloat()
        val sumXY = recent.mapIndexed { i, r -> i * r.value }.sum()
        val sumX2 = (0 until recent.size).sumOf { (it * it).toDouble() }.toFloat()
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n
        val next = intercept + slope * n
        return next.coerceIn(3f, 20f)
    }

    private fun updatePrediction(records: List<HbA1cRecord>) {
        val sorted = records.sortedBy { it.timestamp }
        val projected = projectNextHba1c(sorted)
        val current = sorted.last().value
        val trend = projected - current

        val projStatus = when {
            projected < 5.7f -> "Normal range"
            projected < 6.5f -> "Pre-diabetes range"
            projected < 8.0f -> "Controlled diabetes range"
            else -> "Uncontrolled — doctor consultation recommended"
        }

        val trendText = when {
            abs(trend) < 0.2f -> "stable"
            trend > 0 -> "↑ increasing by %.1f%%".format(trend)
            else -> "↓ decreasing by %.1f%%".format(abs(trend))
        }

        val adviceText = when {
            trend > 0.5f -> "Your HbA1c trend is rising. Consider reviewing your diet and medication adherence with your doctor."
            trend < -0.5f -> "Great progress! Your HbA1c is improving. Keep up the current regimen."
            else -> "Your HbA1c is stable. Continue monitoring regularly."
        }

        binding.tvPrediction.text = "Projected 3-month HbA1c: %.1f%% (%s)\nTrend: %s\n\n%s".format(
            projected, projStatus, trendText, adviceText
        )
        binding.cardPrediction.visibility = View.VISIBLE
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_hba1c, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add HbA1c Reading")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val valueStr = dialogView
                    .findViewById<TextInputEditText>(R.id.et_hba1c_value)?.text?.toString()?.trim()
                val value = valueStr?.toFloatOrNull()
                if (value == null || value < 3f || value > 20f) {
                    Toast.makeText(context, "Enter a valid HbA1c value (3–20%)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val notes = dialogView.findViewById<TextInputEditText>(R.id.et_hba1c_notes)
                    ?.text?.toString()?.trim() ?: ""
                viewModel.insert(HbA1cRecord(value = value, notes = notes))
                Toast.makeText(context, "HbA1c saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
