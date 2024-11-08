package com.appscentric.donot.touch.myphone.antitheft.features.wallpaper

import android.app.ProgressDialog
import android.app.WallpaperManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentWallpaperViewBinding
import com.appscentric.donot.touch.myphone.antitheft.dialog.PremiumFragment
import com.appscentric.donot.touch.myphone.antitheft.manager.InterstitialAdManager
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.manager.SmallNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.model.Wallpaper
import com.appscentric.donot.touch.myphone.antitheft.singleton.SoundManager
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants
import com.appscentric.donot.touch.myphone.antitheft.utils.NetworkUtils
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.premiumPopupClickCounter
import com.appscentric.donot.touch.myphone.antitheft.viewmodel.AdViewModel
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.android.ext.android.inject
import java.io.IOException
import java.util.Locale

class WallpaperViewFragment : DialogFragment() {
    override fun getTheme(): Int = R.style.DialogTheme

    private var _binding: FragmentWallpaperViewBinding? = null
    private val binding get() = _binding!!

    private var wallpaper: Wallpaper? = null

    private var ad: InterstitialAd? = null
    private val adViewModel: AdViewModel by viewModels()

    private val preferencesManager by inject<PreferencesManager>()
    private var soundId: Int = 0
    private var wasWallpaperSet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            wallpaper = it.getParcelable(ARG_WALLPAPER_DATA)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWallpaperViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        soundId = SoundManager.soundPool.load(requireContext(), R.raw.click_sound, 1)

        wallpaper?.let {

            Glide.with(requireContext())
                .load(it.resourceId)
                .into(binding.imageViewWallpaper)

            when {
                preferencesManager.getHomeWallpaper() == it.resourceId && preferencesManager.getLockWallpaper() == it.resourceId -> {
                    binding.applyWallpaperBtn.setImageResource(R.drawable.disable_btn)
                    binding.applyWallpaperBtn.isEnabled = false
                }

                preferencesManager.getHomeWallpaper() == it.resourceId -> {
                    binding.applyWallpaperBtn.setImageResource(R.drawable.disable_btn)
                    binding.applyWallpaperBtn.isEnabled = false
                }

                preferencesManager.getLockWallpaper() == it.resourceId -> {
                    binding.applyWallpaperBtn.setImageResource(R.drawable.disable_btn)
                    binding.applyWallpaperBtn.isEnabled = false
                }
            }
        }

