package com.example

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        LottiePrewarmCache.prewarmAsync(this)
        ProVideoFrameCache.prewarmAsync(this)
        ProVideoFrameCache.prewarmGalleryThumbnailsAsync(this, count = 6)
        ShizukuBridge.init(this)
    }
}
