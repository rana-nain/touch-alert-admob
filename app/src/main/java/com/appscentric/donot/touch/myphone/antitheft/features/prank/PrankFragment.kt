package com.appscentric.donot.touch.myphone.antitheft.features.prank

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentPrankBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.model.PrankData
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.prankItems
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import org.koin.android.ext.android.inject

class PrankFragment : DialogFragment(),PrankAdapter.OnItemSelectedListener {

    private var _binding: FragmentPrankBinding? = null
    private val binding get() = _binding!!
    override fun getTheme(): Int = R.style.DialogTheme
    private var ad: InterstitialAd? = null
    private val adViewModel: AdViewModel by viewModels()

    private val preferencesManager by inject<PreferencesManager>()

    private val adapter: PrankAdapter by lazy { PrankAdapter(prankItems,this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrankBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter

        binding.materialToolbar.setNavigationOnClickListener { dismiss() }

    }

    override fun onItemSelected(item: PrankData) {
        showScreenWithAd(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ad?.fullScreenContentCallback = null
    }

    private fun showScreenWithAd(item: PrankData) {
        val fragment = PrankViewFragment.newInstance(item)

        fun showFragment() {
            fragment.show(childFragmentManager, PrankViewFragment::class.java.simpleName)
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