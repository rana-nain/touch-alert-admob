package com.appscentric.donot.touch.myphone.antitheft.features.whistle

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
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentWhistleBinding
import com.appscentric.donot.touch.myphone.antitheft.features.clap.ClapFragment
import com.appscentric.donot.touch.myphone.antitheft.features.pocket.PocketFragment
import com.appscentric.donot.touch.myphone.antitheft.features.touch.TouchFragment
import com.appscentric.donot.touch.myphone.antitheft.manager.BannerAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.screens.TabbedMainActivity
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class WhistleFragment : DialogFragment() {
    private lateinit var binding: FragmentWhistleBinding
    private var ad: InterstitialAd? = null
    override fun getTheme(): Int = R.style.DialogTheme
    private val preferencesManager by inject<PreferencesManager>()
    val adViewModel: AdViewModel by viewModels()
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

    private val whistleStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "whistle_full_state_changed") {
                val isCharging = intent.getBooleanExtra("isWhistleFullModeRunning", false)
                updateChargingState(binding, isCharging) // Update UI based on received state
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                // All permissions granted
                updateChargingState(binding, true)
            } else {
                // Handle permission denied
                showSettingsDialog()
                binding.switchWhistleMode.isChecked = false
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
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWhistleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val (homeItem, _, _) = preferencesManager.getAllData()

        binding.let { binding ->
            binding.toolbar.setNavigationOnClickListener { dismiss() }
        }

        checkAndLoadCollapsibleBannerAd()
    }

    private fun clickEvents(binding: FragmentWhistleBinding) {
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
        val intent = Intent(requireContext(), TabbedMainActivity::class.java).apply {
            putExtra("dashboard_int_key", 0) // Replace 123 with your Int value
        }
        startActivity(intent)
    }

    private fun setClickListener(vararg views: View, action: () -> Unit) {
        views.forEach { it.setOnClickListener { action() } }
    }

    private fun showAdWithNavigation(tag: String, fragmentProvider: () -> DialogFragment) {
        ad = InterstitialAdManager.getAd()
        ad?.apply {
            fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = false
                    return fragmentProvider().show(parentFragmentManager, tag)
                }

                override fun onAdDismissedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = true
                    this@WhistleFragment.dismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    this@WhistleFragment.dismiss()
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

    override fun onDestroyView() {
        super.onDestroyView()
        ad?.fullScreenContentCallback = null
    }

    private fun checkAndRequestPermissions() {
        if (permissionsToRequest.all { checkPermission(it) }) {
            updateChargingState(binding, true)
        } else {
            updateChargingState(binding, false)
            requestPermissions(permissionsToRequest)
        }
    }

    private fun updateChargingState(
        binding: FragmentWhistleBinding,
        isChecked: Boolean
    ) {
        if (!isAdded) return

        binding.imageView.setImageResource(
            if (isChecked) R.drawable.ic_whistle else R.drawable.ic_whistle_dark
        )
        binding.textViewStatusTitle.text =
            if (isChecked) "Current Status : ON" else "Current Status : OFF"
        binding.cardView.setCardBackgroundColor(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    if (isChecked) R.color.green2 else R.color.card_bg_color
                )
            )
        )
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(
                    whistleStateReceiver,
                    IntentFilter("whistle_full_state_changed")
                )
        }
    }

    override fun onPause() {
        super.onPause()
        if (isAdded) {
            LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(whistleStateReceiver)
        }
    }

    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.Theme_DontTouchMyPhone_DarkDialog
        ).apply {
            setTitle("Permission Required")
            setMessage("This app needs permission to use this feature. You can grant them in app settings.")
            setPositiveButton("App Settings") { _, _ ->
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