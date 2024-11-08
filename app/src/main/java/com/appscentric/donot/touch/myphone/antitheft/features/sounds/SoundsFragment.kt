package com.appscentric.donot.touch.myphone.antitheft.features.sounds

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.adapter.HomeItemAdapter
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentSoundsBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.manager.SmallNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem
import com.appscentric.donot.touch.myphone.antitheft.screens.ItemDetailsScreen
import com.appscentric.donot.touch.myphone.antitheft.screens.TabbedMainActivity
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.homeClickCounter
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.homeRewardedClickCounter
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.items
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.itemsPremium
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class SoundsFragment : Fragment(), HomeItemAdapter.OnItemSelectedListener {

    private var _binding: FragmentSoundsBinding? = null
    private val binding get() = _binding!!

    private val preferencesManager by inject<PreferencesManager>()
    private var selectedItem: HomeItem? = null
    private var ad: InterstitialAd? = null
    private val adViewModel: AdViewModel by viewModels()
    private var soundId: Int = 0

    private val itemsList by lazy {
        if (!preferencesManager.isRemoveAdsPurchased()) {
            items
        } else {
            itemsPremium
        }
    }

    private val adapter: HomeItemAdapter by lazy {
        HomeItemAdapter(itemsList, this, preferencesManager)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoundsBinding.inflate(inflater, container, false)
        val root = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)
        binding.setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        ad?.fullScreenContentCallback = null
    }

    override fun onItemSelected(item: HomeItem, position: Int) {
        if (preferencesManager.isTapSound) playClickSound()

        if (!preferencesManager.isRemoveAdsPurchased()) {
            val updatePosition =
                if (SmallNativeAdManager.getAd() != null && position > TabbedMainActivity.POSITION_THRESHOLD) {
                    position - 1
                } else {
                    position
                }

            if (item.isSound && ++homeRewardedClickCounter % 3 == 0) {
                showScreenWithAd(item, updatePosition)
            } else if (incrementAndCheck()) {
                showScreenWithAd(item, updatePosition)
            } else {
                startActivityIntent(item, updatePosition)
            }
        } else {
            val intent = Intent(requireContext(), ItemDetailsScreen::class.java).apply {
                putExtra("selectedItem", item)
                putExtra("selectedItemPosition", position)
            }
            startActivity(intent)
        }
    }

    private fun incrementAndCheck(): Boolean {
        return ++homeClickCounter % 3 == 0
    }

    private fun FragmentSoundsBinding.setupRecyclerView() {
        val recyclerView: RecyclerView = recyclerview

        val (homeItem, _, _) = preferencesManager.getAllData()
        selectedItem = homeItem


        recyclerView.setHasFixedSize(false)
        recyclerView.isNestedScrollingEnabled = false

        val layoutManager = GridLayoutManager(requireContext(), 3)

        if (SmallNativeAdManager.getAd() != null && !preferencesManager.isRemoveAdsPurchased()) {
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.getItemViewType(position)) {
                        HomeItemAdapter.VIEW_TYPE_AD -> 3 // Span size for ads
                        else -> 1 // Span size for regular items
                    }
                }
            }
            homeItem.position = homeItem.position.takeIf { it > POSITION_THRESHOLD }?.let { it + 1 }
                ?: homeItem.position
        }

        recyclerView.layoutManager = layoutManager

        recyclerView.adapter = adapter

        if (homeItem.position != RecyclerView.NO_POSITION) {
            adapter.setSelectedItem(homeItem.position)
        }
    }

    private fun startActivityIntent(item: HomeItem, position: Int) {
        val intent = Intent(requireContext(), ItemDetailsScreen::class.java).apply {
            putExtra("selectedItem", item)
            putExtra("selectedItemPosition", position)
        }
        startActivity(intent)
    }

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun showScreenWithAd(item: HomeItem, position: Int) {
        val intent = Intent(requireContext(), ItemDetailsScreen::class.java).apply {
            putExtra("selectedItem", item)
            putExtra("selectedItemPosition", position)
        }

        ad = InterstitialAdManager.getAd()
        ad?.let {
            it.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = false
                }

                override fun onAdDismissedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = true
                    startActivity(intent)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    startActivity(intent)
                }
            }
            it.show(requireActivity())
        } ?: startActivity(intent)
        adViewModel.loadInterstitialAd()
    }

    companion object {
        const val POSITION_THRESHOLD = 8
    }
}