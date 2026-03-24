package com.example.quarter.android.billing

import DataModel
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.quarter.android.R
import com.example.quarter.android.auth.AuthManager
import com.example.quarter.android.data.FirestoreSync
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SubscriptionFragment : Fragment() {

    private val dataModel: DataModel by activityViewModels()
    private var billingManager: BillingManager? = null
    private var ruStoreBillingManager: RuStoreBillingManager? = null

    private val webPaymentBaseUrl = "https://quarter-budget.web.app/pay.html"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscription, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val priceText = view.findViewById<TextView>(R.id.price_text)
        val statusText = view.findViewById<TextView>(R.id.status_text)
        val subscribeButton = view.findViewById<Button>(R.id.subscribe_button)
        val altPaymentLink = view.findViewById<TextView>(R.id.alt_payment_link)

        view.findViewById<View>(R.id.clickable_background).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Если уже premium
        if (dataModel.isPremium.value == true) {
            statusText.text = "✓ Premium активен"
            statusText.visibility = View.VISIBLE
            subscribeButton.text = "Уже подписан"
            subscribeButton.isEnabled = false
            priceText.visibility = View.GONE
            return
        }

        // Определяем источник установки
        val store = StoreDetector.getInstallerStore(requireContext())

        when (store) {
            StoreDetector.STORE_RUSTORE -> setupRuStore(priceText, statusText, subscribeButton, altPaymentLink)
            else -> setupGooglePlay(priceText, statusText, subscribeButton, altPaymentLink)
        }
    }

    private fun setupGooglePlay(
        priceText: TextView, statusText: TextView,
        subscribeButton: Button, altPaymentLink: TextView
    ) {
        billingManager = BillingManager(requireContext()) { purchase ->
            val uid = AuthManager.currentUser?.uid
            // Обновляем Firestore с токеном покупки
            if (uid != null) {
                FirestoreSync.updateSubscriptionStatus(
                    uid, true, "google",
                    System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                )
            }
            // Кешируем Premium локально
            PremiumGate.init(requireContext())
            activity?.runOnUiThread {
                dataModel.isPremium.value = true
                statusText.text = "✓ Premium активирован!"
                statusText.visibility = View.VISIBLE
                subscribeButton.text = "Уже подписан"
                subscribeButton.isEnabled = false
            }
        }

        billingManager?.connect { connected ->
            if (!connected) return@connect
            billingManager?.querySubscription { details ->
                activity?.runOnUiThread {
                    if (details != null) {
                        val price = billingManager?.getFormattedPrice()
                        priceText.text = "${price ?: "—"} / месяц"
                    } else {
                        priceText.text = "Подписка недоступна"
                        subscribeButton.isEnabled = false
                    }
                }
            }
        }

        subscribeButton.setOnClickListener {
            if (!AuthManager.isLoggedIn) {
                openAuth()
                return@setOnClickListener
            }
            val activity = activity ?: return@setOnClickListener
            billingManager?.launchPurchaseFlow(activity)
        }

        // Альтернативная оплата (ЮKassa через веб)
        altPaymentLink.visibility = View.VISIBLE
        altPaymentLink.setOnClickListener { openWebPayment() }
    }

    private fun setupRuStore(
        priceText: TextView, statusText: TextView,
        subscribeButton: Button, altPaymentLink: TextView
    ) {
        subscribeButton.text = "Подписаться через RuStore"

        ruStoreBillingManager = RuStoreBillingManager(requireContext())
        ruStoreBillingManager?.queryProduct { product ->
            activity?.runOnUiThread {
                if (product != null) {
                    val price = ruStoreBillingManager?.getFormattedPrice()
                    priceText.text = "${price ?: "—"} / месяц"
                } else {
                    priceText.text = "Подписка недоступна"
                    subscribeButton.isEnabled = false
                }
            }
        }

        subscribeButton.setOnClickListener {
            if (!AuthManager.isLoggedIn) {
                openAuth()
                return@setOnClickListener
            }
            ruStoreBillingManager?.launchPurchaseFlow { result ->
                val uid = AuthManager.currentUser?.uid
                if (uid != null) {
                    FirestoreSync.updateSubscriptionStatus(
                        uid, true, "rustore",
                        System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                    )
                }
                activity?.runOnUiThread {
                    dataModel.isPremium.value = true
                    statusText.text = "✓ Premium активирован!"
                    statusText.visibility = View.VISIBLE
                    subscribeButton.text = "Уже подписан"
                    subscribeButton.isEnabled = false
                }
            }
        }

        // Скрываем альтернативную оплату для RuStore (она уже "альтернативный" способ)
        altPaymentLink.visibility = View.GONE
    }

    private fun openAuth() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.place_holder, com.example.quarter.android.auth.AuthFragment.newInstance())
            .addToBackStack(null)
            .commit()
    }

    private fun openWebPayment() {
        if (!AuthManager.isLoggedIn) {
            openAuth()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser ?: return
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val tokenResult = user.getIdToken(true).await()
                val idToken = tokenResult.token ?: return@launch
                val url = "$webPaymentBaseUrl?uid=${user.uid}&idToken=$idToken"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
                val url = "$webPaymentBaseUrl?uid=${user.uid}"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        billingManager?.disconnect()
        billingManager = null
        ruStoreBillingManager = null
    }

    companion object {
        fun newInstance() = SubscriptionFragment()
    }
}
