package com.appscentric.donot.touch.myphone.antitheft.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingViewModel(
    application: Application,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application), PurchasesUpdatedListener {

    private val context = application.applicationContext
    private lateinit var billingClient: BillingClient

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        startBillingConnection()
    }

    private fun startBillingConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryAvailableProducts()
                } else {
                    _purchaseState.value = PurchaseState.Error("Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Handle reconnection logic if needed
            }
        })
    }

    private fun queryAvailableProducts() {
        val skuList = listOf("remove_ads")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                        _purchaseState.value = PurchaseState.ProductsAvailable(skuDetailsList)
                    } else {
                        _purchaseState.value = PurchaseState.Error("Failed to query products: ${billingResult.debugMessage}")
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            _purchaseState.value = PurchaseState.Error("Purchase update failed: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            // Update SharedPreferences
                            preferencesManager.setRemoveAdsPurchased(true)
                            _purchaseState.value = PurchaseState.Purchased(purchase)
                        } else {
                            _purchaseState.value = PurchaseState.Error("Failed to acknowledge purchase: ${billingResult.debugMessage}")
                        }
                    }
                }
            }
        }
    }

    fun isRemoveAdsPurchased(): Boolean {
        return preferencesManager.isRemoveAdsPurchased()
    }

    override fun onCleared() {
        super.onCleared()
        billingClient.endConnection()
    }

    fun initiatePurchase() {
        val skuDetails = purchaseState.value.let { state ->
            if (state is PurchaseState.ProductsAvailable) {
                state.products.find { it.sku == "remove_ads" }
            } else null
        }

        skuDetails?.let {
            val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(it)
                .build()
            billingClient.launchBillingFlow(getApplication(), flowParams)
        }
    }
}

sealed class PurchaseState {
    data object Idle : PurchaseState()
    data class ProductsAvailable(val products: List<SkuDetails>) : PurchaseState()
    data class Purchased(val purchase: Purchase) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}