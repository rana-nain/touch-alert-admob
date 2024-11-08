package com.appscentric.donot.touch.myphone.antitheft.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.ActivityPermissionScreenBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Extension.Companion.colorizePart
import com.appscentric.donot.touch.myphone.antitheft.utils.NetworkUtils
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.showSettingsDialog
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class PermissionScreen : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionScreenBinding
    private val preferencesManager by inject<PreferencesManager>()
    private var soundId: Int = 0

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                // All permissions granted
//                navigateUpToHome()
            } else {
                // Handle permission denied
                // You can iterate through permissions and handle each one accordingly
                permissions.entries.forEach { entry ->
                    val permissionName = entry.key
                    val isGranted = entry.value
                    if (!isGranted) {
                        // Handle denied permission
                        showSettingsDialog(this@PermissionScreen)
                        when (permissionName) {
                            Manifest.permission.RECORD_AUDIO -> {
                                // Handle RECORD_AUDIO permission denial
                            }

                            Manifest.permission.POST_NOTIFICATIONS -> {
                                // Handle POST_NOTIFICATIONS permission denial
                            }
                        }
                    }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        soundId = SoundManager.soundPool.load(this, R.raw.click_sound, 1)

        binding.switchViewCamera.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (permissionsToRequest.all { checkPermission(it) }) {
                    navigateUpToHome()
                } else {
                    requestPermissions(permissionsToRequest)
                }
            }
        }

        binding.textSkip.setOnClickListener {
            if (preferencesManager.isTapSound) playClickSound()
            navigateUpToHome()
        }

        binding.checkPermission.setOnClickListener {
            if (preferencesManager.isTapSound) playClickSound()
            if (permissionsToRequest.all { checkPermission(it) }) {
                navigateUpToHome()
            } else {
                requestPermissions(permissionsToRequest)
            }
        }

        if (NetworkUtils.isNetworkConnected(this@PermissionScreen) && !preferencesManager.isRemoveAdsPurchased()) {
            displayNativeAd()
        } else {
            binding.animationShimmer.visibility = View.GONE
        }

        val text = getString(R.string.header_grant_permission)
        binding.textViewPermission.text = text.colorizePart("Grant", Color.parseColor("#000000"))
        binding.textViewPermission.text =
            text.colorizePart("Permission", Color.parseColor("#6499E9"))
    }

    private fun displayNativeAd() {
        binding.animationShimmer.visibility = View.VISIBLE
        lifecycleScope.launch {
            val adLoader =
                AdLoader.Builder(this@PermissionScreen, getString(R.string.admob_small_native_ids))
                    .forNativeAd { ad: NativeAd ->
                        if (!isDestroyed && !isFinishing) {
                            binding.templateView.apply {
                                setNativeAd(ad)
                                visibility = View.VISIBLE
                                binding.animationShimmer.visibility = View.GONE
                            }
                        }
                    }
                    .withNativeAdOptions(NativeAdOptions.Builder().build())
                    .build()
            withContext(Dispatchers.IO) {
                adLoader.loadAd(AdRequest.Builder().build())
            }
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun navigateUpToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun requestPermissions(permissions: Array<String>) {
        permissionLauncher.launch(permissions)
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun playClickSound() {
        SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionLauncher.unregister()
        binding.templateView.removeAllViews()
        (binding.root as ViewGroup).removeAllViews()
    }
}