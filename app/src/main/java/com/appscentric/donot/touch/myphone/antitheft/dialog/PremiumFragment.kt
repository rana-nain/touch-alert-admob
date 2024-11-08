package com.appscentric.donot.touch.myphone.antitheft.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentPremiumBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.screens.Splash
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_PREMIMUM
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_WHISTLE
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.SKU_ID_ADS
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.customFirebaseEvent
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PremiumFragment : DialogFragment(), PurchasesUpdatedListener {

    private var soundId: Int = 0
    override fun getTheme(): Int = R.style.DialogTheme
    private var _binding: FragmentPremiumBinding? = null
    private val binding get() = _binding!!

    private var onDismissListener: (() -> Unit)? = null

    private lateinit var billingClient: BillingClient

    private val preferencesManager by inject<PreferencesManager>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.attributes?.windowAnimations = R.style.ZoomDialogAnimation
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)
        // Initialize BillingClient
        billingClient = BillingClient.newBuilder(requireContext())
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        startBillingClientConnection()

        with(binding) {
            closeBtn.setOnClickListener { dismiss() }
            subscribeBtn.setOnClickListener {
                // Disable the button to prevent multiple clicks
                playClickSound()
                subscribeBtn.isEnabled = false
                launchPurchaseFlow()
            }

            oldTextView.paint.flags = Paint.STRIKE_THRU_TEXT_FLAG

            Glide.with(requireContext())
                .load(R.drawable.bg_premium_screen)
                .into(backgroundView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        billingClient.endConnection()
    }

    private fun startBillingClientConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Handle reconnection if needed
            }
        })
    }

    private fun checkExistingPurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.skus.contains(SKU_ID_ADS)) {
                        unlockPremiumFeatures()
                    }
                }
            }
        }
    }

    private fun launchPurchaseFlow() {
        try {
            val productDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(SKU_ID_ADS)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                ).build()

            billingClient.queryProductDetailsAsync(productDetailsParams) { billingResult, productDetailsList ->
                try {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                        val productDetails = productDetailsList[0]
                        val flowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(
                                listOf(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .build()
                                )
                            )
                            .build()

                        billingClient.launchBillingFlow(requireActivity(), flowParams)
                    } else {
                        Log.e(
                            "Billing",
                            "Error fetching product details: ${billingResult.debugMessage}"
                        )
                        showError("Unable to fetch product details. Please try again later.")
                        binding.subscribeBtn.isEnabled = true // Re-enable on error
                    }
                } catch (e: Exception) {
                    Log.e("Billing", "Error during purchase flow: ${e.message}")
                    showError("An error occurred during the purchase process.")
                    binding.subscribeBtn.isEnabled = true // Re-enable on exception
                }
            }
        } catch (e: Exception) {
            Log.e("Billing", "Error launching purchase flow: ${e.message}")
            showError("Unable to start the purchase process. Please try again later.")
            binding.subscribeBtn.isEnabled = true // Re-enable on setup error
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.skus.contains(SKU_ID_ADS)) {
            unlockPremiumFeatures()
        } else {
            preferencesManager.setRemoveAdsPurchased(false)
        }
    }

    private fun unlockPremiumFeatures() {
        binding.subscribeBtn.isEnabled = false
        binding.subscribeBtn.text = "Already Purchased"
        Toast.makeText(requireContext(), "Already Purchased", Toast.LENGTH_SHORT).show()
        preferencesManager.setRemoveAdsPurchased(true)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    refreshApp()
                }
            }
            binding.subscribeBtn.isEnabled = true // Re-enable after successful purchase
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            binding.subscribeBtn.isEnabled = true // Re-enable on cancellation
        } else {
            binding.subscribeBtn.isEnabled = true // Re-enable on other errors
        }
    }

    private fun refreshApp() {
        val intent = Intent(requireContext(), Splash::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()
    }

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }

        lifecycleScope.launch {
            customFirebaseEvent(requireContext(), "EVENT_DASHBOARD_PREMIMUM_PURCHASE")
        }
    }
}
