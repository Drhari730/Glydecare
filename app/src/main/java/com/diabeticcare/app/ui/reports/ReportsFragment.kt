package com.diabeticcare.app.ui.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.diabeticcare.app.data.database.AppDatabase
import com.diabeticcare.app.data.repository.GlucoseRepository
import com.diabeticcare.app.data.repository.MealRepository
import com.diabeticcare.app.data.repository.MedicationRepository
import com.diabeticcare.app.databinding.FragmentReportsBinding
import java.io.File

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        ReportsViewModelFactory(
            GlucoseRepository(db.glucoseDao()),
            MedicationRepository(db.medicationDao()),
            MealRepository(db.mealDao())
        )
    }

    private var lastExportedFile: File? = null
    private var lastMimeType: String = "text/plain"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadAdherence()

        binding.chipReportPeriod.setOnCheckedStateChangeListener { _, _ ->
            viewModel.setReportDays(if (binding.chip7d.isChecked) 7 else 30)
        }

        viewModel.adherence7d.observe(viewLifecycleOwner) { binding.tvAdherence7d.text = "$it%" }
        viewModel.adherence30d.observe(viewLifecycleOwner) { binding.tvAdherence30d.text = "$it%" }

        viewModel.isExporting.observe(viewLifecycleOwner) { exporting ->
            binding.progressExport.visibility = if (exporting) View.VISIBLE else View.GONE
            binding.btnGeneratePdf.isEnabled = !exporting
            binding.btnExportCsv.isEnabled = !exporting
        }

        viewModel.exportedFile.observe(viewLifecycleOwner) { pair ->
            if (pair != null) {
                lastExportedFile = pair.first
                lastMimeType = pair.second
                Toast.makeText(context, "Report ready: ${pair.first.name}", Toast.LENGTH_SHORT).show()
                shareFile(pair.first, pair.second)
            } else {
                Toast.makeText(context, "Export failed. Check storage permission.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGeneratePdf.setOnClickListener {
            viewModel.generatePdfReport(requireContext(), if (binding.chip7d.isChecked) 7 else 30)
        }
        binding.btnExportCsv.setOnClickListener {
            viewModel.exportCsv(requireContext(), if (binding.chip7d.isChecked) 7 else 30)
        }
        binding.btnShareDoctor.setOnClickListener {
            val f = lastExportedFile
            if (f != null && f.exists()) shareFile(f, lastMimeType)
            else Toast.makeText(context, "Generate a report first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Glydecare Health Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Report via…"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
