package com.diabeticcare.app.ui.dashboard

import androidx.lifecycle.*
import com.diabeticcare.app.data.database.dao.HbA1cDao
import com.diabeticcare.app.data.database.dao.HospitalVisitDao
import com.diabeticcare.app.data.model.GlucoseReading
import com.diabeticcare.app.data.model.GlucoseStatus
import com.diabeticcare.app.data.model.HbA1cRecord
import com.diabeticcare.app.data.model.HospitalVisit
import com.diabeticcare.app.data.repository.GlucoseRepository
import com.diabeticcare.app.data.repository.MedicationRepository
import kotlinx.coroutines.launch

class DashboardViewModel(
    glucoseRepo: GlucoseRepository,
    private val medicationRepo: MedicationRepository,
    private val hba1cDao: HbA1cDao,
    private val hospitalVisitDao: HospitalVisitDao
) : ViewModel() {

    // Reactive — auto-updates when DB changes
    val latestGlucose: LiveData<GlucoseReading?> = glucoseRepo.latestLive
    val glucoseTrend: LiveData<List<GlucoseReading>> = glucoseRepo.allReadings
    val latestHba1c: LiveData<HbA1cRecord?> = hba1cDao.getLatestLive()
    val nextVisit: LiveData<HospitalVisit?> = hospitalVisitDao.getNextUpcomingLive()

    val aiSuggestion: LiveData<String> = latestGlucose.map { reading ->
        if (reading != null) generateAiSuggestion(reading)
        else ""
    }

    private val _adherenceScore = MutableLiveData<Int>(0)
    val adherenceScore: LiveData<Int> = _adherenceScore
    val futurePrediction = MediatorLiveData<String>()

    private val _medStatusToday = MutableLiveData<Pair<Int, Int>>(0 to 0)
    val medStatusToday: LiveData<Pair<Int, Int>> = _medStatusToday

    private var currentGlucose: GlucoseReading? = null
    private var currentHba1c: HbA1cRecord? = null
    private var currentAdherence: Int = 0

    init {
        futurePrediction.addSource(latestGlucose) {
            currentGlucose = it
            updateFuturePrediction()
        }
        futurePrediction.addSource(latestHba1c) {
            currentHba1c = it
            updateFuturePrediction()
        }
        futurePrediction.addSource(adherenceScore) {
            currentAdherence = it
            updateFuturePrediction()
        }
        loadAdherence()
    }

    fun refresh() { loadAdherence() }

    private fun loadAdherence() {
        viewModelScope.launch {
            _adherenceScore.postValue(medicationRepo.adherenceScoreLast(7))
            val total = medicationRepo.getActiveMedicationsSnapshot().size
            val taken = medicationRepo.takenCountToday()
            _medStatusToday.postValue(taken to total)
        }
    }

    private fun generateAiSuggestion(reading: GlucoseReading): String = when (reading.status) {
        GlucoseStatus.LOW ->
            "Your glucose is low (${reading.value} mg/dL). Have 15g fast-acting carbs like glucose tablets or fruit juice immediately."
        GlucoseStatus.NORMAL ->
            "Great! Your glucose is in target range (${reading.value} mg/dL). Keep up your healthy habits."
        GlucoseStatus.HIGH ->
            "Your glucose is elevated (${reading.value} mg/dL). Consider a 15-min walk and reduce portions at your next meal."
        GlucoseStatus.VERY_HIGH ->
            "Glucose very high (${reading.value} mg/dL). Take medication if due, avoid high-carb foods, stay hydrated."
    }

    private fun updateFuturePrediction() {
        val glucose = currentGlucose
        val hba1c = currentHba1c
        futurePrediction.value = when {
            glucose == null && hba1c == null ->
                "Add glucose and HbA1c readings to see a future risk projection."
            hba1c == null ->
                "Current glucose is ${glucose!!.value} mg/dL. Add HbA1c to improve the 3-month prediction."
            else -> buildProjectionText(glucose, hba1c, currentAdherence)
        }
    }

    private fun buildProjectionText(glucose: GlucoseReading?, hba1c: HbA1cRecord, adherence: Int): String {
        val glucosePressure = when (glucose?.status) {
            GlucoseStatus.LOW -> -0.2f
            GlucoseStatus.NORMAL, null -> 0f
            GlucoseStatus.HIGH -> 0.3f
            GlucoseStatus.VERY_HIGH -> 0.6f
        }
        val adherencePressure = when {
            adherence >= 85 -> -0.2f
            adherence >= 70 -> 0f
            adherence >= 50 -> 0.3f
            else -> 0.6f
        }
        val projected = (hba1c.value + glucosePressure + adherencePressure).coerceIn(4f, 14f)
        val direction = when {
            projected > hba1c.value + 0.3f -> "rising"
            projected < hba1c.value - 0.3f -> "improving"
            else -> "stable"
        }
        return "Projected HbA1c in 3 months: %.1f%%, %s. This uses your latest HbA1c, current glucose, and %d%% medication adherence.".format(
            projected,
            direction,
            adherence
        )
    }
}

class DashboardViewModelFactory(
    private val glucoseRepo: GlucoseRepository,
    private val medicationRepo: MedicationRepository,
    private val hba1cDao: HbA1cDao,
    private val hospitalVisitDao: HospitalVisitDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(glucoseRepo, medicationRepo, hba1cDao, hospitalVisitDao) as T
    }
}
