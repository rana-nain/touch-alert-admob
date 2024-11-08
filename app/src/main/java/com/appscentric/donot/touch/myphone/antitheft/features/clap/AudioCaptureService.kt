package com.appscentric.donot.touch.myphone.antitheft.features.clap

import android.Manifest
import android.annotation.SuppressLint
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
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
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
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.model.HomeItem
import com.appscentric.donot.touch.myphone.antitheft.screens.MainActivity
import com.appscentric.donot.touch.myphone.antitheft.service.FullScreenDialogService
import com.appscentric.donot.touch.myphone.antitheft.service.NotificationBroadcastReceiver
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.android.ext.android.inject

class AudioCaptureService : Service() {

    private val handler = Handler(Looper.getMainLooper())

    private var isRunning = false
    private var isReceiverRegistered = false
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var notificationManager: NotificationManager
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var isFlashlightOn = false
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private val flashJob = Job()
    private val flashScope = CoroutineScope(Dispatchers.Default + flashJob)

    private val vibrationJob = Job()
    private val vibrationScope = CoroutineScope(Dispatchers.Default + vibrationJob)

    private val preferencesManager by inject<PreferencesManager>()
    private lateinit var homeItem: HomeItem
    private var isVibrate: Boolean = false
    private lateinit var dataMap: Map<String, Any>

    // Top-level declarations
    private var isFlash: Boolean = false
        get() = field || (dataMap["isFlash"] as? Boolean ?: true)

    private var isSound: Boolean = false
        get() = field || (dataMap["isSound"] as? Boolean ?: true)

    private var volumeDuration: String = "30s"
        get() = field.takeIf { it.isNotBlank() } ?: (dataMap["volumeDuration"] as? String ?: "30s")


    private val notificationBroadcastReceiver = NotificationBroadcastReceiver()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP_SERVICE") {
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        upDateUiState()
        super.onDestroy()
        mediaPlayer?.release()
        turnOffFlashlight()

        if (isVibrate) stopVibratorRecording()
        if (isFlash) turnOffFlashlight()
        if (isSound) stopAudioRecording()

        serviceJob.cancel()
        flashJob.cancel()
        vibrationJob.cancel()

        if (isReceiverRegistered) {
            unregisterReceiver(notificationBroadcastReceiver)
            isReceiverRegistered = false // Reset the flag after unregistration
        }


