package com.example.adservices.samples.fledge.adselection.config

import android.net.Uri
import android.util.Log
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.appContext
import com.example.adservices.samples.fledge.util.CommonConstants.emptyAdSelectionSignals
import com.example.adservices.samples.fledge.util.CommonConstants.gson
import java.util.function.Consumer

/** Creates an AdSelectionConfig for on-device ad selection using the provided OnDeviceAdSelectionConfig parameters. */
class OnDeviceAdSelectionJsonConfigLoader {
  fun load(statusReceiver: Consumer<String>): AdSelectionConfig {
    try {
      val inputStream = appContext.assets.open(DEFAULT_FILE)
      val json = inputStream.bufferedReader().use { it.readText() }

      val configFile = gson.fromJson(json, OnDeviceAdSelectionJsonConfig::class.java)
        ?: throw IllegalArgumentException("On Device Ad Selection Config is empty")

      if(configFile.baseUri == null) {
        throw IllegalArgumentException("Base uri must be provided")
      }

      val buyer = AdTechIdentifier(configFile.baseUri.host.toString())
      val seller = AdTechIdentifier(configFile.baseUri.host.toString())
      val decisionLogicUri = configFile.decisionLogicUri ?: Uri.parse("${configFile.baseUri}/scoring")
      val trustedScoringSignalsUri = configFile.trustedScoringSignalsUri ?: Uri.parse("${configFile.baseUri}/scoring/trusted")

      return AdSelectionConfig(
        seller = seller,
        decisionLogicUri = decisionLogicUri,
        customAudienceBuyers = listOf(buyer),
        adSelectionSignals = emptyAdSelectionSignals,
        sellerSignals = emptyAdSelectionSignals,
        perBuyerSignals = mapOf(buyer to emptyAdSelectionSignals),
        trustedScoringSignalsUri = trustedScoringSignalsUri
      )
    } catch (e: Exception) {
      statusReceiver.accept("Exception loading on-device ad selection config: ${e.message}")
      Log.e(TAG, "Exception loading on-device ad selection config: ${e.message}")
      throw e
    }
  }

  companion object {
    private const val DEFAULT_FILE = "OnDeviceAdSelectionJsonConfig.json"
  }
}