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

import android.adservices.adselection.AddAdSelectionOverrideRequest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig
import androidx.privacysandbox.ads.adservices.adselection.ReportEventRequest
import androidx.privacysandbox.ads.adservices.adselection.ReportImpressionRequest
import androidx.privacysandbox.ads.adservices.adselection.UpdateAdCounterHistogramRequest
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters
import com.example.adservices.samples.fledge.clients.AdSelectionClient
import com.example.adservices.samples.fledge.clients.TestAdSelectionClient
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.stream.Collectors


/**
 * Wrapper for the FLEDGE Ad Selection API. This wrapper is opinionated and makes several choices
 * such as running impression reporting immediately after every successful ad auction or leaving the
 * ad signals empty to limit the complexity that is exposed the user.
 *
 * @param buyers A list of buyers for the auction.
 * @param seller The name of the seller for the auction
 * @param decisionUri The URI to retrieve the seller scoring and reporting logic from
 * @param trustedScoringUri The URI to retrieve the trusted scoring signals
 * @param context The application context.
 * @param executor An executor to use with the FLEDGE API calls.
 */
@Suppress("UnstableApiUsage")
@OptIn(ExperimentalFeatures.Ext8OptIn::class, ExperimentalFeatures.Ext10OptIn::class)
@RequiresApi(api = 34)
class AdSelectionWrapper(
        buyers: List<AdTechIdentifier>,
        seller: AdTechIdentifier,
        decisionUri: Uri,
        trustedScoringUri: Uri,
        context: Context,
        executor: Executor,
) {
    private var adSelectionConfig: AdSelectionConfig
    private var adSelectionConfigOverride: android.adservices.adselection.AdSelectionConfig
    private val adClient: AdSelectionClient
    private val executor: Executor
    private val overrideClient: TestAdSelectionClient
    private val context: Context

    /**
     * Runs ad selection and passes a string describing its status to the input receivers. If ad
     * selection succeeds, updates the ad histogram with an impression event and reports the
     * impression.
     *
     * @param statusReceiver A consumer function that is run after ad selection, histogram updating,
     *   and impression reporting with a string describing how the auction, histogram updating, and
     *   reporting went.
     * @param renderUriReceiver A consumer function that is run after ad selection with a message
     *   describing the render URI or lack thereof.
     */
    fun runAdSelection(statusReceiver: Consumer<String>, renderUriReceiver: Consumer<String>) {
        runBlocking {
            val adSelectionOutcome = adClient.selectAds(adSelectionConfig)
            statusReceiver.accept("Ran ad selection! ID: " + adSelectionOutcome.adSelectionId)
            renderUriReceiver.accept("Would display ad from " + adSelectionOutcome.renderUri)
            reportImpression(adSelectionOutcome.adSelectionId, statusReceiver)
        }
    }

    /**
     * Runs ad selection on Auction Servers and passes a string describing its status to the input
     * receivers. If ad selection succeeds, updates the ad histogram with an impression event and
     * reports the impression.
     *
     * @param statusReceiver A consumer function that is run after ad selection, histogram updating,
     * and impression reporting with a string describing how the auction, histogram updating, and
     * reporting went.
     *
     * @param renderUriReceiver A consumer function that is run after ad selection with a message
     * describing the render URI or lack thereof.
     */
    /* FluentFuture */
    @SuppressLint("NewApi")
    fun runAdSelectionOnAuctionServer(
            sellerSfeUri: Uri,
            seller: AdTechIdentifier,
            buyer: AdTechIdentifier,
            statusReceiver: Consumer<String>,
            renderUriReceiver: Consumer<String>) {
        throw UnsupportedOperationException()
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
        runBlocking {
            withContext(Dispatchers.Default) {
                adClient.reportImpression(request)
            }
            statusReceiver.accept("Reported impressions from ad selection")
        }
    }

    /**
     * Runs event reporting.
     *
     * @param adSelectionId The auction associated with the ad.
     * @param eventKey The type of event to be reported.
     * @param eventData Data associated with the event.
     * @param reportingDestinations the destinations to report to, (buyer/seller)
     * @param statusReceiver A consumer function that is run after event reporting with a string
     *   describing how the reporting went.
     */
    @SuppressLint("NewApi")
    fun reportEvent(
            adSelectionId: Long,
            eventKey: String,
            eventData: String,
            reportingDestinations: Int,
            statusReceiver: Consumer<String>,
    ) {
        val request =
                ReportEventRequest(adSelectionId, eventKey, eventData, reportingDestinations)
        runBlocking {
            withContext(Dispatchers.Default) {
                adClient.reportEvent(request)
            }
            statusReceiver.accept("Reported impressions from ad selection")
        }
    }

    /**
     * Helper function of [AdSelectionClient.updateAdCounterHistogram]. Updates the counter histograms
     * for an ad.
     *
     * @param adSelectionId The identifier associated with the winning ad.
     * @param adEventType identifies which histogram should be updated
     * @param statusReceiver A consumer function that is run after that reports how the call went
     *   after it is completed
     */
    @SuppressLint("NewApi")
    fun updateAdCounterHistogram(
            adSelectionId: Long,
            adEventType: Int,
            statusReceiver: Consumer<String>,
    ) {
        val callerAdTech: AdTechIdentifier = adSelectionConfig.seller

        val request =
                UpdateAdCounterHistogramRequest(adSelectionId, adEventType, callerAdTech)
        runBlocking {
            withContext(Dispatchers.Default) {
                adClient.updateAdCounterHistogram(request)
            }
            statusReceiver.accept(
                    String.format(
                            "Updated ad counter histogram with %s event for adtech:%s",
                            fCapEventToString(adEventType),
                            callerAdTech.toString()
                    )
            )
        }
    }

    /**
     * Overrides remote info for an ad selection config.
     *
     * @param decisionLogicJS The overriding decision logic javascript
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *   indicating the outcome of the call.
     */
    fun overrideAdSelection(
            statusReceiver: Consumer<String?>,
            decisionLogicJS: String?,
            trustedScoringSignals: AdSelectionSignals?
    ) {
        val request = AddAdSelectionOverrideRequest(
                adSelectionConfigOverride,
                decisionLogicJS!!,
                android.adservices.common.AdSelectionSignals.fromString(trustedScoringSignals!!.toString()))
        overrideClient.overrideAdSelectionConfigRemoteInfo(request).let {
            Futures.addCallback(it,
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
    }

    /**
     * Resets all ad selection overrides.
     *
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *   indicating the outcome of the call.
     */
    @SuppressLint("NewApi")
    fun resetAdSelectionOverrides(statusReceiver: Consumer<String?>) {
        overrideClient.resetAllAdSelectionConfigRemoteOverrides().let {
            Futures.addCallback(
                    it,
                    object : FutureCallback<Void?> {
                        override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Reset ad selection overrides")
                        }

                        override fun onFailure(e: Throwable) {
                            statusReceiver.accept("Error when resetting all ad selection overrides " + e.message)
                            Log.e(TAG, e.toString(), e)
                        }
                    },
                    executor
            )
        }
    }

    /**
     * Resets the {code adSelectionConfig} with the new decisionUri associated with this
     * `AdSelectionWrapper`. To be used when switching back and forth between dev overrides/mock
     * server states.
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
            trustedScoringUri: Uri
    ) {
        adSelectionConfig =
                AdSelectionConfig(
                        seller,
                        decisionUri,
                        buyers,
                        AdSelectionSignals("{}"),
                        AdSelectionSignals("{}"),
                        buyers.stream().collect(
                                Collectors.toMap({ buyer: AdTechIdentifier -> buyer }, { AdSelectionSignals("{}") })),
                        trustedScoringUri)
    }

    private fun fCapEventToString(eventType: Int): String {
        val result: String =
                when (eventType) {
                    FrequencyCapFilters.AD_EVENT_TYPE_WIN -> "win"
                    FrequencyCapFilters.AD_EVENT_TYPE_CLICK -> "click"
                    FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION -> "impression"
                    FrequencyCapFilters.AD_EVENT_TYPE_VIEW -> "view"
                    else -> "unknown"
                }
        return result
    }

    /**
     * Initializes the ad selection wrapper with a specific seller, list of buyers, and decision
     * endpoint.
     */
    init {
        adSelectionConfig =
                AdSelectionConfig(
                        seller,
                        decisionUri,
                        buyers,
                        AdSelectionSignals("{}"),
                        AdSelectionSignals("{}"),
                        buyers.stream().collect(
                                Collectors.toMap({ buyer: AdTechIdentifier -> buyer },
                                        { AdSelectionSignals("{}") })),
                        trustedScoringUri)
        adSelectionConfigOverride = android.adservices.adselection.AdSelectionConfig.Builder()
                .setSeller(android.adservices.common.AdTechIdentifier.fromString(seller.toString()))
                .setDecisionLogicUri(decisionUri)
                .setCustomAudienceBuyers(buyers.stream()
                        .map { buyer -> android.adservices.common.AdTechIdentifier.fromString(buyer.toString()) }
                        .collect(Collectors.toList()))
                .setAdSelectionSignals(android.adservices.common.AdSelectionSignals.EMPTY)
                .setSellerSignals(android.adservices.common.AdSelectionSignals.EMPTY)
                .setPerBuyerSignals(buyers.associate {
                    android.adservices.common.AdTechIdentifier.fromString(it.toString()) to android.adservices.common.AdSelectionSignals.EMPTY
                })
                .setTrustedScoringSignalsUri(trustedScoringUri)
                .build()
        adClient = AdSelectionClient.Builder().setContext(context).setExecutor(executor).build()
        overrideClient =
                TestAdSelectionClient.Builder().setContext(context).setExecutor(executor).build()
        this.executor = executor
        this.context = context
    }
}

