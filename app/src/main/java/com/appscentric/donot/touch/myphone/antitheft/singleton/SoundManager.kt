package com.appscentric.donot.touch.myphone.antitheft.singleton

import android.media.AudioAttributes
import android.media.SoundPool

object SoundManager {
    val soundPool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
}
