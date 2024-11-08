package com.appscentric.donot.touch.myphone.antitheft.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.adapter.ItemDetailsAdapter
import com.appscentric.donot.touch.myphone.antitheft.databinding.ActivityItemDetailsScreenBinding
import com.appscentric.donot.touch.myphone.antitheft.dialog.PremiumFragment
import com.appscentric.donot.touch.myphone.antitheft.manager.BannerAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.LargeNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.manager.SmallNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.utils.NetworkUtils
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.backPressClickCounter
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.backPressCount
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.items
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.itemsPremium
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.premiumPopupClickCounter
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.settingsClickCounter
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

private val TAG = ItemDetailsScreen::class.java.simpleName

class ItemDetailsScreen : AppCompatActivity(), ItemDetailsAdapter.OnItemSelectedListener {
    private lateinit var binding: ActivityItemDetailsScreenBinding
    private var selectedItemPosition: Int = RecyclerView.NO_POSITION
    private var isPlaying = false
    private lateinit var duration: String
    private var mediaPlayer: MediaPlayer? = null
    private var selectedItem: HomeItem? = null

    private var soundId: Int = 0

    private val preferencesManager by inject<PreferencesManager>()
    private val itemsList by lazy {
        if (!preferencesManager.isRemoveAdsPurchased()) {
            items
        } else {
            itemsPremium
        }
    }

    private val adapter: ItemDetailsAdapter by lazy { ItemDetailsAdapter(itemsList, this) }
    private val audioManager: AudioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val adViewModel: AdViewModel by viewModels()

    private var ad: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private lateinit var dataMap: Map<String, Any>

    // Top-level declarations
    private val isFlash: Boolean
        get() = dataMap["isFlash"] as? Boolean ?: true

    private val isSound: Boolean
        get() = dataMap["isSound"] as? Boolean ?: true

    private val isVibrate: Boolean
        get() = dataMap["isVibrate"] as? Boolean ?: true

    private val volumeDuration: String
        get() = dataMap["volumeDuration"] as? String ?: "30s"

    private val durations = arrayOf("15s", "30s", "1m", "2m")

    companion object {
        private const val KEY_SELECTED_ITEM = "selectedItem"
        private const val KEY_SELECTED_ITEM_POSITION = "selectedItemPosition"
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailsScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        soundId = SoundManager.soundPool.load(this, R.raw.click_sound, 1)

        val (_, _, dataMap) = preferencesManager.getAllData()
        this.dataMap = dataMap

        binding.setupToolbar()

        selectedItem = intent.getParcelableExtra(KEY_SELECTED_ITEM)
        selectedItemPosition =
            intent.getIntExtra(KEY_SELECTED_ITEM_POSITION, RecyclerView.NO_POSITION)

        binding.applySoundTextVeiw.text = getString(selectedItem?.nameResId!!)
        binding.toolbar.setOnClickListener {
            if (preferencesManager.isTapSound) playClickSound()
            if (++backPressClickCounter % backPressCount == 0 && !preferencesManager.isRemoveAdsPurchased()) {
                backWithAd() // Compile function 1 after every 5th click
            } else {
                onBackPressed()
            }
        }

        binding.recyclerView.adapter = adapter

        if (selectedItemPosition != RecyclerView.NO_POSITION) {
            adapter.setSelectedItem(selectedItemPosition)
            binding.recyclerView.scrollToPosition(selectedItemPosition)
        }
        binding.setUpBinding()

        if (NetworkUtils.isNetworkConnected(this@ItemDetailsScreen) && !preferencesManager.isRemoveAdsPurchased()) {
            display1NativeAd()
            loadCollapsibleBannerAd()
        } else {
            binding.animationShimmer.visibility = View.GONE
        }

        setupWidgetViews()

        if (NetworkUtils.isNetworkConnected(this) && !preferencesManager.isRemoveAdsPurchased()) {
            if (++premiumPopupClickCounter % 5 == 0) showPremiumPopup()
        }
    }

    private fun showPremiumPopup() {
        val fragmentManager = supportFragmentManager
        val existingFragment =
            fragmentManager.findFragmentByTag(PremiumFragment::class.java.simpleName)

        if (existingFragment == null) {
            val premiumPreviewDialogFragment = PremiumFragment()
            premiumPreviewDialogFragment.show(
                fragmentManager,
                PremiumFragment::class.java.simpleName
            )
        }
    }

