package com.diabeticcare.app.ui.glucose

import androidx.lifecycle.*
import com.diabeticcare.app.data.model.GlucoseReading
import com.diabeticcare.app.data.repository.GlucoseRepository
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class GlucoseViewModel(private val repo: GlucoseRepository) : ViewModel() {

    private val _periodDays = MutableLiveData(7)

    val readings: LiveData<List<GlucoseReading>> = _periodDays.switchMap { days ->
        repo.readingsSince(days)
    }

    private val _avgGlucose = MutableLiveData<Float?>()
    val avgGlucose: LiveData<Float?> = _avgGlucose

    private val _outOfRange = MutableLiveData(0)
    val outOfRange: LiveData<Int> = _outOfRange

    init {
        loadStats(7)
    }

    fun setPeriodDays(days: Int) {
        _periodDays.value = days
        loadStats(days)
    }

    fun addReading(reading: GlucoseReading) {
        viewModelScope.launch {
            repo.insert(reading)
            loadStats(_periodDays.value ?: 7)
        }
    }

    private fun loadStats(days: Int) {
        viewModelScope.launch {
            _avgGlucose.postValue(repo.averageLast(days))
            _outOfRange.postValue(repo.outOfRangeCountLast(days))
        }
    }
}

class GlucoseViewModelFactory(private val repo: GlucoseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return GlucoseViewModel(repo) as T
    }
}
