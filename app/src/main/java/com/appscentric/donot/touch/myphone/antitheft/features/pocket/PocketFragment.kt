package com.appscentric.donot.touch.myphone.antitheft.features.pocket

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
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentPocketBinding
import com.appscentric.donot.touch.myphone.antitheft.features.battery.BatteryFullDetectionFragment
import com.appscentric.donot.touch.myphone.antitheft.features.plug.ChargeFragment
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
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PocketFragment : DialogFragment() {

    private lateinit var binding: FragmentPocketBinding
    override fun getTheme(): Int = R.style.DialogTheme
    private val preferencesManager by inject<PreferencesManager>()
    val adViewModel: AdViewModel by viewModels()
    private var ad: InterstitialAd? = null

    private var soundId: Int = 0

    private val pocketStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "pocket_state_changed") {
                val isCharging = intent.getBooleanExtra("isPocketModeRunning", false)
                updateChargingState(binding, isCharging) // Update UI based on received state
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPocketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val (homeItem, _, _) = preferencesManager.getAllData()

        binding.let { binding ->
            soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)

            with(binding) {
                switchPocketMode.isChecked = preferencesManager.pocketMode

                toolbar.setNavigationOnClickListener {
                    if (preferencesManager.isTapSound) playClickSound()
                    dismiss()
                }

                binding.switchPocketMode.isChecked =
                    isServiceRunning(requireContext(), PocketService::class.java)

                updateChargingState(
                    binding,
                    isServiceRunning(requireContext(), PocketService::class.java)
                )

                switchPocketMode.setOnCheckedChangeListener { _, isChecked ->
                    preferencesManager.pocketMode = isChecked
                    updateChargingState(binding, isChecked)
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

    private fun updateChargingState(
        binding: FragmentPocketBinding,
        isChecked: Boolean
    ) {
        if (!isAdded) return

        binding.switchPocketMode.isChecked = isChecked

        binding.imageView.setImageResource(
            if (isChecked) R.drawable.ic_pocket else R.drawable.ic_pocket_dark
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
        if (isAdded && !isServiceRunning(requireContext(), PocketService::class.java)) {
            requireContext().startService(Intent(requireContext(), PocketService::class.java))
        }
    }

    private fun stopChargingService() {
        if (isAdded) {
            requireContext().stopService(Intent(requireContext(), PocketService::class.java))
        }
    }

    private fun clickEvents(binding: FragmentPocketBinding) {
        with(binding) {
            setClickListener(wifiCard, wifiButton) {
                showAdWithNavigation(
                    "WifiDetectionFragment",
                    ::WifiDetectionFragment
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
                    this@PocketFragment.dismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    this@PocketFragment.dismiss()
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

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(pocketStateReceiver, IntentFilter("pocket_state_changed"))
            updateChargingState(
                binding,
                isServiceRunning(requireContext(), PocketService::class.java)
            ) // Initial UI update
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(pocketStateReceiver)
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

    override fun onDestroyView() {
        super.onDestroyView()
        ad?.fullScreenContentCallback = null
    }
}