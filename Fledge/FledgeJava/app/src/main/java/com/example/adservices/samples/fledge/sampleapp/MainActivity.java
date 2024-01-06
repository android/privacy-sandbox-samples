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

import static android.adservices.adselection.ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER;
import static android.adservices.adselection.ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER;

import android.adservices.adselection.AdWithBid;
import android.adservices.adselection.BuyersDecisionLogic;
import android.adservices.adselection.SignedContextualAds;
import android.adservices.adselection.DecisionLogic;
import android.adservices.common.AdData;
import android.adservices.common.AdFilters;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.adservices.common.AppInstallFilters;
import android.adservices.common.FrequencyCapFilters;
import android.adservices.common.KeyedFrequencyCap;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.example.adservices.samples.fledge.sampleapp.databinding.ActivityMainBinding;
import com.example.adservices.samples.fledge.signature.SignatureHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * Android application activity for testing FLEDGE API
 */
@RequiresApi(api = 34)
public class MainActivity extends AppCompatActivity {
    private static final String SHOES_SERVER_AUCTION_CA_NAME = "shoes_server";
    private static final String SHIRTS_SERVER_AUCTION_CA_NAME = "shirts_server";
    // Server Auction Custom Audience Render Id
    private static final String SHOES_SERVER_AUCTION_AD_RENDER_ID = "1";
    private static final String SHIRTS_SERVER_AUCTION_AD_RENDER_ID = "2";

    // Log tag
    public static final String TAG = "FledgeSample";

    // JSON string objects that will be used during ad selection
    private static final AdSelectionSignals TRUSTED_SCORING_SIGNALS = AdSelectionSignals.fromString(
        "{\n"
            + "\t\"render_uri_1\": \"signals_for_1\",\n"
            + "\t\"render_uri_2\": \"signals_for_2\"\n"
            + "}");
    private static final AdSelectionSignals TRUSTED_BIDDING_SIGNALS = AdSelectionSignals.fromString(
        "{\n"
            + "\t\"example\": \"example\",\n"
            + "\t\"valid\": \"Also valid\",\n"
            + "\t\"list\": \"list\",\n"
            + "\t\"of\": \"of\",\n"
            + "\t\"keys\": \"trusted bidding signal Values\"\n"
            + "}");

    // JS files
    private static final String BIDDING_LOGIC_V2_FILE = "BiddingLogicV2.js";
    private static final String BIDDING_LOGIC_V3_FILE = "BiddingLogicV3.js";
    private static final String DECISION_LOGIC_FILE = "DecisionLogic.js";
    private static final String CONTEXTUAL_LOGIC_FILE = "ContextualLogic.js";

    // The custom audience names
    private static final String SHOES_CA_NAME = "shoes";
    private static final String SHIRTS_CA_NAME = "shirts";
    private static final String HATS_CA_NAME = "hats";
    private static final String SHORT_EXPIRING_CA_NAME = "short_expiring";
    private static final String INVALID_FIELD_CA_NAME = "invalid_fields";
    private static final String APP_INSTALL_CA_NAME = "app_install";
    private static final String FREQ_CAP_CA_NAME = "freq_cap";

    // Contextual Ad data
    private static final long NO_FILTER_BID = 20;
    private static final long APP_INSTALL_BID = 25;
    private static final String NO_FILTER_RENDER_SUFFIX = "/contextual_ad";
    private static final String APP_INSTALL_RENDER_SUFFIX = "/app_install_contextual_ad";

    // Intents
    private static final String BASE_URL_INTENT = "baseUrl";
    private static final String AUCTION_SERVER_SELLER_SFE_URL_INTENT = "auctionServerSellerSfeUrl";
    private static final String AUCTION_SERVER_SELLER_INTENT = "auctionServerSeller";
    private static final String AUCTION_SERVER_BUYER_INTENT = "auctionServerBuyer";

    public static final String AD_SELECTION_PREBUILT_SCHEMA = "ad-selection-prebuilt";
    public static final String AD_SELECTION_USE_CASE = "ad-selection";
    public static final String AD_SELECTION_HIGHEST_BID_WINS = "highest-bid-wins";

    // Expiry durations
    private static final Duration ONE_DAY_EXPIRY = Duration.ofDays(1);
    private static final Duration THIRTY_SECONDS_EXPIRY = Duration.ofSeconds(30);

