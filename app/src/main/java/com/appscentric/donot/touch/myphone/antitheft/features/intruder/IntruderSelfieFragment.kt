package com.appscentric.donot.touch.myphone.antitheft.features.intruder

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.room.Room
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentIntruderSelfieBinding
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentWifiDetectionBinding
import com.appscentric.donot.touch.myphone.antitheft.features.intruder.dialog.PermissionsBottomSheetFragment
import com.appscentric.donot.touch.myphone.antitheft.features.plug.ChargingService
import com.appscentric.donot.touch.myphone.antitheft.features.wifi.WifiMonitorService
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.room.AppDatabase
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.isServiceRunning
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class IntruderSelfieFragment : DialogFragment() {

    private lateinit var binding: FragmentIntruderSelfieBinding
    override fun getTheme(): Int = R.style.DialogTheme

    private val preferencesManager by inject<PreferencesManager>()

    private lateinit var db: AppDatabase
    private lateinit var adapter: CapturedImageAdapter

    private val deviceAdminComponent by lazy {
        ComponentName(
            requireContext(),
            MyDeviceAdminReceiver::class.java
        )
    }

    private fun loadImages() {
        CoroutineScope(Dispatchers.Main).launch {
            val images = db.intruderImageDao().getAllImages()
            adapter = CapturedImageAdapter(images)
            binding.recyclerView.adapter = adapter
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(requireContext(), AppDatabase::class.java, "app_database")
            .build()
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

        loadImages()

        showPermissionBottomSheetDialog()

        checkAndDisplayDeviceAdminStatus()

        updateChargingState(
            binding,
            preferencesManager.intruderSelfieMode
        )

        binding.switchScreenAdmin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                disableDeviceAdmin()
            }
        }

        binding.switchSelfieMode.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.intruderSelfieMode = isChecked

            val isServiceRunning =
                isServiceRunning(requireContext(), ChargingService::class.java)
            if (!isServiceRunning && isChecked) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Update Intruder Selfie State")
                    .setMessage("Are you sure you want to enable Intruder Selfie mode?")
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes)) { _, _ ->
                        updateChargingState(
                            binding,
                            true
                        )
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

        binding.cardView.setCardBackgroundColor(
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
            binding.switchScreenAdmin.isChecked = true
        }
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

}