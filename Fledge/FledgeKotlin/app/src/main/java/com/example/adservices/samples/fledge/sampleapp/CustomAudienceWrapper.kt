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
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ads.adservices.common.AdData
import androidx.privacysandbox.ads.adservices.common.AdFilters
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters
import androidx.privacysandbox.ads.adservices.common.KeyedFrequencyCap
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience
import androidx.privacysandbox.ads.adservices.customaudience.TrustedBiddingData
import com.example.adservices.samples.fledge.clients.CustomAudienceClient
import com.example.adservices.samples.fledge.clients.TestCustomAudienceClient
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor
import java.util.function.Consumer


/**
 * Wrapper for the FLEDGE Custom Audience (CA) API. Creating the wrapper locks the user into a given
 * owner and buyer. In order to interact with the wrapper they will first need to call the create
 * method to create a CA object. After that they can call joinCA and leaveCA.
 *
 * @param owner The owner field for custom audience created by this wrapper.
 * @param buyer The buyer field for custom audience created by this wrapper.
 * @param context The application context.
 * @param executor An executor to use with the FLEDGE API calls.
 */
@OptIn(ExperimentalFeatures.Ext8OptIn::class, ExperimentalFeatures.Ext10OptIn::class)
@RequiresApi(api = 34)
class CustomAudienceWrapper(private val executor: Executor, context: Context) {
    private val caClient: CustomAudienceClient = CustomAudienceClient.Builder().setContext(context).setExecutor(executor).build()
    private val caOverrideClient: TestCustomAudienceClient

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
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *   indicating the outcome of the call.
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
            expiry: Instant
    ) {
        try {
            joinCustomAudience(
                    CustomAudience(
                            buyer,
                            name,
                            dailyUpdateUri,
                            biddingUri,
                            listOf(AdData(renderUri, JSONObject().toString())),
                            Instant.now(),
                            expiry,
                            AdSelectionSignals("{}"),
                            TrustedBiddingData(trustedBiddingUri,
                                    listOf(name, buyer.toString(), "key1", "key2"))),
                    statusReceiver
            )
        } catch (e: Exception) {
            statusReceiver.accept(
                    "Got the following exception when trying to join " + name + " custom audience: " + e
            )
            Log.e(TAG, "Exception calling joinCustomAudience", e)
        }
    }

    @SuppressLint("NewApi")
    fun joinServerAuctionCa(
            name: String,
            buyer: AdTechIdentifier,
            biddingUri: Uri,
            renderUri: Uri,
            dailyUpdateUri: Uri,
            trustedBiddingUri: Uri,
            statusReceiver: Consumer<String>,
            expiry: Instant,
            adRenderId: Int) {
        try {
            val clickFilterMax = 1
            val oneDayDuration: Duration = Duration.ofDays(1)
            val clickCap: KeyedFrequencyCap =
                    KeyedFrequencyCap(adRenderId, clickFilterMax, oneDayDuration)
            val fCapFilters: FrequencyCapFilters = FrequencyCapFilters(emptyList(), emptyList(),
                    emptyList(), listOf(clickCap))
            val adData = AdData(
                    renderUri,
                    JSONObject().toString(),
                    setOf(adRenderId),
                    AdFilters(fCapFilters),
                    adRenderId.toString())
            Log.v(TAG, String.format("AdRenderId %s is set for ad render uri %s", adRenderId, renderUri))
            joinCustomAudience(
                    CustomAudience(
                            buyer,
                            name,
                            dailyUpdateUri,
                            biddingUri,
                            listOf(adData),
                            Instant.now(),
                            expiry,
                            AdSelectionSignals("{}"),
                            TrustedBiddingData(trustedBiddingUri,
                                    listOf(name, buyer.toString(), "key1", "key2"))),
                    statusReceiver)
        } catch (e: java.lang.Exception) {
            statusReceiver.accept("Got the following exception when trying to join " + name
                    + " custom audience: " + e)
            Log.e(TAG, "Exception calling joinCustomAudience", e)
        }
    }

    /**
     * Joins a CA with ad filters.
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
     * @param adCounterKeys set of keys used in counting events string indicating the outcome of the
     *   call.
     */
    @SuppressLint("NewApi")
    fun joinFilteringCa(
            name: String,
            buyer: AdTechIdentifier,
            biddingUri: Uri,
            renderUri: Uri,
            dailyUpdateUri: Uri,
            trustedBiddingUri: Uri,
            statusReceiver: Consumer<String>,
            expiry: Instant,
            filters: AdFilters,
            adCounterKeys: Set<Int>,
    ) {
        try {
            joinCustomAudience(
                    CustomAudience(
                            buyer,
                            name,
                            dailyUpdateUri,
                            biddingUri,
                            listOf(AdData(renderUri, JSONObject().toString(), adCounterKeys, filters)),
                            Instant.now(),
                            expiry,
                            AdSelectionSignals("{}"),
                            TrustedBiddingData(trustedBiddingUri, listOf("key"))),
                    statusReceiver
            )
        } catch (e: java.lang.Exception) {
            statusReceiver.accept(
                    "Got the following exception when trying to join " + name + " custom audience: " + e
            )
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
                    CustomAudience(
                            buyer,
                            name,
                            dailyUpdateUri,
                            biddingUri,
                            emptyList(),
                            Instant.now(),
                            expiry),
                    statusReceiver
            )
        } catch (e: Exception) {
            statusReceiver.accept(
                    "Got the following exception when trying to join " + name + " custom audience: " + e
            )
            Log.e(TAG, "Exception calling joinCustomAudience", e)
        }
    }

    /**
     * Fetches and joins a CA with from an URI.
     *
     * @param fetchUri The URL to retrieve the CA from.
     * @param name The name of the CA to join.
     * @param activationTime The time when the CA will activate.
     * @param expirationTime The time when the CA will expire.
     * @param userBiddingSignals The user bidding signals used at auction.
     * @param statusReceiver A consumer function that is run after the API call and returns a string.
     */
    fun fetchAndJoinCa(
            fetchUri: Uri,
            name: String,
            activationTime: Instant?,
            expirationTime: Instant?,
            userBiddingSignals: AdSelectionSignals?,
            statusReceiver: Consumer<String?>
    ) {
        runBlocking {
            caClient.fetchAndJoinCustomAudience(
                    fetchUri,
                    name,
                    activationTime,
                    expirationTime,
                    userBiddingSignals
            )
            statusReceiver.accept("Fetched and joined $name custom audience.")
        }
    }

    /**
     * Leaves a CA.
     *
     * @param name The name of the CA to leave.
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *   indicating the outcome of the call.
     */
    fun leaveCa(
            name: String,
            owner: String,
            buyer: AdTechIdentifier,
            statusReceiver: Consumer<String>
    ) {
        runBlocking {
            caClient.leaveCustomAudience(owner, buyer, name)
            statusReceiver.accept("Left $name custom audience")
        }
    }

    /**
     * Overrides remote info for a CA.
     *
     * @param name The name of the CA to override remote info.
     * @param biddingLogicJs The overriding bidding logic javascript
     * @param trustedBiddingSignals The overriding trusted bidding signals
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *   indicating the outcome of the call.
     */
    fun addCAOverride(
            name: String,
            owner: String,
            buyer: AdTechIdentifier,
            biddingLogicJs: String?,
            trustedBiddingSignals: AdSelectionSignals,
            statusReceiver: Consumer<String?>
    ) {
        val request = AddCustomAudienceOverrideRequest(
                android.adservices.common.AdTechIdentifier.fromString(buyer.toString()),
                name,
                biddingLogicJs!!,
                android.adservices.common.AdSelectionSignals.fromString(trustedBiddingSignals.toString()))
        caOverrideClient.overrideCustomAudienceRemoteInfo(request).let {
            Futures.addCallback(
                    it,
                    object : FutureCallback<Void?> {
                        override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Added override for $name custom audience")
                        }

                        override fun onFailure(e: Throwable) {
                            statusReceiver.accept(
                                    "Error adding override for " + name + " custom audience: " + e.message
                            )
                        }
                    },
                    executor
            )
        }
    }

    /**
     * Resets all custom audience overrides.
     *
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *   indicating the outcome of the call.
     */
    fun resetCAOverrides(statusReceiver: Consumer<String?>) {
        caOverrideClient.resetAllCustomAudienceOverrides().let {
            Futures.addCallback(
                    it,
                    object : FutureCallback<Void?> {
                        override fun onSuccess(unused: Void?) {
                            statusReceiver.accept("Reset all CA overrides")
                        }

                        override fun onFailure(e: Throwable) {
                            statusReceiver.accept("Error while resetting all CA overrides")
                        }
                    },
                    executor
            )
        }
    }

    private fun joinCustomAudience(ca: CustomAudience, statusReceiver: Consumer<String>) {
        runBlocking {
            caClient.joinCustomAudience(ca)
            statusReceiver.accept("Joined ${ca.name} custom audience")
        }
    }

    /** Initialize the custom audience wrapper and set the owner and buyer. */
    init {
        caOverrideClient =
                TestCustomAudienceClient.Builder().setContext(context).setExecutor(executor).build()
    }
}
