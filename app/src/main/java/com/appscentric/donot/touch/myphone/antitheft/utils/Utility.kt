package com.appscentric.donot.touch.myphone.antitheft.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem
import com.appscentric.donot.touch.myphone.antitheft.model.PrankData
import com.appscentric.donot.touch.myphone.antitheft.model.Wallpaper
import com.appscentric.donot.touch.myphone.antitheft.singleton.FirebaseAnalyticsSingleton
import com.google.android.gms.ads.AdSize
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class Utility {

    companion object {
        var homeClickCounter = 0
        var homeRewardedClickCounter = 0
        var settingsClickCounter = 0
        var backPressClickCounter = 0
        var premiumPopupClickCounter = 0
        var backPressCount = 4

        fun parseDurationToMillis(duration: String): Long {
            val durationPattern = Regex("(\\d+)([sm])")
            val matchResult = durationPattern.find(duration)
                ?: throw IllegalArgumentException("Invalid duration format")
            val (value, unit) = matchResult.destructured
            return value.toLong() * when (unit) {
                "s" -> 1000
                "m" -> 1000 * 60
                else -> throw IllegalArgumentException("Invalid duration unit")
            }
        }

        fun mapSliderValueToDesiredRange(value: Float): Int {
            return when {
                value < 0.0 || value > 100.0 -> throw IllegalArgumentException("Value should be between 0.0 and 100.0")
                value < 20.0 -> 32000
                value < 40.0 -> 31000
                value < 60.0 -> 30000
                value < 80.0 -> 29000
                else -> 25000
            }
        }

        val  items = mutableListOf(
            createHomeItem(R.string.police, R.drawable.police, R.raw.police, R.raw.sound_police, 0, false, false),
            createHomeItem(R.string.doorbell, R.drawable.bell, R.raw.doorbell, R.raw.sound_door_bell, 1, false, false),
            createHomeItem(R.string.hello, R.drawable.hello, R.raw.hello, R.raw.sound_hello, 2, false, false),
            createHomeItem(R.string.laughing, R.drawable.laughing, R.raw.laughing, R.raw.sound_laughing, 3, false, false),
            createHomeItem(R.string.sneeze, R.drawable.sneeze, R.raw.sneeze, R.raw.sound_sneezing, 4, false, false),
            createHomeItem(R.string.piano, R.drawable.piano, R.raw.piano, R.raw.sound_piano, 5, false, false),
            createHomeItem(R.string.rooster, R.drawable.rooster, R.raw.rooster, R.raw.sound_rooster, 6, false, false),
            createHomeItem(R.string.alarm_clock, R.drawable.alarm, R.raw.alarm_clock, R.raw.sound_clock_alarm, 7, false, false),
            createHomeItem(R.string.train, R.drawable.train, R.raw.train, R.raw.sound_train, 8, false, false),
            createHomeItem(R.string.whistle, R.drawable.whistle, R.raw.whistle, R.raw.sound_whistle, 9, false, false),
            createHomeItem(R.string.wind, R.drawable.wind, R.raw.wind, R.raw.sound_wind_chimes, 10, false, false),
            createHomeItem(R.string.guitar, R.drawable.guitar, R.raw.guitar, R.raw.sound_gittar, 11, false, false),
            createHomeItem(R.string.cat, R.drawable.cat, R.raw.cat_anim, R.raw.cat, 12, false, false),
            createHomeItem(R.string.dog, R.drawable.dog, R.raw.dog_anim, R.raw.dog, 13, false, false),
            createHomeItem(R.string.gun, R.drawable.gun, R.raw.gun_anim, R.raw.gun, 14, false, false),

            createHomeItem(R.string.alarm_one, R.drawable.sound_1, R.raw.sound_1, R.raw.sound_1_sound, 15, true,false),
            createHomeItem(R.string.alarm_two, R.drawable.sound_2, R.raw.sound_2, R.raw.sound_2_sound, 16, true,false),
            createHomeItem(R.string.alarm_three, R.drawable.sound_3, R.raw.sound_3, R.raw.sound_3_sound, 17, true,false),
            createHomeItem(R.string.alarm_four, R.drawable.sound_4, R.raw.sound_4, R.raw.sound_4_sound, 18, true,false),
            createHomeItem(R.string.alarm_five, R.drawable.sound_5, R.raw.sound_5, R.raw.sound_5_sound, 19, true,false),
            createHomeItem(R.string.alarm_six, R.drawable.sound_6, R.raw.sound_6, R.raw.sound_6_sound, 20, true,false),
            createHomeItem(R.string.alarm_seven, R.drawable.sound_7, R.raw.sound_7, R.raw.sound_7_sound, 21, true,false),
            createHomeItem(R.string.alarm_eight, R.drawable.sound_8, R.raw.sound_8, R.raw.sound_8_sound, 22, true,false),
            createHomeItem(R.string.alarm_nine, R.drawable.sound_9, R.raw.sound_9, R.raw.sound_9_sound, 23, true,false)
        )


        val itemsPremium = mutableListOf(
            createHomeItem(R.string.police, R.drawable.police, R.raw.police, R.raw.sound_police, 0, false, false),
            createHomeItem(R.string.doorbell, R.drawable.bell, R.raw.doorbell, R.raw.sound_door_bell, 1, false, false),
            createHomeItem(R.string.hello, R.drawable.hello, R.raw.hello, R.raw.sound_hello, 2, false, false),
            createHomeItem(R.string.laughing, R.drawable.laughing, R.raw.laughing, R.raw.sound_laughing, 3, false, false),
            createHomeItem(R.string.sneeze, R.drawable.sneeze, R.raw.sneeze, R.raw.sound_sneezing, 4, false, false),
            createHomeItem(R.string.piano, R.drawable.piano, R.raw.piano, R.raw.sound_piano, 5, false, false),
            createHomeItem(R.string.rooster, R.drawable.rooster, R.raw.rooster, R.raw.sound_rooster, 6, false, false),
            createHomeItem(R.string.alarm_clock, R.drawable.alarm, R.raw.alarm_clock, R.raw.sound_clock_alarm, 7, false, false),
            createHomeItem(R.string.train, R.drawable.train, R.raw.train, R.raw.sound_train, 8, false, false),
            createHomeItem(R.string.whistle, R.drawable.whistle, R.raw.whistle, R.raw.sound_whistle, 9, false, false),
            createHomeItem(R.string.wind, R.drawable.wind, R.raw.wind, R.raw.sound_wind_chimes, 10, false, false),
            createHomeItem(R.string.guitar, R.drawable.guitar, R.raw.guitar, R.raw.sound_gittar, 11, false, false),
            createHomeItem(R.string.cat, R.drawable.cat, R.raw.cat_anim, R.raw.cat, 12, false, false),
            createHomeItem(R.string.dog, R.drawable.dog, R.raw.dog_anim, R.raw.dog, 13, false, false),
            createHomeItem(R.string.gun, R.drawable.gun, R.raw.gun_anim, R.raw.gun, 14, false, false),

            createHomeItem(R.string.alarm_one, R.drawable.sound_1, R.raw.sound_1, R.raw.sound_1_sound, 15, true,false),
            createHomeItem(R.string.alarm_two, R.drawable.sound_2, R.raw.sound_2, R.raw.sound_2_sound, 16, true,false),
            createHomeItem(R.string.alarm_three, R.drawable.sound_3, R.raw.sound_3, R.raw.sound_3_sound, 17, true,false),
            createHomeItem(R.string.alarm_four, R.drawable.sound_4, R.raw.sound_4, R.raw.sound_4_sound, 18, true,false),
            createHomeItem(R.string.alarm_five, R.drawable.sound_5, R.raw.sound_5, R.raw.sound_5_sound, 19, true,false),
            createHomeItem(R.string.alarm_six, R.drawable.sound_6, R.raw.sound_6, R.raw.sound_6_sound, 20, true,false),
            createHomeItem(R.string.alarm_seven, R.drawable.sound_7, R.raw.sound_7, R.raw.sound_7_sound, 21, true,false),
            createHomeItem(R.string.alarm_eight, R.drawable.sound_8, R.raw.sound_8, R.raw.sound_8_sound, 22, true,false),
            createHomeItem(R.string.alarm_nine, R.drawable.sound_9, R.raw.sound_9, R.raw.sound_9_sound, 23, true,false)
        )

        private fun createHomeItem(
            titleResId: Int,
            imageResId: Int,
            animResId: Int,
            soundResId: Int,
            position: Int,
            isSound: Boolean,
            isRewarded: Boolean
        ): HomeItem {
            return HomeItem(titleResId, imageResId, animResId, soundResId, position, isSound,isRewarded)
        }

        val prankItems = mutableListOf(
            createPrankItem(R.raw.ic_horn_prank, R.raw.ic_bg_1, R.raw.ic_btn_1,R.raw.horn_sound),
            createPrankItem(R.raw.ic_gun_prank, R.raw.ic_bg_8, R.raw.ic_btn_8,R.raw.electric_current_circuit),
            createPrankItem(R.raw.ic_shaving_prank, R.raw.ic_bg_3, R.raw.ic_btn_3,R.raw.clipper_sound),
            createPrankItem(R.raw.ic_boo_prank, R.raw.ic_bg_2, R.raw.ic_btn_2,R.raw.ghost_sound),
            createPrankItem(R.raw.ic_halloween_prank, R.raw.ic_bg_4, R.raw.ic_btn_4,R.raw.horror_sounds),
            createPrankItem(R.raw.ic_bomb_prank, R.raw.ic_bg_5, R.raw.ic_btn_5,R.raw.bomb_sound),
            createPrankItem(R.raw.ic_emoji_prank, R.raw.ic_bg_6, R.raw.ic_btn_6,R.raw.meme_sound),
            createPrankItem(R.raw.ic_christmas_prank, R.raw.ic_bg_7, R.raw.ic_btn_7,R.raw.santa_sound),
            createPrankItem(R.raw.ic_weather_prank, R.raw.ic_bg_9, R.raw.ic_btn_9,R.raw.rain_sound),
            createPrankItem(R.raw.ic_panguin_prank, R.raw.ic_bg_10, R.raw.ic_btn_10,R.raw.tiger_sound)
        )

        private fun createPrankItem(
            imageResId: Int,
            backgroundResId: Int,
            buttonResId: Int,
            soundResId: Int
        ): PrankData {
            return PrankData(imageResId, backgroundResId, buttonResId,soundResId)
        }

        fun showSettingsDialog(context: Context) {
            MaterialAlertDialogBuilder(context).apply {
                setTitle(context.getString(R.string.permission_required))
                setMessage(context.getString(R.string.this_app_needs_permission_to_use_this_feature_you_can_grant_them_in_app_settings))
                setPositiveButton(context.getString(R.string.app_settings)) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                }
                setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                create().show()
            }
        }

        fun isServiceRunning(context: Context,serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        fun customFirebaseEvent(context: Context, itemName: String) {
            val mFirebaseAnalytics = FirebaseAnalyticsSingleton.getInstance(context)
            val params = Bundle()
            params.putString("event_name", itemName)
            mFirebaseAnalytics.logEvent(itemName, params)
        }

        fun getBannerAdSize(context: Context): AdSize {
            val displayMetrics = context.resources.displayMetrics
            val density = displayMetrics.density
            val adWidth = (displayMetrics.widthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
        }


        val wallpaperResources = mutableListOf(
            createWallpaperItem(R.raw.wallpaper45, false),
            createWallpaperItem(R.raw.wallpaper46, false),
            createWallpaperItem(R.raw.wallpaper47, false),
            createWallpaperItem(R.raw.wallpaper48, false),
            createWallpaperItem(R.raw.wallpaper49, false),
            createWallpaperItem(R.raw.wallpaper50, false),
            createWallpaperItem(R.raw.wallpaper51, false),
            createWallpaperItem(R.raw.wallpaper52, false),
            createWallpaperItem(R.raw.wallpaper53, false),
            createWallpaperItem(R.raw.wallpaper54, false),

            createWallpaperItem(R.raw.wallpaper1, false),
            createWallpaperItem(R.raw.wallpaper2, false),
            createWallpaperItem(R.raw.wallpaper4, false),
            createWallpaperItem(R.raw.wallpaper5, false),
            createWallpaperItem(R.raw.wallpaper6, false),
            createWallpaperItem(R.raw.wallpaper7, false),
            createWallpaperItem(R.raw.wallpaper8, false),
            createWallpaperItem(R.raw.wallpaper9, false),
            createWallpaperItem(R.raw.wallpaper10, false),
            createWallpaperItem(R.raw.wallpaper11, false),
            createWallpaperItem(R.raw.wallpaper12, false),
            createWallpaperItem(R.raw.wallpaper13, false),
            createWallpaperItem(R.raw.wallpaper14, false),
            createWallpaperItem(R.raw.wallpaper15, false),
            createWallpaperItem(R.raw.wallpaper16, false),
            createWallpaperItem(R.raw.wallpaper17, false),
            createWallpaperItem(R.raw.wallpaper18, false),
            createWallpaperItem(R.raw.wallpaper20, false),
            createWallpaperItem(R.raw.wallpaper21, false),
            createWallpaperItem(R.raw.wallpaper22, false),
            createWallpaperItem(R.raw.wallpaper24, false),
            createWallpaperItem(R.raw.wallpaper25, false),
            createWallpaperItem(R.raw.wallpaper26, false),
            createWallpaperItem(R.raw.wallpaper27, false),
            createWallpaperItem(R.raw.wallpaper28, false),
            createWallpaperItem(R.raw.wallpaper29, false),
            createWallpaperItem(R.raw.wallpaper30, false),
            createWallpaperItem(R.raw.wallpaper31, false),
            createWallpaperItem(R.raw.wallpaper32, false),
            createWallpaperItem(R.raw.wallpaper33, false),
            createWallpaperItem(R.raw.wallpaper34, false),
            createWallpaperItem(R.raw.wallpaper35, false),
            createWallpaperItem(R.raw.wallpaper36, false),
            createWallpaperItem(R.raw.wallpaper37, false),
            createWallpaperItem(R.raw.wallpaper38, false),
            createWallpaperItem(R.raw.wallpaper39, false),
            createWallpaperItem(R.raw.wallpaper40, false),
            createWallpaperItem(R.raw.wallpaper41, false),
            createWallpaperItem(R.raw.wallpaper42, false),
            createWallpaperItem(R.raw.wallpaper43, false),
            createWallpaperItem(R.raw.wallpaper44, false),
        )

        val wallpaperResourcesPremium = mutableListOf(
            createWallpaperItem(R.raw.wallpaper45, false),
            createWallpaperItem(R.raw.wallpaper46, false),
            createWallpaperItem(R.raw.wallpaper47, false),
            createWallpaperItem(R.raw.wallpaper48, false),
            createWallpaperItem(R.raw.wallpaper49, false),
            createWallpaperItem(R.raw.wallpaper50, false),
            createWallpaperItem(R.raw.wallpaper51, false),
            createWallpaperItem(R.raw.wallpaper52, false),
            createWallpaperItem(R.raw.wallpaper53, false),
            createWallpaperItem(R.raw.wallpaper54, false),

            createWallpaperItem(R.raw.wallpaper1, false),
            createWallpaperItem(R.raw.wallpaper2, false),
            createWallpaperItem(R.raw.wallpaper4, false),
            createWallpaperItem(R.raw.wallpaper5, false),
            createWallpaperItem(R.raw.wallpaper6, false),
            createWallpaperItem(R.raw.wallpaper7, false),
            createWallpaperItem(R.raw.wallpaper8, false),
            createWallpaperItem(R.raw.wallpaper9, false),
            createWallpaperItem(R.raw.wallpaper10, false),
            createWallpaperItem(R.raw.wallpaper11, false),
            createWallpaperItem(R.raw.wallpaper12, false),
            createWallpaperItem(R.raw.wallpaper13, false),
            createWallpaperItem(R.raw.wallpaper14, false),
            createWallpaperItem(R.raw.wallpaper15, false),
            createWallpaperItem(R.raw.wallpaper16, false),
            createWallpaperItem(R.raw.wallpaper17, false),
            createWallpaperItem(R.raw.wallpaper18, false),
            createWallpaperItem(R.raw.wallpaper20, false),
            createWallpaperItem(R.raw.wallpaper21, false),
            createWallpaperItem(R.raw.wallpaper22, false),
            createWallpaperItem(R.raw.wallpaper24, false),
            createWallpaperItem(R.raw.wallpaper25, false),
            createWallpaperItem(R.raw.wallpaper26, false),
            createWallpaperItem(R.raw.wallpaper27, false),
            createWallpaperItem(R.raw.wallpaper28, false),
            createWallpaperItem(R.raw.wallpaper29, false),
            createWallpaperItem(R.raw.wallpaper30, false),
            createWallpaperItem(R.raw.wallpaper31, false),
            createWallpaperItem(R.raw.wallpaper32, false),
            createWallpaperItem(R.raw.wallpaper33, false),
            createWallpaperItem(R.raw.wallpaper34, false),
            createWallpaperItem(R.raw.wallpaper35, false),
            createWallpaperItem(R.raw.wallpaper36, false),
            createWallpaperItem(R.raw.wallpaper37, false),
            createWallpaperItem(R.raw.wallpaper38, false),
            createWallpaperItem(R.raw.wallpaper39, false),
            createWallpaperItem(R.raw.wallpaper40, false),
            createWallpaperItem(R.raw.wallpaper41, false),
            createWallpaperItem(R.raw.wallpaper42, false),
            createWallpaperItem(R.raw.wallpaper43, false),
            createWallpaperItem(R.raw.wallpaper44, false),
        )


        private fun createWallpaperItem(
            titleResId: Int,
            isRewarded: Boolean
        ): Wallpaper {
            return Wallpaper(titleResId, isRewarded)
        }
    }
}