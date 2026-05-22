package com.diabeticcare.app.ui.vitals

import androidx.lifecycle.*
import com.diabeticcare.app.data.model.VitalsRecord
import com.diabeticcare.app.data.repository.VitalsRepository
import kotlinx.coroutines.launch

class VitalsViewModel(private val repo: VitalsRepository) : ViewModel() {

    val records = repo.allRecords

    fun logVitals(record: VitalsRecord) {
        viewModelScope.launch { repo.insert(record) }
    }
}

class VitalsViewModelFactory(private val repo: VitalsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return VitalsViewModel(repo) as T
    }
}
