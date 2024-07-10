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
package com.example.adservices.samples.fledge.sampleapp;

import android.adservices.customaudience.AddCustomAudienceOverrideRequest;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.privacysandbox.ads.adservices.common.AdData;
import androidx.privacysandbox.ads.adservices.common.AdFilters;
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals;
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier;
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.Ext8OptIn;
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters;
import androidx.privacysandbox.ads.adservices.common.KeyedFrequencyCap;
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience;
import androidx.privacysandbox.ads.adservices.customaudience.TrustedBiddingData;

import com.example.adservices.samples.fledge.clients.CustomAudienceClient;
import com.example.adservices.samples.fledge.clients.TestCustomAudienceClient;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import kotlin.Unit;

import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Wrapper for the FLEDGE Custom Audience (CA) API. Creating the wrapper locks the user into a given
 * owner and buyer. In order to interact with the wrapper they will first need to call the create
 * method to create a CA object. After that they can call joinCA and leaveCA.
 */
@RequiresApi(api = 34)
public class CustomAudienceWrapper {
    private final Executor mExecutor;
    private final CustomAudienceClient mCaClient;
    private final TestCustomAudienceClient mCaOverrideClient;

    /**
     * Initialize the custom audience wrapper and set the owner and buyer.
     *
     * @param context The application context.
     * @param executor An executor to use with the FLEDGE API calls.
     */
    public CustomAudienceWrapper(Context context, Executor executor) {
        mExecutor = executor;
        mCaClient = new CustomAudienceClient(context);
        mCaOverrideClient =
                new TestCustomAudienceClient.Builder()
                        .setContext(context)
                        .setExecutor(executor)
                        .build();
    }

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
     *     indicating the outcome of the call.
     */
    public void joinCa(
            String name,
            String owner,
            AdTechIdentifier buyer,
            Uri biddingUri,
            Uri renderUri,
            Uri dailyUpdateUri,
            Uri trustedBiddingUri,
            Consumer<String> statusReceiver,
            Instant expiry) {
        try {
            joinCustomAudience(
                    new CustomAudience.Builder(
                                    buyer,
                                    name,
                                    dailyUpdateUri,
                                    biddingUri,
                                    Collections.singletonList(
                                            new AdData(renderUri, new JSONObject().toString())))
                            .setActivationTime(Instant.now())
                            .setExpirationTime(expiry)
                            .setTrustedBiddingData(
                                    new TrustedBiddingData(
                                            trustedBiddingUri, Collections.singletonList("key")))
                            .setUserBiddingSignals(new AdSelectionSignals("{}"))
                            .build(),
                    statusReceiver);
        } catch (Exception e) {
            statusReceiver.accept(
                    "Got the following exception when trying to join "
                            + name
                            + " custom audience: "
                            + e);
            Log.e(MainActivity.TAG, "Exception calling joinCustomAudience", e);
        }
    }

    /**
     * Creates a CA with empty user bidding signals, trusted bidding data, and ads.
     *
     * @param name The name of the CA to join.
     * @param owner The owner of the CA
     * @param buyer The buyer of ads
     * @param biddingUri The URL to retrieve the bidding logic
     * @param dailyUpdateUri The URL for daily updates for the CA
     * @param statusReceiver A consumer function that is run after the API call and returns a
     */
    public void joinEmptyFieldsCa(
            String name,
            String owner,
            AdTechIdentifier buyer,
            Uri biddingUri,
            Uri dailyUpdateUri,
            Consumer<String> statusReceiver,
            Instant expiry) {
        try {
            joinCustomAudience(
                    new CustomAudience.Builder(
                                    buyer,
                                    name,
                                    dailyUpdateUri,
                                    biddingUri,
                                    Collections.emptyList())
                            .setActivationTime(Instant.now())
                            .setExpirationTime(expiry)
                            .build(),
                    statusReceiver);
        } catch (Exception e) {
            statusReceiver.accept(
                    "Got the following exception when trying to join "
                            + name
                            + " custom audience: "
                            + e);
            Log.e(MainActivity.TAG, "Exception calling joinCustomAudience", e);
        }
    }

