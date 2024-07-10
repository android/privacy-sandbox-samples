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

import android.adservices.adselection.AddAdSelectionOverrideRequest;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome;
import androidx.privacysandbox.ads.adservices.adselection.ReportEventRequest;
import androidx.privacysandbox.ads.adservices.adselection.ReportImpressionRequest;
import androidx.privacysandbox.ads.adservices.adselection.UpdateAdCounterHistogramRequest;
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals;
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier;
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.Ext8OptIn;
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters;

import com.example.adservices.samples.fledge.clients.AdSelectionClient;
import com.example.adservices.samples.fledge.clients.TestAdSelectionClient;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import kotlin.Unit;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Wrapper for the FLEDGE Ad Selection API. This wrapper is opinionated and makes several choices
 * such as running impression reporting immediately after every successful ad auction or leaving the
 * ad signals empty to limit the complexity that is exposed the user.
 */
@RequiresApi(api = 34)
public class AdSelectionWrapper {
    private final AdSelectionClient mAdClient;
    private final TestAdSelectionClient mOverrideClient;
    private final Executor mExecutor;
    private AdSelectionConfig mAdSelectionConfig;
    private android.adservices.adselection.AdSelectionConfig mAdSelectionConfigOverride;

    /**
     * Initializes the ad selection wrapper with a specific seller, list of buyers, and decision
     * endpoint.
     *
     * @param buyers A list of buyers for the auction.
     * @param seller The name of the seller for the auction
     * @param decisionUri The URI to retrieve the seller scoring and reporting logic from
     * @param context The application context.
     * @param executor An executor to use with the FLEDGE API calls.
     */
    public AdSelectionWrapper(
            List<AdTechIdentifier> buyers,
            AdTechIdentifier seller,
            Uri decisionUri,
            Uri trustedDataUri,
            Context context,
            Executor executor) {
        mAdSelectionConfig =
                new AdSelectionConfig(
                        seller,
                        decisionUri,
                        buyers,
                        new AdSelectionSignals("{}"),
                        new AdSelectionSignals("{}"),
                        buyers.stream()
                                .collect(
                                        Collectors.toMap(
                                                buyer -> buyer,
                                                buyer -> new AdSelectionSignals("{}"))),
                        trustedDataUri);
        mAdSelectionConfigOverride =
                new android.adservices.adselection.AdSelectionConfig.Builder()
                        .setSeller(
                                android.adservices.common.AdTechIdentifier.fromString(
                                        seller.toString()))
                        .setDecisionLogicUri(decisionUri)
                        .setCustomAudienceBuyers(
                                buyers.stream()
                                        .map(
                                                buyer ->
                                                        android.adservices.common.AdTechIdentifier
                                                                .fromString(buyer.toString()))
                                        .collect(Collectors.toList()))
                        .setAdSelectionSignals(android.adservices.common.AdSelectionSignals.EMPTY)
                        .setSellerSignals(android.adservices.common.AdSelectionSignals.EMPTY)
                        .setPerBuyerSignals(
                                buyers.stream()
                                        .collect(
                                                Collectors.toMap(
                                                        buyer ->
                                                                android.adservices.common
                                                                        .AdTechIdentifier
                                                                        .fromString(
                                                                                buyer.toString()),
                                                        buyer ->
                                                                android.adservices.common
                                                                        .AdSelectionSignals.EMPTY)))
                        .setTrustedScoringSignalsUri(trustedDataUri)
                        .build();
        mAdClient = new AdSelectionClient(context);
        mOverrideClient =
                new TestAdSelectionClient.Builder()
                        .setContext(context)
                        .setExecutor(executor)
                        .build();
        mExecutor = executor;
    }

