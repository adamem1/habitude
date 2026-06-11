package com.adam.habituator

import android.app.Application
import com.adam.habituator.data.AppContainer

class HabituatorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
