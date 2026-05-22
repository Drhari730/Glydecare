package com.diabeticcare.app.ui.dashboard

import android.os.Bundle
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.diabeticcare.app.R
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.model.GlucoseStatus
import com.diabeticcare.app.data.repository.GlucoseRepository
import com.diabeticcare.app.data.repository.MedicationRepository
import com.diabeticcare.app.databinding.FragmentDashboardBinding
import com.diabeticcare.app.utils.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        DashboardViewModelFactory(
            GlucoseRepository(db.glucoseDao()),
            MedicationRepository(db.medicationDao()),
            db.hba1cDao(),
            db.hospitalVisitDao()
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setGreeting()
        setUserName()
        observeData()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> getString(R.string.good_morning)
            hour < 17 -> getString(R.string.good_afternoon)
            else -> getString(R.string.good_evening)
        }
    }

    private fun setUserName() {
        binding.tvUserName.text = PreferenceManager(requireContext()).getName().ifEmpty { "Friend" }
    }

    private fun observeData() {
        viewModel.latestGlucose.observe(viewLifecycleOwner) { reading ->
            if (reading != null) {
                binding.tvGlucoseValue.text = reading.value.toString()
                val statusText = when (reading.status) {
                    GlucoseStatus.LOW -> getString(R.string.status_low)
                    GlucoseStatus.NORMAL -> getString(R.string.status_normal)
                    GlucoseStatus.HIGH -> getString(R.string.status_high)
                    GlucoseStatus.VERY_HIGH -> getString(R.string.status_very_high)
                }
                binding.tvGlucoseStatusBadge.text = statusText
                val colorRes = when (reading.status) {
                    GlucoseStatus.LOW -> R.color.glucose_low
                    GlucoseStatus.NORMAL -> R.color.glucose_normal
                    GlucoseStatus.HIGH -> R.color.glucose_high
                    GlucoseStatus.VERY_HIGH -> R.color.glucose_very_high
                }
                binding.tvGlucoseStatusBadge.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(colorRes, null))
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                binding.tvGlucoseTime.text = sdf.format(Date(reading.timestamp))
            } else {
                binding.tvGlucoseValue.text = "--"
                binding.tvGlucoseTime.text = getString(R.string.no_data)
                binding.tvGlucoseStatusBadge.text = "--"
            }
        }

        viewModel.glucoseTrend.observe(viewLifecycleOwner) { readings ->
            binding.chartGlucoseTrend.setPoints(
                readings.take(14).reversed().map { it.value.toFloat() },
                "No glucose trend yet"
            )
        }

        viewModel.latestHba1c.observe(viewLifecycleOwner) { record ->
            binding.tvHba1cValue.text = if (record != null) "%.1f".format(record.value) else "--"
        }

        viewModel.nextVisit.observe(viewLifecycleOwner) { visit ->
            if (visit != null) {
                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                binding.tvNextVisit.text = getString(R.string.next_visit_date, sdf.format(Date(visit.scheduledDate)))
            } else {
                binding.tvNextVisit.text = getString(R.string.next_visit_dash)
            }
        }

        viewModel.aiSuggestion.observe(viewLifecycleOwner) { suggestion ->
            binding.tvAiSuggestion.text = suggestion.ifEmpty { getString(R.string.no_suggestion) }
        }

        viewModel.adherenceScore.observe(viewLifecycleOwner) { score ->
            binding.tvAdherenceScore.text = "$score%"
        }

        viewModel.medStatusToday.observe(viewLifecycleOwner) { (taken, total) ->
            binding.tvMedStatus.text = "$taken/$total"
        }

        viewModel.futurePrediction.observe(viewLifecycleOwner) { prediction ->
            binding.tvFuturePrediction.text = prediction
        }
    }

    private fun setupClickListeners() {
        binding.btnLogGlucose.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_glucose)
        }
        binding.btnReports.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_reports)
        }
        binding.tileGlucose.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_glucose)
        }
        binding.tileHba1c.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_hba1c)
        }
        binding.tileMedication.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_medication)
        }
        binding.tileNutrition.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_nutrition)
        }
        binding.tileVitals.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_vitals)
        }
        binding.tileVisits.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_visits)
        }
        binding.tileLifestyle.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_lifestyle)
        }
        binding.tileAbout.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_about)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
