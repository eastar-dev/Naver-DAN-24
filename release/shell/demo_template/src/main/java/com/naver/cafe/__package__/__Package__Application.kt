package dev.eastar.naverdan24.__package__

import android.app.Application
import android.content.Context
import android.log.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class __Package__Application : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        easterEgg(__Package__EasterEgg::class)
    }

    override fun onCreate() {
        super.onCreate()
        //초기화작업
    }
}
