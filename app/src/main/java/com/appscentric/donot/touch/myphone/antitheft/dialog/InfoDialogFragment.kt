package com.appscentric.donot.touch.myphone.antitheft.dialog

import android.content.DialogInterface
import android.media.SoundPool
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentInfoDialogBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.BannerAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import org.koin.android.ext.android.inject

class InfoDialogFragment : DialogFragment() {

    override fun getTheme(): Int = R.style.DialogTheme
    private var _binding: FragmentInfoDialogBinding? = null
    private val binding get() = _binding!!

    private var soundId: Int = 0

    private val preferencesManager by inject<PreferencesManager>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)

        binding.toolbar.setNavigationOnClickListener {
            if (preferencesManager.isTapSound) playClickSound()
            dismiss() }

        if (!preferencesManager.isRemoveAdsPurchased()) loadCollapsibleBannerAd()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.toolbar.setNavigationOnClickListener(null)
        binding.bannerContainer.removeAllViews()
        (binding.root as? ViewGroup)?.removeAllViews()
        _binding = null
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

    interface OnDialogDismissListener {
        fun onInfoDialogDismissed()
    }

    var dismissListener: OnDialogDismissListener? = null

    override fun onDismiss(dialog: DialogInterface) {
        dismissListener?.onInfoDialogDismissed()
        super.onDismiss(dialog)
    }

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }
}