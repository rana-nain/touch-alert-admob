package com.appscentric.donot.touch.myphone.antitheft.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.features.plug.ChargingService
import com.appscentric.donot.touch.myphone.antitheft.features.touch.PhoneService
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.screens.MainActivity
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.isServiceRunning
import com.khaledahmedelsayed.pinview.PinView
import org.koin.android.ext.android.inject

class PinDialogService : Service() {
    private var mOverlayView: View? = null
    private var mWindowManager: WindowManager? = null
    private lateinit var pinView: PinView
    private val preferencesManager by inject<PreferencesManager>()
    private var volumeDurationMillis: Long = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Retrieve the volumeDurationMillis from the intent
        volumeDurationMillis = intent?.getLongExtra("volumeDurationMillis", 0L) ?: 0L

        // Ensure the service keeps running
        startServiceTimer()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startServiceTimer() {
        // Automatically stop the service after the specified duration
        Handler(Looper.getMainLooper()).postDelayed({
            dismissOverlay() // Stop the service after the delay
        }, volumeDurationMillis)
    }

    override fun onCreate() {
        super.onCreate()
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayViewForOreoAndAbove()
    }

    private fun createOverlayViewForOreoAndAbove() {
        if (Settings.canDrawOverlays(this)) {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                PixelFormat.TRANSLUCENT
            )

            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            try {
                mOverlayView = inflater.inflate(R.layout.overlay_pin, null)
                pinView = mOverlayView?.findViewById(R.id.pinView)!!

                pinView.setOnCompletedListener = {
                    val code = preferencesManager.getCode()
                    if (it == code) {
                        pinView.clearPin()
                        mainLogic()
                    } else {
                        pinView.showError(true)
                        pinView.clearPin()
                    }
                }

                mWindowManager?.addView(mOverlayView, params)
            } catch (e: WindowManager.BadTokenException) {
                e.printStackTrace()
                Toast.makeText(this, "Unable to show overlay", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Overlay permission not granted", Toast.LENGTH_SHORT).show()
        }
    }


    private fun mainLogic() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        stopService(Intent(this, PhoneService::class.java))

        val chargingServiceIntent = Intent(this, ChargingService::class.java)
        if (isServiceRunning(this, ChargingService::class.java) && preferencesManager.isChargingRunningSet) {
            stopService(chargingServiceIntent)
        }

        try {
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Failed to open the app", Toast.LENGTH_SHORT).show()
        }

        dismissOverlay()
    }

    private fun dismissOverlay() {
        // Remove the overlay view before stopping the service
        if (mOverlayView != null) {
            mWindowManager?.removeView(mOverlayView)
            mOverlayView = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mOverlayView != null) {
            mWindowManager?.removeView(mOverlayView)
            mOverlayView = null
        }
    }
}