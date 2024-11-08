package com.appscentric.donot.touch.myphone.antitheft.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.SKU_ID_ADS
import com.appscentric.donot.touch.myphone.antitheft.utils.NetworkUtils
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class Splash : AppCompatActivity() {
    private val viewModel: AdViewModel by viewModels()
    private val preferencesManager by inject<PreferencesManager>()
    private var ad: InterstitialAd? = null

    private lateinit var consentInformation: ConsentInformation

    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        setupView()
        saveInitialItemDetails()

        // Initialize the BillingClient
        billingClient = BillingClient.newBuilder(this)
            .setListener { _, _ ->
                // Handle purchase updates if needed
            }
            .enablePendingPurchases()
            .build()

        startBillingConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        ad?.fullScreenContentCallback = null
    }

    private fun startBillingConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // BillingClient is ready, query purchases
                    checkIfAdsAreDisabled()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Handle the error - retry connecting if necessary
                proceedToNextScreen()
            }
        })
    }

    private fun checkIfAdsAreDisabled() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var isAdsDisabled = false
                for (purchase in purchasesList) {
                    if (purchase.skus.contains(SKU_ID_ADS)) {
                        isAdsDisabled = true
                        break
                    }
                }

                // Save the status in shared preferences or a singleton class
                saveAdStatus(isAdsDisabled)

            } else {
                // Handle billing query error
                proceedToNextScreen()

            }
        }
    }

    private fun proceedToNextScreen() {
        if (NetworkUtils.isNetworkConnected(this)) {
            consentRequestParameters()
        } else {
            navigateToLanguageScreen()
        }
    }

    private fun saveAdStatus(adsDisabled: Boolean) {
        preferencesManager.setRemoveAdsPurchased(adsDisabled)

        Log.d("TAG_BILLING", "saveAdStatus: $adsDisabled")

        if (NetworkUtils.isNetworkConnected(this) && !adsDisabled) {
            consentRequestParameters()
        } else {
            navigateToLanguageScreen()
        }
    }

    private fun consentRequestParameters() {
        val params = ConsentRequestParameters
            .Builder()
            .build()
        // Create a ConsentInformation object.
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        // request consent info
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this@Splash
                ) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        // Consent not obtained in current session.
                        Log.w("TAG", "${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                    }

                    // Consent has been gathered.
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk()
                    }
                }
            },
            { requestConsentError ->
                // Consent gathering failed.
                Log.w(
                    "TAG_SPLASH",
                    "${requestConsentError.errorCode}: ${requestConsentError.message}"
                )
            })

    }

    private fun initializeMobileAdsSdk() {
        viewModel.apply {
            loadInterstitialAd()
            loadLargeNativeAds()
            loadSmallNativeAds()
            loadSquareNativeAds()
        }
        observeInterAdState()
    }

    private fun navigateToLanguageScreen() {
        val targetActivity = if (preferencesManager.isFirstTimeLaunch) {
            LanguageScreen::class.java
        } else {
            MainActivity::class.java
        }

        val intent = Intent(this, targetActivity).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }


    private fun setupView() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun saveInitialItemDetails() {
        val item = HomeItem(
            R.string.police,
            R.drawable.police,
            R.raw.police,
            R.raw.sound_police,
            0,
            false,
            false
        )

        try {
            preferencesManager.isCharging = false
            preferencesManager.pocketMode = false
            // Your other initialization code here
            Log.d("Splash", "Dependencies injected successfully")
            preferencesManager.saveItemDetails(item, true, true, true, 80, "30s")
        } catch (e: Exception) {
            Log.e("Splash", "Error injecting dependencies", e)
        }
    }

    private fun observeInterAdState() {
        lifecycleScope.launch {
            viewModel.interAdState.collect { adState ->
                when (adState) {
                    is AdViewModel.InterAdState.Loaded ->
                        showScreenWithAd()

                    is AdViewModel.InterAdState.Failed ->
                        showScreenWithAd()

                    is AdViewModel.InterAdState.Idle -> {
                        // Handle the Idle state if necessary
                    }
                }
            }
        }
    }

    private fun showScreenWithAd() {
        ad = InterstitialAdManager.getAd()
        ad?.let { ad ->
            ad.show(this@Splash)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = false
                }

                override fun onAdDismissedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = true
                    if (!isDestroyed) {
                        navigateToLanguageScreen()
                        viewModel.loadInterstitialAd()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    if (!isDestroyed) {
                        navigateToLanguageScreen()
                        viewModel.loadInterstitialAd()
                    }
                }
            }
        } ?: run {
            // Ad not ready, proceed directly to the activity
            navigateToLanguageScreen()
            viewModel.loadInterstitialAd()
        }
    }
}