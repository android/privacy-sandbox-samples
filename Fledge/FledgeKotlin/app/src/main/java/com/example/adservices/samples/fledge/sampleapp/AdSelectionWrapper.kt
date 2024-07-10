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
import android.adservices.adselection.GetAdSelectionDataRequest
import android.adservices.adselection.PersistAdSelectionResultRequest
import android.adservices.adselection.ReportEventRequest
import android.adservices.adselection.ReportImpressionRequest
import android.adservices.adselection.UpdateAdCounterHistogramRequest
import android.adservices.common.AdSelectionSignals
import android.adservices.common.AdTechIdentifier
import android.adservices.common.FrequencyCapFilters
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import com.example.adservices.samples.fledge.clients.AdSelectionClient
import com.example.adservices.samples.fledge.clients.TestAdSelectionClient
import com.example.adservices.samples.fledge.sdkExtensionsHelpers.VersionCompatUtil.isTestableVersion
import com.example.adservices.samples.fledge.serverauctionhelpers.BiddingAuctionServerClient
import com.example.adservices.samples.fledge.serverauctionhelpers.SelectAdsResponse
import com.google.common.io.BaseEncoding
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.IOException
import java.io.UncheckedIOException
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
    @SuppressLint("NewApi")
    fun runAdSelection(
        statusReceiver: Consumer<String>, renderUriReceiver: Consumer<String>,
        adSelectionIdReceiver: Consumer<String>
    ) {
        try {
            Futures.addCallback(
                adClient.selectAds(adSelectionConfig),
                object : FutureCallback<AdSelectionOutcome?> {
                    override fun onSuccess(adSelectionOutcome: AdSelectionOutcome?) {
                        statusReceiver.accept("Ran ad selection! ID: " + adSelectionOutcome!!.adSelectionId)
                        renderUriReceiver.accept("Would display ad from " + adSelectionOutcome.renderUri)
                        updateAdCounterHistogram(
                            adSelectionOutcome.adSelectionId,
                            FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION,
                            statusReceiver
                        )
                        adSelectionIdReceiver.accept(adSelectionOutcome.adSelectionId.toString())
                        reportImpression(adSelectionOutcome.adSelectionId, statusReceiver)
                    }

                    override fun onFailure(e: Throwable) {
                        statusReceiver.accept("Error when running ad selection: " + e.message)
                        renderUriReceiver.accept("Ad selection failed -- no ad to display")
                        Log.e(TAG, "Exception during ad selection", e)
                    }
                },
                executor
            )
        } catch (e: Exception) {
            statusReceiver.accept("Got the following exception when trying to run ad selection: $e")
            renderUriReceiver.accept("Ad selection failed -- no ad to display")
            Log.e(TAG, "Exception calling runAdSelection", e)
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
        renderUriReceiver: Consumer<String>
    ) {
        if (!isTestableVersion(8, 9)) {
            statusReceiver.accept(
                "Unsupported SDK Extension: Ad counter histogram " +
                        "update requires 8, skipping"
            )
            Log.w(
                TAG,
                "Unsupported SDK Extension: Ad counter histogram " +
                        "update requires 8, skipping"
            )
            return
        }
        Log.i(TAG, "Running ad selection on Auction Servers GetAdSelectionData")
        try {
            Log.i(TAG, "Auction Server ad selection seller:$seller")
            Log.i(TAG, "Auction Server ad selection seller SFE URI:$sellerSfeUri")
            val getDataRequest: GetAdSelectionDataRequest = GetAdSelectionDataRequest.Builder()
                .setSeller(seller).build()
            val adSelectionOutcome: ListenableFuture<AdSelectionOutcome> =
                FluentFuture.from(adClient.getAdSelectionData(getDataRequest))
                    .transform(
                        { outcome ->
                            statusReceiver.accept(
                                "CA data collected from device! Id: "
                                        + outcome!!.getAdSelectionId()
                            )
                            try {
                                val auctionServerClient = BiddingAuctionServerClient(context)
                                val actualResponse: SelectAdsResponse =
                                    auctionServerClient.runServerAuction(
                                        sellerSfeUri.toString(),
                                        seller.toString(),
                                        buyer.toString(),
                                        outcome.getAdSelectionData()
                                    )
                                val serverAuctionResult: Pair<Long, SelectAdsResponse> =
                                    Pair(outcome.getAdSelectionId(), actualResponse)
                                statusReceiver.accept(
                                    "Server auction run successfully for "
                                            + serverAuctionResult.first
                                )
                                return@transform serverAuctionResult
                            } catch (e: IOException) {
                                statusReceiver.accept(
                                    "Something went wrong when calling " +
                                            "bidding auction server"
                                )
                                throw UncheckedIOException(e)
                            }
                        }, executor
                    )
                    .transformAsync(
                        { pair ->
                            val adSelectionId: Long = pair!!.first
                            val response: SelectAdsResponse = pair!!.second
                            val persistResultRequest: PersistAdSelectionResultRequest =
                                PersistAdSelectionResultRequest.Builder()
                                    .setSeller(seller)
                                    .setAdSelectionId(adSelectionId)
                                    .setAdSelectionResult(
                                        BaseEncoding.base64().decode(
                                            response!!.auctionResultCiphertext!!
                                        )
                                    )
                                    .build()
                            adClient.persistAdSelectionResult(persistResultRequest)
                        }, executor
                    )
            Futures.addCallback(
                adSelectionOutcome,
                object : FutureCallback<AdSelectionOutcome> {
                    override fun onSuccess(adSelectionOutcome: AdSelectionOutcome?) {
                        statusReceiver.accept(
                            "Auction Result is persisted for : "
                                    + adSelectionOutcome!!.getAdSelectionId()
                        )
                        if (adSelectionOutcome.hasOutcome()) {
                            renderUriReceiver.accept(
                                "Would display ad from " +
                                        adSelectionOutcome!!.getRenderUri()
                            )
                            reportImpression(
                                adSelectionOutcome!!.getAdSelectionId(), statusReceiver
                            )
                        } else {
                            renderUriReceiver.accept("Would display ad from contextual winner")
                        }
                    }

                    override fun onFailure(e: Throwable) {
                        statusReceiver.accept("Error when running ad selection: " + e.message)
                        renderUriReceiver.accept("Ad selection failed -- no ad to display")
                        Log.e(TAG, "Exception during ad selection", e)
                    }
                }, executor
            )
        } catch (e: Exception) {
            statusReceiver.accept("Got the following exception when trying to run ad selection: $e")
            renderUriReceiver.accept("Ad selection failed -- no ad to display")
            Log.e(TAG, "Exception calling runAdSelection", e)
        }
    }

    /**
     * Runs impression reporting and reports a view event upon success.
     *
     * @param adSelectionId The auction to report impression on.
     * @param statusReceiver A consumer function that is run after impression reporting with a string
     *   describing how the auction and reporting went.
     */
    @SuppressLint("NewApi")
    fun reportImpression(adSelectionId: Long, statusReceiver: Consumer<String>) {
        val request = ReportImpressionRequest(adSelectionId, adSelectionConfig)
        Futures.addCallback(
            adClient.reportImpression(request),
            object : FutureCallback<Void?> {
                override fun onSuccess(unused: Void?) {
                    statusReceiver.accept("Reported impressions from ad selection.")

                    if (isTestableVersion(8, 9)) {
                        statusReceiver.accept(
                            "Unsupported SDK Extension: Event reporting requires 8 for T+ or 9 for S-, skipping"
                        )
                        Log.w(
                            TAG,
                            "Unsupported SDK Extension: Event reporting requires 8, skipping"
                        )
                    } else {
                        statusReceiver.accept("Registered beacons successfully.")
                        val viewInteraction = "view"
                        val eventData = "{\"viewTimeSeconds\":1}"
                        reportEvent(
                            adSelectionId,
                            viewInteraction,
                            eventData,
                            ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER or
                                    ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER,
                            statusReceiver
                        )
                    }
                }

                override fun onFailure(e: Throwable) {
                    statusReceiver.accept("Error when reporting impressions: " + e.message)
                    Log.e(TAG, e.toString(), e)
                }
            },
            executor
        )
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
        if (isTestableVersion(8, 9)) {
            statusReceiver.accept("Unsupported SDK Extension: Event reporting requires 8, skipping")
            Log.w(
                TAG,
                "Unsupported SDK Extension: Event reporting requires 8 for T+ or 9 for S-, skipping"
            )
            return
        }

        val request =
            ReportEventRequest.Builder(adSelectionId, eventKey, eventData, reportingDestinations)
                .build()
        Futures.addCallback(
            adClient.reportEvent(request),
            object : FutureCallback<Void?> {
                override fun onSuccess(unused: Void?) {
                    statusReceiver.accept(String.format("Reported %s event.", eventKey))
                }

                override fun onFailure(e: Throwable) {
                    statusReceiver.accept("Error when reporting event: " + e.message)
                    Log.e(TAG, e.toString(), e)
                }
            },
            executor
        )
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
        if (isTestableVersion(8, 9)) {
            statusReceiver.accept(
                "Unsupported SDK Extension: Ad counter histogram update requires 8 for T+ or 9 for S-, skipping"
            )
            Log.w(
                TAG,
                "Unsupported SDK Extension: Ad counter histogram update requires 8 for T+ or 9 for S-, skipping"
            )
            return
        }

        val callerAdTech: AdTechIdentifier = adSelectionConfig.seller

        val request =
            UpdateAdCounterHistogramRequest.Builder(adSelectionId, adEventType, callerAdTech)
                .build()
        Futures.addCallback(
            adClient.updateAdCounterHistogram(request),
            object : FutureCallback<Void?> {
                override fun onSuccess(unused: Void?) {
                    statusReceiver.accept(
                        String.format(
                            "Updated ad counter histogram with %s event for adtech:%s",
                            fCapEventToString(adEventType),
                            callerAdTech.toString()
                        )
                    )
                }

                override fun onFailure(e: Throwable) {
                    statusReceiver.accept("Error when updating ad counter histogram: " + e.toString())
                    Log.e(TAG, e.toString(), e)
                }
            },
            executor
        )
    }

    /**
     * Overrides remote info for an ad selection config.
     *
     * @param decisionLogicJS The overriding decision logic javascript
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *   indicating the outcome of the call.
     */
    @SuppressLint("NewApi")
    fun overrideAdSelection(
        statusReceiver: Consumer<String?>,
        decisionLogicJS: String?,
        trustedScoringSignals: AdSelectionSignals?
    ) {
        val request =
            AddAdSelectionOverrideRequest(
                adSelectionConfig,
                decisionLogicJS!!,
                trustedScoringSignals!!
            )
        Futures.addCallback(
            overrideClient.overrideAdSelectionConfigRemoteInfo(request),
            object : FutureCallback<Void?> {
                override fun onSuccess(unused: Void?) {
                    statusReceiver.accept("Added override for ad selection")
                }

                override fun onFailure(e: Throwable) {
                    statusReceiver.accept("Error when adding override for ad selection " + e.message)
                    Log.e(TAG, e.toString(), e)
                }
            },
            executor
        )
    }

    /**
     * Resets all ad selection overrides.
     *
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *   indicating the outcome of the call.
     */
    @SuppressLint("NewApi")
    fun resetAdSelectionOverrides(statusReceiver: Consumer<String?>) {
        Futures.addCallback(
            overrideClient.resetAllAdSelectionConfigRemoteOverrides(),
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
            AdSelectionConfig.Builder()
                .setSeller(seller)
                .setDecisionLogicUri(decisionUri)
                .setCustomAudienceBuyers(buyers)
                .setAdSelectionSignals(AdSelectionSignals.EMPTY)
                .setSellerSignals(AdSelectionSignals.EMPTY)
                .setPerBuyerSignals(
                    buyers
                        .stream()
                        .collect(
                            Collectors.toMap(
                                { buyer: AdTechIdentifier -> buyer },
                                { AdSelectionSignals.EMPTY })
                        )
                )
                .setTrustedScoringSignalsUri(trustedScoringUri)
                .build()
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
            AdSelectionConfig.Builder()
                .setSeller(seller)
                .setDecisionLogicUri(decisionUri)
                .setCustomAudienceBuyers(buyers)
                .setAdSelectionSignals(AdSelectionSignals.EMPTY)
                .setSellerSignals(AdSelectionSignals.EMPTY)
                .setPerBuyerSignals(
                    buyers
                        .stream()
                        .collect(
                            Collectors.toMap(
                                { buyer: AdTechIdentifier -> buyer },
                                { AdSelectionSignals.EMPTY })
                        )
                )
                .setTrustedScoringSignalsUri(trustedScoringUri)
                .build()
        adClient = AdSelectionClient.Builder().setContext(context).setExecutor(executor).build()
        overrideClient =
            TestAdSelectionClient.Builder().setContext(context).setExecutor(executor).build()
        this.executor = executor
        this.context = context
    }
}
