package com.example.quarter.android.billing

import android.content.Context
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.RuStoreBillingClientFactory
import ru.rustore.sdk.billingclient.model.product.Product
import ru.rustore.sdk.billingclient.model.purchase.PaymentResult

class RuStoreBillingManager(context: Context) {

    companion object {
        const val PRODUCT_ID = "quarter_premium_monthly"
        // TODO: заменить на реальный consoleApplicationId из RuStore Console
        private const val CONSOLE_APP_ID = "YOUR_RUSTORE_CONSOLE_APP_ID"
        private const val DEEPLINK_SCHEME = "quarter"
    }

    private val billingClient: RuStoreBillingClient = RuStoreBillingClientFactory.create(
        context = context,
        consoleApplicationId = CONSOLE_APP_ID,
        deeplinkScheme = DEEPLINK_SCHEME
    )

    private var product: Product? = null

    fun queryProduct(callback: (Product?) -> Unit) {
        billingClient.products.getProducts(listOf(PRODUCT_ID))
            .addOnSuccessListener { products ->
                product = products.firstOrNull()
                callback(product)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun launchPurchaseFlow(callback: (PaymentResult) -> Unit) {
        val productId = product?.productId ?: PRODUCT_ID
        billingClient.purchases.purchaseProduct(productId)
            .addOnSuccessListener { result ->
                callback(result)
            }
            .addOnFailureListener {
                // Ошибка покупки
            }
    }

    fun checkExistingPurchases(callback: (Boolean) -> Unit) {
        billingClient.purchases.getPurchases()
            .addOnSuccessListener { purchases ->
                val hasActive = purchases.any { it.productId == PRODUCT_ID }
                callback(hasActive)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun getFormattedPrice(): String? {
        return product?.priceLabel
    }
}
