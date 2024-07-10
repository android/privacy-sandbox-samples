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

import android.adservices.adselection.ReportEventRequest;
import android.adservices.common.AdTechIdentifier;
import android.adservices.common.FrequencyCapFilters;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adservices.samples.fledge.sampleapp.databinding.ActivityMainBinding;

import org.json.JSONException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Android application activity for testing FLEDGE API */
@RequiresApi(api = 34)
public class MainActivity extends AppCompatActivity {

    // Log tag
    public static final String TAG = "FledgeSample";

    // Intents
    private static final String BASE_URL_INTENT = "baseUrl";
    private static final String AUCTION_SERVER_SELLER_SFE_URL_INTENT = "auctionServerSellerSfeUrl";
    private static final String AUCTION_SERVER_SELLER_INTENT = "auctionServerSeller";
    private static final String AUCTION_SERVER_BUYER_INTENT = "auctionServerBuyer";

    // Executor to be used for API calls
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    // String to inform user a field in missing
    private static final String MISSING_FIELD_STRING_FORMAT_RESTART_APP =
            "ERROR: %s is missing, restart the activity using the directions in the README. The app"
                    + " will not be usable until this is done.";

    private ConfigUris mConfig;

    private AdSelectionWrapper adWrapper;
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

        mConfig =
                ConfigUris.newBuilder()
                        .setBaseUri(
                                Uri.parse(
                                        getIntentOrError(
                                                BASE_URL_INTENT,
                                                eventLog,
                                                MISSING_FIELD_STRING_FORMAT_RESTART_APP)))
                        .setAuctionServerSellerSfeUri(auctionServerSellerSfeUriOrEmpty())
                        .setAuctionServerSeller(auctionServerSellerOrEmpty())
                        .setAuctionServerBuyer(auctionServerBuyerOrEmpty())
                        .build();

        try {
            setAdSelectionWrapper();
            eventLog.writeEvent("Auction Server set to " + binding.auctionServer.isChecked());

            checkAdServicesEnabledForSdkExtension();

            // Set up Report Impression button and text box
            setupReportImpressionButton(adWrapper, binding, eventLog);

            // Setup Report Click button and text boxes
            setupReportClickButton(adWrapper, binding, eventLog);

            // Set up Update Ad Counter Histogram button and text box
            setupUpdateClickHistogramButton(adWrapper, binding, eventLog);

            // Setup all CA switches.
            setupCASwitches(binding, context);

            if (mConfig.isMaybeServerAuction()) {
                isAuctionServerSetupReady(mConfig);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error when setting up app", e);
        }
    }

    private void setAdSelectionWrapper() {
        List<AdTechIdentifier> buyers = Collections.singletonList(mConfig.getBuyer());

        adWrapper =
                new AdSelectionWrapper(
                        buyers,
                        mConfig.getSeller(),
                        Uri.parse(mConfig.getBaseUri() + "/scoring"),
                        Uri.parse(mConfig.getBaseUri() + "/bidding/trusted"),
                        context,
                        EXECUTOR);

        binding.auctionServer.setChecked(mConfig.isMaybeServerAuction());

        if (mConfig.isMaybeServerAuction()) {
            binding.runAdsButton.setOnClickListener(
                    v ->
                            adWrapper.runAdSelectionOnAuctionServer(
                                    mConfig.getAuctionServerSellerSfeUri(),
                                    mConfig.getAuctionServerSeller(),
                                    mConfig.getAuctionServerBuyer(),
                                    eventLog::writeEvent,
                                    binding.adSpace::setText));
        } else {
            binding.runAdsButton.setOnClickListener(
                    v ->
                            adWrapper.runAdSelection(
                                    eventLog::writeEvent,
                                    binding.adSpace::setText,
                                    adSelectionId -> {
                                        binding.adSelectionIdClickInput.setText(adSelectionId);
                                        binding.adSelectionIdImpressionInput.setText(adSelectionId);
                                        binding.adSelectionIdHistogramInput.setText(adSelectionId);
                                    }));
        }
    }

    private void setupCASwitches(ActivityMainBinding binding, Context context) {
        try {
            CustomAudienceWrapper customAudienceWrapper =
                    new CustomAudienceWrapper(context, EXECUTOR);
            ToggleProvider toggleProvider =
                    new ToggleProvider(
                            getApplicationContext(),
                            eventLog,
                            customAudienceWrapper,
                            adWrapper,
                            mConfig);
            ToggleAdapter adapter = new ToggleAdapter(toggleProvider.getToggles());
            binding.optionRecycler.setLayoutManager(new LinearLayoutManager(context));
            binding.optionRecycler.setAdapter(adapter);
        } catch (JSONException | IOException | RuntimeException e) {
            eventLog.writeEvent("Error! Failed to load custom audience data: " + e.getMessage());
        }
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

    @SuppressLint("NewApi")
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

    private String getIntentOrNull(String intent) {
        String value = getIntent().getStringExtra(intent);
        if (Objects.isNull(value)) {
            Log.e(
                    TAG,
                    String.format(
                            "Intent %s is not available, returning null. This can cause problems"
                                + " later.",
                            intent));
        }
        return value;
    }

    private void checkAdServicesEnabledForSdkExtension() {
        // 5 instead of 4 as FLEDGE wasn't ready at the same time as the other ad selection APIs.
        if (!isTestableVersion(5, 9)) {
            String message =
                    "Unsupported SDK Extension: AdServices APIs require a minimum of version of 5"
                        + " for T+ or 9 for S-.";
            eventLog.writeEvent(message);
            throw new RuntimeException(message);
        }
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

    private void isAuctionServerSetupReady(ConfigUris config) {
        boolean isReady = true;
        StringBuilder errMsgBuilder =
                new StringBuilder("For using auction servers, you have to pass intent(s): ");
        if (config.getAuctionServerSellerSfeUri() == Uri.EMPTY) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_SELLER_SFE_URL_INTENT));
            isReady = false;
        }
        if (config.getAuctionServerSeller().toString().isEmpty()) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_SELLER_INTENT));
            isReady = false;
        }
        if (config.getAuctionServerBuyer().toString().isEmpty()) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_BUYER_INTENT));
            isReady = false;
        }
        if (!isReady) {
            errMsgBuilder.append("\nwhen starting the app.");
            eventLog.writeEvent(errMsgBuilder.toString());
        }
    }
}
