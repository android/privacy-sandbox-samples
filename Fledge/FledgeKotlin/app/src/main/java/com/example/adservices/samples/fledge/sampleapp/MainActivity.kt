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

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.CompoundButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Consumer
import androidx.privacysandbox.ads.adservices.adselection.ReportEventRequest
import androidx.privacysandbox.ads.adservices.common.AdFilters
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters
import androidx.privacysandbox.ads.adservices.common.KeyedFrequencyCap
import com.example.adservices.samples.fledge.sampleapp.databinding.ActivityMainBinding
import com.example.adservices.samples.fledge.sdkExtensionsHelpers.VersionCompatUtil.isTestableVersion
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.stream.Collectors

// Log tag
const val TAG = "FledgeSample"

// The names for the shirts and shoes custom audience
private const val SHOES_CA_NAME = "shoes"
private const val SHIRTS_CA_NAME = "shirts"
private const val HATS_CA_NAME = "hats"
private const val SHORT_EXPIRING_CA_NAME = "short_expiring"
private const val INVALID_FIELD_CA_NAME = "invalid_fields"
private const val FREQ_CAP_CA_NAME = "freq_cap"

// Expiry durations
private val ONE_DAY_EXPIRY: Duration = Duration.ofDays(1)
private val THIRTY_SECONDS_EXPIRY: Duration = Duration.ofSeconds(30)

// JS files
private const val BIDDING_LOGIC_FILE = "BiddingLogic.js"
private const val DECISION_LOGIC_FILE = "DecisionLogic.js"

// Executor to be used for API calls
private val EXECUTOR: Executor = Executors.newCachedThreadPool()

// Strings to inform user a field in missing
private const val MISSING_FIELD_STRING_FORMAT_RESTART_APP =
        "ERROR: %s is missing, " +
                "restart the activity using the directions in the README. The app will not be usable " +
                "until this is done."

/** Android application activity for testing FLEDGE API */
@OptIn(ExperimentalFeatures.Ext8OptIn::class)
@RequiresApi(api = 34)
class MainActivity : AppCompatActivity() {

    private val SHOES_SERVER_AUCTION_CA_NAME = "winningCA"
    private val SHIRTS_SERVER_AUCTION_CA_NAME = "shirts_server"

    // Server Auction Custom Audience Render Id
    private val SHOES_SERVER_AUCTION_AD_RENDER_ID = 1
    private val SHIRTS_SERVER_AUCTION_AD_RENDER_ID = 2

    // Intents
    private val BASE_URL_INTENT = "baseUrl"
    private val AUCTION_SERVER_SELLER_SFE_URL_INTENT = "auctionServerSellerSfeUrl"
    private val AUCTION_SERVER_SELLER_INTENT = "auctionServerSeller"
    private val AUCTION_SERVER_BUYER_INTENT = "auctionServerBuyer"

    // JSON string objects that will be used during ad selection
    private val TRUSTED_SCORING_SIGNALS =
            AdSelectionSignals(
                    "{\n" +
                            "\t\"render_uri_1\": \"signals_for_1\",\n" +
                            "\t\"render_uri_2\": \"signals_for_2\"\n" +
                            "}"
            )
    private val TRUSTED_BIDDING_SIGNALS =
            AdSelectionSignals(
                    "{\n" +
                            "\t\"example\": \"example\",\n" +
                            "\t\"valid\": \"Also valid\",\n" +
                            "\t\"list\": \"list\",\n" +
                            "\t\"of\": \"of\",\n" +
                            "\t\"keys\": \"trusted bidding signal Values\"\n" +
                            "}"
            )

    private var mBaseUriString: String? = null
    private var mBiddingLogicUri: Uri = Uri.EMPTY
    private var mScoringLogicUri: Uri = Uri.EMPTY
    private var mTrustedDataUri: Uri = Uri.EMPTY
    private var mBuyer: AdTechIdentifier? = null
    private var mSeller: AdTechIdentifier? = null
    private var mAuctionServerSellerSfeUri: Uri? = null
    private var mAuctionServerSeller: AdTechIdentifier? = null
    private var mAuctionServerBuyer: AdTechIdentifier? = null
    private var adWrapper: AdSelectionWrapper? = null
    private var caWrapper: CustomAudienceWrapper? = null
    private var overrideDecisionJS: String? = null
    private var overrideBiddingJs: String? = null
    private var context: Context? = null
    private var binding: ActivityMainBinding? = null
    private var eventLog: EventLogManager? = null

