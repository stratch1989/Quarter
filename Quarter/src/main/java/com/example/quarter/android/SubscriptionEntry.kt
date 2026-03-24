package com.example.quarter.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SubscriptionEntry(
    val amount: Double,
    val intervalDays: Int,
    val intervalType: String,   // "daily", "weekly", "monthly", "custom"
    val category: String?,
    var lastChargeDate: String,  // ISO дата
    val createdAt: Long
) {
    fun getDisplayInterval(): String {
        return when (intervalType) {
            "daily" -> "день"
            "weekly" -> "неделю"
            "monthly" -> "месяц"
            "custom" -> "$intervalDays дн."
            else -> "$intervalDays дн."
        }
    }
}

class SubscriptionManager(context: Context) {
    private val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    private val key = "SUBSCRIPTIONS"

    fun loadSubscriptions(): MutableList<SubscriptionEntry> {
        val json = prefs.getString(key, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<SubscriptionEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                SubscriptionEntry(
                    amount = obj.getDouble("amount"),
                    intervalDays = obj.getInt("intervalDays"),
                    intervalType = obj.getString("intervalType"),
                    category = obj.optString("category", null).takeIf { !it.isNullOrEmpty() },
                    lastChargeDate = obj.getString("lastChargeDate"),
                    createdAt = obj.getLong("createdAt")
                )
            )
        }
        return list
    }

    fun saveSubscriptions(subscriptions: List<SubscriptionEntry>) {
        val array = JSONArray()
        for (entry in subscriptions) {
            val obj = JSONObject()
            obj.put("amount", entry.amount)
            obj.put("intervalDays", entry.intervalDays)
            obj.put("intervalType", entry.intervalType)
            if (entry.category != null) obj.put("category", entry.category)
            obj.put("lastChargeDate", entry.lastChargeDate)
            obj.put("createdAt", entry.createdAt)
            array.put(obj)
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    /**
     * Проверяет и списывает просроченные подписки.
     * Возвращает суммарное списание.
     */
    fun processCharges(
        subscriptions: MutableList<SubscriptionEntry>,
        historyManager: HistoryManager
    ): Double {
        val today = LocalDate.now()
        var totalCharged = 0.0

        for (sub in subscriptions) {
            val lastCharge = LocalDate.parse(sub.lastChargeDate)
            val daysSince = ChronoUnit.DAYS.between(lastCharge, today).toInt()
            val chargeCount = daysSince / sub.intervalDays

            if (chargeCount > 0) {
                for (i in 0 until chargeCount) {
                    historyManager.addEntry(sub.amount, sub.category)
                    totalCharged += sub.amount
                }
                // Обновляем дату последнего списания
                val newLastCharge = lastCharge.plusDays((chargeCount * sub.intervalDays).toLong())
                sub.lastChargeDate = newLastCharge.toString()
            }
        }

        if (totalCharged > 0) {
            saveSubscriptions(subscriptions)
        }
        return totalCharged
    }

    companion object {
        fun intervalDaysFor(type: String, customDays: Int = 1, customUnit: String = "days"): Int {
            return when (type) {
                "daily" -> 1
                "weekly" -> 7
                "monthly" -> 30
                "custom" -> when (customUnit) {
                    "weeks" -> customDays * 7
                    "months" -> customDays * 30
                    else -> customDays
                }
                else -> 1
            }
        }
    }
}
