package com.diabeticcare.app.ui.vitals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diabeticcare.app.data.model.VitalsRecord
import com.diabeticcare.app.databinding.ItemVitalsRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VitalsAdapter : ListAdapter<VitalsRecord, VitalsAdapter.VitalsViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VitalsViewHolder {
        val binding = ItemVitalsRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VitalsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VitalsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VitalsViewHolder(private val binding: ItemVitalsRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        private val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        fun bind(record: VitalsRecord) {
            binding.tvRecordTitle.text = if (record.heartRate != null && record.source != "MANUAL") {
                "Auto pulse reading"
            } else {
                "Vitals reading"
            }
            binding.tvRecordSource.text = when (record.source) {
                "OJAS_FFT_PPG" -> "Ojas auto"
                "CAMERA_PPG" -> "Camera"
                else -> "Manual"
            }
            binding.tvRecordValues.text = buildValues(record)
            binding.tvRecordTime.text = formatter.format(Date(record.timestamp))
        }

        private fun buildValues(record: VitalsRecord): String {
            val values = mutableListOf<String>()
            record.heartRate?.let { values.add("Pulse: $it bpm") }
            if (record.systolic != null && record.diastolic != null) {
                values.add("BP: ${record.systolic}/${record.diastolic} mmHg")
            }
            record.spo2?.let { values.add("SpO2: $it%") }
            record.respiratoryRate?.let { values.add("RR: $it/min") }
            return values.ifEmpty { listOf("Vitals saved") }.joinToString("  |  ")
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<VitalsRecord>() {
        override fun areItemsTheSame(oldItem: VitalsRecord, newItem: VitalsRecord): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: VitalsRecord, newItem: VitalsRecord): Boolean =
            oldItem == newItem
    }
}
