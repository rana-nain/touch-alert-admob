package com.appscentric.donot.touch.myphone.antitheft.features.touch

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentTouchBinding
import com.appscentric.donot.touch.myphone.antitheft.dialog.DeactiveTimerFragment
import com.appscentric.donot.touch.myphone.antitheft.dialog.PremiumFragment
import com.appscentric.donot.touch.myphone.antitheft.features.clap.ClapFragment
import com.appscentric.donot.touch.myphone.antitheft.features.plug.ChargingService
import com.appscentric.donot.touch.myphone.antitheft.features.pocket.PocketFragment
import com.appscentric.donot.touch.myphone.antitheft.features.wifi.WifiDetectionFragment
import com.appscentric.donot.touch.myphone.antitheft.manager.BannerAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.screens.TabbedMainActivity
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.isServiceRunning
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.premiumPopupClickCounter
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.showSettingsDialog
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TouchFragment : DialogFragment(), TimerDialogFragment.OnDialogDismissListener {

    private var soundId: Int = 0
    private var mSwitchSet = 0
    private var ad: InterstitialAd? = null
    private val adViewModel: AdViewModel by viewModels()
    private lateinit var binding: FragmentTouchBinding
    override fun getTheme(): Int = R.style.DialogTheme
    private val preferencesManager by inject<PreferencesManager>()

    private val touchAlertStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "touch_alert_state_changed") {
                val isCharging = intent.getBooleanExtra("isTouchAlertRunning", false)
                updateChargingState(binding, isCharging) // Update UI based on received state
            }
        }
    }

    private val permissionsToRequest = arrayOf(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Check for API level 33
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null // Don't request if API level is below 33
        }
    ).filterNotNull().toTypedArray() // Remove null elements

    private fun requestPermissions(permissions: Array<String>) {
        permissionLauncher.launch(permissions)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                // All permissions granted
                showTimerDialog()
            } else {
                // Handle permission denied
                // You can iterate through permissions and handle each one accordingly
                permissions.entries.forEach { entry ->
                    val permissionName = entry.key
                    val isGranted = entry.value
                    if (!isGranted) {
                        // Handle denied permission
                        showSettingsDialog(requireContext())
                        when (permissionName) {
                            Manifest.permission.POST_NOTIFICATIONS -> {
                                // Handle POST_NOTIFICATIONS permission denial
                            }
                        }
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTouchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)
        val (homeItem, _, _) = preferencesManager.getAllData()

        binding.let { binding ->
            binding.toolbar.setNavigationOnClickListener {
                playClickSound()
                dismiss()
            }

            binding.switchTouchAlert.isChecked =
                isServiceRunning(requireContext(), PhoneService::class.java)

            updateChargingState(
                binding, isServiceRunning(requireContext(), PhoneService::class.java)
            )

            binding.switchTouchAlert.setOnCheckedChangeListener { _, isChecked ->
                if (isAdded) {
                    val isServiceRunning = isServiceRunning(requireContext(), PhoneService::class.java)
                    if (!isServiceRunning && isChecked) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.touch_alert_detection_state))
                            .setMessage(getString(R.string.are_you_sure_you_want_to_enable_touch_alert_detection_mode))
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.yes)) { _, _ -> checkAndRequestPermissions() }
                            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                                dialog.dismiss()
                                binding.switchTouchAlert.isChecked = false
                            }
                            .show()
                    } else if (!isChecked) {
                        updateChargingState(binding, false)
                    }
                }
            }


            clickEvents(binding)

            homeItem.let { item ->
                binding.textViewSelectedSound.text = getString(item.nameResId)
                binding.imageViewSelectedSounds.setImageResource(item.thumbnail)
            }
        }
        checkAndLoadCollapsibleBannerAd()
    }

    private fun clickEvents(binding: FragmentTouchBinding) {
        with(binding) {

            setClickListener(wifiCard, wifiButton) {
                showAdWithNavigation(
                    "WifiDetectionFragment", ::WifiDetectionFragment
                )
            }
            setClickListener(pocketCard, pocketButton) {
                showAdWithNavigation(
                    "PocketFragment", ::PocketFragment
                )
            }
            setClickListener(clapCard, clapButton) {
                showAdWithNavigation(
                    "ClapFragment", ::ClapFragment
                )
            }

            soundsCard.setOnClickListener { navigate() }
        }
    }

    private fun navigate() {
        playClickSound()
        val intent = Intent(requireContext(), TabbedMainActivity::class.java).apply {
            putExtra("dashboard_int_key", 0) // Replace 123 with your Int value
        }
        startActivity(intent)
    }

    private fun setClickListener(vararg views: View, action: () -> Unit) {
        views.forEach { it.setOnClickListener { action() } }
    }

    private fun showTimerDialog() {
        val timerDialogFragment = TimerDialogFragment()
        timerDialogFragment.dismissListener = this
        timerDialogFragment.show(
            parentFragmentManager, timerDialogFragment.tag
        )
    }

    override fun onTimerDialogDismissed() {
        onTimerFinish()
    }

    private fun onTimerFinish() {
        mSwitchSet = 1

        Intent(requireContext(), PhoneService::class.java).apply {
            requireContext().startService(this)
        }

        if (preferencesManager.autoCloseApp) {
            requireActivity().moveTaskToBack(true)
        }
    }

    private fun startChargingService() {
        if (isAdded && !isServiceRunning(requireContext(), PhoneService::class.java)) {
            toggleClapDetectionService()
//            requireContext().startService(Intent(requireContext(), PhoneService::class.java))
//
//            if (!preferencesManager.isRemoveAdsPurchased()) {
//                showInterstitialAds()
//            }

//            val filter = IntentFilter("STOP_SERVICE_ACTION")
//            ContextCompat.registerReceiver(
//                requireContext(),
//                notificationReceiver,
//                filter,
//                ContextCompat.RECEIVER_NOT_EXPORTED
//            )
        }
    }


    private fun checkPermission(permission: String): Boolean {
        return if (isAdded) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    private fun toggleClapDetectionService() {
        if (!isServiceRunning(requireContext(), PhoneService::class.java)) {
            showTimerDialog()
        } else {
            stopClapDetectionService()
        }
    }

    private fun stopClapDetectionService() {
        mSwitchSet = 0
        Intent(requireContext(), PhoneService::class.java).apply {
            requireContext().stopService(this)
        }

        val chargingServiceIntent = Intent(context, ChargingService::class.java)
        if (isServiceRunning(
                requireContext(), ChargingService::class.java
            ) && preferencesManager.isChargingRunningSet
        ) {
            requireContext().stopService(chargingServiceIntent)
        }

        if (!preferencesManager.isRemoveAdsPurchased()) {
            ad = InterstitialAdManager.getAd()
            val deactivateTimerFragment = DeactiveTimerFragment()

            ad?.let {
                it.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Constants.SHOW_APP_OPEN = false
                        showDeactiveTimerFragment(deactivateTimerFragment)
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Constants.SHOW_APP_OPEN = true
                        premiumPopupClickCounter++
                        // Show the premium popup after the 5th dismissal and then every 10th dismissal
                        if (premiumPopupClickCounter == 5 || premiumPopupClickCounter % 10 == 5) {
                            showPremiumPopup()
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Constants.SHOW_APP_OPEN = true
                        showDeactiveTimerFragment(deactivateTimerFragment)
                    }
                }
                it.show(requireActivity())
            } ?: showDeactiveTimerFragment(deactivateTimerFragment)

            adViewModel.loadInterstitialAd()
        }

//        binding.apply {
//            if (!selectedItem!!.isSound) binding.animationView.pauseAnimation()
//            button.text = getString(R.string.tap_to_active)
//            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.my_primary))
//        }
    }

    private fun showPremiumPopup() {
        val fragmentManager = parentFragmentManager
        val existingFragment =
            fragmentManager.findFragmentByTag(PremiumFragment::class.java.simpleName)

        if (existingFragment == null) {
            val premiumPreviewDialogFragment = PremiumFragment()
            premiumPreviewDialogFragment.show(
                fragmentManager, PremiumFragment::class.java.simpleName
            )
        }
    }

    private fun showDeactiveTimerFragment(fragment: DeactiveTimerFragment) {
        if (isAdded && activity != null && !requireActivity().isFinishing) {
            fragment.show(parentFragmentManager, "DeactivateTimerFragment")
        }
    }

    private fun updateChargingState(
        binding: FragmentTouchBinding, isChecked: Boolean
    ) {
        if (!isAdded) return

        binding.switchTouchAlert.isChecked = isChecked

        binding.imageView.setImageResource(
            if (isChecked) R.drawable.ic_touch else R.drawable.ic_touch_dark
        )
        binding.textViewStatusTitle.text =
            if (isChecked) getString(R.string.current_status_on) else getString(R.string.current_status_off)
        binding.cardView.setCardBackgroundColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(), if (isChecked) R.color.green2 else R.color.card_bg_color
                )
            )
        )

        if (isChecked) startChargingService() else stopChargingService()
    }

    private fun stopChargingService() {
        if (isAdded) {
            requireContext().stopService(Intent(requireContext(), PhoneService::class.java))
        }
    }

    private fun checkAndRequestPermissions() {
        if (permissionsToRequest.all { checkPermission(it) }) {
            updateChargingState(binding, true)
        } else {
            updateChargingState(binding, false)
            requestPermissions(permissionsToRequest)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        permissionLauncher.unregister()
        ad?.fullScreenContentCallback = null
//        requireContext().unregisterReceiver(notificationReceiver)
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                touchAlertStateReceiver, IntentFilter("touch_alert_state_changed")
            )
            updateChargingState(
                binding, isServiceRunning(requireContext(), PhoneService::class.java)
            ) // Initial UI update
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(touchAlertStateReceiver)
        }
    }

    private fun showAdWithNavigation(tag: String, fragmentProvider: () -> DialogFragment) {
        playClickSound()
        ad = InterstitialAdManager.getAd()
        ad?.apply {
            fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = false
                    return fragmentProvider().show(parentFragmentManager, tag)
                }

                override fun onAdDismissedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = true
                    this@TouchFragment.dismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    this@TouchFragment.dismiss()
                    return fragmentProvider().show(parentFragmentManager, tag)
                }
            }
            show(requireActivity())
        } ?: fragmentProvider().show(
            parentFragmentManager, tag
        ) // If no ad is available, directly show the fragment.

        // Preload the next interstitial ad
        adViewModel.loadInterstitialAd()
    }

    private fun checkAndLoadCollapsibleBannerAd() {
        if (!preferencesManager.isRemoveAdsPurchased()) loadCollapsibleBannerAd()
    }

    private fun loadCollapsibleBannerAd() {
        BannerAdManager.getAd()?.let {
            updateAdView(it)
        } ?: run {
            lifecycleScope.launch {
                adViewModel.bannerView.collect { newAdView ->
                    newAdView?.let { updateAdView(it) }
                }
            }
            adViewModel.loadAd()
        }
    }

    private fun updateAdView(adView: View) {
        val parent = adView.parent as? ViewGroup
        parent?.removeView(adView) // Remove adView from its current parent if necessary
        binding.bannerContainer.addView(adView)
    }


    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }
}