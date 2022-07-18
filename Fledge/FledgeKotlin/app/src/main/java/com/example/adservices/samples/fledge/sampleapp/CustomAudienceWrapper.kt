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
import android.adservices.customaudience.AddCustomAudienceOverrideRequest
import android.adservices.customaudience.CustomAudience
import android.adservices.customaudience.TrustedBiddingData
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.adservices.samples.fledge.clients.CustomAudienceClient
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import java.time.Duration
import java.time.Instant
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
  private val owner: String,
  private val buyer: String,
  private val executor: Executor,
  context: Context
) {
  private val caClient: CustomAudienceClient

  /**
   * Joins a CA.
   *
   * @param name The name of the CA to join.
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  fun joinCa(name: String, biddingUri: Uri, renderUri: Uri, statusReceiver: Consumer<String>) {
    try {
      val ca = CustomAudience.Builder()
        .setOwner(owner)
        .setBuyer(buyer)
        .setName(name)
        .setDailyUpdateUrl(Uri.EMPTY)
        .setBiddingLogicUrl(biddingUri)
        .setAds(listOf(AdData.Builder()
                         .setRenderUri(renderUri)
                         .setMetadata(JSONObject().toString())
                         .build()))
        .setActivationTime(Instant.now())
        .setExpirationTime(Instant.now().plus(Duration.ofDays(1)))
        .setTrustedBiddingData(TrustedBiddingData.Builder()
                                 .setTrustedBiddingKeys(ArrayList())
                                 .setTrustedBiddingUrl(Uri.EMPTY).build())
        .setUserBiddingSignals(JSONObject().toString())
        .build()
      Futures.addCallback(caClient.joinCustomAudience(ca),
                          object : FutureCallback<Void?> {
                            override fun onSuccess(unused: Void?) {
                              statusReceiver.accept("Joined $name custom audience")
                            }

                            override fun onFailure(e: Throwable) {
                              statusReceiver.accept("Error when joining " + name + " custom audience: "
                                                      + e.message)
                              Log.e(TAG, "Exception during CA join process ", e)
                            }
                          }, executor)
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
  fun leaveCa(name: String, statusReceiver: Consumer<String>) {
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
   * @param trustedBiddingData The overriding trusted bidding data
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  fun addCAOverride(
    name: String,
    biddingLogicJs: String?,
    trustedBiddingData: String?,
    statusReceiver: Consumer<String?>
  ) {
    val request = AddCustomAudienceOverrideRequest.Builder()
      .setOwner(owner)
      .setBuyer(buyer)
      .setName(name)
      .setBiddingLogicJs(biddingLogicJs!!)
      .setTrustedBiddingData(trustedBiddingData!!)
      .build()
    Futures.addCallback(caClient.overrideCustomAudienceRemoteInfo(request),
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
    Futures.addCallback(caClient.resetAllCustomAudienceOverrides(),
                        object : FutureCallback<Void?> {
                          override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Reset all CA overrides")
                          }

                          override fun onFailure(e: Throwable) {
                            statusReceiver.accept("Error while resetting all CA overrides")
                          }
                        }, executor)
  }

  /**
   * Initialize the custom audience wrapper and set the owner and buyer.
   */
  init {
    caClient = CustomAudienceClient.Builder().setContext(context).setExecutor(executor).build()
  }
}