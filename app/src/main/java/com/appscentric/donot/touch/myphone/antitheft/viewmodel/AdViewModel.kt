package com.appscentric.donot.touch.myphone.antitheft.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.manager.BannerAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.LargeNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.SmallNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.SquareNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.getBannerAdSize
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context = getApplication<Application>().applicationContext

    private val _interAdState = MutableStateFlow<InterAdState>(InterAdState.Idle)
    val interAdState: StateFlow<InterAdState> = _interAdState

    private val _bannerView = MutableStateFlow<AdView?>(null)
    val bannerView: StateFlow<AdView?> get() = _bannerView

    fun loadInterstitialAd() {
        viewModelScope.launch {
            val adUnitId = appContext.resources.getString(R.string.admob_interstitial_id)
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(appContext, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    InterstitialAdManager.initialize(interstitialAd)
                    _interAdState.value = InterAdState.Loaded(InterstitialAdManager.getAd())
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    _interAdState.value = InterAdState.Failed
                }
            })
        }
    }

    fun loadLargeNativeAds() {
        viewModelScope.launch {
            val adLoader = AdLoader.Builder(appContext, appContext.getString(R.string.admob_large_native_ids))
                .forNativeAd(LargeNativeAdManager::initialize)
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()
                adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    fun loadSquareNativeAds() {
        viewModelScope.launch {

            val adOptions = NativeAdOptions.Builder()
                .setMediaAspectRatio(MediaAspectRatio.SQUARE)
                .build()

            val adLoader = AdLoader.Builder(appContext, appContext.getString(R.string.admob_square_native_ids))
                .forNativeAd(SquareNativeAdManager::initialize)
                .withNativeAdOptions(adOptions)
                .build()
                adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    fun loadSmallNativeAds() {
        viewModelScope.launch {
            val adLoader = AdLoader.Builder(appContext, appContext.getString(R.string.admob_small_native_ids))
                .forNativeAd(SmallNativeAdManager::initialize)
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()
                adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    fun loadAd() {
        viewModelScope.launch {
            AdView(appContext).apply {
                adUnitId = appContext.getString(R.string.admob_banner_ids)
                setAdSize(getBannerAdSize(appContext))
                loadAd(AdRequest.Builder().build())
            }.also {
                _bannerView.value = it
                BannerAdManager.initialize(it)
            }
        }
    }

    sealed class InterAdState {
        object Idle : InterAdState()
        data class Loaded(val ad: InterstitialAd?) : InterAdState()
        object Failed : InterAdState()
    }
}