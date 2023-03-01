/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.example.adservices.samples.fledge.sampleapp

import android.adservices.customaudience.AddCustomAudienceOverrideRequest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ads.adservices.common.AdData
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudienceManager
import androidx.privacysandbox.ads.adservices.customaudience.JoinCustomAudienceRequest
import androidx.privacysandbox.ads.adservices.customaudience.LeaveCustomAudienceRequest
import androidx.privacysandbox.ads.adservices.customaudience.TrustedBiddingData
import com.example.adservices.samples.fledge.clients.TestCustomAudienceClient
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import java.time.Instant
import java.util.Collections
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Wrapper for the FLEDGE Custom Audience (CA) API. Creating the wrapper locks the user into a given owner
 * and buyer. In order to interact with the wrapper they will first need to call the create method
 * to create a CA object. After that they can call joinCA and leaveCA.
 *
 * @param context The application context.
 * @param executor An executor to use with the FLEDGE API calls.
 */
@RequiresApi(api = 34)
class CustomAudienceWrapper(
  private val executor: Executor,
  context: Context
) {
  private val customAudienceManager : CustomAudienceManager
  private val caOverrideClient : TestCustomAudienceClient

  /**
   * Joins a CA.
   *
   * @param name The name of the CA to join.
   * @param owner The owner of the CA
   * @param buyer The buyer of ads
   * @param biddingUri The URL to retrieve the bidding logic
   * @param renderUri The URL to render the ad
   * @param dailyUpdateUri The URL for daily updates for the CA
   * @param trustedBiddingUri The URL to retrieve trusted bidding data
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  fun joinCa(
    name: String,
    owner: String,
    buyer: AdTechIdentifier,
    biddingUri: Uri,
    renderUri: Uri,
    dailyUpdateUri: Uri,
    trustedBiddingUri: Uri,
    statusReceiver: Consumer<String>,
    expiry: Instant) {
    try {
      joinCustomAudience(
        CustomAudience.Builder(
          buyer,
          name,
          dailyUpdateUri,
          biddingUri,
          listOf(AdData(renderUri, JSONObject().toString())))
          .setActivationTime(Instant.now())
          .setExpirationTime(expiry)
          .setTrustedBiddingData(TrustedBiddingData(trustedBiddingUri, Collections.singletonList("key")))
          .setUserBiddingSignals(AdSelectionSignals("{}"))
          .build(),
        statusReceiver)
    } catch (e: Exception) {
      statusReceiver.accept("Got the following exception when trying to join " + name
                              + " custom audience: " + e)
      Log.e(TAG, "Exception calling joinCustomAudience", e)
    }
  }

  fun joinEmptyFieldCa(
    name: String,
    owner: String,
    buyer: AdTechIdentifier,
    biddingUri: Uri,
    dailyUpdateUri: Uri,
    statusReceiver: Consumer<String>,
    expiry: Instant
  ) {
    try {
      joinCustomAudience(
        CustomAudience.Builder(
          buyer,
          name,
          dailyUpdateUri,
          biddingUri,
          Collections.emptyList())
          .setActivationTime(Instant.now())
          .setExpirationTime(expiry)
          .build(),
        statusReceiver
      )
    } catch (e: Exception) {
      statusReceiver.accept("Got the following exception when trying to join " + name
                              + " custom audience: " + e)
      Log.e(TAG, "Exception calling joinCustomAudience", e)
    }
  }

  /**
   * Leaves a CA.
   *
   * @param name The name of the CA to leave.
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  fun leaveCa(name: String, owner: String, buyer: AdTechIdentifier, statusReceiver: Consumer<String>) {
    val request = LeaveCustomAudienceRequest(buyer, name)
    runBlocking {
      withContext(Dispatchers.Default) {
        customAudienceManager.leaveCustomAudience(request)
      }

      statusReceiver.accept("Left $name custom audience")
    }
  }

  /**
   * Overrides remote info for a CA.
   *
   * @param name The name of the CA to override remote info.
   * @param biddingLogicJs The overriding bidding logic javascript
   * @param trustedBiddingSignals The overriding trusted bidding signals
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  fun addCAOverride(
    name: String,
    owner: String,
    buyer: android.adservices.common.AdTechIdentifier,
    biddingLogicJs: String?,
    trustedBiddingSignals: android.adservices.common.AdSelectionSignals,
    statusReceiver: Consumer<String?>
  ) {
    val request = AddCustomAudienceOverrideRequest.Builder()
      .setBuyer(buyer)
      .setName(name)
      .setBiddingLogicJs(biddingLogicJs!!)
      .setTrustedBiddingSignals(trustedBiddingSignals)
      .build()
    caOverrideClient.overrideCustomAudienceRemoteInfo(request).let {
      Futures.addCallback(it,
                          object : FutureCallback<Void?> {
                            override fun onSuccess(unused: Void?) {
                              statusReceiver.accept("Added override for $name custom audience")
                            }

                            override fun onFailure(e: Throwable) {
                              statusReceiver.accept("Error adding override for " + name
                                                      + " custom audience: " + e.message)
                            }
                          }, executor)
    }
  }

  /**
   * Resets all custom audience overrides.
   *
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  fun resetCAOverrides(statusReceiver: Consumer<String?>) {
    caOverrideClient.resetAllCustomAudienceOverrides().let {
      Futures.addCallback(it,
                          object : FutureCallback<Void?> {
                            override fun onSuccess(unused: Void?) {
                              statusReceiver.accept("Reset all CA overrides")
                            }

                            override fun onFailure(e: Throwable) {
                              statusReceiver.accept("Error while resetting all CA overrides")
                            }
                          }, executor)
    }
  }

  private fun joinCustomAudience(
    ca : CustomAudience,
    statusReceiver: Consumer<String>
  ) {
    val request = JoinCustomAudienceRequest(ca)
    runBlocking {
      withContext(Dispatchers.Default) {
        customAudienceManager.joinCustomAudience(request)
      }

      statusReceiver.accept("Joined ${ca.name} custom audience")
    }
  }

  /**
   * Initialize the custom audience wrapper and set the owner and buyer.
   */
  init {
    customAudienceManager = CustomAudienceManager.obtain(context)!!
    caOverrideClient = TestCustomAudienceClient.Builder()
      .setContext(context)
      .setExecutor(executor)
      .build()
  }
}