package com.appscentric.donot.touch.myphone.antitheft.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.adapter.LanguageAdapter
import com.appscentric.donot.touch.myphone.antitheft.databinding.ActivityLanguageBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.LargeNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.model.Language
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.ramt57.easylocale.EasyLocaleAppCompatActivity
import org.koin.android.ext.android.inject
import java.util.Locale
import kotlin.system.exitProcess

class LanguageScreen : EasyLocaleAppCompatActivity(), LanguageAdapter.OnLanguageSelectedListener {

    private lateinit var binding: ActivityLanguageBinding
    private lateinit var languages: List<Language>
    private val preferencesManager by inject<PreferencesManager>()

    private var soundId: Int = 0

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            exitProcess(0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        soundId = SoundManager.soundPool.load(this, R.raw.click_sound, 1)

        if (!preferencesManager.isRemoveAdsPurchased()) display1NativeAd()

        val recyclerView: RecyclerView = binding.recyclerview

        languages = listOf(
            Language("English", R.drawable.ic_english, "en"),
            Language("Hindi", R.drawable.ic_hindi, "hi"),
            Language("Spanish", R.drawable.ic_spanish, "es"),
            Language("Portuguese", R.drawable.ic_portuguese, "pt"),
            Language("Korean", R.drawable.ic_korean, "ko"),
            Language("French", R.drawable.ic_french, "fr"),
            Language("Chinese", R.drawable.ic_chinese, "zh")
        )

//        val languageCode = preferencesManager.getSelectedLanguage()
//        // Programmatically select English as the default language
//        languages.find { it.code == languageCode }?.isSelected = true

        val adapter = LanguageAdapter(languages, this)
        recyclerView.adapter = adapter

        updateApplyButtonVisibility()

        binding.applyButton.setOnClickListener { saveSelectedLanguage() }
    }

    override fun onLanguageSelected(language: Language) {
        if (preferencesManager.isTapSound) playClickSound()

        // Toggle the selected status of the language
        languages.forEach { it.isSelected = it === language }

        // Update the visibility of the apply button
        val isAnyLanguageSelected = languages.any { it.isSelected }
        binding.applyButton.visibility = if (isAnyLanguageSelected) View.VISIBLE else View.INVISIBLE
    }

    private fun saveSelectedLanguage() {
        if (preferencesManager.isTapSound) playClickSound()
        preferencesManager.isFirstTimeLaunch = false

        languages.find { it.isSelected }?.let {
            preferencesManager.saveSelectedLanguage(it.code)

            val localeString = preferencesManager.getSelectedLanguage()
            localeString?.let { lang -> setLocale(Locale(lang)) }

            checkNotificationPermissionAndNavigate()
        }
    }

    private fun checkNotificationPermissionAndNavigate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = NotificationManagerCompat.from(this)
            if (!notificationManager.areNotificationsEnabled()) {
                navigateToOnBoardingScreen()
            } else {
                navigateToMainActivity()
            }
        } else {
            navigateToMainActivity()
        }
    }

    private fun navigateToOnBoardingScreen() {
        val intent = Intent(this@LanguageScreen, OnboardingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this@LanguageScreen, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun updateApplyButtonVisibility() {
        val isAnyLanguageSelected = languages.any { it.isSelected }
        binding.applyButton.visibility = if (isAnyLanguageSelected) View.VISIBLE else View.INVISIBLE
    }

    private fun display1NativeAd() {
        val ad1 = LargeNativeAdManager.getAd()
        ad1?.let {
            binding.templateView.setNativeAd(it)
            binding.templateView.visibility = View.VISIBLE
        } ?: run {
            binding.templateView.visibility = View.GONE
        }
    }

    private fun playClickSound() {
        SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    override fun onDestroy() {
        // Clear references to avoid memory leaks
        binding.applyButton.setOnClickListener(null)
        binding.recyclerview.adapter = null
        binding.templateView.removeAllViews()
        (binding.root as ViewGroup).removeAllViews()
        languages = emptyList()
        onBackPressedCallback.remove()
        super.onDestroy()
    }
}