package com.appscentric.donot.touch.myphone.antitheft.manager

import com.google.android.gms.ads.nativead.NativeAd

object SmallNativeAdManager {
    private var nativeAd: NativeAd? = null

    fun initialize(ad: NativeAd) {
        nativeAd = ad
    }

    fun getAd(): NativeAd? {
        return nativeAd
    }
}