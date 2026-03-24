package com.example.quarter.android.billing

import android.content.Context

/**
 * Утилита для проверки доступа к Premium-функциям.
 * Использует кешированный статус для мгновенных проверок.
 */
object PremiumGate {

    private var manager: PremiumManager? = null

    fun init(context: Context) {
        manager = PremiumManager(context.applicationContext)
    }

    /**
     * Быстрая проверка Premium (из кеша, без сети).
     * Использовать для UI-решений: показывать/скрывать Premium-фичи.
     */
    val isPremium: Boolean
        get() = manager?.isPremiumCached() ?: false

    /**
     * Выполняет действие только для Premium-пользователей.
     * Если не Premium — вызывает onLocked.
     */
    inline fun withPremium(action: () -> Unit, onLocked: () -> Unit) {
        if (isPremium) {
            action()
        } else {
            onLocked()
        }
    }
}
