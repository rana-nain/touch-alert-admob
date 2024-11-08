package com.appscentric.donot.touch.myphone.antitheft.dialog

import android.app.Service
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentSettingsBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.BannerAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.screens.LanguageScreen
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject


private val TAG = SettingsFragment::class.java.simpleName

class SettingsFragment : DialogFragment() {
    override fun getTheme(): Int = R.style.DialogTheme
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val preferencesManager by inject<PreferencesManager>()

    private lateinit var cameraManager: CameraManager
    private lateinit var vibrator: Vibrator
    private var isFlashlightOn: Boolean = false
    private var cameraId: String? = null
    private var flashCoroutine: Job? = null
    private var saveJob: Job? = null

    private var soundId: Int = 0

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            binding.overlaySwitch.isChecked = hasOverlayPermission()
            preferencesManager.isOverlay = hasOverlayPermission()

//            if (hasOverlayPermission()) {
//                PinFragment.newInstance(true).show(parentFragmentManager, "PinFragment")
//            }
        }

    private val flashModes = arrayOf(
        intArrayOf(100), // Default mode (100ms on, 1000ms off)
        intArrayOf(100, 200, 100, 200, 100, 200), // Disco mode (flashes rapidly)
        intArrayOf(500, 500, 500, 500, 500, 500) // SOS mode (SOS pattern)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize CameraManager
        cameraManager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // Initialize Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                requireActivity().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireActivity().getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
        }
        binding.setupToolbar()

        soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)

        if (!preferencesManager.isRemoveAdsPurchased()) loadCollapsibleBannerAd()

        setupCamera()

        updateSliderWidget()

        binding.sliderSensitivity.addOnChangeListener { _, value, _ ->
            saveJob?.cancel() // Cancel the previous job if it exists
            saveJob = saveMappedValueWithDelay(value)
        }

        binding.changeLanguageTextView.setOnClickListener {
             playClickSound()
            startActivity(Intent(requireContext(), LanguageScreen::class.java))
            requireActivity().finish()
        }

        binding.outwardImageView.setOnClickListener {
             playClickSound()
            startActivity(Intent(requireContext(), LanguageScreen::class.java))
            requireActivity().finish()
        }

        binding.privacyPolicyTextView.setOnClickListener {
             playClickSound()
            val url =
                getString(R.string.https_appscentricapps_blogspot_com_2023_11_privacy_policy_html)
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.goProLayout.setOnClickListener { showPremiumPopup() }

        binding.overlaySwitch.isChecked = hasOverlayPermission() && preferencesManager.isOverlay

        binding.pinSwitch.isChecked = preferencesManager.isPinSetup()

        binding.pinSwitch.setOnCheckedChangeListener { _, isChecked ->
             playClickSound()

            if (isChecked) {
                if (!hasOverlayPermission()) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Overlay Permission Required")
                        .setMessage("This app requires overlay permission to function properly.")
                        .setPositiveButton("Grant Permission") { dialog, _ ->
                            // Request overlay permission
                            requestOverlayPermission()
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            // Handle cancel action
                            binding.pinSwitch.isChecked = hasOverlayPermission()
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    // Show the PinFragment when switch is toggled on
                    PinFragment.newInstance(true).show(parentFragmentManager, "PinFragment")
                }
            }
            preferencesManager.setPin(isChecked)
        }

        binding.overlaySwitch.setOnCheckedChangeListener { _, isChecked ->
             playClickSound()
            if (isChecked) {
                // Check if the overlay permission is already granted
                if (!hasOverlayPermission()) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Overlay Permission Required")
                        .setMessage("This app requires overlay permission to function properly.")
                        .setPositiveButton("Grant Permission") { dialog, _ ->
                            // Request overlay permission
                            requestOverlayPermission()
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            // Handle cancel action
                            binding.overlaySwitch.isChecked = hasOverlayPermission()
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    // Overlay permission already granted, proceed with your tasks
                    // For example, enable the overlay feature
                    preferencesManager.isOverlay = true
                }
            } else {
                // Switch turned off, handle accordingly
                preferencesManager.isOverlay = false
            }
        }

        binding.overlaySwitchSound.isChecked = preferencesManager.isTapSound

        binding.overlaySwitchSound.setOnCheckedChangeListener { _, isChecked ->
            playClickSound()
            preferencesManager.isTapSound = isChecked
        }

        binding.autoCloseSwitch.isChecked = preferencesManager.autoCloseApp

        binding.autoCloseSwitch.setOnCheckedChangeListener { _, isChecked ->
            playClickSound()
            preferencesManager.autoCloseApp = isChecked
        }

        updateFlashWidget()

        updateVibrateWidget()

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
             playClickSound()
            val radioButton: RadioButton = view.findViewById(checkedId)
            val selectedText = radioButton.text.toString()
            Log.d(TAG, "Selected text: $selectedText")
            // Call function to handle flashlight based on selected text
            preferencesManager.saveSelectedFlashMode(selectedText)
            handleFlashlight(selectedText)
        }

        binding.vibRadioGroup.setOnCheckedChangeListener { _, checkedId ->
             playClickSound()
            val radioButton: RadioButton = view.findViewById(checkedId)
            val selectedText = radioButton.text.toString()
            Log.d(TAG, "Selected vibration mode: $selectedText")
            // Call function to handle vibration based on selected text
            preferencesManager.saveSelectedVibrateMode(selectedText)

            handleVibration(selectedText)
        }
    }

    private fun showPremiumPopup() {
        val fragmentManager = parentFragmentManager
        val existingFragment =
            fragmentManager.findFragmentByTag(PremiumFragment::class.java.simpleName)

        if (existingFragment == null) {
            val premiumPreviewDialogFragment = PremiumFragment()
            premiumPreviewDialogFragment.show(
                fragmentManager,
                PremiumFragment::class.java.simpleName
            )
        }
    }

    private fun updateSliderWidget() {
        val valueFromPreferences = preferencesManager.getSensitivity()
        Log.d(TAG, "updateSliderWidget: $valueFromPreferences")
        binding.sliderSensitivity.value = valueFromPreferences.toFloat()
    }

    private fun updateVibrateWidget() {
        val selectedFlashMode = preferencesManager.getSelectedVibrate()
        val radioButtonId = when (selectedFlashMode) {
            getString(R.string.defaulttt) -> R.id.radio_vibration_default
            getString(R.string.strong_vibration) -> R.id.radio_vibration_strong
            getString(R.string.heartbeat) -> R.id.radio_vibration_heartbeat
            getString(R.string.ticktock) -> R.id.radio_vibration_tickTock
            else -> -1
        }

        if (radioButtonId != -1) binding.vibRadioGroup.check(radioButtonId) else Log.e(
            TAG,
            "updateFlashWidget: Invalid flash mode selected - $selectedFlashMode"
        )
    }

    private fun updateFlashWidget() {
        val selectedFlashMode = preferencesManager.getSelectedFlash()
        val radioButtonId = when (selectedFlashMode) {
            getString(R.string.defaulttt) -> R.id.radio_default
            getString(R.string.disco_mode) -> R.id.radio_disco_mode
            getString(R.string.sos_mode) -> R.id.radio_sos_mode
            else -> -1
        }

        if (radioButtonId != -1) binding.radioGroup.check(radioButtonId) else Log.e(
            TAG,
            "updateFlashWidget: Invalid flash mode selected - $selectedFlashMode"
        )
    }

    private fun handleVibration(mode: String) {
        val pattern = when (mode) {
            getString(R.string.defaulttt) -> longArrayOf(0, 500) // Default vibration pattern
            getString(R.string.strong_vibration) -> longArrayOf(0, 1000) // Strong vibration pattern
            getString(R.string.heartbeat) -> longArrayOf(
                0,
                300,
                100,
                300,
                1000
            ) // Heartbeat pattern
            getString(R.string.ticktock) -> longArrayOf(0, 500, 100, 500, 1000) // Tick-tock pattern
            else -> {
                Log.d(TAG, "Unknown vibration mode selected")
                return
            }
        }
        // Start vibration
        vibrator.vibrate(pattern, -1)
        preferencesManager.saveSelectedVibrationMode(pattern)
    }

    private fun handleFlashlight(mode: String) {
        when (mode) {
            getString(R.string.defaulttt) -> toggleFlashlight(flashModes[0])
            getString(R.string.disco_mode) -> toggleFlashlight(flashModes[1])
            getString(R.string.sos_mode) -> toggleFlashlight(flashModes[2])
            else -> Log.d(TAG, "Unknown mode selected")
        }
    }

    private fun toggleFlashlight(pattern: IntArray) {
        flashCoroutine?.cancel() // Cancel any ongoing coroutine
        preferencesManager.saveSelectedFlashlightMode(pattern)
        flashCoroutine = CoroutineScope(Dispatchers.Default).launch {

            val startTime = System.currentTimeMillis()
            while (isActive && (System.currentTimeMillis() - startTime) < 100) {
                for (duration in pattern) {
                    withContext(Dispatchers.Main) {
                        if (isActive) {
                            turnOnFlashlight() // Turn on the flashlight
                        }
                    }
                    delay(duration.toLong()) // Delay for the specified duration
                    withContext(Dispatchers.Main) {
                        if (isActive) {
                            turnOffFlashlight() // Turn off the flashlight
                        }
                    }
                    delay(duration.toLong()) // Delay for the specified duration
                }
            }
        }
    }

    private fun setupCamera() {
        val context = context
        if (context != null && context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIdList = cameraManager.cameraIdList
            if (cameraIdList.isNotEmpty()) {
                cameraId = cameraIdList[0] // Use the first available camera
            } else {
                Toast.makeText(context, "No camera available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.bannerContainer.removeAllViews()
        (binding.root as ViewGroup).removeAllViews()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        vibrator.cancel()
        flashCoroutine?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the coroutine when the fragment is destroyed
        vibrator.cancel()
        flashCoroutine?.cancel()
    }


    override fun onDismiss(dialog: DialogInterface) {
        dismissListener?.onDialogDismissed()
        super.onDismiss(dialog)
        // Make sure to turn off the flashlight when the dialog is dismissed
        flashCoroutine?.cancel()
        turnOffFlashlight()
    }

    private fun FragmentSettingsBinding.setupToolbar() {

        toolbar.setNavigationOnClickListener {
             playClickSound()
            dismiss()
        }
    }

    private fun turnOnFlashlight() {
        isFlashlightOn = true
        try {
            cameraId?.let { id ->
                if (isFlashlightAvailable(id)) {
                    cameraManager.setTorchMode(id, true)
                } else {
                    Log.e("NewService", "Flashlight not available for camera: $id")
                }
            }
        } catch (e: CameraAccessException) {
            Log.e("NewService", "Error turning on flashlight: ${e.message}")
        }
    }

    private fun turnOffFlashlight() {
        isFlashlightOn = false
        try {
            cameraId?.let { id ->
                if (isFlashlightAvailable(id)) {
                    cameraManager.setTorchMode(id, false)
                } else {
                    Log.e("NewService", "Flashlight not available for camera: $id")
                }
            }
        } catch (e: CameraAccessException) {
            Log.e("NewService", "Error turning off flashlight: ${e.message}")
        }
    }

    private fun isFlashlightAvailable(cameraId: String): Boolean {
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
            hasFlash == true
        } catch (e: CameraAccessException) {
            Log.e("NewService", "Error accessing camera characteristics: ${e.message}")
            false
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(requireContext())
    }

    private fun requestOverlayPermission() {
        val activity = activity
        if (activity != null && isAdded) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:${activity.packageName}")
            overlayPermissionLauncher.launch(intent)
        } else {
            Log.e("SettingsFragment", "Fragment not attached to activity")
        }
    }

    private fun saveMappedValueWithDelay(value: Float): Job {
        return lifecycleScope.launch {
            delay(500)
            // Save the mapped value here
            preferencesManager.saveSensitivity(value.toInt())
        }
    }

    private fun loadCollapsibleBannerAd() {
        BannerAdManager.getAd()?.let {
            val parent = it.parent as? ViewGroup
            parent?.removeView(it)
            binding.bannerContainer.addView(it)
        } ?: run {
            binding.bannerContainer.visibility = View.GONE
        }
    }

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    interface OnDialogDismissListener {
        fun onDialogDismissed()
    }

    var dismissListener: OnDialogDismissListener? = null
}