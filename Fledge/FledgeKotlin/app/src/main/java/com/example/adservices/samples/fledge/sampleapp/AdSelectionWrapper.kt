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

import android.adservices.adselection.AdSelectionConfig
import android.adservices.adselection.AdSelectionOutcome
import android.adservices.adselection.ReportImpressionRequest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.adservices.samples.fledge.clients.AdSelectionClient
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import java.util.concurrent.Executor
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import org.json.JSONObject

/**
 * Wrapper for the FLEDGE Ad Selection API. This wrapper is opinionated and makes several
 * choices such as running impression reporting immediately after every successful ad auction or leaving
 * the ad signals empty to limit the complexity that is exposed the user.
 */
@RequiresApi(api = 34)
class AdSelectionWrapper(
  buyers: List<String?>, seller: String?, decisionUrl: Uri?, context: Context?,
  executor: Executor
) {
  private val mAdSelectionConfig: AdSelectionConfig
  private val mAdClient: AdSelectionClient
  private val mExecutor: Executor

  /**
   * Runs ad selection and passes a string describing its status to the input receivers. If ad
   * selection succeeds, also report impressions.
   * @param statusReceiver A consumer function that is run after ad selection and impression reporting
   * with a string describing how the auction and reporting went.
   * @param renderUrlReceiver A consumer function that is run after ad selection with a message describing the render URL
   * or lack thereof.
   */
  fun runAdSelection(statusReceiver: Consumer<String?>, renderUrlReceiver: Consumer<String?>) {
    try {
      Futures.addCallback(mAdClient.runAdSelection(mAdSelectionConfig),
                          object : FutureCallback<AdSelectionOutcome?> {
                            override fun onSuccess(adSelectionOutcome: AdSelectionOutcome?) {
                              statusReceiver.accept("Ran ad selection")
                              renderUrlReceiver.accept("Would display ad from " + adSelectionOutcome!!.renderUrl)
                              reportImpression(adSelectionOutcome.adSelectionId,
                                               mAdSelectionConfig,
                                               statusReceiver)
                            }

                            override fun onFailure(e: Throwable) {
                              statusReceiver.accept("Error when running ad selection: " + e.message)
                              renderUrlReceiver.accept("Ad selection failed -- no ad to display")
                              Log.e(MainActivity.TAG, "Exception during ad selection", e)
                            }
                          }, mExecutor)
    } catch (e: Exception) {
      statusReceiver.accept("Got the following exception when trying to run ad selection: $e")
      renderUrlReceiver.accept("Ad selection failed -- no ad to display")
      Log.e(MainActivity.TAG, "Exception calling runAdSelection", e)
    }
  }

  /**
   * Helper function of [.runAdSelection]. Runs impression reporting.
   *
   * @param adSelectionId The auction to report impression on.
   * @param statusReceiver A consumer function that is run after impression reporting
   * with a string describing how the auction and reporting went.
   */
  private fun reportImpression(
    adSelectionId: Long,
    config: AdSelectionConfig,
    statusReceiver: Consumer<String?>
  ) {
    val request = ReportImpressionRequest.Builder()
      .setAdSelectionConfig(config)
      .setAdSelectionId(adSelectionId)
      .build()
    Futures.addCallback(mAdClient.reportImpression(request),
                        object : FutureCallback<Void?> {
                          override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Reported impressions from ad selection")
                          }

                          override fun onFailure(e: Throwable) {
                            statusReceiver.accept("Error when reporting impressions: " + e.message)
                            Log.e(MainActivity.TAG, e.toString(), e)
                          }
                        }, mExecutor)
  }

  /**
   * Initializes the ad selection wrapper with a specific seller, list of buyers, and decision
   * endpoint.
   * @param buyers A list of buyers for the auction.
   * @param seller The name of the seller for the auction
   * @param decisionUrl The URL to retrieve the seller scoring and reporting logic from
   * @param context The application context.
   * @param executor An executor to use with the FLEDGE API calls.
   */
  init {
    mAdSelectionConfig = AdSelectionConfig.Builder()
      .setSeller(seller!!)
      .setDecisionLogicUrl(decisionUrl!!)
      .setCustomAudienceBuyers(buyers)
      .setAdSelectionSignals(JSONObject().toString())
      .setSellerSignals(JSONObject().toString())
      .setPerBuyerSignals(buyers.stream()
                            .collect(Collectors.toMap(
                              Function { buyer: String? -> buyer },
                              Function { buyer: String? -> JSONObject().toString() })))
      .setContextualAds(ArrayList())
      .build()
    mAdClient = AdSelectionClient.Builder().setContext(context!!).setExecutor(executor).build()
    mExecutor = executor
  }
}