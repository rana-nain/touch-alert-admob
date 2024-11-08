package com.appscentric.donot.touch.myphone.antitheft.manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem

class PreferencesManager(context: Context) {
    companion object {
        private const val PREFERENCES_FILE_KEY = "com.appscentric.donot.touch.myphone.antitheft"
        private const val SELECTED_LANGUAGE_KEY = "selectedLanguage"
        private const val SELECTED_FLASH_KEY = "selectedFlash"
        private const val SELECTED_VIBRATE_KEY = "selectedVibrate"
        private const val SELECTED_SENSITIVITY = "sensitivity"
        private const val SELECTED_CODE = "pincode"
        private const val IS_FIRST_TIME_LAUNCH = "IS_FIRST_TIME_LAUNCH"
        private const val SELECTED_FLASHLIGHT_MODE_KEY = "SELECTED_FLASHLIGHT_MODE_KEY"
        private const val SELECTED_VIBRATION_MODE_KEY = "SELECTED_VIBRATION_MODE_KEY"
        private const val IS_OVERLAY = "isOverlay"
        private const val IS_TAP_SOUND = "isTapSound"
        private const val IS_WALLPAPER_SET = "wasWallpaperSet"
        private const val IS_CHARGING = "isCharging"
        private const val IS_BATTERY = "isBattery"
        private const val IS_AUTO_CLOSE_APP = "isAutoCloseApp"
        private const val IS_POCKET_MODE_APP = "isPocketMode"

        private const val SELECTED_HOME_WALLPAPER_KEY = "selectedHomeWallpaper"
        private const val SELECTED_LOCK_WALLPAPER_KEY = "selectedLockWallpaper"
        private const val SELECTED_WALLPAPER_KEY = "selectedWallpaper"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_FILE_KEY, Context.MODE_PRIVATE)

    fun saveSelectedLanguage(languageCode: String) {
        with(sharedPreferences.edit()) {
            putString(SELECTED_LANGUAGE_KEY, languageCode)
            apply()
        }
    }

    fun getSelectedLanguage(): String? = sharedPreferences.getString(SELECTED_LANGUAGE_KEY, "en")


    fun saveSelectedFlashMode(flashMode: String) {
        with(sharedPreferences.edit()) {
            putString(SELECTED_FLASH_KEY, flashMode)
            apply()
        }
    }

    fun getSelectedFlash(): String? = sharedPreferences.getString(SELECTED_FLASH_KEY, "Default")

    fun saveSelectedVibrateMode(vibrateMode: String) {
        with(sharedPreferences.edit()) {
            putString(SELECTED_VIBRATE_KEY, vibrateMode)
            apply()
        }
    }

    fun getSelectedVibrate(): String? = sharedPreferences.getString(SELECTED_VIBRATE_KEY, "Default")


    fun saveSensitivity(sensitivity: Int = 50) {
        with(sharedPreferences.edit()) {
            putInt(SELECTED_SENSITIVITY, sensitivity)
            apply()
        }
        Log.d("TAG_SHARE_PREF", "saveSelectedVibrationMode: $sensitivity")
    }

    fun getSensitivity(): Int = sharedPreferences.getInt(SELECTED_SENSITIVITY, 50)