    /**
     * Resets the {@code AdSelectionConfig} with the new decisionUri associated with this {@code
     * AdSelectionWrapper}. To be used when switching back and forth between dev overrides/mock
     * server states.
     *
     * @param decisionUri the new {@code Uri} to be used
     */
    public void resetAdSelectionConfig(
            List<AdTechIdentifier> buyers,
            AdTechIdentifier seller,
            Uri decisionUri,
            Uri trustedScoringUri) {
        mAdSelectionConfig =
                new AdSelectionConfig(
                        seller,
                        decisionUri,
                        buyers,
                        new AdSelectionSignals("{}"),
                        new AdSelectionSignals("{}"),
                        buyers.stream()
                                .collect(
                                        Collectors.toMap(
                                                buyer -> buyer,
                                                buyer -> new AdSelectionSignals("{}"))),
                        trustedScoringUri);
        mAdSelectionConfigOverride =
                new android.adservices.adselection.AdSelectionConfig.Builder()
                        .setSeller(
                                android.adservices.common.AdTechIdentifier.fromString(
                                        seller.toString()))
                        .setDecisionLogicUri(decisionUri)
                        .setCustomAudienceBuyers(
                                buyers.stream()
                                        .map(
                                                buyer ->
                                                        android.adservices.common.AdTechIdentifier
                                                                .fromString(buyer.toString()))
                                        .collect(Collectors.toList()))
                        .setAdSelectionSignals(android.adservices.common.AdSelectionSignals.EMPTY)
                        .setSellerSignals(android.adservices.common.AdSelectionSignals.EMPTY)
                        .setPerBuyerSignals(
                                buyers.stream()
                                        .collect(
                                                Collectors.toMap(
                                                        buyer ->
                                                                android.adservices.common
                                                                        .AdTechIdentifier
                                                                        .fromString(
                                                                                buyer.toString()),
                                                        buyer ->
                                                                android.adservices.common
                                                                        .AdSelectionSignals.EMPTY)))
                        .setTrustedScoringSignalsUri(trustedScoringUri)
                        .build();
    }

    /**
     * Runs ad selection and passes a string describing its status to the input receivers. If ad
     * selection succeeds, also report impressions.
     *
     * @param statusReceiver A consumer function that is run after ad selection and impression
     *     reporting with a string describing how the auction and reporting went.
     * @param renderUriReceiver A consumer function that is run after ad selection with a message
     *     describing the render URI or lack thereof.
     */
    @OptIn(markerClass = Ext8OptIn.class)
    public void runAdSelection(
            Consumer<String> statusReceiver, Consumer<String> renderUriReceiver) {
        try {
            Futures.addCallback(
                    mAdClient.selectAds(mAdSelectionConfig),
                    new FutureCallback<AdSelectionOutcome>() {
                        public void onSuccess(AdSelectionOutcome adSelectionOutcome) {
                            statusReceiver.accept(
                                    "Ran ad selection! Id: "
                                            + adSelectionOutcome.getAdSelectionId());
                            renderUriReceiver.accept(
                                    "Would display ad from " + adSelectionOutcome.getRenderUri());
                            updateAdCounterHistogram(
                                    adSelectionOutcome.getAdSelectionId(),
                                    FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION,
                                    statusReceiver);
                            reportImpression(adSelectionOutcome.getAdSelectionId(), statusReceiver);
                        }

                        public void onFailure(@NonNull Throwable e) {
                            statusReceiver.accept(
                                    "Error when running ad selection: " + e.getMessage());
                            renderUriReceiver.accept("Ad selection failed -- no ad to display");
                            Log.e(MainActivity.TAG, "Exception during ad selection", e);
                        }
                    },
                    mExecutor);
        } catch (Exception e) {
            statusReceiver.accept(
                    "Got the following exception when trying to run ad selection: " + e);
            renderUriReceiver.accept("Ad selection failed -- no ad to display");
            Log.e(MainActivity.TAG, "Exception calling runAdSelection", e);
        }
    }

    /**
     * Helper function of {@link #runAdSelection}. Runs impression reporting.
     *
     * @param adSelectionId The auction to report impression on.
     * @param statusReceiver A consumer function that is run after impression reporting with a
     *     string describing how the auction and reporting went.
     */
    public void reportImpression(long adSelectionId, Consumer<String> statusReceiver) {
        ReportImpressionRequest request =
                new ReportImpressionRequest(adSelectionId, mAdSelectionConfig);
        Futures.addCallback(
                mAdClient.reportImpression(request),
                new FutureCallback<Unit>() {
                    public void onSuccess(Unit unused) {
                        statusReceiver.accept("Reported impressions from ad selection");
                    }

                    public void onFailure(@NonNull Throwable e) {
                        statusReceiver.accept(
                                "Error when reporting impressions: " + e.getMessage());
                        Log.e(MainActivity.TAG, e.toString(), e);
                    }
                },
                mExecutor);
    }