    // Executor to be used for API calls
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    // String to inform user a field in missing
    private static final String MISSING_FIELD_STRING_FORMAT_RESTART_APP = "ERROR: %s is missing, " +
        "restart the activity using the directions in the README. The app will not be usable " +
        "until this is done.";

    private String mBaseUriString;
    private Uri mBiddingLogicUri;
    private Uri mScoringLogicUri;
    private Uri mTrustedDataUri;
    private Uri mContextualLogicUri;
    private Uri mInvalidContextualLogicUri;
    private AdTechIdentifier mBuyer;
    private AdTechIdentifier mSeller;
    private Uri mAuctionServerSellerSfeUri;
    private AdTechIdentifier mAuctionServerSeller;
    private AdTechIdentifier mAuctionServerBuyer;

    private AdSelectionWrapper adWrapper;
    private CustomAudienceWrapper caWrapper;
    private String overrideDecisionJS;
    private String overrideBiddingJsV2;
    private String overrideBiddingJsV3;
    private String overrideContextualJs;
    private Context context;
    private ActivityMainBinding binding;
    private EventLogManager eventLog;
    private SignedContextualAds signedContextualAds;
    private SignedContextualAds unsignedContextualAds;


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

        // Same for override and non-overrides flows
        mBaseUriString = getIntentOrError(BASE_URL_INTENT, eventLog, MISSING_FIELD_STRING_FORMAT_RESTART_APP);
        mBiddingLogicUri = Uri.parse(mBaseUriString + "/bidding");
        mScoringLogicUri = Uri.parse(mBaseUriString + "/scoring");
        mTrustedDataUri = Uri.parse(mBiddingLogicUri + "/trusted");
        mContextualLogicUri = Uri.parse(mBaseUriString + "/contextual");
        mInvalidContextualLogicUri = Uri.parse("https://www.buyerwithinvalidsignature.com/unsigned_contextual");
        mBuyer = resolveAdTechIdentifier(mBiddingLogicUri);
        mSeller = resolveAdTechIdentifier(mScoringLogicUri);

        mAuctionServerSellerSfeUri = auctionServerSellerSfeUriOrEmpty();
        mAuctionServerSeller = auctionServerSellerOrEmpty();
        mAuctionServerBuyer = auctionServerBuyerOrEmpty();

