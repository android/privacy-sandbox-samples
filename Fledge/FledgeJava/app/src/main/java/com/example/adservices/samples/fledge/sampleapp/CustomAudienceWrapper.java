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

import static com.example.adservices.samples.fledge.SdkExtensionsHelpers.VersionCompatUtil.isTestableVersion;
import static com.example.adservices.samples.fledge.sampleapp.MainActivity.TAG;

import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.adservices.customaudience.AddCustomAudienceOverrideRequest;
import android.adservices.customaudience.CustomAudience;
import android.adservices.customaudience.FetchAndJoinCustomAudienceRequest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.adservices.samples.fledge.clients.CustomAudienceClient;
import com.example.adservices.samples.fledge.clients.TestCustomAudienceClient;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

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
        mCaClient =
                new CustomAudienceClient.Builder()
                        .setContext(context)
                        .setExecutor(executor)
                        .build();
        mCaOverrideClient =
                new TestCustomAudienceClient.Builder()
                        .setContext(context)
                        .setExecutor(executor)
                        .build();
    }

    /**
     * Joins a CA.
     *
     * @param customAudience the custom audience
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *     indicating the outcome of the call.
     */
    public void joinCa(CustomAudience customAudience, Consumer<String> statusReceiver) {
        try {
            Futures.addCallback(
                    mCaClient.joinCustomAudience(customAudience),
                    new FutureCallback<Void>() {
                        public void onSuccess(Void unused) {
                            statusReceiver.accept(
                                    "Joined "
                                            + customAudience.getName()
                                            + " custom audience with buyer '"
                                            + customAudience.getBuyer()
                                            + "'");
                        }

                        public void onFailure(@NonNull Throwable e) {
                            statusReceiver.accept(
                                    "Error when joining "
                                            + customAudience.getName()
                                            + " custom audience with buyer '"
                                            + customAudience.getBuyer()
                                            + "': "
                                            + e.getMessage());
                            Log.e(TAG, "Exception during CA join process ", e);
                        }
                    },
                    mExecutor);
        } catch (Exception e) {
            statusReceiver.accept(
                    "Got the following exception when trying to join "
                            + customAudience.getName()
                            + " custom audience: "
                            + e);
            Log.e(TAG, "Exception calling joinCustomAudience", e);
        }
    }

    /**
     * Leaves a CA.
     *
     * @param name The name of the CA to leave.
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *     indicating the outcome of the call.
     */
    public void leaveCa(String name, AdTechIdentifier buyer, Consumer<String> statusReceiver) {
        try {
            Futures.addCallback(
                    mCaClient.leaveCustomAudience(buyer, name),
                    new FutureCallback<Void>() {
                        public void onSuccess(Void unused) {
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
            Log.e(TAG, "Exception calling leaveCustomAudience", e);
        }
    }

    /**
     * Fetches and joins a CA with from an URI.
     *
     * @param fetchAndJoinCustomAudienceRequest the fetch and join CA request object.
     * @param statusReceiver A consumer function that is run after the API call and returns a
     *     string.
     */
    @SuppressLint("NewApi")
    public void fetchAndJoinCa(
            FetchAndJoinCustomAudienceRequest fetchAndJoinCustomAudienceRequest,
            Consumer<String> statusReceiver) {

        if (!isTestableVersion(10, 10)) {
            statusReceiver.accept(
                    "Unsupported SDK Extension: The fetchAndJoinCustomAudience API requires 10,"
                        + " skipping");
            Log.w(
                    MainActivity.TAG,
                    "Unsupported SDK Extension: The fetchAndJoinCustomAudience API requires 10,"
                        + " skipping");
            return;
        }
        try {
            Futures.addCallback(
                    mCaClient.fetchAndJoinCustomAudience(fetchAndJoinCustomAudienceRequest),
                    new FutureCallback<Void>() {
                        public void onSuccess(Void unused) {
                            statusReceiver.accept(
                                    "Fetched and joined "
                                            + fetchAndJoinCustomAudienceRequest.getName()
                                            + " custom audience.");
                        }

                        public void onFailure(@NonNull Throwable e) {
                            statusReceiver.accept(
                                    "Error when fetching and joining "
                                            + fetchAndJoinCustomAudienceRequest.getName()
                                            + " custom audience: "
                                            + e.getMessage());
                        }
                    },
                    mExecutor);
        } catch (Exception e) {
            statusReceiver.accept(
                    "Got the following exception when trying to fetch and join"
                            + fetchAndJoinCustomAudienceRequest.getName()
                            + " custom audience: "
                            + e);
            Log.e(MainActivity.TAG, "Exception calling fetchAndJoinCustomAudience", e);
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
            AdTechIdentifier buyer,
            String biddingLogicJs,
            AdSelectionSignals trustedBiddingSignals,
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
            Log.e(TAG, "Exception calling overrideCustomAudienceRemoteInfo", e);
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
            Log.e(TAG, "Exception calling resetAllCustomAudienceOverrides", e);
        }
    }
}