    /**
     * Overrides remote info for an ad selection config.
     *
     * @param decisionLogicJS The overriding decision logic javascript
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *     indicating the outcome of the call.
     */
    public void overrideAdSelection(
            Consumer<String> statusReceiver,
            String decisionLogicJS,
            android.adservices.common.AdSelectionSignals trustedScoringSignals) {
        AddAdSelectionOverrideRequest request =
                new AddAdSelectionOverrideRequest(
                        mAdSelectionConfigOverride, decisionLogicJS, trustedScoringSignals);
        try {
            Futures.addCallback(
                    mOverrideClient.overrideAdSelectionConfigRemoteInfo(request),
                    new FutureCallback<Void>() {
                        public void onSuccess(Void unused) {
                            statusReceiver.accept("Added override for ad selection");
                        }

                        public void onFailure(@NonNull Throwable e) {
                            statusReceiver.accept(
                                    "Error when adding override for ad selection "
                                            + e.getMessage());
                        }
                    },
                    mExecutor);
        } catch (Exception e) {
            statusReceiver.accept(
                    "Got the following exception when trying to override remote info for ad selection: "
                            + e);
            Log.e(MainActivity.TAG, "Exception calling overrideAdSelectionConfigRemoteInfo", e);
        }
    }

    /**
     * Resets all ad selection overrides.
     *
     * @param statusReceiver A consumer function that is run after the API call and returns a string
     *     indicating the outcome of the call.
     */
    public void resetAdSelectionOverrides(Consumer<String> statusReceiver) {
        try {
            Futures.addCallback(
                    mOverrideClient.resetAllAdSelectionConfigRemoteOverrides(),
                    new FutureCallback<Void>() {
                        public void onSuccess(Void unused) {
                            statusReceiver.accept("Reset ad selection overrides");
                        }

                        public void onFailure(@NonNull Throwable e) {
                            statusReceiver.accept(
                                    "Error when resetting all ad selection overrides "
                                            + e.getMessage());
                        }
                    },
                    mExecutor);
        } catch (Exception e) {
            statusReceiver.accept(
                    "Got the following exception when trying to reset all ad selection overrides: "
                            + e);
            Log.e(
                    MainActivity.TAG,
                    "Exception calling resetAllAdSelectionConfigRemoteOverrides",
                    e);
        }
    }

    /**
     * Runs ad selection on Auction Servers and passes a string describing its status to the input
     * receivers. If ad selection succeeds, updates the ad histogram with an impression event and
     * reports the impression.
     *
     * @param statusReceiver A consumer function that is run after ad selection, histogram updating,
     *     and impression reporting with a string describing how the auction, histogram updating,
     *     and reporting went.
     * @param renderUriReceiver A consumer function that is run after ad selection with a message
     *     describing the render URI or lack thereof.
     */
    @OptIn(markerClass = ExperimentalFeatures.Ext10OptIn.class)
    public void runAdSelectionOnAuctionServer(
            Uri sellerSfeUri,
            AdTechIdentifier seller,
            AdTechIdentifier buyer,
            Consumer<String> statusReceiver,
            Consumer<String> renderUriReceiver) {
        GetAdSelectionDataRequest getDataRequest = new GetAdSelectionDataRequest(seller);
        try {
            ListenableFuture<AdSelectionOutcome> adSelectionOutcome =
                    FluentFuture.from(mAdClient.getAdSelectionData(getDataRequest))
                            .transform(
                                    outcome -> {
                                        statusReceiver.accept(
                                                "CA data collected from device! Id: " + outcome.getAdSelectionId());
                                        try {
                                            BiddingAuctionServerClient auctionServerClient =
                                                    new BiddingAuctionServerClient(mContext);
                                            SelectAdsResponse actualResponse = auctionServerClient.runServerAuction(
                                                    sellerSfeUri.toString(),
                                                    seller.toString(),
                                                    buyer.toString(),
                                                    outcome.getAdSelectionData());
                                            Pair<Long, SelectAdsResponse> serverAuctionResult =
                                                    new Pair<>(outcome.getAdSelectionId(), actualResponse);
                                            statusReceiver.accept(
                                                    "Server auction run successfully for " + serverAuctionResult.first);
                                            return serverAuctionResult;
                                        } catch (IOException e) {
                                            statusReceiver.accept(
                                                    "Something went wrong when calling bidding auction server");
                                            throw new UncheckedIOException(e);
                                        }
                                    }, mExecutor)
                            .transformAsync(
                                    pair -> {
                                        Objects.requireNonNull(pair);
                                        long adSelectionId = pair.first;
                                        SelectAdsResponse response = pair.second;
                                        Objects.requireNonNull(response);
                                        Objects.requireNonNull(response.auctionResultCiphertext);
                                        PersistAdSelectionResultRequest persistResultRequest =
                                                new PersistAdSelectionResultRequest(adSelectionId, seller,
                                                        BaseEncoding.base64().decode(response.auctionResultCiphertext));
                                        return mAdClient.persistAdSelectionResult(persistResultRequest);
                                    }, mExecutor);
            Futures.addCallback(
                    adSelectionOutcome,
                    new FutureCallback<AdSelectionOutcome>() {
                        public void onSuccess(AdSelectionOutcome adSelectionOutcome) {
                            statusReceiver.accept(
                                    "Auction Result is persisted for : " + adSelectionOutcome.getAdSelectionId());
                            if (adSelectionOutcome.hasOutcome()) {
                                renderUriReceiver.accept(
                                        "Would display ad from " + adSelectionOutcome.getRenderUri());
                                reportImpression(adSelectionOutcome.getAdSelectionId(), statusReceiver);
                            } else {
                                renderUriReceiver.accept("Would display ad from contextual winner");
                            }
                        }

                        public void onFailure(@NonNull Throwable e) {
                            statusReceiver.accept("Error when running ad selection: " + e.getMessage());
                            renderUriReceiver.accept("Ad selection failed -- no ad to display");
                            Log.e(MainActivity.TAG, "Exception during ad selection", e);
                        }
                    }, mExecutor);
        } catch (Exception e) {
            statusReceiver.accept("Got the following exception when trying to run ad selection: " + e);
            renderUriReceiver.accept("Ad selection failed -- no ad to display");
            Log.e(MainActivity.TAG, "Exception calling runAdSelection", e);
        }
    }

