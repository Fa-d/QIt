package dev.sadakat.qit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QItApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
