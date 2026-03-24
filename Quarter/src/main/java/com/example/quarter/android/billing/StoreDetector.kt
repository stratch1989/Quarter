package com.example.quarter.android.billing

import android.content.Context
import android.os.Build

object StoreDetector {

    const val STORE_GOOGLE = "google"
    const val STORE_RUSTORE = "rustore"
    const val STORE_UNKNOWN = "unknown"

    fun getInstallerStore(context: Context): String {
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
        return when (installer) {
            "com.android.vending" -> STORE_GOOGLE
            "ru.vk.store" -> STORE_RUSTORE
            else -> STORE_UNKNOWN
        }
    }
}
