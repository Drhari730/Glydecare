package com.diabeticcare.app.ui.medication

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.diabeticcare.app.R
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.model.Medication
import com.diabeticcare.app.data.repository.MedicationRepository
import com.diabeticcare.app.databinding.FragmentMedicationBinding
import com.diabeticcare.app.utils.ReminderScheduler
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MedicationFragment : Fragment() {

    private var _binding: FragmentMedicationBinding? = null
    private val binding get() = _binding!!
    private val allMedications = mutableListOf<Medication>()
    private var selectedDayStart = startOfDay(System.currentTimeMillis())

    private val viewModel: MedicationViewModel by viewModels {
        MedicationViewModelFactory(
            MedicationRepository(AppDatabase.getInstance(requireContext()).medicationDao())
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvMedications.layoutManager = LinearLayoutManager(context)
        setupWeekStrip()

        viewModel.medications.observe(viewLifecycleOwner) { meds ->
            allMedications.clear()
            allMedications.addAll(meds)
            renderMedicationsForSelectedDate()
        }

        binding.fabAddMedication.setOnClickListener { showAddMedicationDialog() }
    }

    private fun showAddMedicationDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_medication, null)
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        var startDateMs = selectedDayStart
        val etStartDate = dialogView.findViewById<TextInputEditText>(R.id.et_start_date)
        etStartDate.setText(sdf.format(Date(startDateMs)))
        etStartDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = startDateMs }
            DatePickerDialog(requireContext(), { _, year, month, day ->
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startDateMs = cal.timeInMillis
                etStartDate.setText(sdf.format(Date(startDateMs)))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Time state
        val times = arrayOf("08:00", "20:00", "14:00", "21:00")
        val timeButtons = arrayOf(
            dialogView.findViewById<MaterialButton>(R.id.btn_time1),
            dialogView.findViewById<MaterialButton>(R.id.btn_time2),
            dialogView.findViewById<MaterialButton>(R.id.btn_time3),
            dialogView.findViewById<MaterialButton>(R.id.btn_time4)
        )
        val timeRows = arrayOf(
            dialogView.findViewById<View>(R.id.row_time1),
            dialogView.findViewById<View>(R.id.row_time2),
            dialogView.findViewById<View>(R.id.row_time3),
            dialogView.findViewById<View>(R.id.row_time4)
        )

        // Show/hide time rows based on frequency
        val freqGroup = dialogView.findViewById<ChipGroup>(R.id.chip_frequency)
        fun updateTimeRows() {
            val count = when (freqGroup.checkedChipId) {
                R.id.chip_od -> { times[0] = "08:00"; 1 }
                R.id.chip_bd -> { times[0] = "08:00"; times[1] = "20:00"; 2 }
                R.id.chip_tid -> { times[0] = "08:00"; times[1] = "14:00"; times[2] = "20:00"; 3 }
                R.id.chip_qid -> { times[0] = "07:00"; times[1] = "12:00"; times[2] = "17:00"; times[3] = "21:00"; 4 }
                else -> 1
            }
            timeButtons.forEachIndexed { i, btn -> btn.text = times[i] }
            timeRows.forEachIndexed { i, row -> row.visibility = if (i < count) View.VISIBLE else View.GONE }
        }
        updateTimeRows()
        freqGroup.setOnCheckedStateChangeListener { _, _ -> updateTimeRows() }

        // Time picker on click
        timeButtons.forEachIndexed { i, btn ->
            btn.setOnClickListener {
                val parts = times[i].split(":")
                TimePickerDialog(requireContext(), { _, h, m ->
                    times[i] = "%02d:%02d".format(h, m)
                    btn.text = times[i]
                }, parts[0].toInt(), parts[1].toInt(), true).show()
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Medication")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogView.findViewById<TextInputEditText>(R.id.et_med_name)
                ?.text?.toString()?.trim() ?: ""
            val dose = dialogView.findViewById<TextInputEditText>(R.id.et_med_dose)
                ?.text?.toString()?.trim() ?: ""

            if (name.isEmpty()) {
                Toast.makeText(context, "Please enter medication name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val frequency = when (freqGroup.checkedChipId) {
                R.id.chip_od -> "ONCE_DAILY"
                R.id.chip_bd -> "TWICE_DAILY"
                R.id.chip_tid -> "THREE_TIMES"
                R.id.chip_qid -> "FOUR_TIMES"
                else -> "ONCE_DAILY"
            }

            val mealTiming = when (dialogView.findViewById<ChipGroup>(R.id.chip_meal_timing).checkedChipId) {
                R.id.chip_before_food -> "Before Meal"
                R.id.chip_after_food -> "After Meal"
                R.id.chip_with_food -> "With Meal"
                else -> "Before Meal"
            }

            val medicationType = when (dialogView.findViewById<ChipGroup>(R.id.chip_med_type).checkedChipId) {
                R.id.chip_type_insulin -> "Insulin"
                R.id.chip_type_injection -> "Injection"
                R.id.chip_type_syrup -> "Syrup"
                else -> "Tablet"
            }

            val count = when (frequency) {
                "ONCE_DAILY" -> 1; "TWICE_DAILY" -> 2; "THREE_TIMES" -> 3; else -> 4
            }
            val reminderTimes = times.take(count).joinToString(",")

            viewModel.addMedication(
                Medication(
                    name = name,
                    dose = "$dose - $mealTiming",
                    frequency = frequency,
                    reminderTimes = reminderTimes,
                    medicationType = medicationType,
                    startDate = startDateMs
                )
            )
            ReminderScheduler.scheduleMedicationAlarms(requireContext(), name, reminderTimes, name.hashCode().and(0x7fffffff) % 10000)
            Toast.makeText(context, "$name added! Reminders set for: $reminderTimes", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }
    }

    private fun setupWeekStrip() {
        renderWeekStrip()
    }

    private fun renderWeekStrip() {
        binding.weekContainer.removeAllViews()
        val base = Calendar.getInstance().apply {
            timeInMillis = selectedDayStart
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        repeat(14) { index ->
            val cal = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, index) }
            val dayStart = startOfDay(cal.timeInMillis)
            val item = TextView(requireContext()).apply {
                text = SimpleDateFormat("EEE\nd", Locale.getDefault()).format(Date(dayStart))
                gravity = android.view.Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 12f
                setLineSpacing(0f, 1.45f)
                setBackgroundResource(if (dayStart == selectedDayStart) R.drawable.bg_calendar_pill_selected else R.drawable.bg_calendar_pill)
                setTextColor(ContextCompat.getColor(requireContext(), if (dayStart == selectedDayStart) R.color.white else R.color.text_primary))
                typeface = if (dayStart == selectedDayStart) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                minWidth = resources.displayMetrics.density.times(56).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    resources.displayMetrics.density.times(58).toInt(),
                    resources.displayMetrics.density.times(64).toInt()
                ).apply { marginEnd = resources.displayMetrics.density.times(6).toInt() }
                setOnClickListener {
                    selectedDayStart = dayStart
                    renderWeekStrip()
                    renderMedicationsForSelectedDate()
                }
            }
            binding.weekContainer.addView(item)
        }
    }

    private fun renderMedicationsForSelectedDate() {
        binding.tvScheduleTitle.text = "Upcoming medications"
        val meds = allMedications.filter { startOfDay(it.startDate) <= selectedDayStart }
        binding.rvMedications.adapter = MedicationAdapter(meds) { med, taken ->
            viewModel.logDose(med, taken)
            val msg = if (taken) "${med.name} marked as taken" else "${med.name} marked as missed"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private fun startOfDay(timeMs: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = timeMs
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
