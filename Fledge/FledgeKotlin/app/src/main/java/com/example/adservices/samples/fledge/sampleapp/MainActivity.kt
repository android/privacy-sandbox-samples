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

import android.adservices.adselection.ReportEventRequest
import android.adservices.common.AdSelectionSignals
import android.adservices.common.AdTechIdentifier
import android.adservices.common.FrequencyCapFilters
import android.adservices.customaudience.CustomAudienceManager
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Consumer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adservices.samples.fledge.sampleapp.databinding.ActivityMainBinding
import com.example.adservices.samples.fledge.sdkExtensionsHelpers.VersionCompatUtil.isTestableVersion
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
@RequiresApi(api = 34)
class MainActivity : AppCompatActivity() {

    // Intents
    private val BASE_URL_INTENT = "baseUrl"
    private val AUCTION_SERVER_SELLER_SFE_URL_INTENT = "auctionServerSellerSfeUrl"
    private val AUCTION_SERVER_SELLER_INTENT = "auctionServerSeller"
    private val AUCTION_SERVER_BUYER_INTENT = "auctionServerBuyer"

    private var adWrapper: AdSelectionWrapper? = null
    private var caWrapper: CustomAudienceWrapper? = null
    private var context: Context? = null
    private var binding: ActivityMainBinding? = null
    private var eventLog: EventLogManager? = null
    private var mConfig: ConfigUris? = null

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

        mConfig = ConfigUris(
            Uri.parse(
                getIntentOrError(
                    BASE_URL_INTENT, eventLog!!,
                    MISSING_FIELD_STRING_FORMAT_RESTART_APP
                )
            ),
            auctionServerSellerSfeUriOrEmpty(),
            auctionServerSellerOrEmpty(),
            auctionServerBuyerOrEmpty()
        )