        binding.apply {
            toolbar.setOnClickListener {
                if (preferencesManager.isTapSound) playClickSound()
                dismiss()
            }

            applyWallpaperBtn.setOnClickListener {
                if (preferencesManager.isTapSound) playClickSound()
                showApplyDialog()
            }

            if (NetworkUtils.isNetworkConnected(requireContext()) && !preferencesManager.isRemoveAdsPurchased()) {
                displayNativeAd()
            } else {
                animationShimmer.visibility = View.GONE
            }
        }
        // Show the premium popup after the 5th dismissal and then every 10th dismissal
        if (NetworkUtils.isNetworkConnected(requireContext()) && !preferencesManager.isRemoveAdsPurchased()) {
            if (++premiumPopupClickCounter % 5 == 0) showPremiumPopup()
        }
    }

    private fun showPremiumPopup() {
        val fragmentManager = parentFragmentManager
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

    private fun playClickSound() {
        if (preferencesManager.isTapSound) {
            SoundManager.soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun showScreenWithAd(progressDialog: ProgressDialog, resourceId: Int, flag: Int) {
        ad = InterstitialAdManager.getAd()
        ad?.let {
            it.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = false
                }

                override fun onAdDismissedFullScreenContent() {
                    Constants.SHOW_APP_OPEN = true

                    applyWallpaperLogic(progressDialog, resourceId, flag)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Constants.SHOW_APP_OPEN = true
                    applyWallpaperLogic(progressDialog, resourceId, flag)
//                    showApplyDialog()
                }
            }
            it.show(requireActivity())
        } ?: applyWallpaperLogic(progressDialog, resourceId, flag)
        adViewModel.loadInterstitialAd()
    }

    private fun displayNativeAd() {
        SmallNativeAdManager.getAd()?.let { ad ->
            binding.templateView.apply {
                setNativeAd(ad)
                visibility = View.VISIBLE
            }
        } ?: run {
            binding.templateView.visibility = View.GONE
        }
    }

    private fun showApplyDialog() {
        wallpaper?.let { resId ->
            val items = arrayOf("Home Screen", "Lock Screen", "Alarm Popup", "Both")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Set Wallpaper")
                .setItems(items) { dialog, which ->
                    when (which) {
                        0 -> setWallpaper(dialog, resId.resourceId, WallpaperManager.FLAG_SYSTEM)
                        1 -> setWallpaper(dialog, resId.resourceId, WallpaperManager.FLAG_LOCK)
                        2 -> setWallpaper(dialog, resId.resourceId, 1910)
                        3 -> setWallpaper(
                            dialog,
                            resId.resourceId,
                            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                        )
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setWallpaper(dialog: DialogInterface, resourceId: Int, flag: Int) {
        dialog.dismiss()
        // Create and show the progress dialog on the main thread
        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Setting wallpaper...")
            setCancelable(false)
            show()
        }

        if (!preferencesManager.isRemoveAdsPurchased()) {
            showScreenWithAd(progressDialog, resourceId, flag)
        } else {
            applyWallpaperLogic(progressDialog, resourceId, flag)
        }

    }

    override fun onDestroyView() {
        ad?.fullScreenContentCallback = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_WALLPAPER_DATA = "wallpaper_data"

        @JvmStatic
        fun newInstance(wallpaper: Wallpaper) =
            WallpaperViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_WALLPAPER_DATA, wallpaper)
                }
            }
    }

    private fun applyWallpaperLogic(progressDialog: ProgressDialog, resourceId: Int, flag: Int) {

        if (flag == 1910) {
            preferencesManager.saveWallpaper(resourceId)
            progressDialog.dismiss()

            Toast.makeText(
                requireContext(),
                "Popup Image set Successfully!",
                Toast.LENGTH_SHORT
            ).show()

        } else {
            lifecycleScope.launch {
                try {
                    // Decode bitmap on IO dispatcher to avoid blocking the main thread
                    val bitmap = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeResource(resources, resourceId)
                    }
                    val wallpaperManager =
                        WallpaperManager.getInstance(requireContext().applicationContext)

                    withTimeout(10000L) {
                        // Set wallpaper based on the flag
                        withContext(Dispatchers.IO) {
                            wallpaperManager.setBitmap(bitmap, null, true, flag)
                        }
                    }
                    bitmap.recycle()

                    // Ensure UI updates happen on main dispatcher
                    withContext(Dispatchers.Main.immediate) {
                        Toast.makeText(
                            requireContext(),
                            "Wallpaper set successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        val wallpaperType = when (flag) {
                            WallpaperManager.FLAG_SYSTEM -> "home"
                            WallpaperManager.FLAG_LOCK -> "lock"
                            else -> "both"
                        }
                        wasWallpaperSet = true
                        preferencesManager.saveWallpaper(resourceId, wallpaperType)
                        progressDialog.dismiss()
                    }

                } catch (e: Exception) {
                    // Ensure UI-related error handling on the main dispatcher
                    withContext(Dispatchers.Main.immediate) {
                        progressDialog.dismiss()
                        val message = when (e) {
                            is IOException -> "Failed to set wallpaper due to I/O error."
                            is SecurityException -> "Permission denied. Cannot set wallpaper."
                            is IllegalArgumentException -> "Invalid bitmap or flag provided."
                            is TimeoutCancellationException -> "Setting wallpaper took too long and was cancelled."
                            else -> "An unexpected error occurred."
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }
}