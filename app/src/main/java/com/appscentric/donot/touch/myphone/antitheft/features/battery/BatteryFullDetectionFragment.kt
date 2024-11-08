package com.appscentric.donot.touch.myphone.antitheft.features.battery

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
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentBatteryFullDetectionBinding
import com.appscentric.donot.touch.myphone.antitheft.features.clap.ClapFragment
import com.appscentric.donot.touch.myphone.antitheft.features.pocket.PocketFragment
import com.appscentric.donot.touch.myphone.antitheft.features.wifi.WifiDetectionFragment
import com.appscentric.donot.touch.myphone.antitheft.manager.BannerAdManager
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
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class BatteryFullDetectionFragment : DialogFragment() {

    private var soundId: Int = 0
    private var ad: InterstitialAd? = null
    private val adViewModel: AdViewModel by viewModels()
    private lateinit var binding: FragmentBatteryFullDetectionBinding
    override fun getTheme(): Int = R.style.DialogTheme
    private val preferencesManager by inject<PreferencesManager>()

    private val batteryFullStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "battery_full_state_changed") {
                val isCharging = intent.getBooleanExtra("isBatteryFullModeRunning", false)
                updateServiceState(binding, isCharging) // Update UI based on received state
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBatteryFullDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val (homeItem, _, _) = preferencesManager.getAllData()

        binding.let { binding ->
            soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)

            with(binding) {
                switchPocketMode.isChecked = preferencesManager.isBatteryFullDetection

                toolbar.setNavigationOnClickListener {
                    playClickSound()
                    dismiss()
                }

                updateServiceState(binding, preferencesManager.isBatteryFullDetection)

                switchPocketMode.setOnCheckedChangeListener { _, isChecked ->
                    val isServiceRunning =
                        isServiceRunning(requireContext(), BatteryFullDetectionService::class.java)
                    if (!isServiceRunning && isChecked) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.update_battery_full_detection_state))
                            .setMessage(getString(R.string.are_you_sure_you_want_to_enable_battery_full_detection_mode))
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.yes)) { _, _ -> updateServiceState(binding, true) }
                            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                                dialog.dismiss()
                                switchPocketMode.isChecked = false
                            }
                            .show()
                    } else if (!isChecked) {
                        updateServiceState(binding, false)
                    }
                }

                clickEvents(binding)

                homeItem.let { item ->
                    binding.textViewSelectedSound.text = getString(item.nameResId)
                    binding.imageViewSelectedSounds.setImageResource(item.thumbnail)
                }
            }
        }
        checkAndLoadCollapsibleBannerAd()
    }

    private fun clickEvents(binding: FragmentBatteryFullDetectionBinding) {
        with(binding) {

            setClickListener(wifiCard, wifiButton) {
                showAdWithNavigation(
                    "WhistleFragment",
                    ::WifiDetectionFragment
                )
            }
            setClickListener(pocketCard, pocketButton) {
                showAdWithNavigation(
                    "PocketFragment",
                    ::PocketFragment
                )
            }
            setClickListener(clapCard, clapButton) {
                showAdWithNavigation(
                    "ClapFragment",
                    ::ClapFragment
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
                    this@BatteryFullDetectionFragment.dismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    this@BatteryFullDetectionFragment.dismiss()
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

    private fun updateServiceState(
        binding: FragmentBatteryFullDetectionBinding,
        isChecked: Boolean
    ) {
        if (!isAdded) return
        preferencesManager.isBatteryFullDetection = isChecked

        binding.imageView.setImageResource(
            if (isChecked) R.drawable.ic_battery_full else R.drawable.ic_battery_full_dark
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

        if (isChecked) startService() else stopService()
    }

    private fun startService() {
        if (isAdded && !isServiceRunning(
                requireContext(),
                BatteryFullDetectionService::class.java
            )
        ) {
            requireContext().startService(
                Intent(
                    requireContext(),
                    BatteryFullDetectionService::class.java
                )
            )

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
        SoundManager.soundPool.unload(soundId)
        super.onDestroyView()
    }

    private fun stopService() {
        if (isAdded) {
            requireContext().stopService(
                Intent(
                    requireContext(),
                    BatteryFullDetectionService::class.java
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(
                    batteryFullStateReceiver,
                    IntentFilter("battery_full_state_changed")
                )
            updateServiceState(
                binding,
                isServiceRunning(requireContext(), BatteryFullDetectionService::class.java)
            ) // Initial UI update
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(batteryFullStateReceiver)
        }
    }

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
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
}