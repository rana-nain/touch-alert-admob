package com.appscentric.donot.touch.myphone.antitheft.manager

import com.google.android.gms.ads.interstitial.InterstitialAd

object InterstitialAdManager {
    private var interstitialAd: InterstitialAd? = null

    fun initialize(ad: InterstitialAd) {
        interstitialAd = ad
    }

    fun getAd(): InterstitialAd? {
        return interstitialAd
    }
}