package com.appscentric.donot.touch.myphone.antitheft.manager

import android.view.ViewGroup
import com.google.android.gms.ads.AdView

object BannerAdManager {
    private var bannerAd: AdView? = null

    fun initialize(ad: AdView) {
        bannerAd = ad
    }

    fun getAd(): AdView? {
        return bannerAd
    }
    fun clearAd() {
        bannerAd = null
    }
}