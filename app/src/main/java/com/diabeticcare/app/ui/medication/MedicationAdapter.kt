package com.diabeticcare.app.ui.medication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.diabeticcare.app.R
import com.diabeticcare.app.data.model.Medication
import com.diabeticcare.app.databinding.ItemMedicationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationAdapter(
    private val meds: List<Medication>,
    private val onAction: (Medication, Boolean) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.VH>() {

    inner class VH(val binding: ItemMedicationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemMedicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = meds.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val med = meds[position]
        with(holder.binding) {
            tvMedName.text = listOf(med.name, med.dose.substringBefore(" - ").takeIf { it.isNotBlank() })
                .filterNotNull()
                .joinToString(" - ")
            tvMedDose.text = med.dose
            tvMedType.text = med.medicationType
            tvMedFrequency.text = "-${frequencyLabel(med.frequency)}"
            tvReminderTime.text = med.reminderTimes.split(",").firstOrNull()?.trim().orEmpty()
            tvStartDate.text = "Started: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(med.startDate))}"
            ivMedType.setImageResource(
                if (med.medicationType.equals("Insulin", true) || med.medicationType.equals("Injection", true)) {
                    R.drawable.ic_finger_camera
                } else {
                    R.drawable.ic_medication
                }
            )
            btnTaken.setOnClickListener { onAction(med, true) }
            btnMissed.setOnClickListener { onAction(med, false) }
        }
    }

    private fun frequencyLabel(frequency: String): String = when (frequency) {
        "ONCE_DAILY" -> "Daily 1 time"
        "TWICE_DAILY" -> "Daily 2 times"
        "THREE_TIMES" -> "Daily 3 times"
        "FOUR_TIMES" -> "Daily 4 times"
        else -> frequency.replace("_", " ").lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
