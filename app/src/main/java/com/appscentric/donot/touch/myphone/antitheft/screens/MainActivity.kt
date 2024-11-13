package com.appscentric.donot.touch.myphone.antitheft.screens

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.ActivityMainBinding
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentExitBottomSheetDialogBinding
import com.appscentric.donot.touch.myphone.antitheft.dialog.PremiumFragment
import com.appscentric.donot.touch.myphone.antitheft.dialog.SettingsFragment
import com.appscentric.donot.touch.myphone.antitheft.features.battery.BatteryFullDetectionFragment
import com.appscentric.donot.touch.myphone.antitheft.features.clap.ClapFragment
import com.appscentric.donot.touch.myphone.antitheft.features.intruder.IntruderSelfieFragment
import com.appscentric.donot.touch.myphone.antitheft.features.plug.ChargeFragment
import com.appscentric.donot.touch.myphone.antitheft.features.pocket.PocketFragment
import com.appscentric.donot.touch.myphone.antitheft.features.prank.PrankFragment
import com.appscentric.donot.touch.myphone.antitheft.features.touch.TouchFragment
import com.appscentric.donot.touch.myphone.antitheft.features.wallpaper.WallpaperFragment
import com.appscentric.donot.touch.myphone.antitheft.features.wifi.WifiDetectionFragment
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.LargeNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.manager.SmallNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_BATTERY
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_CLAP
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_INTRUDER
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_PLUG
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_POCKET
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_PRANK
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_SETTINGS
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_TOUCH
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_WALLPAPER
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_WHISTLE
import com.appscentric.donot.touch.myphone.antitheft.utils.NetworkUtils
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.customFirebaseEvent
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.bumptech.glide.Glide
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val preferencesManager by inject<PreferencesManager>()
    private var soundId: Int = 0
    private lateinit var adview: AdView
    private var exitConfirmationDialog: Dialog? = null
    private var ad: InterstitialAd? = null
    private val adViewModel: AdViewModel by viewModels()

    private var premiumPreviewDialogFragment: PremiumFragment? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = showExitConfirmationDialog()
    }

    private val appUpdateRequestCode = 1234 // Unique request code for the update flow
    private lateinit var appUpdateManager: AppUpdateManager

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
//        enableEdgeToEdge()
        setContentView(binding.root)
        checkAndRequestNotificationPermission()
        setupAppUpdateLauncher()

        if (!preferencesManager.isRemoveAdsPurchased()) loadCollapsibleBannerAd()

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        soundId = SoundManager.soundPool.load(this, R.raw.click_sound, 1)


        with(binding) {
            settings.setOnClickListener { handleSettingsClick() }

            loadImages()

            buttonWallpaper.setOnClickListener { showWallpaperScreenWithAd() }
            wallpaperLayoutWallpaper.setOnClickListener { showWallpaperScreenWithAd() }

            applyContinuousBounceAnimation(animationView)

            buttonPrank.setOnClickListener { showPrankScreenWithAd() }
            prankLayoutPrank.setOnClickListener { showPrankScreenWithAd() }

            buttonPocketMode.setOnClickListener { showPocketModeScreenWithAd() }
            materialCardViewPocketMode.setOnClickListener { showPocketModeScreenWithAd() }

            materialTouchCardView.setOnClickListener { showAlertScreenWithAd() }
            buttonTouchAlert.setOnClickListener { showAlertScreenWithAd() }

            materialCardViewPlug.setOnClickListener { showChargingScreenWithAd() }
            buttonPlug.setOnClickListener { showChargingScreenWithAd() }

            materialCardViewBattery.setOnClickListener { showBatteryScreenWithAd() }
            buttonBattery.setOnClickListener { showBatteryScreenWithAd() }

            materialClapCardView.setOnClickListener { showClapScreenWithAd() }
            buttonClap.setOnClickListener { showClapScreenWithAd() }

            wallpaperLayoutWifi.setOnClickListener { showWhistleScreenWithAd() }
            buttonWifi.setOnClickListener { showWhistleScreenWithAd() }

            materialCardViewIntruder.setOnClickListener { showIntruderSelfieScreenWithAd() }
            buttonIntruder.setOnClickListener { showIntruderSelfieScreenWithAd() }

            animationView.setOnClickListener { showPremiumPopup() }
        }

        if (NetworkUtils.isNetworkConnected(this) && !preferencesManager.isRemoveAdsPurchased()) {
            lifecycleScope.launch {
                delay(1500)
                showPremiumPopup()
            }
        }

        if (!preferencesManager.isRemoveAdsPurchased()) display1NativeAd()
    }

    private fun showWhistleScreenWithAd() {
        playClickSound(EVENT_DASHBOARD_WHISTLE)
        fun navigate() =
            WifiDetectionFragment().show(supportFragmentManager, "WifiDetectionFragment")

        showAdWithNavigation(navigate())
    }

    private fun showClapScreenWithAd() {
        playClickSound(EVENT_DASHBOARD_CLAP)
        fun navigate() =
            ClapFragment().show(supportFragmentManager, "ClapFragment")

        showAdWithNavigation(navigate())
    }

    private fun showBatteryScreenWithAd() {
        playClickSound(EVENT_DASHBOARD_BATTERY)
        fun navigate() =
            BatteryFullDetectionFragment().show(supportFragmentManager, "BatteryFragment")

        showAdWithNavigation(navigate())
    }

    private fun showIntruderSelfieScreenWithAd() {
        playClickSound(EVENT_DASHBOARD_INTRUDER)
        fun navigate() =
            IntruderSelfieFragment().show(supportFragmentManager, "IntruderSelfieFragment")

        showAdWithNavigation(navigate())
    }

    private fun showAdWithNavigation(navigate: Unit) {
        ad = InterstitialAdManager.getAd()
        ad?.apply {
            fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = false
                    return navigate
                }

                override fun onAdDismissedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = true
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    return navigate
                }
            }
            show(this@MainActivity)
        } ?: navigate // If no ad is available, directly show the fragment.

        // Preload the next interstitial ad
        adViewModel.loadInterstitialAd()
    }

    private fun showPocketModeScreenWithAd() {
        playClickSound(EVENT_DASHBOARD_POCKET)
        fun navigate() = PocketFragment().show(supportFragmentManager, "PocketFragment")

        showAdWithNavigation(navigate())
    }

    private fun loadImages() {

        fun loadImage(resourceId: Int, targetView: ImageView) {
            Glide.with(this@MainActivity)
                .load(resourceId)
                .into(targetView)
        }

        with(binding) {
            loadImage(R.drawable.ic_pro, animationView)
//            loadImage(R.drawable.ic_bg_dashboard, background)
            loadImage(R.drawable.ic_touch_card_bg_dashboard, imageViewAlarms)
            loadImage(R.drawable.ic_wallpaper_card_bg_dashboard, imageViewWallpaper)
            loadImage(R.drawable.ic_pocket_card_bg_dashboard, imageViewPocketMode)
            loadImage(R.drawable.ic_plug_card_bg_dashboard, imageViewPlug)
            loadImage(R.drawable.ic_charge_card_bg_dashboard, imageViewBattery)
            loadImage(R.drawable.ic_intruder_selfie, imageViewIntruder)
            loadImage(R.drawable.ic_wifi_bg, imageViewWifi)
            loadImage(R.drawable.ic_clap_card_bg_dashboard, imageViewClap)
        }
    }

    private fun showAlertScreenWithAd() {
        playClickSound(EVENT_DASHBOARD_TOUCH)
        fun navigate() = TouchFragment().show(supportFragmentManager, "TouchFragment")
        showAdWithNavigation(navigate())
    }

    private fun showChargingScreenWithAd() {
        playClickSound(EVENT_DASHBOARD_PLUG)
        fun navigate() = ChargeFragment().show(supportFragmentManager, "ChargeFragment")
        showAdWithNavigation(navigate())
    }

    private fun showPrankScreenWithAd() {
        playClickSound(EVENT_DASHBOARD_PRANK)
        if (preferencesManager.isRemoveAdsPurchased()) {
            PrankFragment().show(supportFragmentManager, "PrankFragment")
        } else {
            showPremiumPopup()
        }
    }

    private fun applyContinuousBounceAnimation(animationView: ImageView) {
        animationView.animate()
            .scaleX(1.5f)  // Zoom in horizontally (1.5 times)
            .scaleY(1.5f)  // Zoom in vertically (1.5 times)
            .setDuration(500)  // Duration of zoom in
            .withEndAction {
                // Once zoomed in, animate the button to zoom out
                animationView.animate()
                    .scaleX(1.2f)  // Reset to original horizontal size
                    .scaleY(1.2f)  // Reset to original vertical size
                    .setDuration(500)  // Duration of zoom out
                    .withEndAction {
                        // Repeat the zoom in/out animation indefinitely
                        applyContinuousBounceAnimation(animationView)
                    }.start()
            }.start()
    }

    private fun showPremiumPopup() {
        val fragmentManager = supportFragmentManager

        // Avoid showing if the state is already saved
        if (fragmentManager.isStateSaved) return

        // Initialize fragment only if it's null
        if (premiumPreviewDialogFragment == null) {
            premiumPreviewDialogFragment = PremiumFragment().apply {
                setOnDismissListener {
                    // Clear reference on dismiss to prevent memory leak
                    premiumPreviewDialogFragment = null
                }
            }
        }

        // Only show if the fragment is not already added
        if (premiumPreviewDialogFragment?.isAdded == false) {
            premiumPreviewDialogFragment?.show(
                fragmentManager,
                PremiumFragment::class.java.simpleName
            )
        }
    }

    private fun showWallpaperScreenWithAd() {
        playClickSound(EVENT_DASHBOARD_WALLPAPER)
        fun navigate() = WallpaperFragment().show(supportFragmentManager, "WallpaperFragment")
        showAdWithNavigation(navigate())
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun loadCollapsibleBannerAd() {

        adview = AdView(this)
        adview.adUnitId = getString(R.string.admob_collapsable_banner_ids)
        val adSize = getAdSize()

        adview.setAdSize(adSize)
        val extras = Bundle()
        extras.putString("collapsible", "bottom")
        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()

        binding.bannerContainer.addView(adview)
        adview.loadAd(adRequest)

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getAdSize(): AdSize {
        val adWidthPixels: Float
        val density = resources.displayMetrics.density

        // Check for Android 11 (API level 30) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            adWidthPixels =
                binding.bannerContainer.width.toFloat().takeIf { it > 0 } ?: bounds.width()
                    .toFloat()
        } else {
            // Fallback for API levels below 30
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            adWidthPixels = binding.bannerContainer.width.toFloat().takeIf { it > 0 }
                ?: displayMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    private fun handleSettingsClick() {
        playClickSound(EVENT_DASHBOARD_SETTINGS)

        val settingsPreviewDialogFragment = SettingsFragment()
        settingsPreviewDialogFragment.show(
            supportFragmentManager, settingsPreviewDialogFragment.tag
        )
    }

    private fun showExitConfirmationDialog() {
        val binding = FragmentExitBottomSheetDialogBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this).setView(binding.root).create()

        dialog.window?.apply {
            // Adjust dialog properties
            setDimAmount(0.9f) // Set background dim amount
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        with(binding) {
            btnCancel.setOnClickListener {
                playClickSound("ExitApp")
                if (!isFinishing) {
                    dialog.dismiss()
                }
            }
            btnExit.setOnClickListener {
                playClickSound("ExitApp")
                if (!isFinishing) {
                    dialog.dismiss()
                    exitProcess(0)
//                    finish()
                }
            }

            if (!preferencesManager.isRemoveAdsPurchased()) {
                LargeNativeAdManager.getAd()?.let { ad ->
                    binding.templateView.apply {
                        setNativeAd(ad)
                        visibility = View.VISIBLE
                    }
                } ?: run {
                    binding.templateView.visibility = View.GONE
                }
            }
        }

        dialog.show()
        exitConfirmationDialog = dialog
    }

    override fun onDestroy() {
        super.onDestroy()
        onBackPressedCallback.remove()
        exitConfirmationDialog?.dismiss()
        ad?.fullScreenContentCallback = null
    }

    private fun playClickSound(eventName: String) {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }

        lifecycleScope.launch {
            customFirebaseEvent(this@MainActivity, eventName)
        }
    }

    private fun setupAppUpdateLauncher() {
        appUpdateManager = AppUpdateManagerFactory.create(this)

        // Get the update info
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Add listener using a weak reference to prevent memory leaks
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        appUpdateRequestCode
                    )
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                    // Handle the exception
                }
            }
        }.addOnFailureListener { exception ->
            exception.printStackTrace()
            // Handle failure to check for updates
        }
    }

    // Call this in `onResume` to resume the update if it was interrupted
    override fun onResume() {
        super.onResume()

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    appUpdateRequestCode
                )
            }
        }
    }

    // Handle update flow result in onActivityResult
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == appUpdateRequestCode) {
            if (resultCode != RESULT_OK) {
                Log.d("AppUpdate", "Update flow failed! Result code: $resultCode")
            }
        }
    }

    private fun display1NativeAd() {
        val ad1 = SmallNativeAdManager.getAd()
        ad1?.let {
            binding.templateView.setNativeAd(it)
            binding.templateView.visibility = View.VISIBLE
        } ?: run {
            binding.templateView.visibility = View.GONE
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, show explanation and request permission
            showPermissionRationale()
        }
    }

    // Show a rationale explaining why the permission is needed, then request permission
    private fun showPermissionRationale() {
        // Check if we should show an explanation to the user
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            // Show an AlertDialog with the rationale
            MaterialAlertDialogBuilder(this)
                .setMessage("This app needs notification permission to show important updates.")
                .setPositiveButton("OK") { _, _ ->
                    // Request the permission after the user acknowledges
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Directly request the permission without an explanation
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Handle the result of the permission request
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) handlePermissionDenied()
    }

    private fun handlePermissionDenied() {
        // Optionally, you can guide the user to manually enable the permission from settings
        MaterialAlertDialogBuilder(this)
            .setMessage("Notification permission is required to receive updates. Please enable it from settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Open the app settings page where users can manually enable the permission
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}