package com.appscentric.donot.touch.myphone.antitheft.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentPinBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.bumptech.glide.Glide
import org.koin.android.ext.android.inject

class PinFragment : DialogFragment() {
    private var _binding: FragmentPinBinding? = null
    private val binding get() = _binding!!
    private val preferencesManager by inject<PreferencesManager>()

    override fun getTheme(): Int = R.style.DialogTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false // Make the dialog not cancelable
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the boolean value from the arguments
        val isPinEnabled = arguments?.getBoolean(ARG_IS_PIN_ENABLED) ?: false

        with(binding) {

            pinView.setOnCompletedListener = {
                if (isPinEnabled) {
                    preferencesManager.saveCode(it)
                    pinView.clearPin()
                    Toast.makeText(requireContext(), "Pin set Successfully!", Toast.LENGTH_SHORT)
                        .show()
                    dismiss()
                }
            }
        }
    }

    companion object {
        private const val ARG_IS_PIN_ENABLED = "isPinEnabled"

        // Use this method to create a new instance and pass the boolean argument
        fun newInstance(isPinEnabled: Boolean): PinFragment {
            val fragment = PinFragment()
            val args = Bundle().apply {
                putBoolean(ARG_IS_PIN_ENABLED, isPinEnabled)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}