        try {
            checkAdServicesEnabledForSdkExtension()

            setAdSelectionWrapper()
            eventLog!!.writeEvent("Auction Server set to " + binding!!.auctionServer.isChecked)

            // Setup report impressions button
            setupReportImpressionButton(adWrapper!!, binding!!, eventLog!!)

            // Setup report click button
            setupReportClickButton(adWrapper!!, binding!!, eventLog!!)

            // Set up Update Ad Counter Histogram button and text box
            setupUpdateClickHistogramButton(adWrapper!!, binding!!, eventLog!!)

            // Setup all CA switches
            setupCASwitches(eventLog!!, binding!!, context!!);

            if (mConfig!!.isMaybeServerAuction) {
                isAuctionServerSetupReady(mConfig!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error when setting up app", e)
        }
    }

    private fun getIntentOrNull(intent: String): String? {
        val value: String? = getIntent().getStringExtra(intent)
        if (value == null) {
            Log.e(
                TAG, String.format(
                    "Intent %s is not available, returning null. This can cause problems later.",
                    intent
                )
            )
        }
        return value
    }

    private fun setAdSelectionWrapper() {
        val buyers: List<AdTechIdentifier> = listOf(mConfig!!.buyer)
        adWrapper = AdSelectionWrapper(
            buyers,
            mConfig!!.seller,
            Uri.parse(mConfig!!.baseUri.toString() + "/scoring"),
            Uri.parse(mConfig!!.baseUri.toString() + "/bidding/trusted"),
            context!!,
            EXECUTOR
        )
        binding!!.auctionServer.isChecked = mConfig!!.isMaybeServerAuction
        if (binding!!.auctionServer.isChecked) {
            binding!!.runAdsButton.setOnClickListener {
                eventLog!!.flush()
                adWrapper!!.runAdSelectionOnAuctionServer(
                    mConfig!!.auctionServerSellerSfeUri,
                    mConfig!!.auctionServerSeller,
                    mConfig!!.auctionServerBuyer,
                    { event: String? -> eventLog!!.writeEvent(event!!) },
                    binding!!.adSpace::setText
                )
            }
        } else {
            binding!!.runAdsButton.setOnClickListener {
                eventLog!!.flush()
                adWrapper!!.runAdSelection(
                    { event: String? -> eventLog!!.writeEvent(event!!) },
                    binding!!.adSpace::setText,
                    { adSelectionId: String ->
                        binding!!.adSelectionIdClickInput.setText(adSelectionId)
                        binding!!.adSelectionIdImpressionInput.setText(adSelectionId)
                        binding!!.adSelectionIdHistogramInput.setText(adSelectionId)
                    })
            }
        }
    }

    private fun auctionServerSellerSfeUriOrEmpty(): Uri {
        val sfeUriString = getIntentOrNull(AUCTION_SERVER_SELLER_SFE_URL_INTENT)
        return if (sfeUriString != null) {
            Uri.parse(sfeUriString)
        } else {
            Uri.EMPTY
        }
    }

    private fun auctionServerSellerOrEmpty(): AdTechIdentifier {
        val auctionServerSeller = getIntentOrNull(AUCTION_SERVER_SELLER_INTENT)
        return if (auctionServerSeller != null) {
            AdTechIdentifier.fromString(auctionServerSeller)
        } else {
            AdTechIdentifier.fromString("")
        }
    }

    private fun auctionServerBuyerOrEmpty(): AdTechIdentifier {
        val auctionServerBuyer = getIntentOrNull(AUCTION_SERVER_BUYER_INTENT)
        return if (auctionServerBuyer != null) {
            AdTechIdentifier.fromString(auctionServerBuyer)
        } else {
            AdTechIdentifier.fromString("")
        }
    }

    private fun isAuctionServerSetupReady(config: ConfigUris) {
        var isReady = true
        val errMsgBuilder = StringBuilder("For %s, you have to pass intent(s): ")
        if (config.auctionServerSellerSfeUri === Uri.EMPTY) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_SELLER_SFE_URL_INTENT))
            isReady = false
        }
        if (config.auctionServerSeller.toString().isEmpty()) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_SELLER_INTENT))
            isReady = false
        }
        if (config.auctionServerBuyer.toString().isEmpty()) {
            errMsgBuilder.append(String.format("\n - %s", AUCTION_SERVER_BUYER_INTENT))
            isReady = false
        }
        if (!isReady) {
            errMsgBuilder.append("\nwhen starting the app.")
            eventLog!!.writeEvent(errMsgBuilder.toString())
        }
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
        eventLog: EventLogManager,
        binding: ActivityMainBinding,
        context: Context,
    ) {
        val toggleProvider =
            ToggleProvider(
                context,
                eventLog,
                CustomAudienceManager.get(context),
                adWrapper!!,
                ConfigUris(
                    mConfig!!.baseUri,
                    mConfig!!.auctionServerSellerSfeUri,
                    mConfig!!.auctionServerSeller,
                    mConfig!!.auctionServerBuyer
                ),
                EXECUTOR
            )
        val toggles: List<Toggle> = toggleProvider.toggles
        binding.optionRecycler.layoutManager = LinearLayoutManager(context)
        binding.optionRecycler.adapter = ToggleAdapter(toggles)
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
            AdTechIdentifier.fromString(biddingUri.host!!),
            biddingLogicJs,
            trustedScoringSignals
        ) { event: String? ->
            eventLog.writeEvent(event!!)
        }
        customAudienceWrapper.addCAOverride(
            SHIRTS_CA_NAME,
            AdTechIdentifier.fromString(biddingUri.host!!),
            biddingLogicJs,
            trustedBiddingSignals
        ) { event: String? ->
            eventLog.writeEvent(event!!)
        }
        customAudienceWrapper.addCAOverride(
            SHORT_EXPIRING_CA_NAME,
            AdTechIdentifier.fromString(biddingUri.host!!),
            biddingLogicJs,
            trustedBiddingSignals
        ) { event: String? ->
            eventLog.writeEvent(event!!)
        }
        customAudienceWrapper.addCAOverride(
            INVALID_FIELD_CA_NAME,
            AdTechIdentifier.fromString(biddingUri.host!!),
            biddingLogicJs,
            trustedBiddingSignals
        ) { event: String? ->
            eventLog.writeEvent(event!!)
        }
        customAudienceWrapper.addCAOverride(
            FREQ_CAP_CA_NAME,
            AdTechIdentifier.fromString(biddingUri.host!!),
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
        return AdTechIdentifier.fromString(uri.host!!)
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

    companion object {
        public const val TAG = "FledgeSample"
    }
}
