package com.example.quarter.android.billing

import android.content.Context
import com.example.quarter.android.auth.AuthManager
import com.example.quarter.android.data.FirestoreSync
import kotlinx.coroutines.*

/**
 * Единая точка проверки Premium-статуса.
 * Приоритет: локальный кеш → Firestore → Google Play → RuStore
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PremiumManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PREMIUM = "PREMIUM_STATUS"
        private const val KEY_PREMIUM_SOURCE = "PREMIUM_SOURCE"
        private const val KEY_PREMIUM_EXPIRES = "PREMIUM_EXPIRES_AT"
        private const val KEY_PREMIUM_CHECKED = "PREMIUM_LAST_CHECKED"
        private const val GRACE_PERIOD_MS = 3L * 24 * 60 * 60 * 1000 // 3 дня
        private const val CHECK_INTERVAL_MS = 6L * 60 * 60 * 1000    // 6 часов
    }

    /**
     * Быстрая проверка из кеша (без сети). Подходит для UI.
     */
    fun isPremiumCached(): Boolean {
        val premium = prefs.getBoolean(KEY_PREMIUM, false)
        if (!premium) return false

        val expiresAt = prefs.getLong(KEY_PREMIUM_EXPIRES, 0L)
        val now = System.currentTimeMillis()

        // Подписка ещё активна
        if (expiresAt > now) return true

        // Grace period — 3 дня после истечения (для оффлайн-пользователей)
        if (expiresAt + GRACE_PERIOD_MS > now) return true

        // Истекла давно — сбрасываем
        clearPremiumCache()
        return false
    }

    /**
     * Полная проверка: Firestore → Google Play → RuStore.
     * Вызывать при запуске приложения.
     */
    suspend fun checkPremiumStatus(): PremiumResult {
        // Если недавно проверяли — используем кеш
        val lastChecked = prefs.getLong(KEY_PREMIUM_CHECKED, 0L)
        if (System.currentTimeMillis() - lastChecked < CHECK_INTERVAL_MS && isPremiumCached()) {
            return PremiumResult(
                isPremium = true,
                source = prefs.getString(KEY_PREMIUM_SOURCE, null),
                expiresAt = prefs.getLong(KEY_PREMIUM_EXPIRES, 0L)
            )
        }

        // 1. Проверка через Firestore
        val uid = AuthManager.currentUser?.uid
        if (uid != null) {
            try {
                val subData = FirestoreSync.getSubscriptionStatus(uid)
                if (subData != null && subData.premium) {
                    val expiresAt = subData.expiresAt ?: 0L
                    if (expiresAt > System.currentTimeMillis()) {
                        cachePremium(true, subData.source, expiresAt)
                        return PremiumResult(true, subData.source, expiresAt)
                    }
                }
            } catch (_: Exception) {
                // Firestore недоступен — продолжаем проверку
            }
        }

        // 2. Проверка через Google Play
        val store = StoreDetector.getInstallerStore(context)
        if (store == StoreDetector.STORE_GOOGLE || store == StoreDetector.STORE_UNKNOWN) {
            try {
                val gpResult = checkGooglePlay()
                if (gpResult != null) {
                    cachePremium(true, "google", gpResult)
                    // Обновляем Firestore если залогинен
                    if (uid != null) {
                        withContext(Dispatchers.IO) {
                            FirestoreSync.updateSubscriptionStatus(uid, true, "google", gpResult)
                        }
                    }
                    return PremiumResult(true, "google", gpResult)
                }
            } catch (_: Exception) { }
        }

        // 3. Проверка через RuStore
        if (store == StoreDetector.STORE_RUSTORE) {
            try {
                val rsResult = checkRuStore()
                if (rsResult != null) {
                    cachePremium(true, "rustore", rsResult)
                    if (uid != null) {
                        withContext(Dispatchers.IO) {
                            FirestoreSync.updateSubscriptionStatus(uid, true, "rustore", rsResult)
                        }
                    }
                    return PremiumResult(true, "rustore", rsResult)
                }
            } catch (_: Exception) { }
        }

        // Ничего не найдено — не Premium
        // Но если есть кеш с grace period — не сбрасываем сразу
        if (isPremiumCached()) {
            return PremiumResult(
                isPremium = true,
                source = prefs.getString(KEY_PREMIUM_SOURCE, null),
                expiresAt = prefs.getLong(KEY_PREMIUM_EXPIRES, 0L)
            )
        }

        clearPremiumCache()
        return PremiumResult(false, null, null)
    }

    private suspend fun checkGooglePlay(): Long? {
        return suspendCancellableCoroutine { cont ->
            val billing = BillingManager(context)
            billing.connect { connected ->
                if (connected) {
                    billing.checkExistingSubscription { hasSub ->
                        if (hasSub) {
                            // Подписка есть — ставим expiresAt на 30 дней от сейчас
                            cont.resume(
                                System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
                                null
                            )
                        } else {
                            cont.resume(null, null)
                        }
                    }
                } else {
                    cont.resume(null, null)
                }
            }
        }
    }

    private suspend fun checkRuStore(): Long? {
        return suspendCancellableCoroutine { cont ->
            val billing = RuStoreBillingManager(context)
            billing.checkExistingPurchases { hasPurchase ->
                if (hasPurchase) {
                    cont.resume(
                        System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
                        null
                    )
                } else {
                    cont.resume(null, null)
                }
            }
        }
    }

    private fun cachePremium(premium: Boolean, source: String?, expiresAt: Long) {
        prefs.edit()
            .putBoolean(KEY_PREMIUM, premium)
            .putString(KEY_PREMIUM_SOURCE, source)
            .putLong(KEY_PREMIUM_EXPIRES, expiresAt)
            .putLong(KEY_PREMIUM_CHECKED, System.currentTimeMillis())
            .apply()
    }

    private fun clearPremiumCache() {
        prefs.edit()
            .putBoolean(KEY_PREMIUM, false)
            .remove(KEY_PREMIUM_SOURCE)
            .remove(KEY_PREMIUM_EXPIRES)
            .remove(KEY_PREMIUM_CHECKED)
            .apply()
    }

    data class PremiumResult(
        val isPremium: Boolean,
        val source: String?,
        val expiresAt: Long?
    )
}
