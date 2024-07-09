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
package com.example.adservices.samples.signals.sampleapp;

import android.adservices.adselection.AdSelectionConfig;
import android.adservices.adselection.AdSelectionManager;
import android.adservices.adselection.AdSelectionOutcome;
import android.adservices.adselection.GetAdSelectionDataOutcome;
import android.adservices.adselection.GetAdSelectionDataRequest;
import android.adservices.adselection.PersistAdSelectionResultRequest;
import android.adservices.adselection.ReportEventRequest;
import android.adservices.adselection.ReportImpressionRequest;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.adservices.signals.ProtectedSignalsManager;
import android.adservices.signals.UpdateSignalsRequest;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.os.ext.SdkExtensions;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.example.adservices.samples.signals.sampleapp.ServerAuctionHelpers.BiddingAuctionServerClient;
import com.example.adservices.samples.signals.sampleapp.ServerAuctionHelpers.SelectAdsResponse;
import com.example.adservices.samples.signals.sampleapp.databinding.ActivityMainBinding;
import com.google.common.io.BaseEncoding;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Android application activity for testing Signals API
 */
@RequiresApi(api = 35)
public class MainActivity extends AppCompatActivity {
    public static final String TAG = "SignalsSample";
    // Executor to be used for API calls
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private Context context;
    private ActivityMainBinding binding;
    private EventLogManager eventLog;