    /**
     * Leaves a CA.
     *
     * @param name The name of the CA to leave.
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *     indicating the outcome of the call.
     */
    public void leaveCa(
            String name, String owner, AdTechIdentifier buyer, Consumer<String> statusReceiver) {
        try {
            Futures.addCallback(
                    mCaClient.leaveCustomAudience(owner, buyer, name),
                    new FutureCallback<Unit>() {
                        public void onSuccess(Unit unused) {
                            statusReceiver.accept("Left " + name + " custom audience");
                        }

                        public void onFailure(@NonNull Throwable e) {
                            statusReceiver.accept(
                                    "Error when leaving "
                                            + name
                                            + " custom audience: "
                                            + e.getMessage());
                        }
                    },
                    mExecutor);
        } catch (Exception e) {
            statusReceiver.accept(
                    "Got the following exception when trying to leave "
                            + name
                            + " custom audience: "
                            + e);
            Log.e(MainActivity.TAG, "Exception calling leaveCustomAudience", e);
        }
    }

    /**
     * Overrides remote info for a CA.
     *
     * @param name The name of the CA to override remote info.
     * @param biddingLogicJs The overriding bidding logic javascript
     * @param trustedBiddingSignals The overriding trusted bidding signals
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *     indicating the outcome of the call.
     */
    public void addCAOverride(
            String name,
            String owner,
            android.adservices.common.AdTechIdentifier buyer,
            String biddingLogicJs,
            android.adservices.common.AdSelectionSignals trustedBiddingSignals,
            Consumer<String> statusReceiver) {
        try {
            AddCustomAudienceOverrideRequest request =
                    new AddCustomAudienceOverrideRequest.Builder()
                            .setBuyer(buyer)
                            .setName(name)
                            .setBiddingLogicJs(biddingLogicJs)
                            .setTrustedBiddingSignals(trustedBiddingSignals)
                            .build();
            Futures.addCallback(
                    mCaOverrideClient.overrideCustomAudienceRemoteInfo(request),
                    new FutureCallback<Void>() {
                        public void onSuccess(Void unused) {
                            statusReceiver.accept(
                                    "Added override for " + name + " custom audience");
                        }

                        public void onFailure(@NonNull Throwable e) {
                            statusReceiver.accept(
                                    "Error adding override for "
                                            + name
                                            + " custom audience: "
                                            + e.getMessage());
                        }
                    },
                    mExecutor);
        } catch (Exception e) {
            statusReceiver.accept(
                    "Got the following exception when trying to add override for "
                            + name
                            + " custom audience: "
                            + e);
            Log.e(MainActivity.TAG, "Exception calling overrideCustomAudienceRemoteInfo", e);
        }
    }

    /**
     * Resets all custom audience overrides.
     *
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *     indicating the outcome of the call.
     */
    public void resetCAOverrides(Consumer<String> statusReceiver) {
        try {
            Futures.addCallback(
                    mCaOverrideClient.resetAllCustomAudienceOverrides(),
                    new FutureCallback<Void>() {
                        public void onSuccess(Void unused) {
                            statusReceiver.accept("Reset all CA overrides");
                        }

                        public void onFailure(@NonNull Throwable e) {
                            statusReceiver.accept("Error while resetting all CA overrides");
                        }
                    },
                    mExecutor);
        } catch (Exception e) {
            statusReceiver.accept(
                    "Got the following exception when trying to reset all CA overrides: " + e);
            Log.e(MainActivity.TAG, "Exception calling resetAllCustomAudienceOverrides", e);
        }
    }

