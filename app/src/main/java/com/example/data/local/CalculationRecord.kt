package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class CalculationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val plainResult: String,
    val formattedResult: String,
    val isScientificNotation: Boolean = false,
    val digitCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
