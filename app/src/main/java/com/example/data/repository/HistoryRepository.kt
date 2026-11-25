package com.example.data.repository

import com.example.data.local.CalculationDao
import com.example.data.local.CalculationRecord
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: CalculationDao) {
    val historyStream: Flow<List<CalculationRecord>> = dao.observeAll()

    suspend fun saveCalculation(record: CalculationRecord) {
        dao.insert(record)
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
