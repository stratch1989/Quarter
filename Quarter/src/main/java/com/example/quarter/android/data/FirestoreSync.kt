package com.example.quarter.android.data

import android.content.Context
import android.util.Log
import com.example.quarter.android.HistoryManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object FirestoreSync {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Загружает все локальные данные (SharedPreferences + история) в Firestore.
     * Вызывается при первом входе пользователя.
     */
    fun syncLocalToFirestore(context: Context, uid: String) {
        val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        val budgetData = mapOf(
            "howMany" to getDouble(prefs, "HOW_MANY"),
            "numberOfDays" to prefs.getLong("NUMBER_OF_DAYS", 0L),
            "averageDailyValue" to getDouble(prefs, "AVARAGE_DAILY_VALUE"),
            "dateFull" to (prefs.getString("DATE_FULL", "") ?: ""),
            "lastDate" to (prefs.getString("LAST_DATE", "") ?: ""),
            "todayLimit" to getDouble(prefs, "STRING_KEY"),
            "streakCount" to prefs.getInt("DAY_STREAK", 0),
            "streakLastDate" to prefs.getString("STREAK_LAST_DATE", null)
        )

        val categories = mapOf(
            "selectedEmojis" to (prefs.getString("SELECTED_EMOJIS", "") ?: ""),
            "categoryEmojis" to (prefs.getString("CATEGORY_EMOJIS", "") ?: ""),
            "selectedIncomeEmojis" to (prefs.getString("SELECTED_INCOME_EMOJIS", "") ?: ""),
            "incomeCategoryEmojis" to (prefs.getString("INCOME_CATEGORY_EMOJIS", "") ?: "")
        )

        val historyManager = HistoryManager(context)
        val expenseEntries = historyManager.loadEntries().map { entry ->
            mapOf(
                "amount" to entry.amount,
                "date" to entry.date,
                "timestamp" to entry.timestamp,
                "category" to entry.category
            )
        }
        val incomeEntries = historyManager.loadIncomeEntries().map { entry ->
            mapOf(
                "amount" to entry.amount,
                "date" to entry.date,
                "timestamp" to entry.timestamp,
                "category" to entry.category
            )
        }

        val userDoc = db.collection("users").document(uid)
        val batch = db.batch()
        batch.set(userDoc, mapOf("budget" to budgetData, "categories" to categories), SetOptions.merge())
        batch.set(
            userDoc,
            mapOf(
                "history" to expenseEntries,
                "incomeHistory" to incomeEntries,
                "periodStartTs" to historyManager.getPeriodStartTimestamp()
            ),
            SetOptions.merge()
        )
        batch.commit()
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "syncLocalToFirestore failed", e)
            }
    }

    /**
     * Сохраняет текущие бюджетные данные в Firestore (вызывается из saveData).
     */
    fun saveBudgetToFirestore(uid: String, budgetData: BudgetData) {
        val data = mapOf(
            "budget" to mapOf(
                "howMany" to budgetData.howMany,
                "numberOfDays" to budgetData.numberOfDays,
                "averageDailyValue" to budgetData.averageDailyValue,
                "dateFull" to budgetData.dateFull,
                "lastDate" to budgetData.lastDate,
                "todayLimit" to budgetData.todayLimit,
                "streakCount" to budgetData.streakCount,
                "streakLastDate" to budgetData.streakLastDate
            )
        )
        db.collection("users").document(uid).set(data, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "saveBudgetToFirestore failed", e)
            }
    }

    /**
     * Получает статус подписки из Firestore.
     */
    fun getSubscriptionStatus(uid: String, callback: (SubscriptionData?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val sub = doc.get("subscription") as? Map<String, Any?>
                    if (sub != null) {
                        callback(
                            SubscriptionData(
                                premium = sub["premium"] as? Boolean ?: false,
                                source = sub["source"] as? String,
                                expiresAt = sub["expiresAt"] as? Long,
                                purchaseToken = sub["purchaseToken"] as? String
                            )
                        )
                    } else {
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { callback(null) }
    }

    /**
     * Проверяет, существует ли документ пользователя в Firestore.
     */
    fun checkUserExists(uid: String, callback: (Boolean) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc -> callback(doc.exists()) }
            .addOnFailureListener { callback(false) }
    }

    /**
     * Создаёт профиль пользователя в Firestore.
     */
    fun createProfile(uid: String, email: String) {
        val profile = mapOf(
            "profile" to mapOf(
                "email" to email,
                "createdAt" to System.currentTimeMillis()
            )
        )
        db.collection("users").document(uid).set(profile, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "createProfile failed", e)
            }
    }

    /**
     * Suspend-версия getSubscriptionStatus для использования в корутинах.
     */
    suspend fun getSubscriptionStatus(uid: String): SubscriptionData? {
        return suspendCancellableCoroutine { cont ->
            getSubscriptionStatus(uid) { data ->
                cont.resume(data)
            }
        }
    }

    /**
     * Обновляет статус подписки в Firestore.
     */
    fun updateSubscriptionStatus(uid: String, premium: Boolean, source: String, expiresAt: Long) {
        val data = mapOf(
            "subscription" to mapOf(
                "premium" to premium,
                "source" to source,
                "expiresAt" to expiresAt,
                "updatedAt" to System.currentTimeMillis()
            )
        )
        db.collection("users").document(uid).set(data, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "updateSubscriptionStatus failed", e)
            }
    }

    /**
     * Загружает бюджетные данные из Firestore.
     */
    suspend fun loadBudgetFromFirestore(uid: String): BudgetData? {
        return suspendCancellableCoroutine { cont ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val budget = doc.get("budget") as? Map<String, Any?>
                        if (budget != null) {
                            cont.resume(BudgetData(
                                howMany = (budget["howMany"] as? Number)?.toDouble() ?: 0.0,
                                numberOfDays = (budget["numberOfDays"] as? Number)?.toLong() ?: 0L,
                                averageDailyValue = (budget["averageDailyValue"] as? Number)?.toDouble() ?: 0.0,
                                dateFull = budget["dateFull"] as? String ?: "",
                                lastDate = budget["lastDate"] as? String ?: "",
                                todayLimit = (budget["todayLimit"] as? Number)?.toDouble() ?: 0.0,
                                streakCount = (budget["streakCount"] as? Number)?.toInt() ?: 0,
                                streakLastDate = budget["streakLastDate"] as? String
                            ))
                        } else {
                            cont.resume(null)
                        }
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    private fun getDouble(prefs: android.content.SharedPreferences, key: String): Double {
        return try {
            (prefs.getString(key, "0.0") ?: "0.0").toDouble()
        } catch (e: ClassCastException) {
            prefs.getFloat(key, 0f).toDouble()
        }
    }
}
