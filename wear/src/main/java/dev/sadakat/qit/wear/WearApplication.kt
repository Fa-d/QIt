package dev.sadakat.qit.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
