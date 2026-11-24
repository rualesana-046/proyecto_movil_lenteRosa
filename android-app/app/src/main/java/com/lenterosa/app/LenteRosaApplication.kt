package com.lenterosa.app

import android.app.Application
import com.lenterosa.app.map.MapConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LenteRosaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MapConfig.initialize(this)
    }
}
