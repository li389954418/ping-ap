package com.example.nettool

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler(this)
    }
}
