package com.diabeticcare.app.ui.hospitalvisits

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.diabeticcare.app.R
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.model.HospitalVisit
import com.diabeticcare.app.databinding.FragmentHospitalVisitsBinding
import com.diabeticcare.app.utils.ReminderScheduler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class HospitalVisitsFragment : Fragment() {

    private var _binding: FragmentHospitalVisitsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HospitalVisitsAdapter
    private lateinit var viewModel: HospitalVisitsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHospitalVisitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dao = AppDatabase.getInstance(requireContext()).hospitalVisitDao()
        viewModel = HospitalVisitsViewModel(dao)

        adapter = HospitalVisitsAdapter(
            onComplete = { viewModel.markComplete(it) },
            onDelete = { viewModel.delete(it) }
        )

        binding.rvVisits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVisits.adapter = adapter

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.fabAddVisit.setOnClickListener { showAddDialog() }

        viewModel.visits.observe(viewLifecycleOwner) { visits ->
            adapter.submitList(visits)

            val next = visits.firstOrNull { !it.completed }
            if (next != null) {
                val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                binding.tvNextVisitInfo.text = next.visitType.replace("_", " ")
                    .lowercase().replaceFirstChar { it.uppercase() }
                binding.tvNextVisitDate.text = sdf.format(Date(next.scheduledDate))
            } else {
                binding.tvNextVisitInfo.text = "No upcoming visits scheduled"
                binding.tvNextVisitDate.text = ""
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_visit, null)

        val visitTypes = listOf("Clinic", "Lab Test", "Eye Check", "Foot Check", "Other")
        val acv = dialogView.findViewById<AutoCompleteTextView>(R.id.acv_visit_type)
        acv.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, visitTypes))
        acv.setText(visitTypes[0], false)

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        var selectedDateMs = System.currentTimeMillis()
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.et_visit_date)
        etDate.setText(sdf.format(Date(selectedDateMs)))

        etDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                selectedDateMs = cal.timeInMillis
                etDate.setText(sdf.format(Date(selectedDateMs)))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Hospital Visit")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val type = acv.text.toString().trim().uppercase().replace(" ", "_")
                val doctor = dialogView.findViewById<TextInputEditText>(R.id.et_visit_doctor)
                    ?.text?.toString()?.trim() ?: ""
                val hospital = dialogView.findViewById<TextInputEditText>(R.id.et_visit_hospital)
                    ?.text?.toString()?.trim() ?: ""
                val notes = dialogView.findViewById<TextInputEditText>(R.id.et_visit_notes)
                    ?.text?.toString()?.trim() ?: ""

                if (type.isEmpty()) {
                    Toast.makeText(context, "Select a visit type", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val visit = HospitalVisit(
                    visitType = type,
                    doctorName = doctor,
                    hospitalName = hospital,
                    scheduledDate = selectedDateMs,
                    notes = notes
                )
                viewModel.insert(visit)
                ReminderScheduler.scheduleVisitAlarms(
                    requireContext(),
                    (type + selectedDateMs + hospital).hashCode(),
                    type,
                    hospital,
                    selectedDateMs
                )
                Toast.makeText(context, "Visit added!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
