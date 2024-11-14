package com.appscentric.donot.touch.myphone.antitheft.features.intruder.dialog

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentPermissionsBottomSheetBinding
import com.appscentric.donot.touch.myphone.antitheft.features.intruder.MyDeviceAdminReceiver
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PermissionsBottomSheetFragment : BottomSheetDialogFragment() {

    // Declare the binding variable
    private var _binding: FragmentPermissionsBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_CODE_OVERLAY = 100
    private val REQUEST_CODE_CAMERA = 101
    private val REQUEST_CODE_STORAGE = 102

    private val deviceAdminComponent by lazy {
        ComponentName(
            requireContext(),
            MyDeviceAdminReceiver::class.java
        )
    }

    private val deviceAdminLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                binding.switchDeviceAdmin.isChecked = true
                this@PermissionsBottomSheetFragment.dismiss()
            } else {
                binding.switchDeviceAdmin.isChecked = false

                val allowFragment = PermissionAllowPopBottomFragment()
                allowFragment.show(parentFragmentManager, "PermissionAllowPopBottomFragment")

                this@PermissionsBottomSheetFragment.dismiss()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentPermissionsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use the binding to interact with views
        binding.closeBtn.setOnClickListener {
            dismiss() // Close the bottom sheet when the button is clicked
        }

        checkPermissions()

        // Set click listeners for switches
        binding.switchScreenOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestScreenOverlayPermission()
            }
        }

        binding.switchDeviceAdmin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableDeviceAdmin()
            }
        }

        binding.switchCamera.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestCameraPermission()
            }
        }
    }

    private fun checkPermissions() {
        // Check if Screen Overlay permission is granted
        if (isScreenOverlayPermissionGranted()) {
            binding.switchScreenOverlay.isChecked = true
        }

        // Check if Device Admin is enabled
        if (isDeviceAdminEnabled()) {
            binding.switchDeviceAdmin.isChecked = true
        }

        // Check if Camera permission is granted
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // Check if both Camera and storage permissions are granted
        if (permissions.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }) {
            binding.switchCamera.isChecked = true
        }

        // Check if Storage permission (READ_EXTERNAL_STORAGE or READ_MEDIA_IMAGES) is granted
//        if (isStoragePermissionGranted()) {
//            binding.switchCamera.isChecked = true
//        }
    }

    private fun isDeviceAdminEnabled(): Boolean {
        val devicePolicyManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return devicePolicyManager.isAdminActive(deviceAdminComponent)
    }

    private fun requestScreenOverlayPermission() {
        if (!isScreenOverlayPermissionGranted()) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivityForResult(intent, REQUEST_CODE_OVERLAY)
        }
    }

    private fun isScreenOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(requireContext())
    }

    private fun isStoragePermissionGranted(): Boolean {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        // Compare permission status to PackageManager.PERMISSION_GRANTED
        return ContextCompat.checkSelfPermission(
            requireContext(),
            storagePermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.CAMERA,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_CODE_CAMERA
            )
        }

        // Request Storage Permission (READ_EXTERNAL_STORAGE or READ_MEDIA_IMAGES)
        requestStoragePermission()
    }

    private fun requestStoragePermission() {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), storagePermission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(storagePermission),
                REQUEST_CODE_STORAGE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_OVERLAY -> {
                binding.switchScreenOverlay.isChecked = isScreenOverlayPermissionGranted()
            }
        }
    }

    private fun enableDeviceAdmin() {
        val devicePolicyManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        // Check if the device admin is already active
        if (!devicePolicyManager.isAdminActive(deviceAdminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Enable Device Admin to monitor wrong password attempts."
                )
            }
            deviceAdminLauncher.launch(intent)
        } else {
            Toast.makeText(requireContext(), "Device Admin is already enabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_CAMERA -> {
                binding.switchCamera.isChecked =
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            }

            REQUEST_CODE_STORAGE -> {
                binding.switchCamera.isChecked =
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the binding to avoid memory leaks
        _binding = null
        deviceAdminLauncher.unregister()
    }
}
