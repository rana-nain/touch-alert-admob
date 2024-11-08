package com.appscentric.donot.touch.myphone.antitheft.monetization.appopen

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.application.App
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppOpenManager(private val myApp: App) : Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

    companion object {
        private const val NUM_HOURS = 4
        private const val MILLISECONDS_PER_HOUR = 3600000
    }

    private var appOpenAd: AppOpenAd? = null
    private var currentActivity: Activity? = null
    private var isShowingAd = false
    private var loadTime: Long = 0

    init {
        myApp.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    interface AppOpenAdListener {
        fun onAdLoaded()
        fun onAdDismissed()
    }

    private var adListener: AppOpenAdListener? = null

    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference = Date().time - loadTime
        return dateDifference < MILLISECONDS_PER_HOUR * NUM_HOURS
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }

    fun fetchAd() {
        if (isAdAvailable()) return

        CoroutineScope(Dispatchers.IO).launch {
            // Prepare the ad request in the background (if there's any other setup to do)

            withContext(Dispatchers.Main) { // Switch to main thread for the load call
                val loadCallbacks = object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        super.onAdLoaded(ad)
                        appOpenAd = ad
                        loadTime = Date().time
                        adListener?.onAdLoaded()
                    }
                }

                val request = AdRequest.Builder().build()
                AppOpenAd.load(myApp, myApp.getString(R.string.admob_app_open_ids), request, loadCallbacks)
            }
        }
    }


    private fun showAdIfAvailable() {
        if (isShowingAd || !isAdAvailable()) {
            fetchAd()
            return
        }

        if (Constants.SHOW_APP_OPEN) {
            appOpenAd?.let { ad ->
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        adListener?.onAdDismissed()
                        appOpenAd = null
                        isShowingAd = false
                        fetchAd()
                    }

                    override fun onAdShowedFullScreenContent() {
                        isShowingAd = true
                    }
                }

                currentActivity?.let { ad.show(it) }
            }
        }
    }

    private fun updateCurrentActivity(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        updateCurrentActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        updateCurrentActivity(activity)
    }

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityStopped(p0: Activity) {}

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

    override fun onActivityDestroyed(p0: Activity) {
        if (currentActivity == p0) {
            currentActivity = null
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (!App().preferencesManager.isRemoveAdsPurchased()) {
            showAdIfAvailable()
        }
    }
}