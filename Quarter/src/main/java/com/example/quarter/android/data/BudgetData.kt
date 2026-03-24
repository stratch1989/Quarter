package com.example.quarter.android.data

data class BudgetData(
    val howMany: Double = 0.0,
    val numberOfDays: Long = 0L,
    val averageDailyValue: Double = 0.0,
    val dateFull: String = "",
    val lastDate: String = "",
    val todayLimit: Double = 0.0,
    val streakCount: Int = 0,
    val streakLastDate: String? = null
)