    /**
     * Does the initial setup for the app. This includes reading the Javascript server URIs from the
     * start intent, creating the ad selection and custom audience wrappers to wrap the APIs, and
     * tying the UI elements to the wrappers so that button clicks trigger the appropriate methods.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = applicationContext
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding!!.root
        setContentView(view)
        eventLog = EventLogManager(binding!!.eventLog)


        mBaseUriString = getIntentOrError(
                BASE_URL_INTENT, eventLog!!, MISSING_FIELD_STRING_FORMAT_RESTART_APP)
        mAuctionServerSellerSfeUri = auctionServerSellerSfeUriOrEmpty()
        mAuctionServerSeller = auctionServerSellerOrEmpty()
        mAuctionServerBuyer = auctionServerBuyerOrEmpty()

        try {
            checkAdServicesEnabledForSdkExtension()

            // Get override reporting URI
            val reportingUriString: String = mBaseUriString!!
            // Replace override URIs in JS
            overrideDecisionJS =
                    replaceReportingURI(assetFileToString(DECISION_LOGIC_FILE), reportingUriString)
            overrideBiddingJs =
                    replaceReportingURI(assetFileToString(BIDDING_LOGIC_FILE), reportingUriString)

            // Setup overrides since they are on by default
            setupOverrideFlow()

            // Setup report impressions button
            setupReportImpressionButton(adWrapper!!, binding!!, eventLog!!)

            // Setup report click button
            setupReportClickButton(adWrapper!!, binding!!, eventLog!!)

            // Set up Update Ad Counter Histogram button and text box
            setupUpdateClickHistogramButton(adWrapper!!, binding!!, eventLog!!)

            // Set up Override Switch
            binding!!.overrideSwitch.setOnCheckedChangeListener(this::toggleOverrideSwitch)

            // Set up Auction Server ad selection
            binding!!.auctionServer
                    .setOnCheckedChangeListener { ignored1, ignored2 -> toggleAuctionServerCheckbox() }
        } catch (e: Exception) {
            Log.e(TAG, "Error when setting up app", e)
        }
    }

    private fun toggleOverrideSwitch(
            buttonView: CompoundButton,
            isChecked: Boolean,
    ) {
        if (isChecked) {
            setupOverrideFlow()
        } else {
            try {
                val baseUri =
                        getIntentOrError("baseUrl", eventLog!!, MISSING_FIELD_STRING_FORMAT_RESTART_APP)

                mBiddingLogicUri = Uri.parse("$baseUri/bidding")
                mScoringLogicUri = Uri.parse("$baseUri/scoring")
                mTrustedDataUri = Uri.parse("$mBiddingLogicUri/trusted")
                mBuyer = resolveAdTechIdentifier(mBiddingLogicUri)
                mSeller = resolveAdTechIdentifier(mScoringLogicUri)

                // Set with new scoring uri
                adWrapper!!.resetAdSelectionConfig(
                        listOf(mBuyer!!),
                        mSeller!!,
                        mScoringLogicUri,
                        mTrustedDataUri
                )

                // Reset CA switches as they rely on different biddingLogicUri
                setupCASwitches(caWrapper!!, eventLog!!, binding!!, mBiddingLogicUri, context!!)
                resetOverrides(eventLog!!, adWrapper!!, caWrapper!!)
            } catch (e: java.lang.Exception) {
                binding!!.overrideSwitch.isChecked = true
                Log.e(TAG, "Cannot disable overrides because mock URLs not provided", e)
            }
        }
    }

    private fun setupOverrideFlow() {
        // Set override URIS since overrides are on by default
        val overrideUriBase =
                getIntentOrError("baseUrl", eventLog!!, MISSING_FIELD_STRING_FORMAT_RESTART_APP)

        mBiddingLogicUri = Uri.parse("$overrideUriBase/bidding")
        mScoringLogicUri = Uri.parse("$overrideUriBase/scoring")
        mTrustedDataUri = Uri.parse("$mBiddingLogicUri/trusted")

        mBuyer = resolveAdTechIdentifier(mBiddingLogicUri)
        mSeller = resolveAdTechIdentifier(mScoringLogicUri)

        // Set up ad selection
        setAdSelectionWrapper()

        // Set up Custom Audience Wrapper
        caWrapper = CustomAudienceWrapper(EXECUTOR, context!!)

        // Set up CA buttons
        setupCASwitches(caWrapper!!, eventLog!!, binding!!, mBiddingLogicUri, context!!)

        // Setup report impressions button
        setupReportImpressionButton(adWrapper!!, binding!!, eventLog!!)

        // Setup remote overrides by default
        useOverrides(
                eventLog!!,
                adWrapper!!,
                caWrapper!!,
                overrideDecisionJS!!,
                overrideBiddingJs!!,
                TRUSTED_SCORING_SIGNALS,
                TRUSTED_BIDDING_SIGNALS,
                mBiddingLogicUri,
                context!!
        )
    }

    private fun getIntentOrNull(intent: String): String? {
        val value: String? = getIntent().getStringExtra(intent)
        if (value == null) {
            Log.e(TAG, String.format(
                    "Intent %s is not available, returning null. This can cause problems later.",
                    intent))
        }
        return value
    }

    private fun toggleAuctionServerCheckbox() {
        if (!isAuctionServerSetupReady("Using auction servers")) {
            binding!!.auctionServer.setChecked(false)
            return
        }
        Log.i(TAG, "Auction Server check toggled to " + binding!!.auctionServer.isChecked)
        setAdSelectionWrapper()
        eventLog!!.writeEvent("Auction Server check toggled to "
                + binding!!.auctionServer.isChecked)
    }

    private fun setAdSelectionWrapper() {
        val buyers: List<AdTechIdentifier> = listOf<AdTechIdentifier>(mBuyer!!)
        adWrapper = AdSelectionWrapper(
                buyers,
                mSeller!!,
                mScoringLogicUri,
                mTrustedDataUri,
                context!!,
                EXECUTOR)
        if (binding!!.auctionServer.isChecked) {
            binding!!.runAdsButton.setOnClickListener { v ->
                eventLog!!.flush()
                adWrapper!!.runAdSelectionOnAuctionServer(
                        mAuctionServerSellerSfeUri!!,
                        mAuctionServerSeller!!,
                        mAuctionServerBuyer!!,
                        Consumer<String> { event: String? -> eventLog!!.writeEvent(event!!) },
                        binding!!.adSpace::setText)
            }
        } else {
            binding!!.runAdsButton.setOnClickListener { v ->
                eventLog!!.flush()
                adWrapper!!.runAdSelection(
                        Consumer<String> { event: String? -> eventLog!!.writeEvent(event!!) },
                        binding!!.adSpace::setText)
            }
        }
    }

    private fun auctionServerSellerSfeUriOrEmpty(): Uri {
        var sfeUriString = getIntentOrNull(AUCTION_SERVER_SELLER_SFE_URL_INTENT)
        if (sfeUriString != null) {
            return Uri.parse(sfeUriString)
        } else {
            return Uri.EMPTY
        }
    }

    private fun auctionServerSellerOrEmpty(): AdTechIdentifier {
        var auctionServerSeller = getIntentOrNull(AUCTION_SERVER_SELLER_INTENT)
        if (auctionServerSeller != null) {
            return AdTechIdentifier(auctionServerSeller)
        } else {
            return AdTechIdentifier("")
        }
    }

    private fun auctionServerBuyerOrEmpty(): AdTechIdentifier {
        var auctionServerBuyer = getIntentOrNull(AUCTION_SERVER_BUYER_INTENT)
        if (auctionServerBuyer != null) {
            return AdTechIdentifier(auctionServerBuyer)
        } else {
            return AdTechIdentifier("")
        }
    }

    private fun isAuctionServerSetupReady(reason: String): Boolean {
        var isReady = true
        val errMsgBuilder = StringBuilder(String.format("For %s, you have to pass intent(s): ", reason))
        if (mAuctionServerSellerSfeUri === Uri.EMPTY) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_SELLER_SFE_URL_INTENT))
            isReady = false
        }
        if (mAuctionServerSeller.toString().isEmpty()) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_SELLER_INTENT))
            isReady = false
        }
        if (mAuctionServerBuyer.toString().isEmpty()) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_BUYER_INTENT))
            isReady = false
        }
        if (!isReady) {
            errMsgBuilder.append("\nwhen starting the app.")
            eventLog!!.writeEvent(errMsgBuilder.toString())
        }
        return isReady
    }


    /**
     * Gets a given intent extra or notifies the user that it is missing
     *
     * @param intent The intent to get
     * @param eventLog An eventlog to write the error to
     * @param errorMessage the error message to write to the eventlog
     * @return The string value of the intent specified.
     */
    private fun getIntentOrError(
            intent: String,
            eventLog: EventLogManager,
            errorMessage: String,
    ): String {
        val toReturn = getIntent().getStringExtra(intent)
        if (toReturn == null) {
            val message = String.format(errorMessage, intent)
            eventLog.writeEvent(message)
            throw RuntimeException(message)
        }
        return toReturn
    }

