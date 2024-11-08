package com.appscentric.donot.touch.myphone.antitheft.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.features.plug.ChargingService
import com.appscentric.donot.touch.myphone.antitheft.features.touch.PhoneService
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.screens.MainActivity
import com.appscentric.donot.touch.myphone.antitheft.utils.Utility.Companion.isServiceRunning
import com.bumptech.glide.Glide
import org.koin.android.ext.android.inject

class FullScreenDialogService : Service() {
    private var mWindowManager: WindowManager? = null
    private var mOverlayView: View? = null
    private var mLottieAnimationView: LottieAnimationView? = null
    private var imageView: ImageView? = null
    private val preferencesManager by inject<PreferencesManager>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayViewForOreoAndAbove()
    }

    private fun createOverlayViewForOreoAndAbove() {
        val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY,
            PixelFormat.TRANSLUCENT
        )

        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        try {
            mOverlayView = inflater.inflate(R.layout.overlay_layout, null)
            mLottieAnimationView = mOverlayView?.findViewById(R.id.animation_view) as? LottieAnimationView
            imageView = mOverlayView?.findViewById(R.id.backgroundImageView) as? ImageView

            if (mOverlayView != null && mLottieAnimationView != null) {

                imageView?.let {
                    Glide.with(it.context)
                        .load(preferencesManager.getWallpaper())
                        .into(it)
                }

                mLottieAnimationView?.setOnClickListener {

                    val intent = Intent(applicationContext, MainActivity::class.java)
                        .apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }

                    val pendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    Intent(this, PhoneService::class.java).apply {
                        stopService(this)
                    }

                    val chargingServiceIntent = Intent(this, ChargingService::class.java)
                    if (isServiceRunning(this, ChargingService::class.java) && preferencesManager.isChargingRunningSet) {
                        stopService(chargingServiceIntent)
                    }

                    try {
                        pendingIntent.send()
                    } catch (e: PendingIntent.CanceledException) {
                        e.printStackTrace()
                        Toast.makeText(applicationContext, "Failed to open the app", Toast.LENGTH_SHORT)
                            .show()
                    }

                    dismissOverlay()
                }
            }

            mWindowManager?.addView(mOverlayView, params)
        } catch (e: WindowManager.BadTokenException) {
            // Handle the case where permission is denied or another issue occurs
            e.printStackTrace()
            // Display an error message or handle the situation as needed
        }
    }

    private fun dismissOverlay() {
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
