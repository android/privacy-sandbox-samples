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

import android.adservices.common.AdData
import android.adservices.common.AdFilters
import android.adservices.common.AdSelectionSignals
import android.adservices.common.AdTechIdentifier
import android.adservices.customaudience.AddCustomAudienceOverrideRequest
import android.adservices.customaudience.CustomAudience
import android.adservices.customaudience.TrustedBiddingData
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.adservices.samples.fledge.clients.CustomAudienceClient
import com.example.adservices.samples.fledge.clients.TestCustomAudienceClient
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import java.time.Instant
import java.util.Collections
import java.util.concurrent.Executor
import java.util.function.Consumer
import org.json.JSONObject

/**
 * Wrapper for the FLEDGE Custom Audience (CA) API. Creating the wrapper locks the user into a given owner
 * and buyer. In order to interact with the wrapper they will first need to call the create method
 * to create a CA object. After that they can call joinCA and leaveCA.
 *
 * @param owner The owner field for custom audience created by this wrapper.
 * @param buyer The buyer field for custom audience created by this wrapper.
 * @param context The application context.
 * @param executor An executor to use with the FLEDGE API calls.
 */
@RequiresApi(api = 34)
class CustomAudienceWrapper(
  private val executor: Executor,
  context: Context
) {
  private val caClient: CustomAudienceClient
  private val caOverrideClient : TestCustomAudienceClient

  /**
   * Joins a CA.
   *
   * @param name The name of the CA to join.
   * @param buyer The buyer of ads
   * @param biddingUri The URL to retrieve the bidding logic
   * @param renderUri The URL to render the ad
   * @param dailyUpdateUri The URL for daily updates for the CA
   * @param trustedBiddingUri The URL to retrieve trusted bidding data
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   * @param expiry The time when the CA will expire
   */
  fun joinCa(
    name: String,
    buyer: AdTechIdentifier,
    biddingUri: Uri,
    renderUri: Uri,
    dailyUpdateUri: Uri,
    trustedBiddingUri: Uri,
    statusReceiver: Consumer<String>,
    expiry: Instant) {
      joinCa(name, buyer, biddingUri, renderUri, dailyUpdateUri, trustedBiddingUri, statusReceiver, expiry, null);
  }

  /**
   * Joins a CA.
   *
   * @param name The name of the CA to join.
   * @param buyer The buyer of ads
   * @param biddingUri The URL to retrieve the bidding logic
   * @param renderUri The URL to render the ad
   * @param dailyUpdateUri The URL for daily updates for the CA
   * @param trustedBiddingUri The URL to retrieve trusted bidding data
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   * @param expiry The time when the CA will expire
   * @param filters {@link AdFilters} that should be applied to the ad in the CA
   * string indicating the outcome of the call.
   */
  fun joinCa(
    name: String,
    buyer: AdTechIdentifier,
    biddingUri: Uri,
    renderUri: Uri,
    dailyUpdateUri: Uri,
    trustedBiddingUri: Uri,
    statusReceiver: Consumer<String>,
    expiry: Instant,
    filters: AdFilters?
  ) {
    try {
      joinCustomAudience(
        CustomAudience.Builder()
          .setBuyer(buyer)
          .setName(name)
          .setDailyUpdateUri(dailyUpdateUri)
          .setBiddingLogicUri(biddingUri)
          .setAds(listOf(AdData.Builder()
                           .setRenderUri(renderUri)
                           .setMetadata(JSONObject().toString())
                           .setAdFilters(filters)
                           .build()))
          .setActivationTime(Instant.now())
          .setExpirationTime(expiry)
          .setTrustedBiddingData(TrustedBiddingData.Builder()
                                   .setTrustedBiddingKeys(Collections.singletonList("key"))
                                   .setTrustedBiddingUri(trustedBiddingUri).build())
          .setUserBiddingSignals(AdSelectionSignals.EMPTY)
          .build(),
        statusReceiver)
    } catch (e: Exception) {
      statusReceiver.accept("Got the following exception when trying to join " + name
                              + " custom audience: " + e)
      Log.e(TAG, "Exception calling joinCustomAudience", e)
    }
  }

  /**
   * Joins a CA.
   *
   * @param name The name of the CA to join.
   * @param buyer The buyer of ads
   * @param biddingUri The URL to retrieve the bidding logic
   * @param renderUri The URL to render the ad
   * @param dailyUpdateUri The URL for daily updates for the CA
   * @param trustedBiddingUri The URL to retrieve trusted bidding data
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * @param expiry The time when the CA will expire
   * @param filters [AdFilters] that should be applied to the ad in the CA
   * @param adCounterKeys set of keys used in counting events
   * string indicating the outcome of the call.
   */
  fun joinFilteringCa(
    name: String,
    buyer: AdTechIdentifier?,
    biddingUri: Uri?,
    renderUri: Uri?,
    dailyUpdateUri: Uri?,
    trustedBiddingUri: Uri?,
    statusReceiver: Consumer<String>,
    expiry: Instant?,
    filters: AdFilters?,
    adCounterKeys: Set<Int?>?,
  ) {
    try {
      joinCustomAudience(
        CustomAudience.Builder()
          .setBuyer(buyer!!)
          .setName(name)
          .setDailyUpdateUri(dailyUpdateUri!!)
          .setBiddingLogicUri(biddingUri!!)
          .setAds(listOf(AdData.Builder()
                           .setRenderUri(renderUri!!)
                           .setMetadata(JSONObject().toString())
                           .setAdFilters(filters)
                           .setAdCounterKeys(adCounterKeys!!)
                           .build()))
          .setActivationTime(Instant.now())
          .setExpirationTime(expiry)
          .setTrustedBiddingData(TrustedBiddingData.Builder()
                                   .setTrustedBiddingKeys(listOf("key"))
                                   .setTrustedBiddingUri(trustedBiddingUri!!).build())
          .setUserBiddingSignals(AdSelectionSignals.EMPTY)
          .build(),
        statusReceiver)
    } catch (e: java.lang.Exception) {
      statusReceiver.accept("Got the following exception when trying to join " + name
                              + " custom audience: " + e)
      Log.e(TAG, "Exception calling joinCustomAudience", e)
    }
  }

  fun joinEmptyFieldCa(
    name: String,
    buyer: AdTechIdentifier,
    biddingUri: Uri,
    dailyUpdateUri: Uri,
    statusReceiver: Consumer<String>,
    expiry: Instant,
  ) {
    try {
      joinCustomAudience(
        CustomAudience.Builder()
          .setBuyer(buyer)
          .setName(name)
          .setDailyUpdateUri(dailyUpdateUri)
          .setBiddingLogicUri(biddingUri)
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
     * Fetches and joins a CA with from an URI.
     * @param fetchUri The URL to retrieve the CA from.
     * @param name The name of the CA to join.
     * @param activationTime The time when the CA will activate.
     * @param expirationTime The time when the CA will expire.
     * @param userBiddingSignals The user bidding signals used at auction.
     * @param statusReceiver A consumer function that is run after the API call and returns a string.
     */
    fun fetchAndJoinCa(fetchUri: Uri, name: String, activationTime: Instant?, expirationTime:
    Instant?, userBiddingSignals: AdSelectionSignals?, statusReceiver: Consumer<String?>) {
        try {
            Futures.addCallback(
                    caClient.fetchAndJoinCustomAudience(fetchUri, name, activationTime, expirationTime, userBiddingSignals),
                    object : FutureCallback<Void?> {
                        override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Fetched and joined $name custom audience.")
                        }

                        override fun onFailure(e: Throwable) {
                            statusReceiver.accept("Error when fetching and joining " + name
                                    + " custom audience: " + e.message)
                        }
                    }, executor)
        } catch (e: Exception) {
            statusReceiver.accept("Got the following exception when trying to fetch and join"
                    + name + " custom audience: " + e)
            Log.e(TAG, "Exception calling fetchAndJoinCustomAudience", e)
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
    try {
      Futures.addCallback(caClient.leaveCustomAudience(owner, buyer, name),
                          object : FutureCallback<Void?> {
                            override fun onSuccess(unused: Void?) {
                              statusReceiver.accept("Left $name custom audience")
                            }

                            override fun onFailure(e: Throwable) {
                              statusReceiver.accept("Error when leaving " + name
                                                      + " custom audience: " + e.message)
                            }
                          }, executor)
    } catch (e: Exception) {
      statusReceiver.accept("Got the following exception when trying to leave " + name
                              + " custom audience: " + e)
      Log.e(TAG, "Exception calling leaveCustomAudience", e)
    }
  }

  /**
   * Overrides remote info for a CA.
   *
   * @param name The name of the CA to override remote info.
   * @param biddingLogicJs The overriding bidding logic javascript
   * @param biddingLogicJsVersion The override bidding logic javascript version
   * @param trustedBiddingSignals The overriding trusted bidding signals
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  fun addCAOverride(
    name: String,
    owner: String,
    buyer: AdTechIdentifier,
    biddingLogicJs: String?,
    biddingLogicJsVersion: Long,
    trustedBiddingSignals: AdSelectionSignals,
    statusReceiver: Consumer<String?>,
  ) {
    val request = AddCustomAudienceOverrideRequest.Builder()
      .setBuyer(buyer)
      .setName(name)
      .setBiddingLogicJs(biddingLogicJs!!)
      .setBiddingLogicJsVersion(biddingLogicJsVersion)
      .setTrustedBiddingSignals(trustedBiddingSignals)
      .build()
    Futures.addCallback(caOverrideClient.overrideCustomAudienceRemoteInfo(request),
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

  /**
   * Resets all custom audience overrides.
   *
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  fun resetCAOverrides(statusReceiver: Consumer<String?>) {
    Futures.addCallback(caOverrideClient.resetAllCustomAudienceOverrides(),
                        object : FutureCallback<Void?> {
                          override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Reset all CA overrides")
                          }

                          override fun onFailure(e: Throwable) {
                            statusReceiver.accept("Error while resetting all CA overrides")
                          }
                        }, executor)
  }

  private fun joinCustomAudience(
    ca: CustomAudience,
    statusReceiver: Consumer<String>,
  ) {
    Futures.addCallback(caClient.joinCustomAudience(ca),
                        object : FutureCallback<Void?> {
                          override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Joined ${ca.name} custom audience")
                          }

                          override fun onFailure(e: Throwable) {
                            statusReceiver.accept("Error when joining " + ca.name + " custom audience: "
                                                    + e.message)
                            Log.e(TAG, "Exception during CA join process ", e)
                          }
                        }, executor)
  }

  /**
   * Initialize the custom audience wrapper and set the owner and buyer.
   */
  init {
    caClient = CustomAudienceClient.Builder().setContext(context).setExecutor(executor).build()
    caOverrideClient = TestCustomAudienceClient.Builder()
      .setContext(context)
      .setExecutor(executor)
      .build()
  }
}