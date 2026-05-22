package com.diabeticcare.app.ui.hba1c

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeticcare.app.data.database.dao.HbA1cDao
import com.diabeticcare.app.data.model.HbA1cRecord
import kotlinx.coroutines.launch

class HbA1cViewModel(private val dao: HbA1cDao) : ViewModel() {

    val records: LiveData<List<HbA1cRecord>> = dao.getAllRecords()

    fun insert(record: HbA1cRecord) = viewModelScope.launch { dao.insert(record) }
    fun delete(record: HbA1cRecord) = viewModelScope.launch { dao.delete(record) }
}
