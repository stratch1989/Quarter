package com.example.quarter.android.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingManager : PurchasesUpdatedListener {

    private val context: Context
    private val onPurchaseComplete: (Purchase) -> Unit

    constructor(context: Context, onPurchaseComplete: (Purchase) -> Unit) {
        this.context = context
        this.onPurchaseComplete = onPurchaseComplete
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
    }

    /** Конструктор для проверки без обработки покупок (PremiumManager) */
    constructor(context: Context) {
        this.context = context
        this.onPurchaseComplete = {}
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
    }

    companion object {
        const val PRODUCT_ID = "quarter_premium_monthly"
    }

    private var billingClient: BillingClient

    private var productDetails: ProductDetails? = null

    fun connect(onConnected: (Boolean) -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                onConnected(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
            override fun onBillingServiceDisconnected() {
                // Можно реконнектиться при необходимости
            }
        })
    }

    fun querySubscription(callback: (ProductDetails?) -> Unit) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                productDetails = productDetailsList[0]
                callback(productDetails)
            } else {
                callback(null)
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity): Boolean {
        val details = productDetails ?: return false

        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return false

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        onPurchaseComplete(purchase)
                    }
                }
            } else {
                onPurchaseComplete(purchase)
            }
        }
    }

    fun checkExistingSubscription(callback: (Boolean) -> Unit) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActive = purchases.any {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    it.products.contains(PRODUCT_ID)
                }
                callback(hasActive)
            } else {
                callback(false)
            }
        }
    }

    fun getFormattedPrice(): String? {
        return productDetails?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
            ?.formattedPrice
    }

    fun disconnect() {
        billingClient.endConnection()
    }
}