    var isFirstTimeLaunch: Boolean
        get() = sharedPreferences.getBoolean(IS_FIRST_TIME_LAUNCH, true)
        set(isFirstTime) {
            with(sharedPreferences.edit()) {
                putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime)
                apply() // or commit() if you need an immediate write-back
            }
        }

    var isOverlay: Boolean
        get() = sharedPreferences.getBoolean(IS_OVERLAY, false)
        set(isOverlay) {
            with(sharedPreferences.edit()) {
                putBoolean(IS_OVERLAY, isOverlay)
                apply() // or commit() if you need an immediate write-back
            }
            Log.d("TAG_SHARE_PREF", "saveSelectedVibrationMode: $isOverlay")

        }

    var isTapSound: Boolean
        get() = sharedPreferences.getBoolean(IS_TAP_SOUND, true)
        set(isTap) {
            with(sharedPreferences.edit()) {
                putBoolean(IS_TAP_SOUND, isTap)
                apply() // or commit() if you need an immediate write-back
            }
            Log.d("TAG_SHARE_PREF", "saveSelectedVibrationMode: $isTap")

        }

    fun saveItemDetails(
        selectedItem: HomeItem,
        vibration: Boolean,
        flash: Boolean,
        sound: Boolean,
        volume: Int,
        volumeDuration: String
    ) {
        with(sharedPreferences.edit()) {
            putInt("itemName", selectedItem.nameResId)
            putInt("itemThumb", selectedItem.thumbnail)
            putInt("itemAnim", selectedItem.anim)
            putInt("itemSound", selectedItem.sound)
            putInt("itemPos", selectedItem.position)
            putBoolean("isAnim", selectedItem.isSound)

            putBoolean("isVibrate", vibration)
            putBoolean("isFlash", flash)
            putBoolean("isSound", sound)
            putInt("volumeInt", volume)
            putString("volumeDuration", volumeDuration)
            apply()
        }
    }

    fun getAllData(): Triple<HomeItem, Boolean, Map<String, Any>> {
        val itemName = sharedPreferences.getInt("itemName", R.string.police)
        val itemThumb = sharedPreferences.getInt("itemThumb", R.string.police)
        val itemAnim = sharedPreferences.getInt("itemAnim", R.raw.police)
        val itemSound = sharedPreferences.getInt("itemSound", R.raw.sound_police)
        val itemPos = sharedPreferences.getInt("itemPos", 0)
        val isAnim = sharedPreferences.getBoolean("isAnim", false)

        val isVibrate = sharedPreferences.getBoolean("isVibrate", true)
        val isFlash = sharedPreferences.getBoolean("isFlash", true)
        val isSound = sharedPreferences.getBoolean("isSound", true)
        val volumeInt = sharedPreferences.getInt("volumeInt", 80)
        val volumeDuration = sharedPreferences.getString("volumeDuration", "30s") ?: "30s"

        val selectedItem =
            HomeItem(itemName, itemThumb, itemAnim, itemSound, itemPos, isAnim, false)
        val dataMap = mapOf(
            "itemName" to itemName,
            "itemThumb" to itemThumb,
            "itemAnim" to itemAnim,
            "itemSound" to itemSound,
            "isAnim" to isAnim,
            "isVibrate" to isVibrate,
            "isFlash" to isFlash,
            "isSound" to isSound,
            "volumeInt" to volumeInt,
            "volumeDuration" to volumeDuration
        )

        return Triple(selectedItem, isVibrate, dataMap)
    }

    fun saveSelectedFlashlightMode(modeArray: IntArray) {
        val serializedMode = modeArray.joinToString(",") // Serialize the IntArray to a String
        with(sharedPreferences.edit()) {
            putString(SELECTED_FLASHLIGHT_MODE_KEY, serializedMode)
            apply()
        }
        Log.d("TAG_SHARE_PREF", "saveSelectedVibrationMode: $serializedMode")
    }

    fun getSelectedFlashlightMode(): IntArray {
        val serializedMode = sharedPreferences.getString(
            SELECTED_FLASHLIGHT_MODE_KEY,
            flashModes[0].joinToString(",")
        )
        return serializedMode?.split(",")?.map { it.toInt() }?.toIntArray() ?: flashModes[0]
    }

    private val flashModes = arrayOf(
        intArrayOf(100), // Default mode (100ms on, 1000ms off)
        intArrayOf(100, 200, 100, 200, 100, 200), // Disco mode (flashes rapidly)
        intArrayOf(500, 500, 500, 500, 500, 500) // SOS mode (SOS pattern)
    )

    fun saveSelectedVibrationMode(pattern: LongArray) {
        val serializedPattern = pattern.joinToString(",") // Serialize the LongArray to a String
        with(sharedPreferences.edit()) {
            putString(SELECTED_VIBRATION_MODE_KEY, serializedPattern)
            apply()
        }
        Log.d("TAG_SHARE_PREF", "saveSelectedVibrationMode: $serializedPattern")
    }

    fun getSelectedVibrationMode(): LongArray {
        val serializedPattern = sharedPreferences.getString(SELECTED_VIBRATION_MODE_KEY, null)
        return serializedPattern?.split(",")?.map { it.toLong() }?.toLongArray()
            ?: getDefaultVibrationPattern()
    }

    private fun getDefaultVibrationPattern(): LongArray {
        return longArrayOf(0, 500) // Default vibration pattern
    }

    var isChargingRunningSet: Boolean
        get() = sharedPreferences.getBoolean(IS_WALLPAPER_SET, false)
        set(wasWallpaperSet) {
            with(sharedPreferences.edit()) {
                putBoolean(IS_WALLPAPER_SET, wasWallpaperSet)
                apply() // or commit() if you need an immediate write-back
            }
            Log.d("TAG_SHARE_PREF", "saveSelectedVibrationMode: $wasWallpaperSet")

        }

    var isCharging: Boolean
        get() = sharedPreferences.getBoolean(IS_CHARGING, false)
        set(isCharge) {
            with(sharedPreferences.edit()) {
                putBoolean(IS_CHARGING, isCharge)
                apply() // or commit() if you need an immediate write-back
            }
        }

    var isBatteryFullDetection: Boolean
        get() = sharedPreferences.getBoolean(IS_BATTERY, false)
        set(isCharge) {
            with(sharedPreferences.edit()) {
                putBoolean(IS_BATTERY, isCharge)
                apply() // or commit() if you need an immediate write-back
            }
        }

    var autoCloseApp: Boolean
        get() = sharedPreferences.getBoolean(IS_AUTO_CLOSE_APP, false)
        set(isAutoClose) {
            with(sharedPreferences.edit()) {
                putBoolean(IS_AUTO_CLOSE_APP, isAutoClose)
                apply() // or commit() if you need an immediate write-back
            }
        }

    var pocketMode: Boolean
        get() = sharedPreferences.getBoolean(IS_POCKET_MODE_APP, false)
        set(isPocketMode) {
            with(sharedPreferences.edit()) {
                putBoolean(IS_POCKET_MODE_APP, isPocketMode)
                apply() // or commit() if you need an immediate write-back
            }
        }

    fun saveWallpaper(resourceId: Int, type: String) {
        with(sharedPreferences.edit()) {
            when (type) {
                "home" -> putInt(SELECTED_HOME_WALLPAPER_KEY, resourceId)
                "lock" -> putInt(SELECTED_LOCK_WALLPAPER_KEY, resourceId)
                "both" -> {
                    putInt(SELECTED_HOME_WALLPAPER_KEY, resourceId)
                    putInt(SELECTED_LOCK_WALLPAPER_KEY, resourceId)
                }
            }
            apply()
        }
    }

    fun getHomeWallpaper(): Int = sharedPreferences.getInt(SELECTED_HOME_WALLPAPER_KEY, -1)
    fun getLockWallpaper(): Int = sharedPreferences.getInt(SELECTED_LOCK_WALLPAPER_KEY, -1)

    fun saveWallpaper(wallpaper: Int = R.raw.wallpaper1) {
        with(sharedPreferences.edit()) {
            putInt(SELECTED_WALLPAPER_KEY, wallpaper)
            apply()
        }
        Log.d("TAG_SHARE_PREF", "saveSelectedVibrationMode: $wallpaper")
    }

    fun getWallpaper(): Int = sharedPreferences.getInt(SELECTED_WALLPAPER_KEY, R.raw.wallpaper1)

    fun isRemoveAdsPurchased(): Boolean {
        return sharedPreferences.getBoolean("remove_ads_purchased", false)
    }

    fun setRemoveAdsPurchased(purchased: Boolean) {
        sharedPreferences.edit()
            .putBoolean("remove_ads_purchased", purchased)
            .apply()
    }

    fun isPinSetup(): Boolean {
        return sharedPreferences.getBoolean("setup_pin", false)
    }

    fun setPin(purchased: Boolean) {
        sharedPreferences.edit()
            .putBoolean("setup_pin", purchased)
            .apply()
    }

    // Function to save the pin code as a String
    fun saveCode(pinCode: String) {
        with(sharedPreferences.edit()) {
            putString(SELECTED_CODE, pinCode)
            apply()
        }
    }

    // Function to retrieve the saved pin code as a String
    fun getCode(): String = sharedPreferences.getString(SELECTED_CODE, "1910") ?: "1910"

}