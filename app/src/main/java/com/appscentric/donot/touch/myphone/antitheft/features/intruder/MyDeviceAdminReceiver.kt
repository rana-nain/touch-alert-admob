package com.appscentric.donot.touch.myphone.antitheft.features.intruder

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.inject

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        try {
            val preferencesManager = PreferencesManager(context)
            val isCapture = preferencesManager.intruderSelfieMode

            if (isCapture) {
                context.startService(Intent(context, IntruderService::class.java))
            }
        } catch (e: Exception) {
            Log.e("MyDeviceAdminReceiver", "Error in onPasswordFailed: ${e.message}", e)
        }
    }
}