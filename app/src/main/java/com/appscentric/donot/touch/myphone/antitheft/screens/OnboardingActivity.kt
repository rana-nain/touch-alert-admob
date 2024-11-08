package com.appscentric.donot.touch.myphone.antitheft.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.adapter.OnboardingViewPagerAdapter
import com.appscentric.donot.touch.myphone.antitheft.databinding.ActivityOnboardingBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.LargeNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import org.koin.android.ext.android.inject
import kotlin.system.exitProcess

class OnboardingActivity : AppCompatActivity() {

    private var soundId: Int = 0
    private lateinit var binding: ActivityOnboardingBinding
    private val preferencesManager by inject<PreferencesManager>()
    private val adapter by lazy { OnboardingViewPagerAdapter(this, this) }
    private val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            exitProcess(0)
        }
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            binding.apply {
                val isLastPage = position == adapter.itemCount - 1
                textSkip.visibility = if (isLastPage) View.INVISIBLE else View.VISIBLE
                btnNextStep.text = getString(if (isLastPage) R.string.get_started else R.string.next)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        soundId = SoundManager.soundPool.load(this, R.raw.click_sound, 1)

        with(binding){
            viewPager.adapter = adapter
            binding.pageIndicator.attachTo(viewPager)

            onBackPressedDispatcher.addCallback(this@OnboardingActivity, onBackPressedCallback)

            textSkip.setOnClickListener {
                if (preferencesManager.isTapSound) playClickSound()
                setFirstTimeLaunchToFalse()
            }

            btnNextStep.setOnClickListener {
                if (preferencesManager.isTapSound) playClickSound()
                if (viewPager.currentItem < adapter.itemCount - 1) {
                    viewPager.setCurrentItem(viewPager.currentItem + 1, true)
                } else {
                    setFirstTimeLaunchToFalse()
                }
            }

            if (!preferencesManager.isRemoveAdsPurchased()) displayNativeAd()

            viewPager.registerOnPageChangeCallback(pageChangeCallback)
        }
    }

    private fun checkPermission(): Boolean =
        permissionToRequest?.let {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        } ?: true

    private fun navigateToActivity(activityClass: Class<out Activity>) {
        val intent = Intent(this, activityClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun setFirstTimeLaunchToFalse() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || checkPermission()) {
            navigateToActivity(MainActivity::class.java)
        } else {
            navigateToActivity(PermissionScreen::class.java)
        }
    }

    private fun displayNativeAd() {
        val parentViewGroup = binding.templateView.parent as? ViewGroup
        parentViewGroup?.removeView(binding.templateView)

        LargeNativeAdManager.getAd()?.let { ad ->
            parentViewGroup?.addView(binding.templateView) // Re-add templateView to its parent
            binding.templateView.apply {
                setNativeAd(ad)
                visibility = View.VISIBLE
            }
        } ?: run {
            binding.templateView.visibility = View.GONE
        }
    }

    private fun playClickSound() {
        SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister listeners and callbacks to prevent memory leaks
        binding.apply {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
            viewPager.adapter = null
            textSkip.setOnClickListener(null)
            btnNextStep.setOnClickListener(null)
            templateView.removeAllViews()
            templateView.setNativeAd(null)
        }

        onBackPressedCallback.remove()
        (binding.root as? ViewGroup)?.removeAllViews()
    }
}
