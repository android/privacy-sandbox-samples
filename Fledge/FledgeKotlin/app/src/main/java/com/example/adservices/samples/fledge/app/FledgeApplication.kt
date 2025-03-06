package com.example.adservices.samples.fledge.app

import android.app.Application
import android.content.Context

class FledgeApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    appContext = applicationContext
  }

  companion object {
    const val TAG = "FledgeSample"
    lateinit var appContext: Context
  }
}