package com.example.quarter.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class HistoryEntry(val amount: Double, val date: String, val timestamp: Long, val category: String? = null, val note: String? = null, val time: String? = null)

class HistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    private val key = "HISTORY"
    private val incomeKey = "INCOME_HISTORY"
    private val periodKey = "PERIOD_START_TS"

    fun addEntry(amount: Double, category: String? = null, note: String? = null, time: String? = null, date: String? = null) {
        val entries = loadEntries().toMutableList()
        entries.add(0, HistoryEntry(amount, date ?: LocalDate.now().toString(), System.currentTimeMillis(), category, note, time))
        saveEntries(entries, key)
    }

    fun addIncomeEntry(amount: Double, category: String? = null, note: String? = null, time: String? = null, date: String? = null) {
        val entries = loadIncomeEntries().toMutableList()
        entries.add(0, HistoryEntry(amount, date ?: LocalDate.now().toString(), System.currentTimeMillis(), category, note, time))
        saveEntries(entries, incomeKey)
    }

    fun removeEntryByTimestamp(timestamp: Long, isIncome: Boolean) {
        val k = if (isIncome) incomeKey else key
        val entries = loadEntriesFromKey(k).toMutableList()
        entries.removeAll { it.timestamp == timestamp }
        saveEntries(entries, k)
    }

    fun loadEntries(): List<HistoryEntry> = loadEntriesFromKey(key)

    fun loadIncomeEntries(): List<HistoryEntry> = loadEntriesFromKey(incomeKey)

    private fun loadEntriesFromKey(storageKey: String): List<HistoryEntry> {
        val json = prefs.getString(storageKey, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<HistoryEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val note = if (obj.has("note")) obj.getString("note") else null
            val time = if (obj.has("time")) obj.getString("time") else null
            list.add(HistoryEntry(
                obj.getDouble("amount"),
                obj.getString("date"),
                obj.optLong("timestamp", 0L),
                obj.optString("category", null).takeIf { !it.isNullOrEmpty() },
                note,
                time
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

    fun removeCurrentEntry(currentIndex: Int, income: Boolean = false): HistoryEntry? {
        val periodTs = getPeriodStartTimestamp()
        val storageKey = if (income) incomeKey else key
        val allEntries = (if (income) loadIncomeEntries() else loadEntries()).toMutableList()
        val currentEntries = allEntries.filter { it.timestamp >= periodTs }
        if (currentIndex < 0 || currentIndex >= currentEntries.size) return null
        val target = currentEntries[currentIndex]
        val globalIndex = allEntries.indexOf(target)
        if (globalIndex == -1) return null
        allEntries.removeAt(globalIndex)
        saveEntries(allEntries, storageKey)
        return target
    }

    private fun saveEntries(entries: List<HistoryEntry>, storageKey: String) {
        val array = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("amount", entry.amount)
            obj.put("date", entry.date)
            obj.put("timestamp", entry.timestamp)
            if (entry.category != null) obj.put("category", entry.category)
            if (entry.note != null) obj.put("note", entry.note)
            if (entry.time != null) obj.put("time", entry.time)
            array.put(obj)
        }
        prefs.edit().putString(storageKey, array.toString()).apply()
    }
}
