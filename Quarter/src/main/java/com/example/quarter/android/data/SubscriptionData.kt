package com.example.quarter.android.data

data class SubscriptionData(
    val premium: Boolean = false,
    val source: String? = null,   // "google", "rustore", "web"
    val expiresAt: Long? = null,
    val purchaseToken: String? = null
)
