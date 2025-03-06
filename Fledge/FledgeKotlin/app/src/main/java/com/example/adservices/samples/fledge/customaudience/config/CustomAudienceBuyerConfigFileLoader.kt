package com.example.adservices.samples.fledge.customaudience.config

import android.util.Log
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.appContext
import com.google.gson.Gson
import java.util.function.Consumer

class CustomAudienceBuyerConfigFileLoader {
  fun load(statusReceiver: Consumer<String>): CustomAudienceBuyerConfig? {
    var config: CustomAudienceBuyerConfig? = null

    try {
      val inputStream = appContext.assets.open(DEFAULT_FILE)
      val json = inputStream.bufferedReader().use { it.readText() }
      config = Gson().fromJson(json, CustomAudienceBuyerConfig::class.java)
    } catch (e: Exception) {
      Log.w(TAG, "Exception loading custom audience buyer config: $e")
      statusReceiver.accept("Exception loading custom audience buyer config: $e")
    }
    return config
  }

  companion object {
    private const val DEFAULT_FILE = "CustomAudienceBuyerConfig.json"
  }
}