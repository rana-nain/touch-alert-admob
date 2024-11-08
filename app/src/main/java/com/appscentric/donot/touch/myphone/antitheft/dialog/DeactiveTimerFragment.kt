package com.appscentric.donot.touch.myphone.antitheft.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentDeactiveTimerBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class DeactiveTimerFragment : DialogFragment() {

    private var _binding: FragmentDeactiveTimerBinding? = null
    private val binding get() = _binding!!
    private var isDestroyed = false
    override fun getTheme(): Int = R.style.DialogTheme

    private var soundId: Int = 0

    private val preferencesManager by inject<PreferencesManager>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeactiveTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)

        binding.closeBtn.setOnClickListener {
            if (preferencesManager.isTapSound) playClickSound()
            dismiss()}

        if (!preferencesManager.isRemoveAdsPurchased()) loadAd()
    }

    private fun loadAd() {
        binding.animationShimmer.visibility = View.VISIBLE
        lifecycleScope.launch {
            val adLoader =
                AdLoader.Builder(requireContext(), getString(R.string.admob_large_native_ids))
                    .forNativeAd { ad: NativeAd ->
                        if (_binding != null && !isDestroyed && isAdded && activity != null && activity?.isFinishing == false) {
                            binding.largeTemplateView.apply {
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

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isDestroyed = true
        binding.largeTemplateView.apply {
            setNativeAd(null)
            removeAllViews()
        }
        (binding.root as? ViewGroup)?.removeAllViews()
        _binding = null
    }
}