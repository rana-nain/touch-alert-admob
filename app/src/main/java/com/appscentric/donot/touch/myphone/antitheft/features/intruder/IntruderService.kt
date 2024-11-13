package com.appscentric.donot.touch.myphone.antitheft.features.intruder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.room.Room
import com.appscentric.donot.touch.myphone.antitheft.R
import com.appscentric.donot.touch.myphone.antitheft.room.AppDatabase
import com.appscentric.donot.touch.myphone.antitheft.room.IntruderImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class IntruderService : LifecycleService() {

    private var imageCapture: ImageCapture? = null
    private val notificationChannelId = "1925"
    private val notificationId = 1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForegroundService()
        startCamera()
        return START_STICKY // Ensure the service keeps running even if the app is killed
    }

    override fun onDestroy() {
        super.onDestroy()
        imageCapture = null
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Intruder Selfie Service")
            .setContentText("Monitoring for intruders")
            .setSmallIcon(R.drawable.logo)
            .build()

        startForeground(notificationId, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            notificationChannelId,
            "Intruder Selfie Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Service to monitor and capture intruder selfies"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    private fun startCamera() {
        Log.d("TAG_DEBUG", "startCamera: Initializing camera")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imageCapture
            )

            captureImage()

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureImage() {
        // Ensure imageCapture is not null
        imageCapture?.let { imageCapture ->

            val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "app_database")
                .build()

            // Create a file to save the image
            val appFolder = File(getExternalFilesDir(null), "MyAppFolder")
            if (!appFolder.exists()) {
                appFolder.mkdirs()
            }

            val photoFile = File(appFolder, "intruder_${System.currentTimeMillis()}.jpg")

            // Set up image capture metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // Capture image and save it to the specified location
            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d("TAG_DEBUG", "Image saved at: ${photoFile.absolutePath}")

                        val capturedAt = System.currentTimeMillis()
                        val intruderImage = IntruderImage(imagePath = photoFile.absolutePath, capturedAt = capturedAt)

                        CoroutineScope(Dispatchers.IO).launch {
                            db.intruderImageDao().insertImage(intruderImage)
                        }

                        stopSelf()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("TAG_DEBUG", "Image capture failed: ${exception.message}", exception)
                        stopSelf()
                    }
                })
        }
    }
}
