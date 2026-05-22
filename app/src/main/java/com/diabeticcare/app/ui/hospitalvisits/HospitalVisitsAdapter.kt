package com.diabeticcare.app.ui.hospitalvisits

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diabeticcare.app.data.model.HospitalVisit
import com.diabeticcare.app.databinding.ItemHospitalVisitBinding
import java.text.SimpleDateFormat
import java.util.*

class HospitalVisitsAdapter(
    private val onComplete: (HospitalVisit) -> Unit,
    private val onDelete: (HospitalVisit) -> Unit
) : ListAdapter<HospitalVisit, HospitalVisitsAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemHospitalVisitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(visit: HospitalVisit) {
            binding.tvVisitType.text = visit.visitType.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() }
            binding.tvVisitDoctor.text = buildString {
                if (visit.doctorName.isNotEmpty()) append(visit.doctorName)
                if (visit.hospitalName.isNotEmpty()) {
                    if (isNotEmpty()) append(" · ")
                    append(visit.hospitalName)
                }
            }
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            binding.tvVisitDate.text = sdf.format(Date(visit.scheduledDate))

            if (visit.completed) {
                binding.tvVisitType.paintFlags =
                    binding.tvVisitType.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.btnCompleteVisit.isEnabled = false
            } else {
                binding.tvVisitType.paintFlags =
                    binding.tvVisitType.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.btnCompleteVisit.isEnabled = true
            }

            binding.btnCompleteVisit.setOnClickListener { onComplete(visit) }
            binding.btnDeleteVisit.setOnClickListener { onDelete(visit) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemHospitalVisitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<HospitalVisit>() {
            override fun areItemsTheSame(a: HospitalVisit, b: HospitalVisit) = a.id == b.id
            override fun areContentsTheSame(a: HospitalVisit, b: HospitalVisit) = a == b
        }
    }
}
