package com.omooooori.civitdeck

import android.app.Application
import com.omooooori.civitdeck.di.initKoin
import org.koin.android.ext.koin.androidContext

class CivitDeckApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CivitDeckApplication)
        }
    }
}
