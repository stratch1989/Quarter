package com.example.quarter.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class HistoryEntry(val amount: Double, val date: String)

class HistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    private val key = "HISTORY"

    fun addEntry(amount: Double) {
        val entries = loadEntries().toMutableList()
        entries.add(0, HistoryEntry(amount, LocalDate.now().toString()))
        saveEntries(entries)
    }

    fun loadEntries(): List<HistoryEntry> {
        val json = prefs.getString(key, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<HistoryEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(HistoryEntry(obj.getDouble("amount"), obj.getString("date")))
        }
        return list
    }

    fun removeEntry(index: Int): HistoryEntry? {
        val entries = loadEntries().toMutableList()
        if (index < 0 || index >= entries.size) return null
        val removed = entries.removeAt(index)
        saveEntries(entries)
        return removed
    }

    private fun saveEntries(entries: List<HistoryEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("amount", entry.amount)
            obj.put("date", entry.date)
            array.put(obj)
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