    private fun setupWidgetViews() {
        binding.apply {
            flashSwitch.isChecked = isFlash
            vibrationSwitch.isChecked = isVibrate
            soundSwitch.isChecked = isSound
        }
    }

    private fun ActivityItemDetailsScreenBinding.setUpBinding() {
        selectedItem?.let { item ->
            if (item.isSound) {
                soundsImageView.apply {
                    Glide.with(this@ItemDetailsScreen)
                        .load(item.thumbnail)
                        .into(this)
                    visibility = View.VISIBLE
                }
                animationView.visibility = View.GONE
            } else {
                animationView.apply {
                    setAnimation(item.anim)
                    visibility = View.VISIBLE
                }
                soundsImageView.visibility = View.GONE
            }
        }

        mediaPlayer = MediaPlayer.create(applicationContext, selectedItem!!.sound).apply {
            isLooping = true
        }

        binding.floatingActionButton.setOnClickListener {
            if (preferencesManager.isTapSound) playClickSound()
            if (isPlaying) pauseAnimation() else playAnimation()
        }

        binding.sliderVolume.apply {
            value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            valueFrom = 0f
            valueTo = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()

            addOnChangeListener { _, value, _ ->
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    value.toInt(),
                    0
                )
            }
        }

        binding.flashSwitch.setOnCheckedChangeListener { _, _ ->
            if (preferencesManager.isTapSound) playClickSound()
        }

