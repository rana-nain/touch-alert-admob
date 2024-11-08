package com.appscentric.donot.touch.myphone.antitheft.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.appscentric.donot.touch.myphone.antitheft.features.touch.PhoneService

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "STOP_SERVICE_ACTION") {
            Log.d("NotificationReceiver", "Received stop service action")
            context?.stopService(Intent(context, PhoneService::class.java))
        }
    }
}
