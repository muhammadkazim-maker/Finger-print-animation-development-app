package com.example

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        LottiePrewarmCache.prewarmAsync(this)
        ShizukuBridge.init(this)
    }
}
