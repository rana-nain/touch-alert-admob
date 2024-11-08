package com.appscentric.donot.touch.myphone.antitheft.features.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentWifiDetectionBinding
import com.appscentric.donot.touch.myphone.antitheft.features.battery.BatteryFullDetectionFragment
import com.appscentric.donot.touch.myphone.antitheft.features.plug.ChargeFragment
import com.appscentric.donot.touch.myphone.antitheft.features.plug.ChargingService
import com.appscentric.donot.touch.myphone.antitheft.features.pocket.PocketFragment
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.screens.TabbedMainActivity
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.isServiceRunning
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.android.ext.android.inject

class WifiDetectionFragment : DialogFragment() {
    private var soundId: Int = 0
    private var ad: InterstitialAd? = null
    private lateinit var binding: FragmentWifiDetectionBinding
    private val preferencesManager by inject<PreferencesManager>()
    override fun getTheme(): Int = R.style.DialogTheme
    val adViewModel: AdViewModel by viewModels()

    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "wifi_state_changed") {
                val isCharging = intent.getBooleanExtra("isWifiModeRunning", false)
                updateChargingState(binding, isCharging) // Update UI based on received state
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWifiDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)
        val (homeItem, _, _) = preferencesManager.getAllData()

        with(binding) {

            toolbar.setNavigationOnClickListener {
                playClickSound()
                dismiss()
            }

            switchWifiMode.isChecked =
                isServiceRunning(requireContext(), WifiMonitorService::class.java)

            updateChargingState(
                binding,
                isServiceRunning(requireContext(), WifiMonitorService::class.java)
            )

            switchWifiMode.setOnCheckedChangeListener { _, isChecked ->
                val isServiceRunning =
                    isServiceRunning(requireContext(), ChargingService::class.java)
                if (!isServiceRunning && isChecked) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.update_wifi_detection_state))
                        .setMessage(getString(R.string.are_you_sure_you_want_to_enable_wifi_detection_mode))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes)) { _, _ ->
                            updateChargingState(
                                binding,
                                true
                            )
                        }
                        .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                            dialog.dismiss()
                            switchWifiMode.isChecked = false
                        }
                        .show()
                } else if (!isChecked) {
                    updateChargingState(binding, false)
                }
            }

            clickEvents(binding)

            homeItem.let { item ->
                binding.textViewSelectedSound.text = getString(item.nameResId)
                binding.imageViewSelectedSounds.setImageResource(item.thumbnail)
            }
        }
    }

    private fun clickEvents(binding: FragmentWifiDetectionBinding) {
        with(binding) {
            setClickListener(pocketCard, pocketButton) {
                showAdWithNavigation(
                    "PocketFragment",
                    ::PocketFragment
                )
            }
            setClickListener(plugCard, plugButton) {
                showAdWithNavigation(
                    "ChargeFragment",
                    ::ChargeFragment
                )
            }
            setClickListener(batteryCard, batteryButton) {
                showAdWithNavigation(
                    "BatteryFullDetectionFragment",
                    ::BatteryFullDetectionFragment
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
                    this@WifiDetectionFragment.dismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    this@WifiDetectionFragment.dismiss()
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


    private fun updateChargingState(
        binding: FragmentWifiDetectionBinding,
        isChecked: Boolean
    ) {
        if (!isAdded) return

        binding.switchWifiMode.isChecked = isChecked

        binding.imageView.setImageResource(
            if (isChecked) R.drawable.ic_wifi else R.drawable.ic_wifi_dark
        )
        binding.textViewStatusTitle.text =
            if (isChecked) getString(R.string.current_status_on) else getString(R.string.current_status_off)

        binding.cardView.setCardBackgroundColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    if (isChecked) R.color.green2 else R.color.card_bg_color
                )
            )
        )

        if (isChecked) startChargingService() else stopChargingService()
    }

    private fun startChargingService() {
        if (isAdded && !isServiceRunning(requireContext(), WifiMonitorService::class.java)) {
            requireContext().startService(Intent(requireContext(), WifiMonitorService::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(wifiStateReceiver, IntentFilter("wifi_state_changed"))
            updateChargingState(
                binding,
                isServiceRunning(requireContext(), WifiMonitorService::class.java)
            ) // Initial UI update
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(wifiStateReceiver)
        }
    }

    private fun stopChargingService() {
        if (isAdded) {
            requireContext().stopService(Intent(requireContext(), WifiMonitorService::class.java))
        }
    }

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ad?.fullScreenContentCallback = null
    }
}