        handler.removeCallbacksAndMessages(null)
    }

    private fun upDateUiState() {
        val intent = Intent("clap_state_changed")
        intent.putExtra("isClapModeRunning", false) // Update value based on your logic
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()

            val (homeItem, isVibrate, dataMap) = preferencesManager.getAllData()
            this.homeItem = homeItem
            this.isVibrate = isVibrate
            this.dataMap = dataMap

            setupAudioRecording()

            val filter = IntentFilter().apply {
                addAction("STOP_SERVICE_ACTION")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(notificationBroadcastReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(notificationBroadcastReceiver, filter)
            }
            isReceiverRegistered = true
        } catch (e: Exception) {
            Log.e("NewService", "Error in onCreate: ${e.message}")
            stopSelf() // Stop the service if an exception occurs during initialization
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

    private fun onClapDetected() {
        isRunning = false

        val (homeItem, isVibrate, dataMap) = preferencesManager.getAllData()
        this.homeItem = homeItem
        this.isVibrate = isVibrate
        this.dataMap = dataMap

        this.isSound = dataMap["isSound"] as? Boolean ?: true
        this.isFlash = dataMap["isFlash"] as? Boolean ?: true
        this.isVibrate = dataMap["isVibrate"] as? Boolean ?: true
        this.volumeDuration = dataMap["volumeDuration"] as? String ?: "30s"
        val volumeDurationMillis = Utility.parseDurationToMillis(volumeDuration)

        if (isSound) playClapSound(homeItem)
        if (isFlash) toggleFlashlightDim()
        if (isVibrate) vibrate()

//        if (preferencesManager.isOverlay) {
//            startFullScreenDialogService(volumeDurationMillis)
//        }
        acquireWakeLock(volumeDurationMillis)

        handler.postDelayed({
            if (isVibrate) stopVibratorRecording()
            if (isFlash) turnOffFlashlight()
            if (isSound) stopAudioRecording()
            setupAudioRecording()
        }, volumeDurationMillis)
    }


    private fun startFullScreenDialogService(volumeDurationMillis: Long) {
        Intent(this, FullScreenDialogService::class.java).apply {
            putExtra("volumeDurationMillis", volumeDurationMillis)
            startService(this)
        }
    }

    private fun playClapSound(homeItem: HomeItem) {
        try {
            if (isSound) {
                mediaPlayer?.release()
                startForegroundServiceCompat()

                val soundResource = homeItem.sound
                if (soundResource != 0) {
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

                    mediaPlayer = MediaPlayer.create(this, soundResource)
                    mediaPlayer?.isLooping = true // Loop the music playback
                    mediaPlayer?.setVolume(currentVolume.toFloat(), currentVolume.toFloat())

                    mediaPlayer?.setOnCompletionListener {
                        // Release the MediaPlayer resources after playing the sound
                        mediaPlayer?.release()
                    }
                    mediaPlayer?.start()
                }
            }
        } catch (e: Exception) {
            Log.e("NewService", "Error playing clap sound: ${e.message}")
        }
    }

    private fun setupCamera() {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            try {
                cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraId = cameraManager.cameraIdList[0] // Use the first available camera
            } catch (e: CameraAccessException) {
                Log.e("NewService", "Error accessing camera: ${e.message}")
            }
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun turnOnFlashlight() {
        isFlashlightOn = true
        try {
            if (cameraId != null && cameraManager.getCameraCharacteristics(cameraId!!)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            ) {
                cameraId?.let { id ->
                    cameraManager.setTorchMode(id, true)
                }
            } else {
                Log.e("NewService", "Flashlight is not available on this device")
            }
        } catch (e: CameraAccessException) {
            Log.e("NewService", "Error turning on flashlight: ${e.message}")
        }
    }

    private fun turnOffFlashlight() {
        isFlashlightOn = false
        try {
            if (cameraId != null && cameraManager.getCameraCharacteristics(cameraId!!).get(
                    CameraCharacteristics.FLASH_INFO_AVAILABLE
                ) == true
            ) {
                cameraId?.let { id ->
                    cameraManager.setTorchMode(id, false)
                }
            } else {
                Log.e("NewService", "Flashlight is not available on this device")
            }
        } catch (e: CameraAccessException) {
            Log.e("NewService", "Error turning off flashlight: ${e.message}")
        }
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

    private fun createNotificationChannel() {
        try {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        } catch (e: Exception) {
            Log.e("NewService", "Error creating notification channel: ${e.message}")
        }
    }

    private fun startForegroundServiceCompat() {
        try {
            // Create a PendingIntent to launch the app's main activity
            val launchAppIntent = Intent(this, MainActivity::class.java)
            val launchAppPendingIntent = PendingIntent.getActivity(
                this, 0, launchAppIntent, PendingIntent.FLAG_IMMUTABLE
            )

            // Create an Intent to stop the service
            val stopServiceIntent = Intent(this, AudioCaptureService::class.java).apply {
                action = "ACTION_STOP_SERVICE" // Custom action to stop the service
            }
            val stopServicePendingIntent = PendingIntent.getService(
                this, 1, stopServiceIntent, PendingIntent.FLAG_IMMUTABLE
            )

            // Build the notification with an action to stop the service
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Clap Detection Service Running")
                .setContentText(getString(R.string.tap_to_deactivate))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(launchAppPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(
                    R.drawable.ic_info, // Icon for the action button
                    "Stop Service", // Text for the action button
                    stopServicePendingIntent // PendingIntent to stop the service
                )
                .setOngoing(true)
                .build()

            startForeground(FOREGROUND_NOTIFICATION_ID, notificationBuilder)
        } catch (e: Exception) {
            Log.e("NewService", "Error starting foreground service: ${e.message}")
        }
    }

    private fun setupAudioRecording() {

        if (isFlash) setupCamera()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions here if needed
            return
        }

        val recordingSampleRate = 44100 // Hz
        val recordingChannelConfig = AudioFormat.CHANNEL_IN_MONO
        val recordingFormat = AudioFormat.ENCODING_PCM_16BIT

        val bufferElements = 2048 // Adjust buffer size based on needs
        val audioBuffer = ShortArray(bufferElements)

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            recordingSampleRate,
            recordingChannelConfig,
            recordingFormat,
            bufferElements * 2
        )

        // Ensure audioRecord is initialized successfully
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecord", "AudioRecord initialization failed")
            return
        }

        isRunning = true
        audioRecord.startRecording()

        serviceScope.launch {
            try {
                while (isRunning) {
                    val shortsRead = audioRecord.read(audioBuffer, 0, bufferElements)
                    if (shortsRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.d("ERROR_INVALID_OPERATION", "setupAudioRecording: $shortsRead")
                    } else {
                        val sensitivity = Utility.mapSliderValueToDesiredRange(
                            preferencesManager.getSensitivity().toFloat()
                        )
                        val peak = audioBuffer.maxOrNull() ?: 0
                        Log.d("TAG_PEAK", "setupAudioRecording: $peak")
                        if (peak > sensitivity) onClapDetected()
                    }
                }
            } finally {
                audioRecord.release()
            }
        }
    }


    private fun stopAudioRecording() {
        isRunning = false
        mediaPlayer?.release()
    }

    private fun stopVibratorRecording() {
        vibrator.cancel()
    }


    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val channelId = "clap_service_channel"
        private const val channelName = "Clap Service Channel"
    }
}