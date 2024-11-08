package com.appscentric.donot.touch.myphone.antitheft.features.wallpaper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.adapter.WallpaperAdapter
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentWallpaperBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.model.Wallpaper
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.WallpaperViewModel
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.factory.WallpaperViewModelFactory
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class WallpaperFragment : DialogFragment() { //, WallpaperViewFragment.OnDialogDismissListener

    private lateinit var binding: FragmentWallpaperBinding
    private var ad: InterstitialAd? = null
    private val adViewModel: AdViewModel by viewModels()
    private val preferencesManager by inject<PreferencesManager>()
    private var clickCounter = 0
    override fun getTheme(): Int = R.style.DialogTheme
    private val viewModel: WallpaperViewModel by viewModels {
        WallpaperViewModelFactory(preferencesManager)
    }

    private val adapter by lazy {
        WallpaperAdapter(
            { resourceId ->
                openWallpaperViewFragment(
                    resourceId
                )
            },
            { playClickSound() }
        )
    }
    private var soundId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWallpaperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            if (preferencesManager.isTapSound) playClickSound()
            dismiss()
        }

        binding.recyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.recyclerView.adapter = adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)

        lifecycleScope.launch {
            viewModel.wallpapers.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
    }

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun openWallpaperViewFragment(data: Wallpaper) {
        clickCounter++
        if (clickCounter > 1) {
            showScreenWithAd(data)
        } else {
            val fragment = WallpaperViewFragment.newInstance(data)
            fragment.show(childFragmentManager, WallpaperViewFragment::class.java.simpleName)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ad?.fullScreenContentCallback = null
    }

    private fun showScreenWithAd(item: Wallpaper) {
        val fragment = WallpaperViewFragment.newInstance(item)

        fun showFragment() {
            fragment.show(childFragmentManager, WallpaperViewFragment::class.java.simpleName)
        }

        ad = InterstitialAdManager.getAd()
        ad?.apply {
            fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = false
                    showFragment()
                }

                override fun onAdDismissedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = true
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    showFragment()
                }
            }
            show(requireActivity())
        } ?: showFragment() // If no ad is available, directly show the fragment.

        // Preload the next interstitial ad
        adViewModel.loadInterstitialAd()
    }
}