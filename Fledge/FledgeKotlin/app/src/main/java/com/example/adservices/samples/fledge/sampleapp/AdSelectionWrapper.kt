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
import android.adservices.adselection.AddAdSelectionOverrideRequest
import android.adservices.adselection.ReportImpressionRequest
import android.adservices.common.AdSelectionSignals
import android.adservices.common.AdTechIdentifier
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.adservices.samples.fledge.clients.AdSelectionClient
import com.example.adservices.samples.fledge.clients.TestAdSelectionClient
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import java.util.concurrent.Executor
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * Wrapper for the FLEDGE Ad Selection API. This wrapper is opinionated and makes several
 * choices such as running impression reporting immediately after every successful ad auction or leaving
 * the ad signals empty to limit the complexity that is exposed the user.
 *
 * @param buyers A list of buyers for the auction.
 * @param seller The name of the seller for the auction
 * @param decisionUri The URI to retrieve the seller scoring and reporting logic from
 * @param trustedScoringUri The URI to retrieve the trusted scoring signals
 * @param context The application context.
 * @param executor An executor to use with the FLEDGE API calls.
 */
@RequiresApi(api = 34)
class AdSelectionWrapper(
  buyers: List<AdTechIdentifier>, seller: AdTechIdentifier, decisionUri: Uri, trustedScoringUri: Uri, context: Context,
  executor: Executor
) {
  private var adSelectionConfig: AdSelectionConfig
  private val adClient: AdSelectionClient
  private val executor: Executor
  private val overrideClient: TestAdSelectionClient

  /**
   * Runs ad selection and passes a string describing its status to the input receivers. If ad
   * selection succeeds, also report impressions.
   * @param statusReceiver A consumer function that is run after ad selection and impression reporting
   * with a string describing how the auction and reporting went.
   * @param renderUriReceiver A consumer function that is run after ad selection with a message describing the render URI
   * or lack thereof.
   */
  fun runAdSelection(statusReceiver: Consumer<String>, renderUriReceiver: Consumer<String>) {
    try {
      Futures.addCallback(adClient.selectAds(adSelectionConfig),
                          object : FutureCallback<AdSelectionOutcome?> {
                            override fun onSuccess(adSelectionOutcome: AdSelectionOutcome?) {
                              statusReceiver.accept("Ran ad selection! ID: " + adSelectionOutcome!!.adSelectionId)
                              renderUriReceiver.accept("Would display ad from " + adSelectionOutcome.renderUri)
                              reportImpression(adSelectionOutcome.adSelectionId,
                                               statusReceiver)
                            }

                            override fun onFailure(e: Throwable) {
                              statusReceiver.accept("Error when running ad selection: " + e.message)
                              renderUriReceiver.accept("Ad selection failed -- no ad to display")
                              Log.e(TAG, "Exception during ad selection", e)
                            }
                          }, executor)
    } catch (e: Exception) {
      statusReceiver.accept("Got the following exception when trying to run ad selection: $e")
      renderUriReceiver.accept("Ad selection failed -- no ad to display")
      Log.e(TAG, "Exception calling runAdSelection", e)
    }
  }

  /**
   * Helper function of [.runAdSelection]. Runs impression reporting.
   *
   * @param adSelectionId The auction to report impression on.
   * @param statusReceiver A consumer function that is run after impression reporting
   * with a string describing how the auction and reporting went.
   */
  fun reportImpression(
    adSelectionId: Long,
    statusReceiver: Consumer<String>
  ) {
    val request = ReportImpressionRequest(adSelectionId, adSelectionConfig)
    Futures.addCallback(adClient.reportImpression(request),
                        object : FutureCallback<Void?> {
                          override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Reported impressions from ad selection")
                          }

                          override fun onFailure(e: Throwable) {
                            statusReceiver.accept("Error when reporting impressions: " + e.message)
                            Log.e(TAG, e.toString(), e)
                          }
                        }, executor)
  }

  /**
   * Overrides remote info for an ad selection config.
   *
   * @param decisionLogicJS The overriding decision logic javascript
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  fun overrideAdSelection(statusReceiver: Consumer<String?>, decisionLogicJS: String?, trustedScoringSignals: AdSelectionSignals?) {
    val request = AddAdSelectionOverrideRequest(adSelectionConfig, decisionLogicJS!!, trustedScoringSignals!!);
    Futures.addCallback(overrideClient.overrideAdSelectionConfigRemoteInfo(request),
                        object : FutureCallback<Void?> {
                          override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Added override for ad selection")
                          }

                          override fun onFailure(e: Throwable) {
                            statusReceiver.accept("Error when adding override for ad selection " + e.message)
                            Log.e(TAG, e.toString(), e)
                          }
                        }, executor)
  }

  /**
   * Resets all ad selection overrides.
   *
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  fun resetAdSelectionOverrides(statusReceiver: Consumer<String?>) {
    Futures.addCallback(overrideClient.resetAllAdSelectionConfigRemoteOverrides(),
                        object : FutureCallback<Void?> {
                          override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Reset ad selection overrides")
                          }

                          override fun onFailure(e: Throwable) {
                            statusReceiver.accept("Error when resetting all ad selection overrides " + e.message)
                            Log.e(TAG, e.toString(), e)
                          }
                        }, executor)
  }

  /**
   * Resets the {code adSelectionConfig} with the new decisionUri associated with this `AdSelectionWrapper`.
   * To be used when switching back and forth between dev overrides/mock server states.
   *
   * @param buyers the list of buyers to be used
   * @param seller the seller to be users
   * @param decisionUri the new {@code Uri} to be used
   * @param trustedScoringUri the scoring signal uri to be used.
   */
  fun resetAdSelectionConfig(
    buyers: List<AdTechIdentifier>,
    seller: AdTechIdentifier,
    decisionUri: Uri,
    trustedScoringUri: Uri) {
    adSelectionConfig = AdSelectionConfig.Builder()
      .setSeller(seller)
      .setDecisionLogicUri(decisionUri)
      .setCustomAudienceBuyers(buyers)
      .setAdSelectionSignals(AdSelectionSignals.EMPTY)
      .setSellerSignals(AdSelectionSignals.EMPTY)
      .setPerBuyerSignals(buyers.stream()
                            .collect(Collectors.toMap(
                              { buyer: AdTechIdentifier -> buyer },
                              { AdSelectionSignals.EMPTY })))
      .setTrustedScoringSignalsUri(trustedScoringUri)
      .build()
  }

  /**
   * Initializes the ad selection wrapper with a specific seller, list of buyers, and decision
   * endpoint.
   */
  init {
    adSelectionConfig = AdSelectionConfig.Builder()
      .setSeller(seller)
      .setDecisionLogicUri(decisionUri)
      .setCustomAudienceBuyers(buyers)
      .setAdSelectionSignals(AdSelectionSignals.EMPTY)
      .setSellerSignals(AdSelectionSignals.EMPTY)
      .setPerBuyerSignals(buyers.stream()
                            .collect(Collectors.toMap(
                              { buyer: AdTechIdentifier -> buyer },
                              { AdSelectionSignals.EMPTY })))
      .setTrustedScoringSignalsUri(trustedScoringUri)
      .build()
    adClient = AdSelectionClient.Builder().setContext(context).setExecutor(executor).build()
    overrideClient = TestAdSelectionClient.Builder()
      .setContext(context)
      .setExecutor(executor)
      .build()
    this.executor = executor
  }
}