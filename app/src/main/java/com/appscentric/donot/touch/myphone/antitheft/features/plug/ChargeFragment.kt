package com.appscentric.donot.touch.myphone.antitheft.features.plug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentChargeBinding
import com.appscentric.donot.touch.myphone.antitheft.features.battery.BatteryFullDetectionFragment
import com.appscentric.donot.touch.myphone.antitheft.features.pocket.PocketFragment
import com.appscentric.donot.touch.myphone.antitheft.features.wifi.WifiDetectionFragment
import com.appscentric.donot.touch.myphone.antitheft.manager.BannerAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.screens.TabbedMainActivity
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.EVENT_DASHBOARD_SETTINGS
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.customFirebaseEvent
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.isServiceRunning
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ChargeFragment : DialogFragment() {
    private var soundId: Int = 0
    private lateinit var binding: FragmentChargeBinding
    private var ad: InterstitialAd? = null
    private val adViewModel: AdViewModel by viewModels()
    override fun getTheme(): Int = R.style.DialogTheme
    private val preferencesManager by inject<PreferencesManager>()

    private val chargingStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "charging_state_changed") {
                val isCharging = intent.getBooleanExtra("isCharging", false)
                updateUI(isCharging) // Update UI based on received state
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChargeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)
        val (homeItem, _, _) = preferencesManager.getAllData()

        with(binding) {
            switch1.setOnCheckedChangeListener { _, isChecked ->
                val isServiceRunning =
                    isServiceRunning(requireContext(), ChargingService::class.java)
                if (!isServiceRunning && isChecked) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.update_plug_out_charging_state))
                        .setMessage(getString(R.string.are_you_sure_you_want_to_enable_plug_out_charging_mode))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes)) { _, _ ->
                            updateChargingState(
                                true
                            )
                        }
                        .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                            dialog.dismiss()
                            switch1.isChecked = false
                        }
                        .show()
                } else if (!isChecked) {
                    updateChargingState(false)
                }
            }

            toolbar.setNavigationOnClickListener {
                playClickSound()
                dismiss()
            }

            clickEvents(binding)

            homeItem.let { item ->
                binding.textViewSelectedSound.text = getString(item.nameResId)
                binding.imageViewSelectedSounds.setImageResource(item.thumbnail)
            }
        }
        checkAndLoadCollapsibleBannerAd()
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(chargingStateReceiver, IntentFilter("charging_state_changed"))
            updateUI(preferencesManager.isCharging) // Initial UI update
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(chargingStateReceiver)
        }
    }

    private fun updateUI(charging: Boolean) {
        if (!isAdded) return
        with(binding) {
            val isServiceRunning = isServiceRunning(requireContext(), ChargingService::class.java)
            switch1.isChecked = isServiceRunning && preferencesManager.isCharging
            updateChargingState(switch1.isChecked)
        }
    }

    private fun updateChargingState(isChecked: Boolean) {
        if (!isAdded) return
        preferencesManager.isCharging = isChecked
        binding.imageView.setImageResource(
            if (isChecked) R.drawable.charge_active else R.drawable.charge_inactive
        )
        binding.textViewStatusTitle.text =
            if (isChecked) getString(R.string.current_status_on) else getString(R.string.current_status_off)
        binding.cardView.setCardBackgroundColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    if (isChecked) R.color.green2 else R.color.gray
                )
            )
        )

        if (isChecked) startChargingService() else stopChargingService()
    }

    private fun startChargingService() {
        if (isAdded && !isServiceRunning(requireContext(), ChargingService::class.java)) {
            requireContext().startService(Intent(requireContext(), ChargingService::class.java))

            if (!preferencesManager.isRemoveAdsPurchased()) {
                showInterstitialAds()
            }
        }
    }

    private fun showInterstitialAds() {
        ad = InterstitialAdManager.getAd()
        ad?.let {
            it.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = false
                }

                override fun onAdDismissedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = true
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                }
            }
            if (isAdded) {
                it.show(requireActivity())
            }
        }
        adViewModel.loadInterstitialAd()
    }

    override fun onDestroyView() {
        ad?.fullScreenContentCallback = null
        ad = null  // Clear ad reference to avoid memory leaks
        super.onDestroyView()
    }


    private fun stopChargingService() {
        if (isAdded) {
            requireContext().stopService(Intent(requireContext(), ChargingService::class.java))
        }
    }

    private fun clickEvents(binding: FragmentChargeBinding) {
        with(binding) {

            setClickListener(batteryCard, batteryButton) {
                showAdWithNavigation(
                    "BatteryFullDetectionFragment",
                    ::BatteryFullDetectionFragment
                )
            }
            setClickListener(pocketCard, pocketButton) {
                showAdWithNavigation(
                    "PocketFragment",
                    ::PocketFragment
                )
            }
            setClickListener(wifiCard, wifiButton) {
                showAdWithNavigation(
                    "WifiDetectionFragment",
                    ::WifiDetectionFragment
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
                    this@ChargeFragment.dismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    this@ChargeFragment.dismiss()
                    return fragmentProvider().show(parentFragmentManager, tag)
                }
            }
            show(requireActivity())
        } ?: fragmentProvider().show(
            parentFragmentManager,
            tag
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