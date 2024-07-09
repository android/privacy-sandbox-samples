/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.adservices.samples.signals.sampleapp

import android.adservices.adselection.AdSelectionManager
import android.adservices.adselection.AdSelectionOutcome
import android.adservices.adselection.GetAdSelectionDataOutcome
import android.adservices.adselection.GetAdSelectionDataRequest
import android.adservices.adselection.PersistAdSelectionResultRequest
import android.adservices.common.AdTechIdentifier
import android.adservices.signals.ProtectedSignalsManager
import android.adservices.signals.UpdateSignalsRequest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
import android.util.Log
import android.util.Pair
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.adservices.samples.signals.sampleapp.ServerAuctionHelpers.BiddingAuctionServerClient
import com.example.adservices.samples.signals.sampleapp.ServerAuctionHelpers.SelectAdsResponse
import com.example.adservices.samples.signals.sampleapp.databinding.ActivityMainBinding
import com.google.common.io.BaseEncoding
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Android application activity for testing Signals API
 */
@RequiresApi(api = 35)
class MainActivity : AppCompatActivity() {
  private lateinit var context: Context
  private lateinit var binding: ActivityMainBinding
  private lateinit var eventLog: EventLogManager

  /**
   * Does the initial setup for the app. This includes reading the Javascript server URIs from the
   * start intent, creating the ad selection and custom audience wrappers to wrap the APIs, and
   * tying the UI elements to the wrappers so that button clicks trigger the appropriate methods.
   */
   override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    context = applicationContext
    binding = ActivityMainBinding.inflate(layoutInflater)
    val view: View = binding.root
    setContentView(view)
    eventLog = EventLogManager(binding.eventLog)
    binding.updateSignalsButton.setOnClickListener { _ -> updateSignals() }
    binding.auctionButton.setOnClickListener { _ -> runAuction() }
  }

  @RequiresApi(35)
  fun updateSignals() {
    val uri = Uri.parse(binding.urlInput.text.toString())
    val psManager: ProtectedSignalsManager = context.getSystemService(
      ProtectedSignalsManager::class.java
    )
    val updateSignalsRequest: UpdateSignalsRequest = UpdateSignalsRequest.Builder(uri)
      .build()
    val receiver: OutcomeReceiver<Any, Exception> = object : OutcomeReceiver<Any, Exception> {
      override fun onResult(o: Any) {
        eventLog.writeEvent("Signal update with URL: $uri succeeded!")
      }

      override fun onError(error: Exception) {
        eventLog.writeEvent(
          "Signal update with URL: " + uri + " failed with error: " +
            error
        )
      }
    }
    eventLog.writeEvent("Attempting signal update with URL: $uri")
    psManager.updateSignals(updateSignalsRequest, EXECUTOR, receiver)
  }

  private fun runAuction() {
    if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) < 10) {
      throw RuntimeException("Bad SDK")
    }
    val buyerUri = Uri.parse(binding.urlInput.text.toString())
    val sellerUri = Uri.parse(binding.auctionUrlInput.text.toString())
    val seller: AdTechIdentifier =
      AdTechIdentifier.fromString(binding.auctionSellerInput.text.toString())
    Log.v(TAG, "Running auction with seller=$sellerUri, buyer=$buyerUri")
    val adManager: AdSelectionManager = context.getSystemService(
      AdSelectionManager::class.java
    )
    val getAdSelectionDataRequest: GetAdSelectionDataRequest = GetAdSelectionDataRequest.Builder()
      .setSeller(seller)
      .build()
    val receiver: OutcomeReceiver<GetAdSelectionDataOutcome, Exception> =
      object : OutcomeReceiver<GetAdSelectionDataOutcome, Exception> {
        override fun onResult(o: GetAdSelectionDataOutcome) {
          if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) < 10) {
            throw RuntimeException("Bad SDK")
          }
          eventLog.writeEvent("getAdSelectionData with URL: $sellerUri succeeded!")
          eventLog.writeEvent("Calling Ad Selection Server")
          val auctionServerClient = BiddingAuctionServerClient(context)
          var actualResponse: SelectAdsResponse? = null
          var failed = false
          try {
            actualResponse = auctionServerClient.runServerAuction(
              sellerUri.toString(),
              seller.toString(),
              buyerUri.host.toString(),
              o.adSelectionData!!
            )
          } catch (e: Exception) {
            eventLog.writeEvent("Error calling adSelection server $e")
            Log.e(TAG, "Error calling server", e)
            failed = true
          }
          if (!failed) {
            val serverAuctionResult: Pair<Long, SelectAdsResponse?> =
              Pair<Long, SelectAdsResponse?>(o.adSelectionId, actualResponse)
            eventLog.writeEvent("Got Response from server")
            Log.v(
              TAG, "Response cipher text"
                + serverAuctionResult.second!!.auctionResultCiphertext
            )
            val persistAdSelectionResultRequest: PersistAdSelectionResultRequest =
              PersistAdSelectionResultRequest.Builder()
                .setSeller(seller)
                .setAdSelectionId(o.adSelectionId)
                .setAdSelectionResult(
                  BaseEncoding.base64().decode(serverAuctionResult.second!!.auctionResultCiphertext!!)
                )
                .build()
            val persistReceiver: OutcomeReceiver<AdSelectionOutcome, Exception> =
              object : OutcomeReceiver<AdSelectionOutcome, Exception> {
                override fun onResult(adSelectionOutcome: AdSelectionOutcome) {
                  eventLog.writeEvent(
                    "Render uri " + adSelectionOutcome.renderUri
                  )
                }

                override fun onError(error: Exception) {
                  eventLog.writeEvent("Persist failed $error")
                  Log.v(TAG, "Persist failed", error)
                }
              }
            adManager.persistAdSelectionResult(
              persistAdSelectionResultRequest, EXECUTOR,
              persistReceiver
            )
          }
        }

        override fun onError(error: Exception) {
          eventLog.writeEvent(
            "getAdSelectionData with URL: " + sellerUri + " failed with error: " +
              error
          )
        }
      }
    eventLog.writeEvent("Attempting getAdSelectionData with URL: $sellerUri")
    try {
      adManager.getAdSelectionData(getAdSelectionDataRequest, EXECUTOR, receiver)
    } catch (e: Exception) {
      eventLog.writeEvent("Error getting data $e")
      Log.e(TAG, "Error getting data", e)
    }
  }

  companion object {
    const val TAG = "SignalsSample"

    // Executor to be used for API calls
    private val EXECUTOR: Executor = Executors.newCachedThreadPool()
  }
}