    @SuppressLint("NewApi")
    private fun setupCASwitches(
            caWrapper: CustomAudienceWrapper,
            eventLog: EventLogManager,
            binding: ActivityMainBinding,
            biddingUri: Uri,
            context: Context,
    ) {
        val buyer: AdTechIdentifier = AdTechIdentifier(biddingUri.host!!)
        // Shoes
        binding.shoesCaSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                caWrapper.joinCa(
                        SHOES_CA_NAME,
                        context.packageName,
                        buyer,
                        biddingUri,
                        Uri.parse("$biddingUri/render_$SHOES_CA_NAME"),
                        Uri.parse("$biddingUri/daily"),
                        Uri.parse("$biddingUri/trusted"),
                        eventLog::writeEvent,
                        calcExpiry(ONE_DAY_EXPIRY)
                )
            } else {
                caWrapper.leaveCa(
                        SHOES_CA_NAME,
                        context.packageName,
                        buyer,
                        eventLog::writeEvent
                )
            }
        }

        // Shirt
        binding.shirtsCaSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                caWrapper.joinCa(
                        SHIRTS_CA_NAME,
                        context.packageName,
                        buyer,
                        biddingUri,
                        Uri.parse("$biddingUri/render_$SHOES_CA_NAME"),
                        Uri.parse("$biddingUri/daily"),
                        Uri.parse("$biddingUri/trusted"),
                        eventLog::writeEvent,
                        calcExpiry(ONE_DAY_EXPIRY)
                )
            } else {
                caWrapper.leaveCa(
                        SHIRTS_CA_NAME,
                        context.packageName,
                        buyer,
                        eventLog::writeEvent
                )
            }
        }

        // Short expiring CA
        binding.shortExpiryCaSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                caWrapper.joinCa(
                        SHORT_EXPIRING_CA_NAME,
                        context.packageName,
                        buyer,
                        biddingUri,
                        Uri.parse("$biddingUri/render_$SHOES_CA_NAME"),
                        Uri.parse("$biddingUri/daily"),
                        Uri.parse("$biddingUri/trusted"),
                        eventLog::writeEvent,
                        calcExpiry(THIRTY_SECONDS_EXPIRY)
                )
            } else {
                caWrapper.leaveCa(
                        SHORT_EXPIRING_CA_NAME,
                        context.packageName,
                        buyer,
                        eventLog::writeEvent
                )
            }
        }

        // Invalid field CA
        binding.invalidFieldsCaSwitch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            if (isChecked) {
                caWrapper.joinEmptyFieldCa(
                        INVALID_FIELD_CA_NAME,
                        context.packageName,
                        buyer,
                        biddingUri,
                        Uri.parse("$biddingUri/render_$SHOES_CA_NAME"),
                        eventLog::writeEvent,
                        calcExpiry(THIRTY_SECONDS_EXPIRY)
                )
            } else {
                caWrapper.leaveCa(
                        SHORT_EXPIRING_CA_NAME,
                        context.packageName,
                        buyer,
                        eventLog::writeEvent
                )
            }
        }

        // Frequency Capped CA
        binding.freqCapCaSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isTestableVersion(8, 9)) {
                eventLog.writeEvent("Unsupported SDK Extension: Frequency cap CA requires 8 for T+ or 9 for S-, skipping")
                return@setOnCheckedChangeListener
            }

            val adCounterKey = 1

            // Caps is exceeded after 2 impression events
            val keyedFrequencyCapImpression =
                    KeyedFrequencyCap(adCounterKey, 2, Duration.ofSeconds(10))

            // Caps is exceeded after 1 click event
            val keyedFrequencyCapClick =
                    KeyedFrequencyCap(adCounterKey, 1, Duration.ofSeconds(10))

            val filters =
                    AdFilters(
                            FrequencyCapFilters(
                                    emptyList(),
                                    ImmutableList.of(keyedFrequencyCapImpression),
                                    emptyList(),
                                    ImmutableList.of(keyedFrequencyCapClick)))

            if (isChecked) {
                caWrapper.joinFilteringCa(
                        FREQ_CAP_CA_NAME,
                        buyer,
                        biddingUri,
                        Uri.parse("$biddingUri/render_$FREQ_CAP_CA_NAME"),
                        Uri.parse("$biddingUri/daily"),
                        Uri.parse("$biddingUri/trusted"),
                        { event: String -> eventLog.writeEvent(event) },
                        calcExpiry(ONE_DAY_EXPIRY),
                        filters,
                        ImmutableSet.of(adCounterKey)
                )
            } else {
                caWrapper.leaveCa(
                        FREQ_CAP_CA_NAME,
                        context.packageName,
                        buyer,
                        eventLog::writeEvent
                )
            }
        }

        // Fetch CA
        val baseUri = getIntentOrError("baseUrl", eventLog, MISSING_FIELD_STRING_FORMAT_RESTART_APP)
        binding.fetchAndJoinCaSwitch.setOnCheckedChangeListener { compoundButton, isChecked: Boolean ->
            if (isTestableVersion(10, 10)) {
                eventLog.writeEvent(
                        "Unsupported SDK Extension: The fetchAndJoinCustomAudience API requires 10,"
                                + " skipping"
                )
                Log.w(
                        TAG,
                        "Unsupported SDK Extension: The fetchAndJoinCustomAudience API requires 10,"
                                + " skipping"
                )
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                caWrapper.fetchAndJoinCa(
                        Uri.parse("$baseUri/fetch/ca"),
                        HATS_CA_NAME,
                        Instant.now(),
                        calcExpiry(ONE_DAY_EXPIRY),
                        AdSelectionSignals("{}")
                ) { event: String? ->
                    eventLog.writeEvent(event!!)
                }
            } else {
                caWrapper.leaveCa(
                        HATS_CA_NAME,
                        context.packageName,
                        AdTechIdentifier(biddingUri.host!!)
                ) { event: String? ->
                    eventLog.writeEvent(event!!)
                }
            }
        }

        // Server Auction CA
        val serverAuctionBiddingUri: Uri =
                biddingUri.buildUpon().authority(mAuctionServerBuyer.toString()).build()
        val serverBuyer: AdTechIdentifier =
                AdTechIdentifier(serverAuctionBiddingUri.host!!)
        binding.shoesServerAuctionCaSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                if (!isAuctionServerSetupReady("joining auction server CA")) {
                    compoundButton.setChecked(false)
                    return@setOnCheckedChangeListener
                }
                caWrapper.joinServerAuctionCa(
                        SHOES_SERVER_AUCTION_CA_NAME,
                        serverBuyer,
                        serverAuctionBiddingUri,
                        Uri.parse("https://$mAuctionServerBuyer/$SHOES_SERVER_AUCTION_AD_RENDER_ID"),
                        Uri.parse("$serverAuctionBiddingUri/daily"),
                        Uri.parse("$serverAuctionBiddingUri/trusted"),
                        { event: String? -> eventLog.writeEvent(event!!) },
                        calcExpiry(ONE_DAY_EXPIRY),
                        SHOES_SERVER_AUCTION_AD_RENDER_ID)
            } else {
                caWrapper.leaveCa(
                        SHOES_SERVER_AUCTION_CA_NAME,
                        context.packageName,
                        serverBuyer) { event: String? -> eventLog.writeEvent(event!!) }
            }
        }

        binding.shirtsServerAuctionCaSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                if (!isAuctionServerSetupReady("joining auction server CA")) {
                    compoundButton.setChecked(false)
                    return@setOnCheckedChangeListener
                }
                caWrapper.joinServerAuctionCa(
                        SHIRTS_SERVER_AUCTION_CA_NAME,
                        serverBuyer,
                        serverAuctionBiddingUri,
                        Uri.parse("https://$mAuctionServerBuyer/$SHIRTS_SERVER_AUCTION_AD_RENDER_ID"),
                        Uri.parse("$serverAuctionBiddingUri/daily"),
                        Uri.parse("$serverAuctionBiddingUri/trusted"),
                        { event: String? -> eventLog.writeEvent(event!!) },
                        calcExpiry(ONE_DAY_EXPIRY),
                        SHIRTS_SERVER_AUCTION_AD_RENDER_ID)
            } else {
                caWrapper.leaveCa(
                        SHIRTS_SERVER_AUCTION_CA_NAME,
                        context.packageName,
                        serverBuyer) { event: String? -> eventLog.writeEvent(event!!) }
            }
        }

    }

    private fun setupReportImpressionButton(
            adSelectionWrapper: AdSelectionWrapper,
            binding: ActivityMainBinding,
            eventLog: EventLogManager,
    ) {
        binding.runReportImpressionButton.setOnClickListener {
            try {
                val adSelectionIdInput = binding.adSelectionIdImpressionInput.text.toString()
                val adSelectionId = adSelectionIdInput.toLong()
                adSelectionWrapper.reportImpression(adSelectionId) { event: String? ->
                    eventLog.writeEvent(event!!)
                }
            } catch (e: NumberFormatException) {
                Log.e(TAG, String.format("Error while parsing the ad selection id: %s", e))
                eventLog.writeEvent("Invalid AdSelectionId. Cannot run report impressions!")
            }
        }
    }

    @SuppressLint("NewApi")
    private fun setupReportClickButton(
            adSelectionWrapper: AdSelectionWrapper,
            binding: ActivityMainBinding,
            eventLog: EventLogManager,
    ) {
        binding.runReportClickButton.setOnClickListener { l ->
            try {
                val adSelectionIdInput = binding.adSelectionIdClickInput.text.toString()
                val interactionData = binding.interactionDataInput.text.toString()
                val clickInteraction = "click"
                val adSelectionId = adSelectionIdInput.toLong()
                adSelectionWrapper.reportEvent(
                        adSelectionId,
                        clickInteraction,
                        interactionData,
                        ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER or
                                ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER
                ) { event: String? ->
                    eventLog.writeEvent(event!!)
                }
            } catch (e: java.lang.NumberFormatException) {
                Log.e(TAG, String.format("Error while parsing the ad selection id: %s", e))
                eventLog.writeEvent("Invalid AdSelectionId. Cannot run report event!")
            }
        }
    }

    @SuppressLint("NewApi")
    private fun setupUpdateClickHistogramButton(
            adSelectionWrapper: AdSelectionWrapper,
            binding: ActivityMainBinding,
            eventLog: EventLogManager,
    ) {
        binding.runUpdateAdCounterHistogramButton.setOnClickListener {
            try {
                val adSelectionIdInput = binding.adSelectionIdHistogramInput.text.toString()
                val adSelectionId = adSelectionIdInput.toLong()
                adSelectionWrapper.updateAdCounterHistogram(
                        adSelectionId,
                        FrequencyCapFilters.AD_EVENT_TYPE_CLICK
                ) { event: String ->
                    eventLog.writeEvent(event)
                }
            } catch (e: java.lang.NumberFormatException) {
                Log.e(TAG, String.format("Error while parsing the ad selection id: %s", e))
                eventLog.writeEvent("Invalid AdSelectionId. Cannot run update ad counter histogram!")
            }
        }
    }

    private fun useOverrides(
            eventLog: EventLogManager,
            adSelectionWrapper: AdSelectionWrapper,
            customAudienceWrapper: CustomAudienceWrapper,
            decisionLogicJs: String,
            biddingLogicJs: String,
            trustedScoringSignals: AdSelectionSignals,
            trustedBiddingSignals: AdSelectionSignals,
            biddingUri: Uri,
            context: Context,
    ) {
        adSelectionWrapper.overrideAdSelection(
                { event: String? -> eventLog.writeEvent(event!!) },
                decisionLogicJs,
                trustedScoringSignals
        )
        customAudienceWrapper.addCAOverride(
                SHOES_CA_NAME,
                context.packageName,
                AdTechIdentifier(biddingUri.host!!),
                biddingLogicJs,
                trustedScoringSignals
        ) { event: String? ->
            eventLog.writeEvent(event!!)
        }
        customAudienceWrapper.addCAOverride(
                SHIRTS_CA_NAME,
                context.packageName,
                AdTechIdentifier(biddingUri.host!!),
                biddingLogicJs,
                trustedBiddingSignals
        ) { event: String? ->
            eventLog.writeEvent(event!!)
        }
        customAudienceWrapper.addCAOverride(
                SHORT_EXPIRING_CA_NAME,
                context.packageName,
                AdTechIdentifier(biddingUri.host!!),
                biddingLogicJs,
                trustedBiddingSignals
        ) { event: String? ->
            eventLog.writeEvent(event!!)
        }
        customAudienceWrapper.addCAOverride(
                INVALID_FIELD_CA_NAME,
                context.packageName,
                AdTechIdentifier(biddingUri.host!!),
                biddingLogicJs,
                trustedBiddingSignals
        ) { event: String? ->
            eventLog.writeEvent(event!!)
        }
        customAudienceWrapper.addCAOverride(
                FREQ_CAP_CA_NAME,
                context.packageName,
                AdTechIdentifier(biddingUri.host!!),
                biddingLogicJs,
                trustedBiddingSignals
        ) { event: String? ->
            eventLog.writeEvent(event!!)
        }
    }

    private fun resetOverrides(
            eventLog: EventLogManager,
            adSelectionWrapper: AdSelectionWrapper,
            customAudienceWrapper: CustomAudienceWrapper,
    ) {
        adSelectionWrapper.resetAdSelectionOverrides { event: String? -> eventLog.writeEvent(event!!) }
        customAudienceWrapper.resetCAOverrides { event: String? -> eventLog.writeEvent(event!!) }
    }

    /**
     * Gets a given intent extra or returns the given default value
     *
     * @param intent The intent to get
     * @param defaultValue The default value to return if intent doesn't exist
     */
    private fun getIntentOrDefault(intent: String, defaultValue: String): String {
        var toReturn = getIntent().getStringExtra(intent)
        if (toReturn == null) {
            val message = String.format("No value for %s, defaulting to %s", intent, defaultValue)
            Log.w(TAG, message)
            toReturn = defaultValue
        }

        return toReturn
    }

    /**
     * Resolve the host of the given URI and returns an {@code AdTechIdentifier} object
     *
     * @param uri Uri to resolve
     */
    private fun resolveAdTechIdentifier(uri: Uri): AdTechIdentifier {
        return AdTechIdentifier(uri.host!!)
    }

    /** Reads a file into a string, to be used to read the .js files into a string. */
    @Throws(IOException::class)
    private fun assetFileToString(location: String): String {
        return BufferedReader(InputStreamReader(applicationContext.assets.open(location)))
                .lines()
                .collect(Collectors.joining("\n"))
    }

    /** Replaces the override URI in the .js files with an actual reporting URI */
    private fun replaceReportingURI(js: String, reportingUri: String): String {
        return js.replace("https://reporting.example.com", reportingUri)
    }

    private fun calcExpiry(duration: Duration): Instant {
        return Instant.now().plus(duration)
    }

    private fun checkAdServicesEnabledForSdkExtension() {
        // 5 instead of 4 as FLEDGE wasn't ready at the same time as the other ad selection APIs.
        if (!isTestableVersion(5, 9)) {
            val message =
                    "Unsupported SDK Extension: AdServices APIs require a minimum of version of 5 for T+ or 9 for S-."
            eventLog!!.writeEvent(message)
            throw RuntimeException(message)
        }
    }
}
