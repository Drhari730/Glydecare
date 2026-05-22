package com.diabeticcare.app.ui.medication

import androidx.lifecycle.*
import com.diabeticcare.app.data.model.Medication
import com.diabeticcare.app.data.model.MedicationLog
import com.diabeticcare.app.data.repository.MedicationRepository
import kotlinx.coroutines.launch

class MedicationViewModel(private val repo: MedicationRepository) : ViewModel() {

    val medications = repo.activeMedications

    fun addMedication(med: Medication) {
        viewModelScope.launch { repo.insertMedication(med) }
    }

    fun logDose(med: Medication, taken: Boolean) {
        viewModelScope.launch {
            repo.logDose(
                MedicationLog(
                    medicationId = med.id,
                    medicationName = med.name,
                    status = if (taken) "TAKEN" else "MISSED",
                    scheduledTime = med.reminderTimes.split(",").firstOrNull() ?: "08:00"
                )
            )
        }
    }
}

class MedicationViewModelFactory(private val repo: MedicationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MedicationViewModel(repo) as T
    }
}
