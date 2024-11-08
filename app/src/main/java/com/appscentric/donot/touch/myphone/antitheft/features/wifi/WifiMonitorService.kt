package com.appscentric.donot.touch.myphone.antitheft.features.wifi

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem
import com.appscentric.donot.touch.myphone.antitheft.screens.MainActivity
import com.appscentric.donot.touch.myphone.antitheft.service.NotificationBroadcastReceiver
import com.appscentric.donot.touch.myphone.antitheft.service.PinDialogService
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.mapSliderValueToDesiredRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.android.ext.android.inject
import kotlin.properties.Delegates

class WifiMonitorService : Service() {

    private lateinit var notificationManager: NotificationManager
    private val notificationBroadcastReceiver = NotificationBroadcastReceiver()
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var cameraManager: CameraManager

    private val preferencesManager by inject<PreferencesManager>()
    private lateinit var dataMap: Map<String, Any>

    private var SENSITIVITY by Delegates.notNull<Int>()

    private var mediaPlayer: MediaPlayer? = null

    private val isFlash: Boolean
        get() = dataMap["isFlash"] as? Boolean ?: true

    private val isSound: Boolean
        get() = dataMap["isSound"] as? Boolean ?: true

    private val volumeDuration: String
        get() = dataMap["volumeDuration"] as? String ?: "30s"

    private lateinit var homeItem: HomeItem
    private var isVibrate: Boolean = false

    private lateinit var vibrator: Vibrator

    private var isFlashlightOn = false

    private val flashJob = Job()
    private val flashScope = CoroutineScope(Dispatchers.Default + flashJob)

    private val vibrationJob = Job()
    private val vibrationScope = CoroutineScope(Dispatchers.Default + vibrationJob)

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private val handler = Handler(Looper.getMainLooper())