        try {
            // Get override reporting URI
            String reportingUriString = mBaseUriString;

            // Replace override URIs in JS
            overrideDecisionJS = replaceReportingURI(assetFileToString(DECISION_LOGIC_FILE),
                    reportingUriString);
            overrideBiddingJsV2 = replaceReportingURI(assetFileToString(BIDDING_LOGIC_V2_FILE),
                    reportingUriString);
            overrideBiddingJsV3 = replaceReportingURI(assetFileToString(BIDDING_LOGIC_V3_FILE),
                    reportingUriString);
            overrideContextualJs = replaceReportingURI(assetFileToString(CONTEXTUAL_LOGIC_FILE),
                reportingUriString);

            signedContextualAds = new SignedContextualAds.Builder()
                .setBuyer(AdTechIdentifier.fromString(mBiddingLogicUri.getHost()))
                .setDecisionLogicUri(mContextualLogicUri)
                .setAdsWithBid(new ArrayList<>())
                // Signature to be set right before ad selection run
                .setSignature(new byte[] {})
                .build();
            unsignedContextualAds = new SignedContextualAds.Builder()
                .setBuyer(AdTechIdentifier.fromString(mInvalidContextualLogicUri.getHost()))
                .setDecisionLogicUri(mInvalidContextualLogicUri)
                .setAdsWithBid(new ArrayList<>())
                .setSignature(new byte[] {})
                .build();

            // Set up the contextual ads switches
            setupContextualAdsSwitches(mBaseUriString, eventLog);

            // Setup overrides since they are on by default
            setupOverrideFlow(2L);

            // Set up Report Impression button and text box
            setupReportImpressionButton(adWrapper, binding, eventLog);

            // Setup Report Click button and text boxes
            setupReportClickButton(adWrapper, binding, eventLog);
            // Set up Update Ad Counter Histogram button and text box
            setupUpdateClickHistogramButton(adWrapper, binding, eventLog);

            // Set up Override Switch
            binding.overrideSelect.setOnCheckedChangeListener(this::toggleOverrideSwitch);

            // Set up Prebuilt Switch
            binding.usePrebuiltForScoring.setOnCheckedChangeListener(this::togglePrebuiltUriForScoring);

            // Set up No buyers ad selection
            binding.noBuyers.setOnCheckedChangeListener((ignored1, ignored2) -> toggleNoBuyersCheckbox());

            // Set up Auction Server ad selection
            binding.auctionServer.setOnCheckedChangeListener((ignored1, ignored2) -> toggleAuctionServerCheckbox());

            // Set package names
            setupPackageNames();
        } catch (Exception e) {
            Log.e(TAG, "Error when setting up app", e);
        }
    }

    private void togglePrebuiltUriForScoring(CompoundButton compoundButton, boolean isPrebuiltForScoringEnabled) {
        if (!isPrebuiltForScoringEnabled) {
            mScoringLogicUri = Uri.parse(mBaseUriString + "/scoring");
            setAdSelectionWrapper();
            Log.i(TAG, "prebuilt is turned off for scoring. ScoringUri: " + mScoringLogicUri);
            eventLog.writeEvent("Prebuilt is turned off for scoring!");
            return;
        }

        if (!binding.overrideOff.isChecked()) {
            compoundButton.setChecked(false);
            Log.i(TAG, "Cant apply prebuilt URI when overrides are on.");
            eventLog.writeEvent("Cant use prebuilt when override is on!");
            return;
        }

        mScoringLogicUri = getPrebuiltUriForScoringPickHighest();
        Log.i(TAG, "Switched to using prebuilt uri for scoring that picks the highest as "
            + "winner. Scoring Uri: " + mScoringLogicUri);
        setAdSelectionWrapper();
        eventLog.writeEvent("Set prebuilt uri for scoring: pick highest bid");
    }

    private void toggleNoBuyersCheckbox() {
        Log.i(TAG, "No Buyers check toggled to " + binding.noBuyers.isChecked());
        setAdSelectionWrapper();
        eventLog.writeEvent("No Buyers check toggled to " + binding.noBuyers.isChecked());
    }

    private void toggleAuctionServerCheckbox() {
        if (mAuctionServerSellerSfeUri == Uri.EMPTY) {
            eventLog.writeEvent(String.format("To enable server auctions you have to pass %s intent when starting the app.",
                AUCTION_SERVER_SELLER_SFE_URL_INTENT));
            binding.auctionServer.setChecked(false);
            return;
        }
        if (mAuctionServerSeller.toString().isEmpty()) {
            eventLog.writeEvent(String.format("To enable server auctions you have to pass %s intent when starting the app.",
                AUCTION_SERVER_SELLER_INTENT));
            binding.auctionServer.setChecked(false);
            return;
        }
        if (mAuctionServerBuyer.toString().isEmpty()) {
            eventLog.writeEvent(String.format("To enable server auctions you have to pass %s intent when starting the app.",
                AUCTION_SERVER_BUYER_INTENT));
            binding.auctionServer.setChecked(false);
            return;
        }
        Log.i(TAG, "Auction Server check toggled to " + binding.auctionServer.isChecked());
        setAdSelectionWrapper();
        eventLog.writeEvent("Auction Server check toggled to " + binding.auctionServer.isChecked());
    }

    private void setupPackageNames() {
        binding.contextualAiDataInput.setText(context.getPackageName());
        binding.caAiDataInput.setText(context.getPackageName());
    }

    private void setAdSelectionWrapper() {
        List<AdTechIdentifier> buyers = (binding.noBuyers.isChecked()) ?
            Collections.emptyList() : Collections.singletonList(mBuyer);
        signedContextualAds = SignatureHelper.signContextualAdsOrReturn(signedContextualAds);
        // don't sign the invalid contextual ad bundle
        List<SignedContextualAds> contextualAdsList = new ArrayList<>();
        if (binding.contextualAdSwitch.isChecked() || binding.contextualAdAiSwitch.isChecked()) {
            contextualAdsList.add(signedContextualAds);
        }
        if (binding.contextualAdInvalidSignature.isChecked()) {
            contextualAdsList.add(unsignedContextualAds);
        }
        adWrapper = new AdSelectionWrapper(
            buyers, mSeller, mScoringLogicUri, mTrustedDataUri, contextualAdsList, binding.usePrebuiltForScoring.isChecked(), Uri.parse(mBaseUriString + "/scoring"), context, EXECUTOR);

        if (binding.auctionServer.isChecked()) {
            binding.runAdsButton.setOnClickListener(
                v -> adWrapper.runAdSelectionOnAuctionServer(mAuctionServerSellerSfeUri, mAuctionServerSeller, mAuctionServerBuyer, eventLog::writeEvent, binding.adSpace::setText));
        } else {
            binding.runAdsButton.setOnClickListener(
                v -> adWrapper.runAdSelection(eventLog::writeEvent, binding.adSpace::setText));
        }
    }

    private void toggleOverrideSwitch(RadioGroup buttonView, int checkedId) {
        if (binding.overrideOff.isChecked()) {
            try {
                // Set with new scoring uri
                adWrapper.resetAdSelectionConfig(Collections.singletonList(mBuyer), mSeller, mScoringLogicUri, mTrustedDataUri,
                    signedContextualAds);

                // Reset CA switches as they rely on different biddingLogicUri
                setupCASwitches(caWrapper, eventLog, binding, mBiddingLogicUri, context);

                resetOverrides(eventLog, adWrapper, caWrapper);
            } catch (Exception e) {
                binding.overrideV2BiddingLogic.setChecked(true);
                Log.e(TAG, "Cannot disable overrides because mock URLs not provided", e);
            }
        } else if (binding.overrideV2BiddingLogic.isChecked()) {
            setupOverrideFlow(2L);
        } else if (binding.overrideV3BiddingLogic.isChecked()) {
            setupOverrideFlow(3L);
        }
    }

    private void setupOverrideFlow(long biddingLogicVersion) {
        if (binding.usePrebuiltForScoring.isChecked()) {
            binding.usePrebuiltForScoring.setChecked(false);
        }

        setAdSelectionWrapper();

        // Uncheck prebuilt checkbox because prebuilt is not available when overrides are on yet
        binding.usePrebuiltForScoring.setChecked(false);

        // Set up Custom Audience Wrapper(CAs)
        caWrapper = new CustomAudienceWrapper(context, EXECUTOR);

        // Set up the app install switch
        setupAppInstallSwitch(mBiddingLogicUri, eventLog);

        // Set up CA switches
        setupCASwitches(caWrapper, eventLog, binding, mBiddingLogicUri, context);
        String biddingLogicJs = biddingLogicVersion == 2 ? overrideBiddingJsV2 : overrideBiddingJsV3;

        // Set up remote overrides by default
        useOverrides(eventLog, adWrapper, caWrapper, overrideDecisionJS, biddingLogicJs, overrideContextualJs, biddingLogicVersion, TRUSTED_SCORING_SIGNALS, TRUSTED_BIDDING_SIGNALS, mBiddingLogicUri, context);
    }

    private void setupContextualAdsSwitches(String baseUri, EventLogManager eventLog) {
        binding.contextualAdSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            AdData noFilterAd = new AdData.Builder()
                .setMetadata(new JSONObject().toString())
                .setRenderUri(Uri.parse(baseUri + NO_FILTER_RENDER_SUFFIX))
                .build();
            AdWithBid noFilterAdWithBid = new AdWithBid(noFilterAd, NO_FILTER_BID);
            if (isChecked && !signedContextualAds.getAdsWithBid().contains(noFilterAdWithBid)) {
                eventLog.writeEvent("Will insert a normal contextual ad into all auctions");
                signedContextualAds.getAdsWithBid().add(noFilterAdWithBid);
            } else {
                eventLog.writeEvent("Will stop inserting a normal contextual ad into all auctions");
                signedContextualAds.getAdsWithBid().remove(noFilterAdWithBid);
            }

            setAdSelectionWrapper();
        });
        binding.contextualAdAiSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            AdData appInstallAd = new AdData.Builder()
                .setMetadata(new JSONObject().toString())
                .setRenderUri(Uri.parse(baseUri + APP_INSTALL_RENDER_SUFFIX))
                .setAdFilters(getAppInstallFilterForPackage(binding.contextualAiDataInput.getText().toString()))
                .build();
            AdWithBid appInstallAdWithBid = new AdWithBid(appInstallAd, APP_INSTALL_BID);
            if (isChecked && !signedContextualAds.getAdsWithBid().contains(appInstallAdWithBid)) {
                eventLog.writeEvent("Will insert an app install contextual ad into all auctions");
                signedContextualAds.getAdsWithBid().add(appInstallAdWithBid);
                binding.contextualAiDataInput.setEnabled(false);
            } else {
                eventLog.writeEvent("Will stop inserting an app install contextual ad into all auctions");
                signedContextualAds.getAdsWithBid().remove(appInstallAdWithBid);
                binding.contextualAiDataInput.setEnabled(true);
            }

            setAdSelectionWrapper();
        });
    }
    private void setupAppInstallSwitch(Uri biddingUri, EventLogManager eventLog) {
        binding.appInstallSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                adWrapper.setAppInstallAdvertisers(Collections.singleton(AdTechIdentifier.fromString(biddingUri.getHost())),
                    eventLog::writeEvent);
            } else {
                adWrapper.setAppInstallAdvertisers(Collections.EMPTY_SET, eventLog::writeEvent);
            }
        });
    }

    private void setupCASwitches(CustomAudienceWrapper caWrapper, EventLogManager eventLog, ActivityMainBinding binding, Uri biddingUri, Context context) {
        AdTechIdentifier buyer = AdTechIdentifier.fromString(biddingUri.getHost());
        // Shoes
        binding.shoesCaSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                Log.i(TAG, "joining SHOES CA with buyer: " + buyer);
                caWrapper.joinCa(SHOES_CA_NAME, buyer, biddingUri,
                    Uri.parse(biddingUri + "/render_" + SHOES_CA_NAME), Uri.parse(biddingUri + "/daily"), Uri.parse(biddingUri + "/trusted"),
                    eventLog::writeEvent, calcExpiry(ONE_DAY_EXPIRY));
            } else {
                Log.i(TAG, "leaving SHOES CA with buyer: " + buyer);
                caWrapper.leaveCa(SHOES_CA_NAME, context.getPackageName(), buyer, eventLog::writeEvent);
            }
        });
        // Shirts
        binding.shirtsCaSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                Log.i(TAG, "joining SHIRTS CA with buyer: " + buyer);
                caWrapper.joinCa(SHIRTS_CA_NAME, buyer, biddingUri,
                    Uri.parse(biddingUri + "/render_" + SHIRTS_CA_NAME), Uri.parse(biddingUri + "/daily"), Uri.parse(biddingUri + "/trusted"),
                    eventLog::writeEvent, calcExpiry(ONE_DAY_EXPIRY));
            } else {
                Log.i(TAG, "leaving SHIRTS CA with buyer: " + buyer);
                caWrapper.leaveCa(SHIRTS_CA_NAME, context.getPackageName(), buyer, eventLog::writeEvent);
            }
        });
        // Server Auction CA
        binding.shoesServerAuctionCaSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                Uri serverAuctionBiddingUri = biddingUri.buildUpon().authority(mAuctionServerBuyer.toString()).build();
                caWrapper.joinServerAuctionCa(SHOES_SERVER_AUCTION_CA_NAME, AdTechIdentifier.fromString(serverAuctionBiddingUri.getHost()), serverAuctionBiddingUri,
                    Uri.parse(serverAuctionBiddingUri + "/render_" + SHOES_SERVER_AUCTION_CA_NAME), Uri.parse(serverAuctionBiddingUri + "/daily"), Uri.parse(serverAuctionBiddingUri + "/trusted"),
                    eventLog::writeEvent, calcExpiry(ONE_DAY_EXPIRY), SHOES_SERVER_AUCTION_AD_RENDER_ID );
            } else {
                caWrapper.leaveCa(SHOES_SERVER_AUCTION_CA_NAME, context.getPackageName(), AdTechIdentifier.fromString(biddingUri.getHost()), eventLog::writeEvent);
            }
        });
        binding.shirtsServerAuctionCaSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                Uri serverAuctionBiddingUri = biddingUri.buildUpon().authority(mAuctionServerBuyer.toString()).build();
                caWrapper.joinServerAuctionCa(SHIRTS_SERVER_AUCTION_CA_NAME, AdTechIdentifier.fromString(serverAuctionBiddingUri.getHost()), serverAuctionBiddingUri,
                    Uri.parse(serverAuctionBiddingUri + "/render_" + SHIRTS_SERVER_AUCTION_CA_NAME), Uri.parse(serverAuctionBiddingUri + "/daily"), Uri.parse(serverAuctionBiddingUri + "/trusted"),
                    eventLog::writeEvent, calcExpiry(ONE_DAY_EXPIRY), SHIRTS_SERVER_AUCTION_AD_RENDER_ID );
            } else {
                caWrapper.leaveCa(SHIRTS_SERVER_AUCTION_CA_NAME, context.getPackageName(), AdTechIdentifier.fromString(biddingUri.getHost()), eventLog::writeEvent);
            }
        });
        // Short expiring CA
        binding.shortExpiryCaSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                caWrapper.joinCa(SHORT_EXPIRING_CA_NAME, buyer, biddingUri,
                    Uri.parse(biddingUri + "/render_" + SHORT_EXPIRING_CA_NAME), Uri.parse(biddingUri + "/daily"), Uri.parse(biddingUri + "/trusted"),
                    eventLog::writeEvent, calcExpiry(THIRTY_SECONDS_EXPIRY));
            } else {
                caWrapper.leaveCa(SHORT_EXPIRING_CA_NAME, context.getPackageName(), buyer, eventLog::writeEvent);
            }
        });
        // Invalid fields CA
        binding.invalidFieldsCaSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                caWrapper.joinEmptyFieldsCa(INVALID_FIELD_CA_NAME, buyer, biddingUri,
                    Uri.parse(biddingUri + "/daily"), eventLog::writeEvent, calcExpiry(ONE_DAY_EXPIRY));
            } else {
                caWrapper.leaveCa(INVALID_FIELD_CA_NAME, context.getPackageName(), buyer, eventLog::writeEvent);
            }
        });
        // App Install CA
        binding.appInstallCaSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                caWrapper.joinCa(APP_INSTALL_CA_NAME, buyer, biddingUri,
                    Uri.parse(biddingUri + "/render_" + APP_INSTALL_CA_NAME), Uri.parse(biddingUri + "/daily"), Uri.parse(biddingUri + "/trusted"),
                    eventLog::writeEvent, calcExpiry(ONE_DAY_EXPIRY), getAppInstallFilterForPackage(binding.caAiDataInput.getText().toString()));
                binding.caAiDataInput.setEnabled(false);
            } else {
                caWrapper.leaveCa(APP_INSTALL_CA_NAME, context.getPackageName(), buyer, eventLog::writeEvent);
                binding.caAiDataInput.setEnabled(true);
            }
        });
        // Frequency Capped CA
        binding.freqCapCaSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            int adCounterKey = 1;

            // Caps is exceeded after 2 impression events
            KeyedFrequencyCap keyedFrequencyCapImpression =
                new KeyedFrequencyCap.Builder(adCounterKey, 2, Duration.ofSeconds(10))
                    .build();

            // Caps is exceeded after 1 click event
            KeyedFrequencyCap keyedFrequencyCapClick =
                new KeyedFrequencyCap.Builder(adCounterKey, 1, Duration.ofSeconds(10))
                    .build();

            AdFilters filters = new AdFilters.Builder()
                .setFrequencyCapFilters(new FrequencyCapFilters.Builder()
                    .setKeyedFrequencyCapsForImpressionEvents(ImmutableList.of(keyedFrequencyCapImpression))
                    .setKeyedFrequencyCapsForClickEvents(ImmutableList.of(keyedFrequencyCapClick))
                    .build()
                )
                .build();
            if (isChecked) {
                caWrapper.joinFilteringCa(FREQ_CAP_CA_NAME, buyer, biddingUri,
                    Uri.parse(biddingUri + "/render_" + FREQ_CAP_CA_NAME), Uri.parse(biddingUri + "/daily"), Uri.parse(biddingUri + "/trusted"),
                    eventLog::writeEvent, calcExpiry(ONE_DAY_EXPIRY), filters, ImmutableSet.of(adCounterKey));
            } else {
                caWrapper.leaveCa(FREQ_CAP_CA_NAME, context.getPackageName(), buyer, eventLog::writeEvent);
            }
        });
        // Fetch CA
        binding.fetchAndJoinCaSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                caWrapper.fetchAndJoinCa(
                    Uri.parse(mBaseUriString + "/fetch/ca"),
                    HATS_CA_NAME,
                    Instant.now(),
                    calcExpiry(ONE_DAY_EXPIRY),
                    AdSelectionSignals.EMPTY, eventLog::writeEvent);
            } else {
                caWrapper.leaveCa(HATS_CA_NAME, context.getPackageName(), AdTechIdentifier.fromString(biddingUri.getHost()), eventLog::writeEvent);
            }
        });
    }

    private void setupReportImpressionButton(AdSelectionWrapper adSelectionWrapper, ActivityMainBinding binding, EventLogManager eventLog) {
        binding.runReportImpressionButton.setOnClickListener(
            (l) ->  {
                try {
                    String adSelectionIdInput = binding.adSelectionIdImpressionInput.getText().toString();
                    long adSelectionId = Long.parseLong(adSelectionIdInput);
                    adSelectionWrapper.reportImpression(adSelectionId, eventLog::writeEvent);
                } catch (NumberFormatException e) {
                    Log.e(TAG, String.format("Error while parsing the ad selection id: %s", e));
                    eventLog.writeEvent("Invalid AdSelectionId. Cannot run report impressions!");
                }

            });
    }

    private void setupReportClickButton(AdSelectionWrapper adSelectionWrapper, ActivityMainBinding binding, EventLogManager eventLog) {
        binding.runReportClickButton.setOnClickListener(
            (l) ->  {
                try {
                    String adSelectionIdInput = binding.adSelectionIdClickInput.getText().toString();
                    String interactionData = binding.interactionDataInput.getText().toString();
                    String clickInteraction = "click";
                    long adSelectionId = Long.parseLong(adSelectionIdInput);
                    adSelectionWrapper.reportInteraction(adSelectionId, clickInteraction, interactionData, FLAG_REPORTING_DESTINATION_SELLER | FLAG_REPORTING_DESTINATION_BUYER, eventLog::writeEvent);
                } catch (NumberFormatException e) {
                    Log.e(TAG, String.format("Error while parsing the ad selection id: %s", e));
                    eventLog.writeEvent("Invalid AdSelectionId. Cannot run report interaction!");
                }

            });
    }

    private void setupUpdateClickHistogramButton(AdSelectionWrapper adSelectionWrapper, ActivityMainBinding binding, EventLogManager eventLog) {
        binding.runUpdateAdCounterHistogramButton.setOnClickListener(
            (l) ->  {
                try {
                    String adSelectionIdInput = binding.adSelectionIdHistogramInput.getText().toString();
                    long adSelectionId = Long.parseLong(adSelectionIdInput);
                    adSelectionWrapper.updateAdCounterHistogram(adSelectionId, FrequencyCapFilters.AD_EVENT_TYPE_CLICK, eventLog::writeEvent);
                } catch (NumberFormatException e) {
                    Log.e(TAG, String.format("Error while parsing the ad selection id: %s", e));
                    eventLog.writeEvent("Invalid AdSelectionId. Cannot run update ad counter histogram!");
                }

            });
    }

    private void useOverrides(EventLogManager eventLog,
                              AdSelectionWrapper adSelectionWrapper,
                              CustomAudienceWrapper customAudienceWrapper,
                              String decisionLogicJs,
                              String biddingLogicJs,
                              String contextualLogicJs,
                              long biddingLogicJsVersion,
                              AdSelectionSignals trustedScoringSignals,
                              AdSelectionSignals trustedBiddingSignals,
                              Uri biddingUri, Context context) {
        BuyersDecisionLogic buyersDecisionLogic = new BuyersDecisionLogic(Collections.singletonMap(mBuyer,
            new DecisionLogic(contextualLogicJs)));
        adSelectionWrapper.overrideAdSelection(eventLog::writeEvent, decisionLogicJs, trustedScoringSignals, buyersDecisionLogic);
        customAudienceWrapper.addCAOverride(SHOES_CA_NAME, AdTechIdentifier.fromString(biddingUri.getHost()), biddingLogicJs, biddingLogicJsVersion, trustedBiddingSignals, eventLog::writeEvent);
        customAudienceWrapper.addCAOverride(SHIRTS_CA_NAME, AdTechIdentifier.fromString(biddingUri.getHost()), biddingLogicJs, biddingLogicJsVersion, trustedBiddingSignals, eventLog::writeEvent);
        customAudienceWrapper.addCAOverride(SHORT_EXPIRING_CA_NAME, AdTechIdentifier.fromString(biddingUri.getHost()), biddingLogicJs, biddingLogicJsVersion, trustedBiddingSignals, eventLog::writeEvent);
        customAudienceWrapper.addCAOverride(INVALID_FIELD_CA_NAME, AdTechIdentifier.fromString(biddingUri.getHost()), biddingLogicJs, biddingLogicJsVersion, trustedBiddingSignals, eventLog::writeEvent);
        customAudienceWrapper.addCAOverride(APP_INSTALL_CA_NAME, AdTechIdentifier.fromString(biddingUri.getHost()), biddingLogicJs, biddingLogicJsVersion, trustedBiddingSignals, eventLog::writeEvent);
        customAudienceWrapper.addCAOverride(FREQ_CAP_CA_NAME, AdTechIdentifier.fromString(biddingUri.getHost()), biddingLogicJs, biddingLogicJsVersion, trustedBiddingSignals, eventLog::writeEvent);
    }

    private void resetOverrides(EventLogManager eventLog, AdSelectionWrapper adSelectionWrapper, CustomAudienceWrapper customAudienceWrapper) {
        adSelectionWrapper.resetAdSelectionOverrides(eventLog::writeEvent);
        customAudienceWrapper.resetCAOverrides(eventLog::writeEvent);
    }

    /**
     * Replaces the override URI in the .js files with an actual reporting URI
     */
    private String replaceReportingURI(String js, String reportingUri) {
        return js.replace("https://reporting.example.com", reportingUri);
    }

    /**
     * Gets a given intent extra or notifies the user that it is missing
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
     * Resolve the host of the given URI and returns an {@code AdTechIdentifier} object
     * @param uri Uri to resolve
     */
    private AdTechIdentifier resolveAdTechIdentifier(Uri uri) {
        if (uri == Uri.EMPTY) {
            return AdTechIdentifier.fromString("");
        }
        return AdTechIdentifier.fromString(uri.getHost());
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
            return AdTechIdentifier.fromString(auctionServerSeller);
        } else {
            return AdTechIdentifier.fromString("");
        }
    }

    private AdTechIdentifier auctionServerBuyerOrEmpty() {
        String auctionServerBuyer;
        if ((auctionServerBuyer = getIntentOrNull(AUCTION_SERVER_BUYER_INTENT)) != null) {
            return AdTechIdentifier.fromString(auctionServerBuyer);
        } else {
            return AdTechIdentifier.fromString("");
        }
    }

    private Uri getPrebuiltUriForScoringPickHighest() {
        String paramKey = "reportingUrl";
        String paramValue = mBaseUriString + "/reporting";
        return Uri.parse(
                String.format(
                    "%s://%s/%s/?%s=%s",
                    AD_SELECTION_PREBUILT_SCHEMA,
                    AD_SELECTION_USE_CASE,
                    AD_SELECTION_HIGHEST_BID_WINS,
                    paramKey,
                    paramValue));
    }

    /**
     * Reads a file into a string, to be used to read the .js files into a string.
     */
    private String assetFileToString(String location) throws IOException {
        return new BufferedReader(new InputStreamReader(getApplicationContext().getAssets().open(location)))
            .lines().collect(Collectors.joining("\n"));
    }

    /**
     * Returns a point in time at {@code duration} in the future from now
     * @param duration Amount of time in the future
     */
    private Instant calcExpiry(Duration duration) {
        return Instant.now().plus(duration);
    }

    private AdFilters getAppInstallFilterForPackage(String packageName) {
        return new AdFilters.Builder()
            .setAppInstallFilters(new AppInstallFilters.Builder()
                .setPackageNames(Collections.singleton(packageName))
                .build())
            .build();
    }

    private String getIntentOrNull(String intent) {
        String value = getIntent().getStringExtra(intent);
        if (Objects.isNull(value)) {
            Log.e(TAG, String.format(
                "Intent %s is not available, returning null. This can cause problems later.",
                intent));
        }
        return value;
    }
}