        binding.vibrationSwitch.setOnCheckedChangeListener { _, _ ->
            if (preferencesManager.isTapSound) playClickSound()
        }

        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (preferencesManager.isTapSound) playClickSound()
            binding.sliderVolume.isEnabled = isChecked
        }

        val chipToSelect = chipGroup.children
            .filterIsInstance<Chip>()
            .find { it.text.toString() == volumeDuration }

        chipToSelect?.let { chipGroup.check(it.id) }


        chipGroup.setOnCheckedStateChangeListener { chipGroup, checkedIds ->
            if (preferencesManager.isTapSound) playClickSound()
            if (checkedIds.isNotEmpty()) {
                val selectedChipId = checkedIds.first()
                val selectedChip = chipGroup.findViewById<Chip>(selectedChipId)
                duration = selectedChip.text.toString()
                // Do something with the selected text
                Log.d(TAG, "setUpBinding: $duration")

            } else {
                // Handle the case where no chip is selected (optional)
            }
        }
    }

    private fun ActivityItemDetailsScreenBinding.setupToolbar() {
        applySoundBtn.setOnClickListener {
            if (preferencesManager.isTapSound) playClickSound()
            selectedItem?.let { item ->
                if (++settingsClickCounter % 3 == 0 && !preferencesManager.isRemoveAdsPurchased()) {
                    showScreenWithAd(item) // Compile function 1 after every 3rd click
                } else {
                    savePreferences(item) // Compile function 2 otherwise
                }
            }
        }
    }


    private fun showScreenWithAd(homeItem: HomeItem) {
        ad = InterstitialAdManager.getAd()
        ad?.let {
            it.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = false
                }

                override fun onAdDismissedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = true
                    savePreferences(homeItem)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    savePreferences(homeItem)
                }
            }
            it.show(this@ItemDetailsScreen)
        } ?: savePreferences(homeItem)
        adViewModel.loadInterstitialAd()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onItemSelected(item: HomeItem, adapterPosition: Int) {
        if (preferencesManager.isTapSound) playClickSound()
        // Check if the item is already selected
        if (selectedItem == item) return

        // Update the selected item
        selectedItem = item

        // Update UI elements with the selected item details
        binding.apply {
            applySoundTextVeiw.text = getString(item.nameResId)
            floatingActionButton.setImageResource(R.drawable.round_play_arrow_24)
        }

        item.let { eachItem ->
            with(binding) {
                soundsImageView.visibility =
                    if (eachItem.isSound && !eachItem.isRewarded) View.VISIBLE else View.GONE
                animationView.visibility = if (eachItem.isSound) View.GONE else View.VISIBLE

                if (eachItem.isSound) {
                    if (!eachItem.isRewarded) {
                        Glide.with(this@ItemDetailsScreen)
                            .load(eachItem.thumbnail)
                            .into(soundsImageView)
                    } else if (NetworkUtils.isNetworkConnected(this@ItemDetailsScreen)) {
                        loadRewardedAd(eachItem)
                    } else {
                        Toast.makeText(
                            this@ItemDetailsScreen,
                            "Please Connect to Network",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    animationView.setAnimation(eachItem.anim)
                }
            }
        }

        // Release the previous media player and create a new one
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(applicationContext, item.sound).apply {
            isLooping = true
        }
        isPlaying = false
    }


    private fun playAnimation() {
        if (!selectedItem!!.isSound) binding.animationView.playAnimation()
        binding.floatingActionButton.setImageResource(R.drawable.round_pause_24)
        isPlaying = true
        mediaPlayer?.start()
    }

    private fun pauseAnimation() {
        if (!selectedItem!!.isSound) binding.animationView.pauseAnimation()
        binding.floatingActionButton.setImageResource(R.drawable.round_play_arrow_24)
        isPlaying = false
        mediaPlayer?.pause()
    }

    private fun savePreferences(selectedItem: HomeItem) {
        val volumeInt = binding.sliderVolume.value.toInt()

        try {
            val selectedChipId = binding.chipGroup.checkedChipId
            if (selectedChipId != View.NO_ID) {
                val index = binding.chipGroup.indexOfChild(
                    binding.chipGroup.findViewById<Chip>(selectedChipId)
                )
                if (index >= 0 && index < durations.size) {
                    duration = durations[index]
                    Log.d(TAG, "setUpBinding: $duration")
                } else {
                    throw IndexOutOfBoundsException("Invalid chip index: $index")
                }
            } else {
                throw IllegalStateException("No chip selected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
        }


        preferencesManager.saveItemDetails(
            selectedItem,
            vibration = binding.vibrationSwitch.isChecked,
            flash = binding.flashSwitch.isChecked,
            sound = binding.soundSwitch.isChecked,
            volume = volumeInt,
            volumeDuration = duration
        )

        val intent = Intent(applicationContext, TabbedMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }


    override fun onResume() {
        super.onResume()
        registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(volumeReceiver)
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        ad?.fullScreenContentCallback = null
        rewardedAd?.fullScreenContentCallback = null
        binding.templateView.removeAllViews()
        super.onDestroy()
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                binding.sliderVolume.value =
                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            }
        }
    }

    private fun loadCollapsibleBannerAd() {
        BannerAdManager.getAd()?.let {
            val parent = it.parent as? ViewGroup
            parent?.removeView(it)
            binding.bannerContainer.addView(it)
        } ?: run {
            binding.bannerContainer.visibility = View.GONE
        }
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

    private fun backWithAd() {
        ad = InterstitialAdManager.getAd()
        ad?.let {
            it.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = false
                }

                override fun onAdDismissedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = true
                    onBackPressed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    onBackPressed()
                }
            }
            it.show(this@ItemDetailsScreen)
        } ?: onBackPressed()
        adViewModel.loadInterstitialAd()
    }

    private fun loadRewardedAd(eachItem: HomeItem) {
        val progressDialog = createProgressDialog()
        progressDialog.show()

        RewardedAd.load(
            this, resources.getString(R.string.admob_rewarded_id),
            AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    progressDialog.dismiss()
                    rewardedAd = ad

                    rewardedAd?.show(this@ItemDetailsScreen) {
                        eachItem.isRewarded = false
                        items[eachItem.position] = eachItem
                        adapter.updateItems(items)
                    }

                    rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            Constants.SHOW_APP_OPEN = true

                            Toast.makeText(
                                this@ItemDetailsScreen,
                                "New Sounds Awarded Successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            binding.apply {
                                soundsImageView.apply {
                                    setImageResource(eachItem.thumbnail)
                                    visibility = View.VISIBLE
                                }
                                animationView.visibility = View.GONE
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            rewardedAd = null
                            Constants.SHOW_APP_OPEN = false
                            Toast.makeText(this@ItemDetailsScreen, adError.message, Toast.LENGTH_SHORT).show()
                        }
                        override fun onAdShowedFullScreenContent() {
                            Constants.SHOW_APP_OPEN = false
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { Log.d(TAG, it) }
                    Constants.SHOW_APP_OPEN = false
                    rewardedAd = null
                    progressDialog.dismiss()
                    Toast.makeText(this@ItemDetailsScreen, adError.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createProgressDialog(): AlertDialog {
        val builder = MaterialAlertDialogBuilder(this@ItemDetailsScreen)
        val inflater = LayoutInflater.from(this@ItemDetailsScreen)
        val progressBar = inflater.inflate(R.layout.progress_dialog, null)
        builder.setView(progressBar)
        builder.setCancelable(false)
        return builder.create()
    }

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }
}