    private var cameraId: String? = null
    private var isPlaying = false
    private var playOnce = true

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val channelId = "wifi_service_channel"
        private const val channelName = "Wifi Monitor Service Channel"
    }

    private fun createNotificationChannel() {
        try {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                channelId,
                channelName, importance
            )
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        } catch (e: Exception) {
            Log.e("NewService", "Error creating notification channel: ${e.message}")
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        createNotificationChannel()
        startForegroundService()
        monitorWifiConnection()

        val filter = IntentFilter().apply {
            addAction("STOP_SERVICE_ACTION")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationBroadcastReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(notificationBroadcastReceiver, filter)
        }

        val (homeItem, isVibrate, dataMap) = preferencesManager.getAllData()
        this.homeItem = homeItem
        this.isVibrate = isVibrate
        this.dataMap = dataMap

        // Initialize mediaPlayer here
        mediaPlayer = MediaPlayer()

        SENSITIVITY = mapSliderValueToDesiredRange(preferencesManager.getSensitivity().toFloat())

        setupCamera()

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun startForegroundService() {
        val channelId = "wifi_monitor_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        val notification: Notification =
            NotificationCompat.Builder(this, channelId).setContentTitle(channelName)
                .setContentText("Monitoring WiFi disconnections").setSmallIcon(R.drawable.logo)
                .build()

        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun monitorWifiConnection() {
        val networkRequest =
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                serviceScope.launch {
                    playAlarmActions()
                }
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        updateUiState()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        mediaPlayer?.release()
        turnOffFlashlight()
        flashJob.cancel()
        vibrationJob.cancel()
        isPlaying = false
        vibrator.cancel()
        handler.removeCallbacksAndMessages(null)
        serviceJob.cancel()
        unregisterReceiver(notificationBroadcastReceiver)
        stopForeground(true)
        super.onDestroy()
    }

    private fun updateUiState() {
        val intent = Intent("wifi_state_changed")
        intent.putExtra("isWifiModeRunning", false)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun setupCamera() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIdList = cameraManager.cameraIdList
            if (cameraIdList.isNotEmpty()) {
                cameraId = cameraIdList[0] // Use the first available camera
            } else {
                Toast.makeText(this, "No camera available", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun turnOnFlashlight() {
        isFlashlightOn = true
        try {
            if (cameraId != null) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
                val hasFlash =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) {
                    cameraId?.let { id ->
                        cameraManager.setTorchMode(id, true)
                    }
                } else {
                    showFlashlightNotAvailableMessage()
                }
            } else {
                Log.e("NewService", "No camera available on the device")
                // Disable flashlight functionality for the user
            }
        } catch (e: CameraAccessException) {
            Log.e("NewService", "Error turning on flashlight: ${e.message}")
        } catch (e: Exception) {
            Log.e("NewService", "Unexpected error: ${e.message}")
        }
    }

    private fun turnOffFlashlight() {
        isFlashlightOn = false
        try {
            if (cameraId != null) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
                val hasFlash =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) {
                    cameraId?.let { id ->
                        cameraManager.setTorchMode(id, false)
                    }
                } else {
                    showFlashlightNotAvailableMessage()
                }
            } else {
                Log.e("NewService", "No camera available on the device")
                // Disable flashlight functionality for the user
            }
        } catch (e: CameraAccessException) {
            Log.e("NewService", "Error turning off flashlight: ${e.message}")
        } catch (e: Exception) {
            Log.e("NewService", "Unexpected error: ${e.message}")
        }
    }

    private fun showFlashlightNotAvailableMessage() {
        Toast.makeText(
            this,
            "Flashlight is not available on this device",
            Toast.LENGTH_SHORT
        ).show()
        Log.e("NewService", "Flashlight is not available on this device")
    }

    private fun toggleFlashlightDim() {
        flashScope.launch {
            withTimeout(Utility.parseDurationToMillis(volumeDuration)) {
                val mode = preferencesManager.getSelectedFlashlightMode()
                while (isActive) {
                    for (duration in mode) {

                        withContext(Dispatchers.Main) {
                            if (isActive) turnOnFlashlight()
                        }
                        delay(duration.toLong())

                        withContext(Dispatchers.Main) {
                            if (isActive) turnOffFlashlight()
                        }
                        delay(duration.toLong())
                    }
                }
            }
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun playAlarmActions() {
        if (isPlaying) return
        isPlaying = true

        startForegroundServiceCompat()

        val volumeDurationMillis = Utility.parseDurationToMillis(volumeDuration)

        if (preferencesManager.isPinSetup()) {
            val serviceIntent =
                Intent(this@WifiMonitorService, PinDialogService::class.java)
            serviceIntent.putExtra("volumeDurationMillis", volumeDurationMillis)

            // Check if the service is not already running
            if (!isServiceRunning(PinDialogService::class.java)) {
                startService(serviceIntent)
            } else {
                // Service is already running, handle accordingly
                Log.d("ServiceCheck", "Service is already running")
            }
        }

        if (isSound && mediaPlayer != null) playAlarm()
        if (isVibrate) vibrate()
        if (isFlash) toggleFlashlightDim()

        // Acquire a WakeLock
        acquireWakeLock(volumeDurationMillis)

        handler.postDelayed({
            stopActions()
        }, volumeDurationMillis) // Delay based on volumeDurationMillis
    }

    private fun playAlarm() {
        val (homeItem, _, _) = preferencesManager.getAllData()
        val soundResource = homeItem.sound

        if (soundResource != 0) { // Ensure sound resource is valid
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Get current volume level (consider security and privacy on Android 10+)
            val currentVolume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use SAFER methods for Android 10+
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            } else {
                // Use legacy method for older versions (may require permission on some devices)
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * audioManager.getStreamVolume(
                    AudioManager.STREAM_MUSIC
                ) / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            }

            mediaPlayer = MediaPlayer.create(this, soundResource)?.apply {
                isLooping = true // Loop the music playback
                // Set volume to current system volume
                setVolume(currentVolume.toFloat(), currentVolume.toFloat())
                start() // Start the playback
            }

        } else {
            // Handle case where sound resource is invalid
            Log.e("MediaPlayer", "Invalid sound resource ID")
        }
    }

    private fun stopMediaPlayer() = mediaPlayer?.release()

    private fun stopVibratorRecording() = vibrator.cancel()

    private fun vibrate() {
        val pattern = preferencesManager.getSelectedVibrationMode()

        if (pattern.isNotEmpty()) { // Check if pattern is not empty and not default
            vibrationScope.launch {
                withTimeout(Utility.parseDurationToMillis(volumeDuration)) {
                    while (isActive) {
                        Log.d("TAG", "vibrate: $pattern")
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))

                        delay((pattern.sum() + pattern.sum()).coerceAtMost(Int.MAX_VALUE.toLong())) // Ensure delay doesn't exceed Int.MAX_VALUE ms
                    }
                }
            }
        }
    }

    private fun acquireWakeLock(volumeDurationMillis: Long) {
        val powerManager = this.getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) { // if screen is not already on, turn it on (get wake_lock)
            @SuppressLint("InvalidWakeLockTag") val wl = powerManager.newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE or PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                "id:wakeupscreen"
            )
            wl.acquire(volumeDurationMillis)
        }
    }

    private fun stopActions() {
        if (isPlaying) stopMediaPlayer()
        if (isFlash) turnOffFlashlight()
        if (isVibrate) stopVibratorRecording()

        isPlaying = false
        playOnce = true
    }

    private fun startForegroundServiceCompat() {
        try {
            // Create a PendingIntent to launch the app's main activity
            val launchAppIntent = Intent(this, MainActivity::class.java)
            val launchAppPendingIntent = PendingIntent.getActivity(
                this, 0, launchAppIntent, PendingIntent.FLAG_IMMUTABLE
            )

            // Create an Intent to stop the service
            val stopServiceIntent = Intent(this, WifiMonitorService::class.java).apply {
                action = "ACTION_STOP_SERVICE" // Custom action for stopping the service
            }
            val stopServicePendingIntent = PendingIntent.getService(
                this, 1, stopServiceIntent, PendingIntent.FLAG_IMMUTABLE
            )

            // Build the notification with an action to stop the service
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Wifi Detection Service Running")
                .setContentText(getString(R.string.tap_to_deactivate))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(launchAppPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(
                    R.drawable.ic_info, // Icon for the action button
                    "Stop Service", // Text for the action button
                    stopServicePendingIntent // PendingIntent to stop the service
                )
                .setOngoing(true)
                .setAutoCancel(false)
                .build()

            startForeground(FOREGROUND_NOTIFICATION_ID, notificationBuilder)
        } catch (e: Exception) {
            Log.e("NewService", "Error starting foreground service: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP_SERVICE") {
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }
}