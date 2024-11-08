package com.appscentric.donot.touch.myphone.antitheft.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.adapter.ViewPagerAdapter
import com.appscentric.donot.touch.myphone.antitheft.databinding.ActivityTabbedMainBinding
import com.appscentric.donot.touch.myphone.antitheft.dialog.InfoDialogFragment
import com.appscentric.donot.touch.myphone.antitheft.dialog.SettingsFragment
import com.appscentric.donot.touch.myphone.antitheft.manager.BannerAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.LargeNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.manager.SmallNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.SquareNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.NetworkUtils
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TabbedMainActivity : AppCompatActivity(), SettingsFragment.OnDialogDismissListener,
    InfoDialogFragment.OnDialogDismissListener {

    //    private lateinit var tabsArray: Array<String>
    lateinit var binding: ActivityTabbedMainBinding
    private val preferencesManager by inject<PreferencesManager>()
    private var soundId: Int = 0
    private val adViewModel: AdViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabbedMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadClickSound()

        val intValue = intent.getIntExtra("dashboard_int_key", 0)
        setupViewPager(intValue)
    }

    override fun onResume() {
        super.onResume()
        if (NetworkUtils.isNetworkConnected(this@TabbedMainActivity) && !preferencesManager.isRemoveAdsPurchased()) {
            loadCollapsibleBannerAd()
            if (SmallNativeAdManager.getAd() == null) adViewModel.loadSmallNativeAds()
            if (LargeNativeAdManager.getAd() == null) adViewModel.loadLargeNativeAds()
            if (SquareNativeAdManager.getAd() == null) adViewModel.loadSquareNativeAds()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener { handleBackPress() }
        }
    }

    private fun setupViewPager(initialPage: Int) {
        val adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        binding.viewPager.apply {
            this.adapter = adapter
            isUserInputEnabled = false
            setCurrentItem(initialPage, false)
        }

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) = playClickSound()
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })
    }

    private fun loadCollapsibleBannerAd() {
        BannerAdManager.getAd()?.let {
            updateAdView(it)
        } ?: run {
            lifecycleScope.launch {
                adViewModel.bannerView.collect { newAdView ->
                    newAdView?.let { updateAdView(it) }
                }
            }
            adViewModel.loadAd()
        }
    }

    private fun updateAdView(adView: View) {
        val parent = adView.parent as? ViewGroup
        parent?.removeView(adView) // Remove adView from its current parent if necessary
        binding.bannerContainer.addView(adView)
    }

    override fun onDialogDismissed() = checkAndLoadCollapsibleBannerAd()

    override fun onInfoDialogDismissed() = checkAndLoadCollapsibleBannerAd()

    private fun checkAndLoadCollapsibleBannerAd() {
        if (!preferencesManager.isRemoveAdsPurchased()) loadCollapsibleBannerAd()
    }

    private fun loadClickSound() {
        soundId = SoundManager.soundPool.load(this, R.raw.click_sound, 1)
    }

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun handleBackPress() {
        // Check if there's only one activity in the task stack
        playClickSound()
        if (isTaskRoot) {
            val intent = Intent(this@TabbedMainActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (isTaskRoot) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val POSITION_THRESHOLD = 8
    }
}