    /**
     * Does the initial setup for the app. This includes reading the Javascript server URIs from the
     * start intent, creating the ad selection and custom audience wrappers to wrap the APIs, and
     * tying the UI elements to the wrappers so that button clicks trigger the appropriate methods.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        eventLog = new EventLogManager(binding.eventLog);

        binding.updateSignalsButton.setOnClickListener(this::updateSignals);
        binding.auctionButton.setOnClickListener(this::runAuction);
        binding.reportImpressionButton.setOnClickListener(this::reportImpression);
    }

    private void updateSignals(View v) {
        Uri uri = Uri.parse(binding.urlInput.getText().toString());
        ProtectedSignalsManager psManager = context.getSystemService(ProtectedSignalsManager.class);
        UpdateSignalsRequest updateSignalsRequest = new UpdateSignalsRequest
            .Builder(uri)
            .build();
        OutcomeReceiver<Object, Exception> receiver = new OutcomeReceiver<Object, Exception>() {
            @Override
            public void onResult(Object o) {
                eventLog.writeEvent("Signal update with URL: " + uri + " succeeded!");
            }

            @Override
            public void onError(@NonNull Exception error) {
                eventLog.writeEvent("Signal update with URL: " + uri + " failed with error: " +
                error);
            }
        };
        eventLog.writeEvent("Attempting signal update with URL: " + uri);
        psManager.updateSignals(updateSignalsRequest, EXECUTOR, receiver);


    }

    private void runAuction(View v) {
        if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) < 10) {
            throw new RuntimeException("AdServices extension version must be greater than 10");
        }
        Uri buyerUri = Uri.parse(binding.urlInput.getText().toString());
        Uri sellerUri = Uri.parse(binding.auctionUrlInput.getText().toString());
        AdTechIdentifier seller =
            AdTechIdentifier.fromString(binding.auctionSellerInput.getText().toString());
        Log.v(TAG, "Running auction with seller=" + sellerUri + ", buyer=" + buyerUri);
        AdSelectionManager adManager = context.getSystemService(AdSelectionManager.class);
        GetAdSelectionDataRequest getAdSelectionDataRequest = new GetAdSelectionDataRequest.Builder()
            .setSeller(seller)
            .build();
        OutcomeReceiver<GetAdSelectionDataOutcome, Exception> receiver =
            new OutcomeReceiver<GetAdSelectionDataOutcome, Exception>() {
            @Override
            public void onResult(GetAdSelectionDataOutcome o) {
                if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) < 10) {
                    throw new RuntimeException("AdServices extension version must be greater than 10");
                }
                eventLog.writeEvent("getAdSelectionData with URL: " + sellerUri + " succeeded!");
                eventLog.writeEvent("Calling Ad Selection Server");
                BiddingAuctionServerClient auctionServerClient =
                    new BiddingAuctionServerClient(context);
                SelectAdsResponse actualResponse = null;
                boolean failed = false;
                try {
                    actualResponse = auctionServerClient.runServerAuction(
                        sellerUri.toString(),
                        seller.toString(),
                        buyerUri.getHost(),
                        o.getAdSelectionData(),
                        binding.isContextualAdsRequested.isChecked());
                } catch (Exception e) {
                    eventLog.writeEvent("Error calling adSelection server " + e);
                    Log.e(TAG,"Error calling server", e);
                    failed = true;
                }
                if (!failed) {
                    Pair<Long, SelectAdsResponse> serverAuctionResult =
                        new Pair<>(o.getAdSelectionId(), actualResponse);
                    eventLog.writeEvent("Got Response from server");
                    Log.v(TAG, "Response cipher text"
                        + serverAuctionResult.second.auctionResultCiphertext);

                    PersistAdSelectionResultRequest persistAdSelectionResultRequest =
                        new PersistAdSelectionResultRequest.Builder()
                            .setSeller(seller)
                            .setAdSelectionId(o.getAdSelectionId())
                            .setAdSelectionResult(
                                BaseEncoding.base64().decode(serverAuctionResult.second.auctionResultCiphertext))
                            .build();

                    OutcomeReceiver<AdSelectionOutcome, Exception> persistReceiver =
                        new OutcomeReceiver<AdSelectionOutcome, Exception>() {
                            @Override
                            public void onResult(AdSelectionOutcome adSelectionOutcome) {
                                eventLog.writeEvent(
                                    "Render uri " + adSelectionOutcome.getRenderUri());
                            }

                            @Override
                            public void onError(@NonNull Exception error) {
                                eventLog.writeEvent("Persist failed " + error);
                                Log.v(TAG, "Persist failed", error);
                            }
                        };
                    adManager.persistAdSelectionResult(persistAdSelectionResultRequest, EXECUTOR,
                        persistReceiver);
                }
            }

            @Override
            public void onError(@NonNull Exception error) {
                eventLog.writeEvent("getAdSelectionData with URL: " + sellerUri + " failed with error: " +
                    error);
            }
        };
        eventLog.writeEvent("Attempting getAdSelectionData with URL: " + sellerUri);
        try {
            adManager.getAdSelectionData(getAdSelectionDataRequest, EXECUTOR, receiver);
        } catch (Exception e) {
            eventLog.writeEvent("Error getting data " + e);
            Log.e(TAG,"Error getting data", e);
        }
    }

    private void reportImpression(View v) {
        if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) < 10) {
            throw new RuntimeException("AdServices extension version must be greater than 10");
        }

        // This should not be needed. Hold here due to a bug.
        List<AdTechIdentifier> buyers = Collections.singletonList(
            AdTechIdentifier.fromString(binding.urlInput.getText().toString()));
        AdTechIdentifier seller =
            AdTechIdentifier.fromString(binding.auctionSellerInput.getText().toString());
        AdSelectionConfig adSelectionConfig = new AdSelectionConfig.Builder()
            .setSeller(seller)
            .setDecisionLogicUri(Uri.parse("https://" + seller + "/decisionLogicUri"))
            .setCustomAudienceBuyers(buyers)
            .setAdSelectionSignals(AdSelectionSignals.EMPTY)
            .setSellerSignals(AdSelectionSignals.EMPTY)
            .setPerBuyerSignals(
                buyers.stream()
                    .collect(Collectors.toMap(buyer -> buyer,
                        buyer -> AdSelectionSignals.EMPTY)))
            .setTrustedScoringSignalsUri(Uri.parse("https://" + seller + "/trust"))
            .build();
        try {
            String adSelectionIdInput = binding.adSelectionId.getText().toString();
            long adSelectionId = Long.parseLong(adSelectionIdInput);
            ReportImpressionRequest request =
                new ReportImpressionRequest(adSelectionId, adSelectionConfig);
            AdSelectionManager adManager = context.getSystemService(AdSelectionManager.class);

            CountDownLatch countDownLatch = new CountDownLatch(1);

            adManager.reportImpression(
                request,
                EXECUTOR,
                new OutcomeReceiver<Object, Exception>() {
                    @Override
                    public void onResult(@NonNull Object ignoredResult) {
                        eventLog.writeEvent("Registered beacons successfully.");
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onError(@NonNull Exception error) {
                        eventLog.writeEvent(
                            "Error when reporting impressions: " + error.getMessage());
                    }
                });

            if (countDownLatch.await(5, TimeUnit.SECONDS)){

                String viewInteraction = "clickEvent";
                String interactionData = "{\"clickTimeSeconds\":1}";
                if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= 8) {
                    reportEvent(
                        adSelectionId,
                        viewInteraction,
                        interactionData,
                        ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER
                            | ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER);
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, String.format("Error while parsing the ad selection id: %s", e));
            eventLog.writeEvent("Invalid AdSelectionId. Cannot run report impressions!");
        } catch (Exception e) {
            eventLog.writeEvent("Error when reporting impressions: " + e.getMessage());
        }
    }

    /**
     * Runs interaction reporting.
     *
     * @param adSelectionId         The auction associated with the ad.
     * @param eventKey              The type of event to be reported.
     * @param eventData             Data associated with the event.
     * @param reportingDestinations the destinations to report to, (buyer/seller)
     */
    public void reportEvent(
        long adSelectionId,
        String eventKey,
        String eventData,
        int reportingDestinations) {
        if (SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) < 8) {
            eventLog.writeEvent("Unsupported SDK Extension: Event reporting requires 8, skipping");
            Log.w(MainActivity.TAG,
                "Unsupported SDK Extension: Event reporting requires 8, skipping");
            return;
        }

        ReportEventRequest request =
            new ReportEventRequest.Builder(adSelectionId, eventKey, eventData,
                reportingDestinations)
                .build();

        AdSelectionManager adManager = context.getSystemService(AdSelectionManager.class);
        adManager.reportEvent(
            request,
            EXECUTOR,
            new OutcomeReceiver<Object, Exception>() {
                @Override
                public void onResult(@NonNull Object ignoredResult) {
                    eventLog.writeEvent(String.format("Reported %s event.", eventKey));
                }

                @Override
                public void onError(@NonNull Exception error) {
                    eventLog.writeEvent("Error when reporting event: " + error.getMessage());
                }
            });
    }

    /**
     * Resolve the host of the given URI and returns an {@code AdTechIdentifier} object
     *
     * @param uri Uri to resolve
     */
    private AdTechIdentifier resolveAdTechIdentifier(Uri uri) {
        if (uri == Uri.EMPTY || uri.getHost() != null) {
            return AdTechIdentifier.fromString("");
        }
        return AdTechIdentifier.fromString(uri.getHost());
    }
}