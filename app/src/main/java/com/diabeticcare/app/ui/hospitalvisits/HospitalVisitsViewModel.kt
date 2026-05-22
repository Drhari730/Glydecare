package com.diabeticcare.app.ui.hospitalvisits

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeticcare.app.data.database.dao.HospitalVisitDao
import com.diabeticcare.app.data.model.HospitalVisit
import kotlinx.coroutines.launch

class HospitalVisitsViewModel(private val dao: HospitalVisitDao) : ViewModel() {

    val visits: LiveData<List<HospitalVisit>> = dao.getAllVisits()

    fun insert(visit: HospitalVisit) = viewModelScope.launch { dao.insert(visit) }
    fun delete(visit: HospitalVisit) = viewModelScope.launch { dao.delete(visit) }
    fun markComplete(visit: HospitalVisit) = viewModelScope.launch {
        dao.update(visit.copy(completed = true))
    }
}
