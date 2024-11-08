package com.appscentric.donot.touch.myphone.antitheft.features.touch

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.CountDownLayoutBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.LargeNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import org.koin.android.ext.android.inject

class TimerDialogFragment : DialogFragment() {

    private var _binding: CountDownLayoutBinding? = null
    private val binding get() = _binding!!
    private var countDownTimer: CountDownTimer? = null
    private var isDestroyed = false
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
        _binding = CountDownLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!preferencesManager.isRemoveAdsPurchased()) display1NativeAd()

        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update UI if needed
            }

            override fun onFinish() {
                // Check if the fragment and activity are in a valid state
                if (!isDestroyed && isAdded && activity?.isFinishing == false) {
                    dismissAllowingStateLoss()
                    dismissListener?.onTimerDialogDismissed()

                    // Access the parent fragment
//                    val parentFragment = parentFragment as? SoundsFragment
//                    parentFragment?.onTimerFinish()
                }
            }

        }.start()
    }

    private fun display1NativeAd() {
        val ad1 = LargeNativeAdManager.getAd()
        ad1?.let {
            binding.largeTemplateView.setNativeAd(it)
            binding.largeTemplateView.visibility = View.VISIBLE
        } ?: run {
            binding.largeTemplateView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isDestroyed = true
        countDownTimer?.cancel()
        binding.largeTemplateView.setNativeAd(null)
        binding.largeTemplateView.removeAllViews()
        (binding.root as? ViewGroup)?.removeAllViews()
        _binding = null
    }

    interface OnDialogDismissListener {
        fun onTimerDialogDismissed()
    }

    var dismissListener: OnDialogDismissListener? = null
}
