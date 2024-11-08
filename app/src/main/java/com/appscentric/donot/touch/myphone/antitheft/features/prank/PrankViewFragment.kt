package com.appscentric.donot.touch.myphone.antitheft.features.prank

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.databinding.FragmentPrankViewBinding
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.manager.SmallNativeAdManager
import com.appscentric.donot.touch.myphone.antitheft.model.PrankData
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdView
import org.koin.android.ext.android.inject

class PrankViewFragment : DialogFragment() {

    private var _binding: FragmentPrankViewBinding? = null
    private val binding get() = _binding!!

    private var prankData: PrankData? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var isLooping = false
//    private var duration: String = "15s"

    private val audioManager: AudioManager by lazy { requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    override fun getTheme(): Int = R.style.DialogTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            prankData = it.getParcelable(ARG_PRANK_DATA)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrankViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prankData?.let { data ->
            with(binding) {
                // Handle toolbar navigation click
                materialToolbar.setNavigationOnClickListener { dismiss() }
                // Load image using Glide
                Glide.with(root.context)
                    .load(data.resourceId)
                    .into(imageViewPrank)

                // Handle play/pause button click
                floatingActionButton.setOnClickListener {
                    if (isPlaying) pauseSound() else playSound()
                }

//                chipGroup.setOnCheckedStateChangeListener { chipGroup, checkedIds ->
//                    if (checkedIds.isNotEmpty()) {
//                        val selectedChipId = checkedIds.first()
//                        val selectedChip = chipGroup.findViewById<Chip>(selectedChipId)
//                        duration = selectedChip.text.toString()
//
//                        if (isPlaying) pauseSound()
//                    }
//                }
//
//                // Handle radio group selection changes
                radioGroup.setOnCheckedChangeListener { _, checkedId ->
                    val radioButton: RadioButton = view.findViewById(checkedId)
                    isLooping = radioButton.text.toString() == getString(R.string.loop)

                    // Pause the sound if it is playing
                    if (isPlaying) pauseSound()

                    // Set visibility based on the isLooping flag
//                    val visibility = if (isLooping) View.VISIBLE else View.GONE
//                    textViewDuration.visibility = visibility
//                    chipGroup.visibility = visibility
                }

                sliderVolume.apply {
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
            }
        }

        // Set up MediaPlayer once
        prankData?.let {
            mediaPlayer = MediaPlayer.create(requireContext(), it.soundId).apply {
                setOnPreparedListener {
                    isLooping = false
                }
            }
        }
    }

    private fun playSound() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                if (player.isLooping != isLooping) {
                    player.isLooping = isLooping
                }
                player.start()
                isPlaying = true
                binding.floatingActionButton.setImageResource(R.drawable.round_pause_24)

                // Set up a listener to detect when playback completes
                player.setOnCompletionListener {
                    isPlaying = false
                    binding.floatingActionButton.setImageResource(R.drawable.round_play_arrow_24)
                }
            }
        }
    }

    private fun pauseSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                binding.floatingActionButton.setImageResource(R.drawable.round_play_arrow_24)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                binding.sliderVolume.value =
                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(
            volumeReceiver,
            IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        )
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(volumeReceiver)
    }

    companion object {
        private const val ARG_PRANK_DATA = "prank_data"

        @JvmStatic
        fun newInstance(item: PrankData) =
            PrankViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRANK_DATA, item)
                }
            }
    }
}