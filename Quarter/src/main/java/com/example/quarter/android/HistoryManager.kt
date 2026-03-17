package com.example.quarter.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class HistoryEntry(val amount: Double, val date: String, val timestamp: Long)

class HistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    private val key = "HISTORY"
    private val periodKey = "PERIOD_START_TS"

    fun addEntry(amount: Double) {
        val entries = loadEntries().toMutableList()
        entries.add(0, HistoryEntry(amount, LocalDate.now().toString(), System.currentTimeMillis()))
        saveEntries(entries)
    }

    fun loadEntries(): List<HistoryEntry> {
        val json = prefs.getString(key, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<HistoryEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(HistoryEntry(
                obj.getDouble("amount"),
                obj.getString("date"),
                obj.optLong("timestamp", 0L)
            ))
        }
        return list
    }

    fun getPeriodStartTimestamp(): Long {
        return prefs.getLong(periodKey, 0L)
    }

    fun updatePeriodStart() {
        prefs.edit().putLong(periodKey, System.currentTimeMillis()).apply()
    }

    fun removeCurrentEntry(currentIndex: Int): HistoryEntry? {
        val periodTs = getPeriodStartTimestamp()
        val allEntries = loadEntries().toMutableList()
        val currentEntries = allEntries.filter { it.timestamp >= periodTs }
        if (currentIndex < 0 || currentIndex >= currentEntries.size) return null
        val target = currentEntries[currentIndex]
        val globalIndex = allEntries.indexOf(target)
        if (globalIndex == -1) return null
        allEntries.removeAt(globalIndex)
        saveEntries(allEntries)
        return target
    }

    private fun saveEntries(entries: List<HistoryEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("amount", entry.amount)
            obj.put("date", entry.date)
            obj.put("timestamp", entry.timestamp)
            array.put(obj)
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
