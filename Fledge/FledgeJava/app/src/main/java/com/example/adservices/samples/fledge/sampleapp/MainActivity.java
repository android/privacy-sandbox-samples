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


import android.adservices.adselection.ReportEventRequest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.privacysandbox.ads.adservices.common.AdFilters;
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals;
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier;
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.Ext8OptIn;
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters;
import androidx.privacysandbox.ads.adservices.common.KeyedFrequencyCap;

import com.example.adservices.samples.fledge.sampleapp.databinding.ActivityMainBinding;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/** Android application activity for testing FLEDGE API */
@RequiresApi(api = 34)
public class MainActivity extends AppCompatActivity {

    // Log tag
    public static final String TAG = "FledgeSample";
    private static final String SHOES_SERVER_AUCTION_CA_NAME = "winningCA";
    private static final String SHIRTS_SERVER_AUCTION_CA_NAME = "shirts_server";
    // Server Auction Custom Audience Render Id
    private static final int SHOES_SERVER_AUCTION_AD_RENDER_ID = 1;
    private static final int SHIRTS_SERVER_AUCTION_AD_RENDER_ID = 2;
    // JSON string objects that will be used during ad selection
    private static final AdSelectionSignals TRUSTED_SCORING_SIGNALS =
            new AdSelectionSignals(
                    "{\n"
                            + "\t\"render_uri_1\": \"signals_for_1\",\n"
                            + "\t\"render_uri_2\": \"signals_for_2\"\n"
                            + "}");
    private static final AdSelectionSignals TRUSTED_BIDDING_SIGNALS =
            new AdSelectionSignals(
                    "{\n"
                            + "\t\"example\": \"example\",\n"
                            + "\t\"valid\": \"Also valid\",\n"
                            + "\t\"list\": \"list\",\n"
                            + "\t\"of\": \"of\",\n"
                            + "\t\"keys\": \"trusted bidding signal Values\"\n"
                            + "}");
    // JS files
    private static final String BIDDING_LOGIC_FILE = "BiddingLogic.js";
    private static final String DECISION_LOGIC_FILE = "DecisionLogic.js";
    // The names for the shirts and shoes custom audience
    private static final String SHOES_CA_NAME = "shoes";
    private static final String SHIRTS_CA_NAME = "shirts";
    private static final String HATS_CA_NAME = "hats";
    private static final String SHORT_EXPIRING_CA_NAME = "short_expiring";
    private static final String INVALID_FIELD_CA_NAME = "invalid_fields";
    private static final String FREQ_CAP_CA_NAME = "freq_cap";
    // Intents
    private static final String BASE_URL_INTENT = "baseUrl";
    private static final String AUCTION_SERVER_SELLER_SFE_URL_INTENT = "auctionServerSellerSfeUrl";
    private static final String AUCTION_SERVER_SELLER_INTENT = "auctionServerSeller";
    private static final String AUCTION_SERVER_BUYER_INTENT = "auctionServerBuyer";
    // Expiry durations
    private static final Duration ONE_DAY_EXPIRY = Duration.ofDays(1);
    private static final Duration THIRTY_SECONDS_EXPIRY = Duration.ofSeconds(30);
    // Executor to be used for API calls
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    // String to inform user a field in missing
    private static final String MISSING_FIELD_STRING_FORMAT_RESTART_APP =
            "ERROR: %s is missing, restart the activity using the directions in the README. The app"
                    + " will not be usable until this is done.";
    private String mBaseUriString;
    private Uri mBiddingLogicUri;
    private Uri mScoringLogicUri;
    private Uri mTrustedDataUri;
    private AdTechIdentifier mBuyer;
    private AdTechIdentifier mSeller;
    private Uri mAuctionServerSellerSfeUri;
    private AdTechIdentifier mAuctionServerSeller;
    private AdTechIdentifier mAuctionServerBuyer;
    private AdSelectionWrapper adWrapper;
    private CustomAudienceWrapper caWrapper;
    private String overrideDecisionJS;
    private String overrideBiddingJs;
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
        mBaseUriString =
                getIntentOrError(
                        BASE_URL_INTENT, eventLog, MISSING_FIELD_STRING_FORMAT_RESTART_APP);
        mAuctionServerSellerSfeUri = auctionServerSellerSfeUriOrEmpty();
        mAuctionServerSeller = auctionServerSellerOrEmpty();
        mAuctionServerBuyer = auctionServerBuyerOrEmpty();
        try {
            // Get override reporting URI
            String reportingUriString = mBaseUriString;
            // Replace override URIs in JS
            overrideDecisionJS =
                    replaceReportingURI(assetFileToString(DECISION_LOGIC_FILE), reportingUriString);
            overrideBiddingJs =
                    replaceReportingURI(assetFileToString(BIDDING_LOGIC_FILE), reportingUriString);
            // Setup overrides since they are on by default
            setupOverrideFlow();
            // Set up Report Impression button and text box
            setupReportImpressionButton(adWrapper, binding, eventLog);
            // Setup Report Click button and text boxes
            setupReportClickButton(adWrapper, binding, eventLog);
            // Set up Update Ad Counter Histogram button and text box
            setupUpdateClickHistogramButton(adWrapper, binding, eventLog);
            // Set up Override Switch
            binding.overrideSwitch.setOnCheckedChangeListener(this::toggleOverrideSwitch);
            // Set up Auction Server ad selection
            binding.auctionServer.setOnCheckedChangeListener(
                    (ignored1, ignored2) -> toggleAuctionServerCheckbox());
        } catch (Exception e) {
            Log.e(TAG, "Error when setting up app", e);
        }
    }

    private void toggleOverrideSwitch(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            setupOverrideFlow();
        } else {
            try {
                String baseUri =
                        getIntentOrError(
                                BASE_URL_INTENT, eventLog, MISSING_FIELD_STRING_FORMAT_RESTART_APP);
                mBiddingLogicUri = Uri.parse(baseUri + "/bidding");
                mScoringLogicUri = Uri.parse(baseUri + "/scoring");
                mTrustedDataUri = Uri.parse(mBiddingLogicUri + "/trusted");
                mBuyer = resolveAdTechIdentifier(mBiddingLogicUri);
                mSeller = resolveAdTechIdentifier(mScoringLogicUri);
                // Set with new scoring uri
                adWrapper.resetAdSelectionConfig(
                        Collections.singletonList(mBuyer),
                        mSeller,
                        mScoringLogicUri,
                        mTrustedDataUri);
                // Reset CA switches as they rely on different biddingLogicUri
                setupCASwitches(caWrapper, eventLog, binding, mBiddingLogicUri, context);
                resetOverrides(eventLog, adWrapper, caWrapper);
            } catch (Exception e) {
                binding.overrideSwitch.setChecked(true);
                Log.e(TAG, "Cannot disable overrides because mock URLs not provided", e);
            }
        }
    }

    private void toggleAuctionServerCheckbox() {
        if (!isAuctionServerSetupReady("Using auction servers")) {
            binding.auctionServer.setChecked(false);
            return;
        }
        Log.i(TAG, "Auction Server check toggled to " + binding.auctionServer.isChecked());
        setAdSelectionWrapper();
        eventLog.writeEvent("Auction Server check toggled to " + binding.auctionServer.isChecked());
    }

    private void setAdSelectionWrapper() {
        List<AdTechIdentifier> buyers = Collections.singletonList(mBuyer);
        adWrapper =
                new AdSelectionWrapper(
                        buyers, mSeller, mScoringLogicUri, mTrustedDataUri, context, EXECUTOR);
        if (binding.auctionServer.isChecked()) {
            binding.runAdsButton.setOnClickListener(
                    v -> {
                        eventLog.flush();
                        adWrapper.runAdSelectionOnAuctionServer(
                                mAuctionServerSellerSfeUri,
                                mAuctionServerSeller,
                                mAuctionServerBuyer,
                                eventLog::writeEvent,
                                binding.adSpace::setText);
                    });
        } else {
            binding.runAdsButton.setOnClickListener(
                    v -> {
                        eventLog.flush();
                        adWrapper.runAdSelection(eventLog::writeEvent, binding.adSpace::setText);
                    });
        }
    }

    private void setupOverrideFlow() {
        // Set override URIS since overrides are on by default
        String overrideUriBase =
                getIntentOrError(
                        BASE_URL_INTENT, eventLog, MISSING_FIELD_STRING_FORMAT_RESTART_APP);
        mBiddingLogicUri = Uri.parse(overrideUriBase + "/bidding");
        mScoringLogicUri = Uri.parse(overrideUriBase + "/scoring");
        mTrustedDataUri = Uri.parse(mBiddingLogicUri + "/trusted");
        mBuyer = resolveAdTechIdentifier(mBiddingLogicUri);
        mSeller = resolveAdTechIdentifier(mScoringLogicUri);
        // Set up ad selection
        adWrapper =
                new AdSelectionWrapper(
                        Collections.singletonList(mBuyer),
                        mSeller,
                        mScoringLogicUri,
                        mTrustedDataUri,
                        context,
                        EXECUTOR);
        binding.runAdsButton.setOnClickListener(
                v -> {
                    eventLog.flush();
                    adWrapper.runAdSelection(eventLog::writeEvent, binding.adSpace::setText);
                });
        // Set up Custom Audience Wrapper(CAs)
        caWrapper = new CustomAudienceWrapper(context, EXECUTOR);
        // Set up CA buttons
        setupCASwitches(caWrapper, eventLog, binding, mBiddingLogicUri, context);
        // Set up remote overrides by default
        useOverrides(
                eventLog,
                adWrapper,
                caWrapper,
                overrideDecisionJS,
                overrideBiddingJs,
                TRUSTED_SCORING_SIGNALS,
                TRUSTED_BIDDING_SIGNALS,
                mBiddingLogicUri,
                context);
    }

    @OptIn(markerClass = Ext8OptIn.class)
    private void setupCASwitches(
            CustomAudienceWrapper caWrapper,
            EventLogManager eventLog,
            ActivityMainBinding binding,
            Uri biddingUri,
            Context context) {
        AdTechIdentifier buyer = new AdTechIdentifier(Objects.requireNonNull(biddingUri.getHost()));
        // Shoes
        binding.shoesCaSwitch.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if (isChecked) {
                        caWrapper.joinCa(
                                SHOES_CA_NAME,
                                context.getPackageName(),
                                buyer,
                                biddingUri,
                                Uri.parse(biddingUri + "/render_" + SHOES_CA_NAME),
                                Uri.parse(biddingUri + "/daily"),
                                Uri.parse(biddingUri + "/trusted"),
                                eventLog::writeEvent,
                                calcExpiry(ONE_DAY_EXPIRY));
                    } else {
                        caWrapper.leaveCa(
                                SHOES_CA_NAME,
                                context.getPackageName(),
                                buyer,
                                eventLog::writeEvent);
                    }
                });
        // Shirts
        binding.shirtsCaSwitch.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if (isChecked) {
                        caWrapper.joinCa(
                                SHIRTS_CA_NAME,
                                context.getPackageName(),
                                buyer,
                                biddingUri,
                                Uri.parse(biddingUri + "/render_" + SHIRTS_CA_NAME),
                                Uri.parse(biddingUri + "/daily"),
                                Uri.parse(biddingUri + "/trusted"),
                                eventLog::writeEvent,
                                calcExpiry(ONE_DAY_EXPIRY));
                    } else {
                        caWrapper.leaveCa(
                                SHIRTS_CA_NAME,
                                context.getPackageName(),
                                buyer,
                                eventLog::writeEvent);
                    }
                });
        // Short expiring CA
        binding.shortExpiryCaSwitch.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if (isChecked) {
                        caWrapper.joinCa(
                                SHORT_EXPIRING_CA_NAME,
                                context.getPackageName(),
                                buyer,
                                biddingUri,
                                Uri.parse(biddingUri + "/render_" + SHORT_EXPIRING_CA_NAME),
                                Uri.parse(biddingUri + "/daily"),
                                Uri.parse(biddingUri + "/trusted"),
                                eventLog::writeEvent,
                                calcExpiry(THIRTY_SECONDS_EXPIRY));
                    } else {
                        caWrapper.leaveCa(
                                SHORT_EXPIRING_CA_NAME,
                                context.getPackageName(),
                                buyer,
                                eventLog::writeEvent);
                    }
                });
        // Invalid fields CA
        binding.invalidFieldsCaSwitch.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if (isChecked) {
                        caWrapper.joinEmptyFieldsCa(
                                INVALID_FIELD_CA_NAME,
                                context.getPackageName(),
                                buyer,
                                biddingUri,
                                Uri.parse(biddingUri + "/daily"),
                                eventLog::writeEvent,
                                calcExpiry(ONE_DAY_EXPIRY));
                    } else {
                        caWrapper.leaveCa(
                                INVALID_FIELD_CA_NAME,
                                context.getPackageName(),
                                buyer,
                                eventLog::writeEvent);
                    }
                });
        // Frequency Capped CA
        binding.freqCapCaSwitch.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    int adCounterKey = 1;
                    // Caps is exceeded after 2 impression events
                    KeyedFrequencyCap keyedFrequencyCapImpression =
                            new KeyedFrequencyCap(adCounterKey, 2, Duration.ofSeconds(10));
                    // Caps is exceeded after 1 click event
                    KeyedFrequencyCap keyedFrequencyCapClick =
                            new KeyedFrequencyCap(adCounterKey, 1, Duration.ofSeconds(10));
                    AdFilters filters =
                            new AdFilters(
                                    new FrequencyCapFilters(
                                            ImmutableList.of(keyedFrequencyCapImpression),
                                            ImmutableList.of(keyedFrequencyCapClick)));
                    if (isChecked) {
                        caWrapper.joinFilteringCa(
                                FREQ_CAP_CA_NAME,
                                buyer,
                                biddingUri,
                                Uri.parse(biddingUri + "/render_" + FREQ_CAP_CA_NAME),
                                Uri.parse(biddingUri + "/daily"),
                                Uri.parse(biddingUri + "/trusted"),
                                eventLog::writeEvent,
                                calcExpiry(ONE_DAY_EXPIRY),
                                filters,
                                ImmutableSet.of(adCounterKey));
                    } else {
                        caWrapper.leaveCa(
                                FREQ_CAP_CA_NAME,
                                context.getPackageName(),
                                buyer,
                                eventLog::writeEvent);
                    }
                });
        // Server Auction CA
        Uri serverAuctionBiddingUri =
                biddingUri.buildUpon().authority(mAuctionServerBuyer.toString()).build();
        AdTechIdentifier buyerServer =
                new AdTechIdentifier(Objects.requireNonNull(serverAuctionBiddingUri.getHost()));
        binding.shoesServerAuctionCaSwitch.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if (isChecked) {
                        if (!isAuctionServerSetupReady("joining auction server CA")) {
                            compoundButton.setChecked(false);
                            return;
                        }
                        caWrapper.joinServerAuctionCa(
                                SHOES_SERVER_AUCTION_CA_NAME,
                                buyerServer,
                                serverAuctionBiddingUri,
                                Uri.parse(
                                        "https://"
                                                + mAuctionServerBuyer
                                                + "/"
                                                + SHOES_SERVER_AUCTION_AD_RENDER_ID),
                                Uri.parse(serverAuctionBiddingUri + "/daily"),
                                Uri.parse(serverAuctionBiddingUri + "/trusted"),
                                eventLog::writeEvent,
                                calcExpiry(ONE_DAY_EXPIRY),
                                SHOES_SERVER_AUCTION_AD_RENDER_ID);
                    } else {
                        caWrapper.leaveCa(
                                SHOES_SERVER_AUCTION_CA_NAME,
                                context.getPackageName(),
                                buyerServer,
                                eventLog::writeEvent);
                    }
                });
        binding.shirtsServerAuctionCaSwitch.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if (isChecked) {
                        if (!isAuctionServerSetupReady("joining auction server CA")) {
                            compoundButton.setChecked(false);
                            return;
                        }
                        caWrapper.joinServerAuctionCa(
                                SHIRTS_SERVER_AUCTION_CA_NAME,
                                buyerServer,
                                serverAuctionBiddingUri,
                                Uri.parse(
                                        "https://"
                                                + mAuctionServerBuyer
                                                + "/"
                                                + SHIRTS_SERVER_AUCTION_AD_RENDER_ID),
                                Uri.parse(serverAuctionBiddingUri + "/daily"),
                                Uri.parse(serverAuctionBiddingUri + "/trusted"),
                                eventLog::writeEvent,
                                calcExpiry(ONE_DAY_EXPIRY),
                                SHIRTS_SERVER_AUCTION_AD_RENDER_ID);
                    } else {
                        caWrapper.leaveCa(
                                SHIRTS_SERVER_AUCTION_CA_NAME,
                                context.getPackageName(),
                                buyerServer,
                                eventLog::writeEvent);
                    }
                });
        // Fetch and Join CA
        String baseUri =
                getIntentOrError("baseUrl", eventLog, MISSING_FIELD_STRING_FORMAT_RESTART_APP);
        binding.fetchAndJoinCaSwitch.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    if (isChecked) {
                        caWrapper.fetchAndJoinCa(
                                Uri.parse(baseUri + "/fetch/ca"),
                                HATS_CA_NAME,
                                Instant.now(),
                                calcExpiry(ONE_DAY_EXPIRY),
                                new AdSelectionSignals("{}"),
                                eventLog::writeEvent);
                    } else {
                        caWrapper.leaveCa(
                                HATS_CA_NAME,
                                context.getPackageName(),
                                new AdTechIdentifier(biddingUri.getHost()),
                                eventLog::writeEvent);
                    }
                });
    }

    private void setupReportImpressionButton(
            AdSelectionWrapper adSelectionWrapper,
            ActivityMainBinding binding,
            EventLogManager eventLog) {
        binding.runReportImpressionButton.setOnClickListener(
                (l) -> {
                    try {
                        String adSelectionIdInput =
                                binding.adSelectionIdImpressionInput.getText().toString();
                        long adSelectionId = Long.parseLong(adSelectionIdInput);
                        adSelectionWrapper.reportImpression(adSelectionId, eventLog::writeEvent);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, String.format("Error while parsing the ad selection id: %s", e));
                        eventLog.writeEvent(
                                "Invalid AdSelectionId. Cannot run report impressions!");
                    }
                });
    }

    @SuppressLint("NewApi")
    private void setupReportClickButton(
            AdSelectionWrapper adSelectionWrapper,
            ActivityMainBinding binding,
            EventLogManager eventLog) {
        binding.runReportClickButton.setOnClickListener(
                (l) -> {
                    try {
                        String adSelectionIdInput =
                                binding.adSelectionIdClickInput.getText().toString();
                        String interactionData = binding.interactionDataInput.getText().toString();
                        String clickInteraction = "click";
                        long adSelectionId = Long.parseLong(adSelectionIdInput);
                        adSelectionWrapper.reportEvent(
                                adSelectionId,
                                clickInteraction,
                                interactionData,
                                ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER
                                        | ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER,
                                eventLog::writeEvent);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, String.format("Error while parsing the ad selection id: %s", e));
                        eventLog.writeEvent("Invalid AdSelectionId. Cannot run report event!");
                    }
                });
    }

    @OptIn(markerClass = Ext8OptIn.class)
    private void setupUpdateClickHistogramButton(
            AdSelectionWrapper adSelectionWrapper,
            ActivityMainBinding binding,
            EventLogManager eventLog) {
        binding.runUpdateAdCounterHistogramButton.setOnClickListener(
                (l) -> {
                    try {
                        String adSelectionIdInput =
                                binding.adSelectionIdHistogramInput.getText().toString();
                        long adSelectionId = Long.parseLong(adSelectionIdInput);
                        adSelectionWrapper.updateAdCounterHistogram(
                                adSelectionId,
                                FrequencyCapFilters.AD_EVENT_TYPE_CLICK,
                                eventLog::writeEvent);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, String.format("Error while parsing the ad selection id: %s", e));
                        eventLog.writeEvent(
                                "Invalid AdSelectionId. Cannot run update ad counter histogram!");
                    }
                });
    }

    private void useOverrides(
            EventLogManager eventLog,
            AdSelectionWrapper adSelectionWrapper,
            CustomAudienceWrapper customAudienceWrapper,
            String decisionLogicJs,
            String biddingLogicJs,
            AdSelectionSignals trustedScoringSignals,
            AdSelectionSignals trustedBiddingSignals,
            Uri biddingUri,
            Context context) {
        Objects.requireNonNull(biddingUri.getHost());
        adSelectionWrapper.overrideAdSelection(
                eventLog::writeEvent,
                decisionLogicJs,
                android.adservices.common.AdSelectionSignals.fromString(
                        trustedBiddingSignals.toString()));
        customAudienceWrapper.addCAOverride(
                SHOES_CA_NAME,
                context.getPackageName(),
                android.adservices.common.AdTechIdentifier.fromString(biddingUri.getHost()),
                biddingLogicJs,
                android.adservices.common.AdSelectionSignals.fromString(
                        trustedBiddingSignals.toString()),
                eventLog::writeEvent);
        customAudienceWrapper.addCAOverride(
                SHIRTS_CA_NAME,
                context.getPackageName(),
                android.adservices.common.AdTechIdentifier.fromString(biddingUri.getHost()),
                biddingLogicJs,
                android.adservices.common.AdSelectionSignals.fromString(
                        trustedBiddingSignals.toString()),
                eventLog::writeEvent);
        customAudienceWrapper.addCAOverride(
                SHORT_EXPIRING_CA_NAME,
                context.getPackageName(),
                android.adservices.common.AdTechIdentifier.fromString(biddingUri.getHost()),
                biddingLogicJs,
                android.adservices.common.AdSelectionSignals.fromString(
                        trustedBiddingSignals.toString()),
                eventLog::writeEvent);
        customAudienceWrapper.addCAOverride(
                INVALID_FIELD_CA_NAME,
                context.getPackageName(),
                android.adservices.common.AdTechIdentifier.fromString(biddingUri.getHost()),
                biddingLogicJs,
                android.adservices.common.AdSelectionSignals.fromString(
                        trustedBiddingSignals.toString()),
                eventLog::writeEvent);
        customAudienceWrapper.addCAOverride(
                FREQ_CAP_CA_NAME,
                context.getPackageName(),
                android.adservices.common.AdTechIdentifier.fromString(biddingUri.getHost()),
                biddingLogicJs,
                android.adservices.common.AdSelectionSignals.fromString(
                        trustedBiddingSignals.toString()),
                eventLog::writeEvent);
    }

    private void resetOverrides(
            EventLogManager eventLog,
            AdSelectionWrapper adSelectionWrapper,
            CustomAudienceWrapper customAudienceWrapper) {
        adSelectionWrapper.resetAdSelectionOverrides(eventLog::writeEvent);
        customAudienceWrapper.resetCAOverrides(eventLog::writeEvent);
    }

    /** Replaces the override URI in the .js files with an actual reporting URI */
    private String replaceReportingURI(String js, String reportingUri) {
        return js.replace("https://reporting.example.com", reportingUri);
    }

    /**
     * Gets a given intent extra or notifies the user that it is missing
     *
     * @param intent The intent to get
     * @param eventLog An eventlog to write the error to
     * @param errorMessage the error message to write to the eventlog
     * @return The string value of the intent specified.
     */
    private String getIntentOrError(String intent, EventLogManager eventLog, String errorMessage) {
        String toReturn = getIntent().getStringExtra(intent);
        if (toReturn == null) {
            String message = String.format(errorMessage, intent);
            eventLog.writeEvent(message);
            throw new RuntimeException(message);
        }
        return toReturn;
    }

    /**
     * Gets a given intent extra or returns the given default value
     *
     * @param intent The intent to get
     * @param defaultValue The default value to return if intent doesn't exist
     */
    private String getIntentOrDefault(String intent, String defaultValue) {
        String toReturn = getIntent().getStringExtra(intent);
        if (toReturn == null) {
            String message =
                    String.format("No value for %s, defaulting to %s", intent, defaultValue);
            Log.w(TAG, message);
            toReturn = defaultValue;
        }
        return toReturn;
    }

    private String getIntentOrNull(String intent) {
        String value = getIntent().getStringExtra(intent);
        if (Objects.isNull(value)) {
            Log.e(
                    TAG,
                    String.format(
                            "Intent %s is not available, returning null. This can cause problems later.",
                            intent));
        }
        return value;
    }

    /**
     * Resolve the host of the given URI and returns an {@code AdTechIdentifier} object
     *
     * @param uri Uri to resolve
     */
    private AdTechIdentifier resolveAdTechIdentifier(Uri uri) {
        if (uri == Uri.EMPTY) {
            return new AdTechIdentifier("");
        }
        return new AdTechIdentifier(Objects.requireNonNull(uri.getHost()));
    }

    private Uri auctionServerSellerSfeUriOrEmpty() {
        String sfeUriString;
        if ((sfeUriString = getIntentOrNull(AUCTION_SERVER_SELLER_SFE_URL_INTENT)) != null) {
            return Uri.parse(sfeUriString);
        } else {
            return Uri.EMPTY;
        }
    }

    private AdTechIdentifier auctionServerSellerOrEmpty() {
        String auctionServerSeller;
        if ((auctionServerSeller = getIntentOrNull(AUCTION_SERVER_SELLER_INTENT)) != null) {
            return new AdTechIdentifier(auctionServerSeller);
        } else {
            return new AdTechIdentifier("");
        }
    }

    private AdTechIdentifier auctionServerBuyerOrEmpty() {
        String auctionServerBuyer;
        if ((auctionServerBuyer = getIntentOrNull(AUCTION_SERVER_BUYER_INTENT)) != null) {
            return new AdTechIdentifier(auctionServerBuyer);
        } else {
            return new AdTechIdentifier("");
        }
    }

    private boolean isAuctionServerSetupReady(String reason) {
        boolean isReady = true;
        StringBuilder errMsgBuilder =
                new StringBuilder(String.format("For %s, you have to pass intent(s): ", reason));
        if (mAuctionServerSellerSfeUri == Uri.EMPTY) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_SELLER_SFE_URL_INTENT));
            isReady = false;
        }
        if (mAuctionServerSeller.toString().isEmpty()) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_SELLER_INTENT));
            isReady = false;
        }
        if (mAuctionServerBuyer.toString().isEmpty()) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_BUYER_INTENT));
            isReady = false;
        }
        if (!isReady) {
            errMsgBuilder.append("\nwhen starting the app.");
            eventLog.writeEvent(errMsgBuilder.toString());
        }
        return isReady;
    }

    /** Reads a file into a string, to be used to read the .js files into a string. */
    private String assetFileToString(String location) throws IOException {
        return new BufferedReader(
                        new InputStreamReader(getApplicationContext().getAssets().open(location)))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    /**
     * Returns a point in time at {@code duration} in the future from now
     *
     * @param duration Amount of time in the future
     */
    private Instant calcExpiry(Duration duration) {
        return Instant.now().plus(duration);
    }
}
