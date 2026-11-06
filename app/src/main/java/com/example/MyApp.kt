package com.example

import android.app.Application
import com.example.core.di.AppContainer

class MyApp : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() { super.onCreate(); container = AppContainer(this) }
    override fun onTerminate() { container.close(); super.onTerminate() }
}
