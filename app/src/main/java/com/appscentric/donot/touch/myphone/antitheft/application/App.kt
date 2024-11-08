package com.appscentric.donot.touch.myphone.antitheft.application

import com.appscentric.donot.touch.myphone.antitheft.manager.PreferencesManager
import com.appscentric.donot.touch.myphone.antitheft.monetization.appopen.AppOpenManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.ramt57.easylocale.EasyLocaleApplication
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class App : EasyLocaleApplication() {

    val preferencesManager by inject<PreferencesManager>()

    private val appModule = module {
        single { PreferencesManager(context = androidContext()) }
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        MobileAds.initialize(applicationContext) {
            AppOpenManager(this@App)
        }


        initKoin()
    }

    private fun initKoin() {
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
    }
}