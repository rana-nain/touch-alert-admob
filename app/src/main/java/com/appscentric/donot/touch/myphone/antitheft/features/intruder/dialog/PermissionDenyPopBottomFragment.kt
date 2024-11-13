package com.appscentric.donot.touch.myphone.antitheft.features.intruder.dialog

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentPermissionAllowPopBottomBinding
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentPermissionDenyPopBottomBinding
import com.appscentric.donot.touch.myphone.antitheft.features.intruder.MyDeviceAdminReceiver
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PermissionDenyPopBottomFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPermissionDenyPopBottomBinding? = null
    private val binding get() = _binding!!


    private val deviceAdminComponent by lazy {
        ComponentName(
            requireContext(),
            MyDeviceAdminReceiver::class.java
        )
    }

    private val deviceAdminLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                this@PermissionDenyPopBottomFragment.dismiss()
            } else {
                val denyFragment = PermissionDenyPopBottomFragment()
                denyFragment.show(parentFragmentManager, "PermissionDenyPopBottomFragment")

                // Dismiss the current fragment after showing the new one
                this@PermissionDenyPopBottomFragment.dismiss()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentPermissionDenyPopBottomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeBtn.setOnClickListener { dismiss() }

        binding.allowPermissionBtn.setOnClickListener { enableDeviceAdmin() }
    }

    private fun enableDeviceAdmin() {
        val devicePolicyManager =
            requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

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
            Toast.makeText(requireContext(), "Device Admin is already enabled", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the binding to avoid memory leaks
        _binding = null
        deviceAdminLauncher.unregister()
    }
}