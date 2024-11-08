package com.appscentric.donot.touch.myphone.antitheft.singleton

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object FirebaseAnalyticsSingleton {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun getInstance(context: Context): FirebaseAnalytics {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
        }
        return firebaseAnalytics!!
    }

    fun logEvent(event: String, bundle: Bundle) {
        firebaseAnalytics?.logEvent(event, bundle)
    }
}