    /**
     * Runs interaction reporting.
     *
     * @param adSelectionId The auction associated with the ad.
     * @param eventKey The type of event to be reported.
     * @param eventData Data associated with the event.
     * @param reportingDestinations the destinations to report to, (buyer/seller)
     * @param statusReceiver A consumer function that is run after event reporting with a string
     *     describing how the reporting went.
     */
    @OptIn(markerClass = Ext8OptIn.class)
    public void reportEvent(
            long adSelectionId,
            String eventKey,
            String eventData,
            int reportingDestinations,
            Consumer<String> statusReceiver) {
        Futures.addCallback(
                mAdClient.reportEvent(
                        new ReportEventRequest(
                                adSelectionId, eventKey, eventData, reportingDestinations, null)),
                new FutureCallback<Unit>() {
                    public void onSuccess(Unit unused) {
                        statusReceiver.accept(String.format("Reported %s event.", eventKey));
                    }

                    public void onFailure(@NonNull Throwable e) {
                        statusReceiver.accept("Error when reporting event: " + e.getMessage());
                        Log.e(MainActivity.TAG, e.toString(), e);
                    }
                },
                mExecutor);
    }

    /**
     * Helper function of {@link AdSelectionClient#updateAdCounterHistogram}. Updates the counter
     * histograms for an ad.
     *
     * @param adSelectionId The identifier associated with the winning ad.
     * @param adEventType identifies which histogram should be updated
     * @param statusReceiver A consumer function that is run after that reports how the call went
     *     after it is completed
     */
    @OptIn(markerClass = Ext8OptIn.class)
    public void updateAdCounterHistogram(
            long adSelectionId, int adEventType, Consumer<String> statusReceiver) {
        AdTechIdentifier callerAdTech = mAdSelectionConfig.getSeller();

        Futures.addCallback(
                mAdClient.updateAdCounterHistogram(
                        new UpdateAdCounterHistogramRequest(
                                adSelectionId, adEventType, callerAdTech)),
                new FutureCallback<Unit>() {
                    public void onSuccess(Unit unused) {
                        statusReceiver.accept(
                                String.format(
                                        "Updated ad counter histogram with %s event for adtech: %s",
                                        fCapEventToString(adEventType), callerAdTech));
                    }

                    public void onFailure(@NonNull Throwable e) {
                        e.printStackTrace();
                        statusReceiver.accept("Error when updating ad counter histogram: " + e);
                        Log.e(MainActivity.TAG, e.toString(), e);
                    }
                },
                mExecutor);
    }

    @OptIn(markerClass = Ext8OptIn.class)
    private String fCapEventToString(int eventType) {
        String result;
        switch (eventType) {
            case FrequencyCapFilters.AD_EVENT_TYPE_WIN:
                result = "win";
                break;
            case FrequencyCapFilters.AD_EVENT_TYPE_CLICK:
                result = "click";
                break;
            case FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION:
                result = "impression";
                break;
            case FrequencyCapFilters.AD_EVENT_TYPE_VIEW:
                result = "view";
                break;
            default:
                result = "unknown";
        }
        return result;
    }
}
