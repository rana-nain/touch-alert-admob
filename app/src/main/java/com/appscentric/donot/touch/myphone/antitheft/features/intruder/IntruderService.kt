package com.appscentric.donot.touch.myphone.antitheft.features.intruder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.utils.Constants.STORAGE_FOLDER_NAME
import java.io.File

class IntruderService : LifecycleService() {

    private var imageCapture: ImageCapture? = null
    private val notificationChannelId = "1925"
    private val notificationId = 1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            super.onStartCommand(intent, flags, startId)
            startForegroundService()
            startCamera()
            START_STICKY
        } catch (e: Exception) {
            Log.e("IntruderService", "Error in onStartCommand: ${e.message}", e)
            START_NOT_STICKY // Do not restart the service if an error occurs
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageCapture = null
    }

    private fun startForegroundService() {
        try {
            createNotificationChannel()

            Log.d("IntruderService", "Starting foreground service with notification.")
            val notification = NotificationCompat.Builder(this, notificationChannelId)
                .setContentTitle("Intruder Selfie Service")
                .setContentText("Monitoring for intruders")
                .setSmallIcon(R.drawable.logo)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            Log.d("IntruderService", "Posting foreground notification.")
            startForeground(notificationId, notification)
        } catch (e: Exception) {
            Log.e("IntruderService", "Error starting foreground service: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        try {
            val channel = NotificationChannel(
                notificationChannelId,
                "Intruder Selfie Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service to monitor and capture intruder selfies"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            Log.d("IntruderService", "Notification channel created.")
        } catch (e: Exception) {
            Log.e("IntruderService", "Error creating notification channel: ${e.message}", e)
        }
    }

    private fun startCamera() {
        try {
            Log.d("IntruderService", "Initializing camera.")
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
                    captureImage()
                } catch (e: Exception) {
                    Log.e("IntruderService", "Error initializing camera: ${e.message}", e)
                    stopSelf() // Stop service on camera initialization failure
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e("IntruderService", "Error setting up camera provider: ${e.message}", e)
            stopSelf()
        }
    }

    private fun captureImage() {
        try {
            Log.d("IntruderService", "Capturing image.")

            imageCapture?.let { imageCapture ->
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appFolder = File(downloadsFolder, STORAGE_FOLDER_NAME)

                if (!appFolder.exists()) {
                    appFolder.mkdirs()
                }

                val photoFile = File(appFolder, "intruder_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Log.d("IntruderService", "Image saved at: ${photoFile.absolutePath}")
                            stopSelf()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("IntruderService", "Image capture failed: ${exception.message}", exception)
                            stopSelf()
                        }
                    }
                )
            } ?: Log.e("IntruderService", "ImageCapture instance is null.")
        } catch (e: Exception) {
            Log.e("IntruderService", "Error capturing image: ${e.message}", e)
            stopSelf()
        }
    }
}