    private void joinCustomAudience(CustomAudience ca, Consumer<String> statusReceiver) {
        Futures.addCallback(
                mCaClient.joinCustomAudience(ca),
                new FutureCallback<Unit>() {
                    public void onSuccess(Unit unused) {
                        statusReceiver.accept("Joined " + ca.getName() + " custom audience");
                    }

                    public void onFailure(@NonNull Throwable e) {
                        statusReceiver.accept(
                                "Error when joining "
                                        + ca.getName()
                                        + " custom audience: "
                                        + e.getMessage());
                        Log.e(MainActivity.TAG, "Exception during CA join process ", e);
                    }
                },
                mExecutor);
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
     * @param filters {@link AdFilters} that should be applied to the ad in the CA
     * @param adCounterKeys set of keys used in counting events string indicating the outcome of the
     *     call.
     */
    @OptIn(markerClass = Ext8OptIn.class)
    public void joinFilteringCa(
            String name,
            AdTechIdentifier buyer,
            Uri biddingUri,
            Uri renderUri,
            Uri dailyUpdateUri,
            Uri trustedBiddingUri,
            Consumer<String> statusReceiver,
            Instant expiry,
            AdFilters filters,
            Set<Integer> adCounterKeys) {
        joinCustomAudience(
                new CustomAudience(
                        buyer,
                        name,
                        dailyUpdateUri,
                        biddingUri,
                        Collections.singletonList(
                                new AdData(
                                        renderUri,
                                        new JSONObject().toString(),
                                        adCounterKeys,
                                        filters)),
                        Instant.now(),
                        expiry,
                        new AdSelectionSignals("{}"),
                        new TrustedBiddingData(
                                trustedBiddingUri, Collections.singletonList("key"))),
                statusReceiver);
    }

    @OptIn(markerClass = Ext8OptIn.class)
    public void joinServerAuctionCa(
            String name,
            AdTechIdentifier buyer,
            Uri biddingUri,
            Uri renderUri,
            Uri dailyUpdateUri,
            Uri trustedBiddingUri,
            Consumer<String> statusReceiver,
            Instant expiry,
            int adRenderId) {
        int clickFilterMax = 1;
        KeyedFrequencyCap clickCap =
                new KeyedFrequencyCap(adRenderId, clickFilterMax, Duration.ofDays(1));
        joinCustomAudience(
                new CustomAudience(
                        buyer,
                        name,
                        dailyUpdateUri,
                        biddingUri,
                        Collections.singletonList(
                                new AdData(
                                        renderUri,
                                        new JSONObject().toString(),
                                        Collections.singleton(adRenderId),
                                        new AdFilters(
                                                new FrequencyCapFilters(
                                                        Collections.singletonList(clickCap))))),
                        Instant.now(),
                        expiry,
                        new AdSelectionSignals("{}"),
                        new TrustedBiddingData(trustedBiddingUri, Collections.emptyList())),
                statusReceiver);
    }

    /**
     * Fetches and joins a CA with from an URI.
     *
     * @param fetchUri The URL to retrieve the CA from.
     * @param name The name of the CA to join.
     * @param activationTime The time when the CA will activate.
     * @param expirationTime The time when the CA will expire.
     * @param userBiddingSignals The user bidding signals used at auction.
     * @param statusReceiver A consumer function that is run after the API call and returns a
     *     string.
     */
    public void fetchAndJoinCa(
            Uri fetchUri,
            String name,
            Instant activationTime,
            Instant expirationTime,
            AdSelectionSignals userBiddingSignals,
            Consumer<String> statusReceiver) {
        Futures.addCallback(
                mCaClient.fetchAndJoinCustomAudience(
                        fetchUri, name, activationTime, expirationTime, userBiddingSignals),
                new FutureCallback<Unit>() {
                    @Override
                    public void onSuccess(Unit result) {
                        statusReceiver.accept("Fetched and joined " + name + " custom audience.");
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        statusReceiver.accept(
                                "Error when fetching and joining "
                                        + name
                                        + " custom audience: "
                                        + e.getMessage());
                    }
                },
                mExecutor);
    }
}
