package com.appscentric.donot.touch.myphone.antitheft.features.intruder

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentIntruderSelfieBinding
import com.appscentric.donot.touch.myphone.antitheft.features.intruder.dialog.FullScreenImageDialogFragment
import com.appscentric.donot.touch.myphone.antitheft.features.intruder.dialog.PermissionsBottomSheetFragment
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.isServiceRunning
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class IntruderSelfieFragment : DialogFragment(), CapturedImageAdapter.OnImageClickListener {

    private lateinit var binding: FragmentIntruderSelfieBinding
    override fun getTheme(): Int = R.style.DialogTheme

    private val preferencesManager by inject<PreferencesManager>()

    private val imageViewModel: ImageViewModel by viewModels()
    private lateinit var imageAdapter: CapturedImageAdapter

    private val deviceAdminComponent by lazy {
        ComponentName(
            requireContext(),
            MyDeviceAdminReceiver::class.java
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIntruderSelfieBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { dismiss() }

        updateChargingState(
            binding,
            preferencesManager.intruderSelfieMode
        )

        binding.switchScreenAdmin.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) disableDeviceAdmin()
        }

        binding.switchSelfieMode.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.intruderSelfieMode = isChecked

            val isServiceRunning =
                isServiceRunning(requireContext(), IntruderService::class.java)
            if (!isServiceRunning && isChecked) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Update Intruder Selfie State")
                    .setMessage("Are you sure you want to enable Intruder Selfie mode?")
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes)) { _, _ ->

                        if (arePermissionsGranted()){
                            updateChargingState(
                                binding,
                                true
                            )
                        }else{
                            showPermissionBottomSheetDialog()
                            binding.switchSelfieMode.isChecked = false
                        }

                    }
                    .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                        dialog.dismiss()
                        binding.switchSelfieMode.isChecked = false
                    }
                    .show()
            } else if (!isChecked) {
                updateChargingState(binding, false)
            }
        }

        binding.btnClearAll.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Intruder Selfie Images")
                .setMessage("Are you sure you want to delete all Intruder Images?")
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    imageViewModel.clearImages()
                    binding.btnClearAll.visibility = View.GONE
                }
                .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun updateChargingState(
        binding: FragmentIntruderSelfieBinding,
        isChecked: Boolean
    ) {
        if (!isAdded) return

        binding.switchSelfieMode.isChecked = isChecked

        binding.imageView.setImageResource(
            if (isChecked) R.drawable.ic_intruder_selfie_light else R.drawable.ic_intruder_selfie_dark
        )
        binding.textViewStatusTitle.text =
            if (isChecked) getString(R.string.current_status_on) else getString(R.string.current_status_off)

        binding.cardView.setCardBackgroundColor (
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    if (isChecked) R.color.green2 else R.color.card_bg_color
                )
            )
        )
    }

    private fun showPermissionBottomSheetDialog() {
        PermissionsBottomSheetFragment().show(
            childFragmentManager,
            "PermissionsBottomSheetFragment"
        )
    }


    private fun checkAndDisplayDeviceAdminStatus() {
        val devicePolicyManager =
            requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        // Check if device admin is active
        if (devicePolicyManager.isAdminActive(deviceAdminComponent)) {
            // Show Deactivate button if Device Admin is active
            binding.adminCardView.visibility = View.VISIBLE
            binding.switchScreenAdmin.isChecked = true
        } else {
            // Hide Deactivate button if Device Admin is not active
            binding.adminCardView.visibility = View.GONE
            binding.switchScreenAdmin.isChecked = false
        }
    }

    private fun loadImages() {
        imageViewModel.loadImages()

        if (!::imageAdapter.isInitialized) {
            imageAdapter = CapturedImageAdapter(this)  // Pass the fragment as the listener
            binding.recyclerView.adapter = imageAdapter
        }

        lifecycleScope.launch {
            imageViewModel.images.collect { images ->
                val hasImages = images.isNotEmpty()

                // Update UI visibility based on image availability
                binding.apply {
                    ivNoImage.isVisible = !hasImages
                    recyclerView.isVisible = hasImages
                    btnClearAll.isVisible = hasImages
                }

                // Submit the new list of images to the adapter
                imageAdapter.submitList(images)
            }
        }
    }

    override fun onResume() {
        Log.d("TAG_RESUME", "onResume: LOADED")
        super.onResume()
        loadImages()
        checkAndDisplayDeviceAdminStatus()
    }

    private fun disableDeviceAdmin() {
        val devicePolicyManager =
            requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        // Check if the device admin is currently active
        if (devicePolicyManager.isAdminActive(deviceAdminComponent)) {
            // Remove the device admin
            devicePolicyManager.removeActiveAdmin(deviceAdminComponent)

            // After deactivating, update the button visibility
            checkAndDisplayDeviceAdminStatus()
        }
    }

    private fun isScreenOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(requireContext())
    }


    private fun arePermissionsGranted(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val readMediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        val screenOverlayPermission = isScreenOverlayPermissionGranted()

        val devicePolicyManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val isDeviceAdminActive = devicePolicyManager.isAdminActive(deviceAdminComponent)

        return cameraPermission && readMediaPermission && screenOverlayPermission && isDeviceAdminActive
    }

    override fun onImageClick(imagePath: String) {
        val fragment = FullScreenImageDialogFragment.newInstance(imagePath)
        // Safely get FragmentManager using the fragment's context
        val fragmentManager = parentFragmentManager  // Use parentFragmentManager for fragments
        fragmentManager.let {
            fragment.show(it, "FullScreenImageDialogFragment")
        }
    }
}