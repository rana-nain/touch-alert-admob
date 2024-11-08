package com.appscentric.donot.touch.myphone.antitheft.features.clap

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentClapBinding
import com.appscentric.donot.touch.myphone.antitheft.features.pocket.PocketFragment
import com.appscentric.donot.touch.myphone.antitheft.features.touch.TouchFragment
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

class ClapFragment : DialogFragment() {
    private var soundId: Int = 0
    private var ad: InterstitialAd? = null
    private val adViewModel: AdViewModel by viewModels()
    private lateinit var binding: FragmentClapBinding
    override fun getTheme(): Int = R.style.DialogTheme
    private val preferencesManager by inject<PreferencesManager>()

    private val clapStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "clap_state_changed") {
                val isCharging = intent.getBooleanExtra("isClapModeRunning", false)
                updateChargingState(binding, isCharging) // Update UI based on received state
            }
        }
    }

    private val permissionsToRequest = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Manifest.permission.FOREGROUND_SERVICE
        } else {
            null
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Check for API level 33
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    ).filterNotNull().toTypedArray() // Remove null elements

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                // All permissions granted
                updateChargingState(binding, true)
            } else {
                // Handle permission denied
                showSettingsDialog()
                binding.switchPocketMode.isChecked = false
                permissions.entries.forEach { entry ->
                    val permissionName = entry.key
                    val isGranted = entry.value
                    if (!isGranted) {
                        // Handle denied permission
                        when (permissionName) {
                            Manifest.permission.RECORD_AUDIO -> {
                                // Handle RECORD_AUDIO permission denial
                            }

                            Manifest.permission.FOREGROUND_SERVICE -> {
                                // Handle FOREGROUND_SERVICE permission denial (New addition)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    // Show a Toast for FOREGROUND_SERVICE permission denial
                                    Toast.makeText(
                                        requireContext(),
                                        "Foreground Service permission required",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            Manifest.permission.POST_NOTIFICATIONS -> {
                                // Handle POST_NOTIFICATIONS permission denial
                            }
                        }
                    }
                }
            }
        }

    private fun requestPermissions(permissions: Array<String>) {
        permissionLauncher.launch(permissions)
    }

    private fun checkPermission(permission: String): Boolean {
        return if (isAdded) {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClapBinding.inflate(inflater, container, false)
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

//            binding.switchPocketMode.isChecked =
//                isServiceRunning(requireContext(), AudioCaptureService::class.java)

            updateChargingState(
                binding,
                isServiceRunning(requireContext(), AudioCaptureService::class.java)
            )

            binding.switchPocketMode.setOnCheckedChangeListener { _, isChecked ->
                val isServiceRunning =
                    isServiceRunning(requireContext(), AudioCaptureService::class.java)
                if (!isServiceRunning && isChecked) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.update_clap_detection_state))
                        .setMessage(getString(R.string.are_you_sure_you_want_to_enable_clap_detection_mode))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.yes)) { _, _ -> checkAndRequestPermissions() }
                        .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                            dialog.dismiss()
                            binding.switchPocketMode.isChecked = false
                        }
                        .show()
                } else if (!isChecked) updateChargingState(binding, false)
            }

            clickEvents(binding)

            homeItem.let { item ->
                binding.textViewSelectedSound.text = getString(item.nameResId)
                binding.imageViewSelectedSounds.setImageResource(item.thumbnail)
            }
        }
        checkAndLoadCollapsibleBannerAd()
    }

    private fun clickEvents(binding: FragmentClapBinding) {
        with(binding) {
            setClickListener(touchCard, touchButton) {
                showAdWithNavigation(
                    "TouchFragment",
                    ::TouchFragment
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
                    this@ClapFragment.dismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    this@ClapFragment.dismiss()
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

    private fun checkAndRequestPermissions() {
        if (permissionsToRequest.all { checkPermission(it) }) {
            updateChargingState(binding, true)
        } else {
            updateChargingState(binding, false)
            requestPermissions(permissionsToRequest)
        }
    }

    private fun startChargingService() {
        if (isAdded && !isServiceRunning(requireContext(), AudioCaptureService::class.java)) {
            requireContext().startService(Intent(requireContext(), AudioCaptureService::class.java))

            if (!preferencesManager.isRemoveAdsPurchased()) {
                showInterstitialAds()
            }
        }
    }

    private fun updateChargingState(
        binding: FragmentClapBinding,
        isChecked: Boolean
    ) {
        if (!isAdded) return

        binding.switchPocketMode.isChecked = isChecked

        binding.imageView.setImageResource(
            if (isChecked) R.drawable.ic_clap else R.drawable.ic_clap_dark
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

    private fun stopChargingService() {
        if (isAdded) {
            requireContext().stopService(Intent(requireContext(), AudioCaptureService::class.java))
        }
    }

    override fun onDestroyView() {
        ad?.fullScreenContentCallback = null
        ad = null  // Clear ad reference to avoid memory leaks
        super.onDestroyView()
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

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(clapStateReceiver, IntentFilter("clap_state_changed"))
            updateChargingState(
                binding,
                isServiceRunning(requireContext(), AudioCaptureService::class.java)
            ) // Initial UI update
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(clapStateReceiver)
        }
    }


    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.permission_required))
            setMessage(getString(R.string.this_app_needs_permission_to_use_this_feature_you_can_grant_them_in_app_settings))
            setPositiveButton(getString(R.string.app_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            setNegativeButton("Cancel") { dialog, _ ->
                updateChargingState(binding, false)
                dialog.dismiss()
            }
            create().show()
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