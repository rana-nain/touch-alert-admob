package com.appscentric.donot.touch.myphone.antitheft.monetization.banner

import android.content.Context
import android.view.ViewGroup
import com.appscentric.donot.touch.myphone.antitheft.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BannerManager {

    private var adView: AdView? = null

    fun initializeAd(context: Context) {
        if (adView == null) {
            CoroutineScope(Dispatchers.IO).launch {
                adView = AdView(context)
                adView?.adUnitId = context.getString(R.string.admob_banner_ids)
                val adSize = getAdSize(context)
                withContext(Dispatchers.Main) {
                    adView?.setAdSize(adSize)
                    val adRequest = AdRequest.Builder().build()
                    adView?.loadAd(adRequest)
                }
            }
        }
    }

    private fun getAdSize(context: Context): AdSize {
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        val adWidth = (displayMetrics.widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    fun getBannerAdView(): AdView? {
        return adView
    }

    fun removeAdFromParent() {
        adView?.let {
            it.parent?.let { parent ->
                if (parent is ViewGroup) {
                    parent.removeView(it)
                }
            }
            it.destroy()
            adView = null
        }
    }
}