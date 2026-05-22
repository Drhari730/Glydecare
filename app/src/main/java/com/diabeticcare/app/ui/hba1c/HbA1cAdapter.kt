package com.diabeticcare.app.ui.hba1c

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diabeticcare.app.data.model.HbA1cRecord
import com.diabeticcare.app.data.model.HbA1cStatus
import com.diabeticcare.app.databinding.ItemHba1cBinding
import java.text.SimpleDateFormat
import java.util.*

class HbA1cAdapter(private val onDelete: (HbA1cRecord) -> Unit) :
    ListAdapter<HbA1cRecord, HbA1cAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemHba1cBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: HbA1cRecord) {
            binding.tvItemHba1cValue.text = "%.1f%%".format(record.value)
            binding.tvItemHba1cStatus.text = when (record.status) {
                HbA1cStatus.NORMAL -> "Normal"
                HbA1cStatus.PREDIABETES -> "Pre-diabetes"
                HbA1cStatus.CONTROLLED -> "Controlled"
                HbA1cStatus.UNCONTROLLED -> "Uncontrolled"
            }
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.tvItemHba1cDate.text = sdf.format(Date(record.timestamp))
            binding.btnDeleteHba1c.setOnClickListener { onDelete(record) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemHba1cBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<HbA1cRecord>() {
            override fun areItemsTheSame(a: HbA1cRecord, b: HbA1cRecord) = a.id == b.id
            override fun areContentsTheSame(a: HbA1cRecord, b: HbA1cRecord) = a == b
        }
    }
}
