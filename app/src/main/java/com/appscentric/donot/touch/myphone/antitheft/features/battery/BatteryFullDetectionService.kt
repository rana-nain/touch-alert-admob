package com.appscentric.donot.touch.myphone.antitheft.features.battery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.screens.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class BatteryFullDetectionService : Service() {
    private val preferencesManager by inject<PreferencesManager>()
    private lateinit var batteryReceiver: BroadcastReceiver
    private var mediaPlayer: MediaPlayer? = null
    private var isAlertPlayed = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val CHANNEL_ID = "BatteryChannel"
        private const val CHANNEL_NAME = "Battery Alert"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        // Register BroadcastReceiver for battery changes0
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val batteryStatus = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val isCharging = intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    -1
                ) == BatteryManager.BATTERY_STATUS_CHARGING

                if (batteryStatus == 100 && isCharging && !isAlertPlayed) {
                    // Battery fully charged, play alert and send notification
                    setVolumeToMax()
                    playAlertSound()
                    showNotification()
                    isAlertPlayed = true
                    // Stop service after playing alert
                    stopServiceAfterDelay()
                }
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    private fun setVolumeToMax() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
    }

    private fun playAlertSound() {
        mediaPlayer = MediaPlayer.create(
            this,
            R.raw.sound_police
        ) // Add your alert sound file in res/raw directory
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    private fun showNotification() {
        // Create an intent to open the main activity
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create an intent to stop the service
        val stopServiceIntent = Intent(this, BatteryFullDetectionService::class.java).apply {
            action = "ACTION_BATTERY_STOP_SERVICE" // Custom action to stop the service
        }
        val stopServicePendingIntent = PendingIntent.getService(
            this,
            1,
            stopServiceIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification with an action to stop the service
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Full Detection Service Running")
            .setContentText("Your battery is fully charged.")
            .setSmallIcon(R.mipmap.ic_launcher_round) // Add your notification icon
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_info, // Icon for the action button
                "Stop Service", // Text for the action button
                stopServicePendingIntent // PendingIntent to stop the service
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    // Add this in the onStartCommand method to handle the stop action
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_BATTERY_STOP_SERVICE") {
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    private fun stopServiceAfterDelay() {
        serviceScope.launch {
            delay(30000)
            preferencesManager.isBatteryFullDetection = false
            stopSelf()
        }
    }

    override fun onDestroy() {
        upDateUiState()
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        mediaPlayer?.release()
        mediaPlayer = null
        serviceScope.cancel() // Cancel the coroutine scope to prevent memory leaks
    }

    private fun upDateUiState() {
        val intent = Intent("battery_full_state_changed")
        intent.putExtra("isBatteryFullModeRunning", false) // Update value based on your logic
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service doesn't support binding
        return null
    }
}