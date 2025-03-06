package com.example.adservices.samples.fledge.adselection.config

import android.net.Uri
import android.util.Log
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.appContext
import com.example.adservices.samples.fledge.util.CommonConstants.gson
import com.example.adservices.samples.fledge.util.VersionCompatUtil.isSdkCompatible
import java.util.function.Consumer

class ServerAdSelectionJsonConfigLoader {
  fun load(statusReceiver: Consumer<String>): ServerAdSelectionJsonConfig {
    try {
      val inputStream = appContext.assets.open(DEFAULT_FILE)
      val json = inputStream.bufferedReader().use { it.readText() }

      val serverAdSelectionJsonConfig = gson.fromJson(json, ServerAdSelectionJsonConfig::class.java)?.apply {
        if (!isSdkCompatible(12, 12)) {
          Log.w(
            TAG,
            "Unsupported SDK Extension: Setting up coordinator URI requires SDK Extension 12, using default coordinator"
          )
          statusReceiver.accept("Unsupported SDK Extension: Setting up coordinator URI requires SDK Extension 12, using default coordinator")
          coordinatorOriginUri = Uri.EMPTY
        }
      } ?: throw IllegalArgumentException("Server Ad Selection Config is empty")

      val missingParams = mapOf(
        "buyer" to serverAdSelectionJsonConfig.buyer,
        "seller" to serverAdSelectionJsonConfig.seller,
        "sellerSfeUri" to serverAdSelectionJsonConfig.sellerSfeUri,
        "coordinatorOriginUri" to serverAdSelectionJsonConfig.coordinatorOriginUri
      ).filter { it.value == null }.keys

      if(missingParams.isNotEmpty()) {
        val missingParamsStr = missingParams.joinToString(", ")
        throw IllegalArgumentException("Missing parameters: $missingParamsStr")
      }

      Log.d(TAG, "serverAdSelectionConfig: $serverAdSelectionJsonConfig")

      return serverAdSelectionJsonConfig
    } catch (e: Exception) {
      statusReceiver.accept("Exception loading server ad selection config: ${e.message}")
      Log.e(TAG, "Exception loading server ad selection config: ${e.message}")
      throw e
    }
  }

  companion object {
    private const val DEFAULT_FILE = "ServerAdSelectionJsonConfig.json"
  }
}