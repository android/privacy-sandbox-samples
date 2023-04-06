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

import android.adservices.adselection.AdWithBid
import android.adservices.adselection.BuyersDecisionLogic
import android.adservices.adselection.ContextualAds
import android.adservices.adselection.DecisionLogic
import android.adservices.adselection.ReportInteractionRequest
import android.adservices.common.AdData
import android.adservices.common.AdFilters
import android.adservices.common.AdSelectionSignals
import android.adservices.common.AdTechIdentifier
import android.adservices.common.AppInstallFilters
import android.adservices.common.FrequencyCapFilters
import android.adservices.common.KeyedFrequencyCap
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.RadioGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.adservices.samples.fledge.sampleapp.databinding.ActivityMainBinding
import com.google.common.collect.ImmutableSet
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.stream.Collectors
import org.json.JSONObject


// Log tag
const val TAG = "FledgeSample"

// The custom audience names
private const val SHOES_CA_NAME = "shoes"
private const val SHIRTS_CA_NAME = "shirts"
private const val SHORT_EXPIRING_CA_NAME = "short_expiring"
private const val INVALID_FIELD_CA_NAME = "invalid_fields"
private const val APP_INSTALL_CA_NAME = "app_install"
private const val FREQ_CAP_CA_NAME = "freq_cap"

// Contextual Ad data
private const val NO_FILTER_BID: Long = 20
private const val APP_INSTALL_BID: Long = 25
private const val NO_FILTER_RENDER_SUFFIX = "/contextual_ad"
private const val APP_INSTALL_RENDER_SUFFIX = "/app_install_contextual_ad"

// Expiry durations
private val ONE_DAY_EXPIRY: Duration = Duration.ofDays(1)
private val THIRTY_SECONDS_EXPIRY: Duration = Duration.ofSeconds(30)

// JS files
private const val BIDDING_LOGIC_FILE_V2 = "BiddingLogicV2.js"
private const val BIDDING_LOGIC_FILE_V3 = "BiddingLogicV3.js"
private const val DECISION_LOGIC_FILE = "DecisionLogic.js"
private const val CONTEXTUAL_LOGIC_FILE = "ContextualLogic.js"

// Executor to be used for API calls
private val EXECUTOR: Executor = Executors.newCachedThreadPool()

// Strings to inform user a field in missing
private const val MISSING_FIELD_STRING_FORMAT_RESTART_APP = "ERROR: %s is missing, " +
  "restart the activity using the directions in the README. The app will not be usable " +
  "until this is done."

/**
 * Android application activity for testing FLEDGE API
 */
@RequiresApi(api = 34)
class MainActivity : AppCompatActivity() {
  // JSON string objects that will be used during ad selection
  private val TRUSTED_SCORING_SIGNALS = AdSelectionSignals.fromString("{\n"
                                                                        + "\t\"render_uri_1\": \"signals_for_1\",\n"
                                                                        + "\t\"render_uri_2\": \"signals_for_2\"\n"
                                                                        + "}")
  private val TRUSTED_BIDDING_SIGNALS = AdSelectionSignals.fromString("{\n"
                                                                              + "\t\"example\": \"example\",\n"
                                                                              + "\t\"valid\": \"Also valid\",\n"
                                                                              + "\t\"list\": \"list\",\n"
                                                                              + "\t\"of\": \"of\",\n"
                                                                              + "\t\"keys\": \"trusted bidding signal Values\"\n"
                                                                              + "}")

  private var mBiddingLogicUri: Uri = Uri.EMPTY
  private var mScoringLogicUri: Uri = Uri.EMPTY
  private var mTrustedDataUri: Uri = Uri.EMPTY
  private var mContextualLogicUri: Uri = Uri.EMPTY
  private var mBuyer: AdTechIdentifier? = null
  private var mSeller: AdTechIdentifier? = null
  private var adWrapper: AdSelectionWrapper? = null
  private var caWrapper: CustomAudienceWrapper? = null
  private var overrideDecisionJS: String? = null
  private var overrideContextualJs: String? = null
  private var overrideBiddingJsV2: String? = null
  private var overrideBiddingJsV3: String? = null
  private var context: Context? = null
  private var binding: ActivityMainBinding? = null
  private var eventLog: EventLogManager? = null
  private var contextualAds: ContextualAds? = null

  /**
   * Does the initial setup for the app. This includes reading the Javascript server URIs from the
   * start intent, creating the ad selection and custom audience wrappers to wrap the APIs, and
   * tying the UI elements to the wrappers so that button clicks trigger the appropriate methods.
   */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState);
    context = applicationContext;
    binding = ActivityMainBinding.inflate(layoutInflater);
    val view = binding!!.root;
    setContentView(view);
    eventLog = EventLogManager(binding!!.eventLog);
    try {
      // Get override reporting URI
      val reportingUriString = getIntentOrError("baseUrl", eventLog!!, MISSING_FIELD_STRING_FORMAT_RESTART_APP)
      // Replace override URIs in JS
      overrideDecisionJS = replaceReportingURI(assetFileToString(DECISION_LOGIC_FILE),
              reportingUriString)
      overrideContextualJs = replaceReportingURI(assetFileToString(CONTEXTUAL_LOGIC_FILE),
              reportingUriString)
      overrideBiddingJsV2 = replaceReportingURI(assetFileToString(BIDDING_LOGIC_FILE_V2),
              reportingUriString)
      overrideBiddingJsV3 = replaceReportingURI(assetFileToString(BIDDING_LOGIC_FILE_V3),
              reportingUriString)

      // Setup overrides since they are on by default
      setupOverrideFlow(2L);

      // Setup report impressions button
      setupReportImpressionButton(adWrapper!!, binding!!, eventLog!!)

      // Set up Update Ad Counter Histogram button and text box
      setupUpdateClickHistogramButton(adWrapper!!, binding!!, eventLog!!)

      // Setup report click button
      setupReportClickButton(adWrapper!!, binding!!, eventLog!!)

      // Set up Override Switch
      binding!!.overrideSelect.setOnCheckedChangeListener(this::toggleOverrideSwitch)

      // Set package names
      setupPackageNames();
    } catch (e: Exception) {
      Log.e(TAG, "Error when setting up app", e)
    }
  }
  private fun setupPackageNames() {
    binding!!.contextualAiDataInput.setText(context!!.packageName)
    binding!!.caAiDataInput.setText(context!!.packageName)
  }


  private fun toggleOverrideSwitch(
    radioGroup: RadioGroup,
    checkedId: Int,
  ) {
    if (binding!!.overrideOff.isChecked) {
      try {
        val baseUri = getIntentOrError("baseUrl",
                                       eventLog!!,
                                       MISSING_FIELD_STRING_FORMAT_RESTART_APP)

        mBiddingLogicUri = Uri.parse("$baseUri/bidding")
        mScoringLogicUri = Uri.parse("$baseUri/scoring")
        mTrustedDataUri = Uri.parse("$mBiddingLogicUri/trusted")
        mContextualLogicUri = Uri.parse("$baseUri/contextual")
        mBuyer = resolveAdTechIdentifier(mBiddingLogicUri)
        mSeller = resolveAdTechIdentifier(mScoringLogicUri)

        contextualAds = ContextualAds.Builder()
          .setBuyer(AdTechIdentifier.fromString(mBiddingLogicUri.host!!))
          .setDecisionLogicUri(mContextualLogicUri)
          .setAdsWithBid(ArrayList())
          .build()

        // Set with new scoring uri
        adWrapper!!.resetAdSelectionConfig(listOf(mBuyer!!),
                                           mSeller!!,
                                           mScoringLogicUri,
                                           mTrustedDataUri,
                                           contextualAds!!)

        // Reset CA switches as they rely on different biddingLogicUri
        setupCASwitches(caWrapper!!, eventLog!!, binding!!, mBiddingLogicUri, context!!)
        resetOverrides(eventLog!!, adWrapper!!, caWrapper!!)

        // Set up the contextual ads switches
        setupContextualAdsSwitches(baseUri, eventLog!!);

      } catch (e: Exception) {
        binding!!.overrideV2BiddingLogic.isChecked = true
        Log.e(TAG, "Cannot disable overrides because mock URLs not provided", e)
      }
    } else if (binding!!.overrideV2BiddingLogic.isChecked) {
      setupOverrideFlow(2L)
    } else if (binding!!.overrideV3BiddingLogic.isChecked) {
      setupOverrideFlow(3L)
    }
  }

  private fun setupOverrideFlow(biddingLogicVersion: Long) {
    // Set override URIS since overrides are on by default
    val overrideUriBase = getIntentOrError("baseUrl",
                                           eventLog!!,
                                           MISSING_FIELD_STRING_FORMAT_RESTART_APP)

    mBiddingLogicUri = Uri.parse("$overrideUriBase/bidding")
    mScoringLogicUri = Uri.parse("$overrideUriBase/scoring")
    mTrustedDataUri = Uri.parse("$mBiddingLogicUri/trusted")
    mContextualLogicUri = Uri.parse("$overrideUriBase/contextual");

    mBuyer = resolveAdTechIdentifier(mBiddingLogicUri)
    mSeller = resolveAdTechIdentifier(mScoringLogicUri)

    // Set up ad selection
    contextualAds = ContextualAds.Builder()
      .setBuyer(AdTechIdentifier.fromString(mBiddingLogicUri.host!!))
      .setDecisionLogicUri(mContextualLogicUri)
      .setAdsWithBid(ArrayList())
      .build()
    adWrapper = AdSelectionWrapper(listOf(mBuyer!!), mSeller!!, mScoringLogicUri, mTrustedDataUri,
                                   contextualAds!!, context!!, EXECUTOR)
    binding!!.runAdsButton.setOnClickListener{ _ -> adWrapper!!.runAdSelection(eventLog!!::writeEvent, binding!!.adSpace::setText)}

    // Set up Custom Audience Wrapper
    caWrapper = CustomAudienceWrapper(EXECUTOR, context!!)

    // Set up the app install switch
    setupAppInstallSwitch(mBiddingLogicUri, eventLog!!)

    // Set up the contextual ads switches
    setupContextualAdsSwitches(overrideUriBase, eventLog!!);

    // Set up CA switches
    setupCASwitches(caWrapper!!, eventLog!!, binding!!, mBiddingLogicUri, context!!)

    // Decide which js file to use.
    val biddingLogicJs = if (biddingLogicVersion == 2L) overrideBiddingJsV2 else overrideBiddingJsV3

    // Setup remote overrides by default
    useOverrides(eventLog!! ,adWrapper!!, caWrapper!!, overrideDecisionJS!!, biddingLogicJs!!, overrideContextualJs!!, biddingLogicVersion, TRUSTED_SCORING_SIGNALS, TRUSTED_BIDDING_SIGNALS, mBiddingLogicUri, context!!);
  }

  private fun setupContextualAdsSwitches(baseUri: String, eventLog: EventLogManager) {
    binding!!.contextualAdSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
      val noFilterAd = AdData.Builder()
        .setMetadata(JSONObject().toString())
        .setRenderUri(Uri.parse(baseUri + NO_FILTER_RENDER_SUFFIX))
        .build()
      val noFilterAdWithBid = AdWithBid(noFilterAd, NO_FILTER_BID.toDouble())
      if (isChecked && !contextualAds!!.adsWithBid.contains(noFilterAdWithBid)) {
        eventLog.writeEvent("Will insert a normal contextual ad into all auctions")
        contextualAds!!.adsWithBid.add(noFilterAdWithBid)
      } else {
        eventLog.writeEvent("Will stop inserting a normal contextual ad into all auctions")
        contextualAds!!.adsWithBid.remove(noFilterAdWithBid)
      }
      adWrapper = AdSelectionWrapper(
        listOf(mBuyer!!),
        mSeller!!, mScoringLogicUri, mTrustedDataUri, contextualAds!!, context!!, EXECUTOR)
    }
    binding!!.contextualAdAiSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
      val appInstallAd = AdData.Builder()
        .setMetadata(JSONObject().toString())
        .setRenderUri(Uri.parse(baseUri + APP_INSTALL_RENDER_SUFFIX))
        .setAdFilters(getAppInstallFilterForPackage(binding!!.contextualAiDataInput.text.toString()))
        .build()
      val appInstallAdWithBid = AdWithBid(appInstallAd, APP_INSTALL_BID.toDouble())
      if (isChecked && !contextualAds!!.adsWithBid.contains(appInstallAdWithBid)) {
        eventLog.writeEvent("Will insert an app install contextual ad into all auctions")
        contextualAds!!.adsWithBid.add(appInstallAdWithBid)
        binding!!.contextualAiDataInput.isEnabled = false;
      } else {
        eventLog.writeEvent("Will stop inserting an app install contextual ad into all auctions")
        contextualAds!!.adsWithBid.remove(appInstallAdWithBid)
        binding!!.contextualAiDataInput.isEnabled = true;
      }
      adWrapper = AdSelectionWrapper(
        listOf(mBuyer!!),
        mSeller!!, mScoringLogicUri, mTrustedDataUri, contextualAds!!, context!!, EXECUTOR)
    }
  }
  /**
   * Gets a given intent extra or notifies the user that it is missing
   * @param intent The intent to get
   * @param eventLog An eventlog to write the error to
   * @param errorMessage the error message to write to the eventlog
   * @return The string value of the intent specified.
   */
  private fun getIntentOrError(intent: String, eventLog: EventLogManager, errorMessage: String): String {
    val toReturn = getIntent().getStringExtra(intent)
    if (toReturn == null) {
      val message = String.format(errorMessage, intent)
      eventLog.writeEvent(message)
      throw RuntimeException(message)
    }
    return toReturn
  }

  private fun setupAppInstallSwitch(biddingUri: Uri, eventLog: EventLogManager) {
    binding!!.appInstallSwitch.setOnCheckedChangeListener {_ , isChecked : Boolean ->
      if (isChecked) {
        adWrapper!!.setAppInstallAdvertisers(setOf(AdTechIdentifier.fromString(biddingUri.host!!)), eventLog::writeEvent)
      } else {
        adWrapper!!.setAppInstallAdvertisers(emptySet(), eventLog::writeEvent)
      }
    }
  }

  private fun setupCASwitches(
    caWrapper: CustomAudienceWrapper,
    eventLog: EventLogManager,
    binding: ActivityMainBinding,
    biddingUri: Uri,
    context: Context,
  ) {
    // Shoes
    binding.shoesCaSwitch.setOnCheckedChangeListener { _ , isChecked : Boolean ->
     if (isChecked) {
       caWrapper.joinCa(SHOES_CA_NAME,
                        AdTechIdentifier.fromString(biddingUri.host!!),
                        biddingUri,
                        Uri.parse(
                          "$biddingUri/render_$SHOES_CA_NAME"),
                        Uri.parse("$biddingUri/daily"),
                        Uri.parse("$biddingUri/trusted"),
                        eventLog::writeEvent,
                        calcExpiry(ONE_DAY_EXPIRY))
     } else {
       caWrapper.leaveCa(SHOES_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), eventLog::writeEvent)
     }
    }

    // Shirt
    binding.shirtsCaSwitch.setOnCheckedChangeListener { _ , isChecked: Boolean ->
      if (isChecked) {
        caWrapper.joinCa(SHIRTS_CA_NAME,
                         AdTechIdentifier.fromString(biddingUri.host!!),
                         biddingUri,
                         Uri.parse(
                           "$biddingUri/render_$SHIRTS_CA_NAME"),
                         Uri.parse("$biddingUri/daily"),
                         Uri.parse("$biddingUri/trusted"),
                         eventLog::writeEvent,
                         calcExpiry(ONE_DAY_EXPIRY))
      } else {
        caWrapper.leaveCa(SHIRTS_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), eventLog::writeEvent)
      }
    }

    // Short expiring CA
    binding.shortExpiryCaSwitch.setOnCheckedChangeListener{ _ , isChecked: Boolean ->
      if (isChecked) {
        caWrapper.joinCa(SHORT_EXPIRING_CA_NAME,
                         AdTechIdentifier.fromString(biddingUri.host!!),
                         biddingUri,
                         Uri.parse(
                           "$biddingUri/render_$SHOES_CA_NAME"),
                         Uri.parse("$biddingUri/daily"),
                         Uri.parse("$biddingUri/trusted"),
                         eventLog::writeEvent,
                         calcExpiry(THIRTY_SECONDS_EXPIRY))
      } else {
        caWrapper.leaveCa(SHORT_EXPIRING_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), eventLog::writeEvent)
      }
    }

    // Invalid field CA
    binding.invalidFieldsCaSwitch.setOnCheckedChangeListener{ _ , isChecked: Boolean ->
      if (isChecked) {
        caWrapper.joinEmptyFieldCa(INVALID_FIELD_CA_NAME,
                                   AdTechIdentifier.fromString(biddingUri.host!!),
                                   biddingUri,
                                   Uri.parse(
                           "$biddingUri/render_$SHOES_CA_NAME"),
                                   eventLog::writeEvent,
                                   calcExpiry(THIRTY_SECONDS_EXPIRY))
      } else {
        caWrapper.leaveCa(SHORT_EXPIRING_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), eventLog::writeEvent)
      }
    }

    // App Install CA
    binding.appInstallCaSwitch.setOnCheckedChangeListener{ _ , isChecked: Boolean ->
      if (isChecked) {
        caWrapper.joinCa(APP_INSTALL_CA_NAME,
                                   AdTechIdentifier.fromString(biddingUri.host!!),
                                   biddingUri,
                                   Uri.parse(
                                     "$biddingUri/render_$APP_INSTALL_CA_NAME"),
                                   Uri.parse("$biddingUri/daily"),
                                   Uri.parse("$biddingUri/trusted"),
                                   eventLog::writeEvent,
                                   calcExpiry(ONE_DAY_EXPIRY),
                                   getAppInstallFilterForPackage(binding.caAiDataInput.getText().toString()))
        binding.caAiDataInput.isEnabled = false;
      } else {
        caWrapper.leaveCa(APP_INSTALL_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), eventLog::writeEvent)
        binding.caAiDataInput.isEnabled = true;
      }
    }

    // Frequency Capped CA
    binding.freqCapCaSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->
      val adCounterKey = "key"

      // Caps is exceeded after 2 impression events
      val keyedFrequencyCapImpression =
        KeyedFrequencyCap.Builder().setAdCounterKey(adCounterKey)
          .setMaxCount(1)
          .setInterval(Duration.ofSeconds(10))
          .build()

      // Caps is exceeded after 1 click event
      val keyedFrequencyCapClick =
        KeyedFrequencyCap.Builder().setAdCounterKey(adCounterKey)
          .setMaxCount(0)
          .setInterval(Duration.ofSeconds(10))
          .build()
      val filters = AdFilters.Builder()
        .setFrequencyCapFilters(FrequencyCapFilters.Builder()
                                  .setKeyedFrequencyCapsForImpressionEvents(ImmutableSet.of(
                                    keyedFrequencyCapImpression))
                                  .setKeyedFrequencyCapsForClickEvents(ImmutableSet.of(
                                    keyedFrequencyCapClick))
                                  .build()
        )
        .build()
      if (isChecked) {
        caWrapper.joinFilteringCa(FREQ_CAP_CA_NAME,
                                  AdTechIdentifier.fromString(biddingUri.host!!),
                                  biddingUri,
                                  Uri.parse(biddingUri.toString() + "/render_" + FREQ_CAP_CA_NAME),
                                  Uri.parse("$biddingUri/daily"),
                                  Uri.parse("$biddingUri/trusted"),
                                  { event: String? ->
                           eventLog.writeEvent(
                             event!!)
                         },
                                  calcExpiry(ONE_DAY_EXPIRY),
                                  filters,
                                  ImmutableSet.of(adCounterKey))
      } else {
        caWrapper.leaveCa(FREQ_CAP_CA_NAME,
                          context.packageName,
                          AdTechIdentifier.fromString(
                            biddingUri.host!!)
        ) { event: String? ->
          eventLog.writeEvent(
            event!!)
        }
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
        adSelectionWrapper.reportInteraction(adSelectionId,
                                             clickInteraction,
                                             interactionData,
                                             ReportInteractionRequest.FLAG_REPORTING_DESTINATION_SELLER or ReportInteractionRequest.FLAG_REPORTING_DESTINATION_BUYER
        ) { event: String? ->
          eventLog.writeEvent(
            event!!)
        }
      } catch (e: java.lang.NumberFormatException) {
        Log.e(TAG,
              String.format("Error while parsing the ad selection id: %s", e))
        eventLog.writeEvent("Invalid AdSelectionId. Cannot run report interaction!")
      }
    }
  }

  private fun setupUpdateClickHistogramButton(
    adSelectionWrapper: AdSelectionWrapper,
    binding: ActivityMainBinding,
    eventLog: EventLogManager,
  ) {
    binding.runUpdateAdCounterHistogramButton.setOnClickListener { l ->
      try {
        val adSelectionIdInput =
          binding.adSelectionIdHistogramInput.text.toString()
        val adSelectionId = adSelectionIdInput.toLong()
        adSelectionWrapper.updateAdCounterHistogram(adSelectionId,
                                                    FrequencyCapFilters.AD_EVENT_TYPE_CLICK
        ) { event: String? ->
          eventLog.writeEvent(
            event!!)
        }
      } catch (e: java.lang.NumberFormatException) {
        Log.e(TAG,
              String.format("Error while parsing the ad selection id: %s", e))
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
    contextualLogicJs: String,
    biddingLogicJsVersion: Long,
    trustedScoringSignals: AdSelectionSignals,
    trustedBiddingSignals: AdSelectionSignals,
    biddingUri: Uri,
    context: Context,
  ) {
    val buyersDecisionLogic = BuyersDecisionLogic(mapOf(Pair(mBuyer,
                                                             DecisionLogic(
                                                               contextualLogicJs))))
    adSelectionWrapper.overrideAdSelection({ event: String? ->
                                             eventLog.writeEvent(
                                               event!!)
                                           }, decisionLogicJs, trustedScoringSignals, buyersDecisionLogic)
    customAudienceWrapper.addCAOverride(SHOES_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), biddingLogicJs, biddingLogicJsVersion, trustedScoringSignals) { event: String? ->
      eventLog.writeEvent(
        event!!)
    }
    customAudienceWrapper.addCAOverride(SHIRTS_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), biddingLogicJs, biddingLogicJsVersion,  trustedBiddingSignals) { event: String? ->
      eventLog.writeEvent(
        event!!)
    }
    customAudienceWrapper.addCAOverride(SHORT_EXPIRING_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), biddingLogicJs, biddingLogicJsVersion,  trustedBiddingSignals) { event: String? ->
      eventLog.writeEvent(
        event!!)
    }
    customAudienceWrapper.addCAOverride(INVALID_FIELD_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), biddingLogicJs, biddingLogicJsVersion,  trustedBiddingSignals) { event: String? ->
      eventLog.writeEvent(
        event!!)
    }
    customAudienceWrapper.addCAOverride(APP_INSTALL_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), biddingLogicJs, biddingLogicJsVersion,  trustedBiddingSignals) { event: String? ->
      eventLog.writeEvent(
        event!!)
    }
    customAudienceWrapper.addCAOverride(FREQ_CAP_CA_NAME, context.packageName, AdTechIdentifier.fromString(biddingUri.host!!), biddingLogicJs, biddingLogicJsVersion,  trustedBiddingSignals) { event: String? ->
      eventLog.writeEvent(
        event!!)
    }
  }

  private fun resetOverrides(
    eventLog: EventLogManager,
    adSelectionWrapper: AdSelectionWrapper,
    customAudienceWrapper: CustomAudienceWrapper,
  ) {
    adSelectionWrapper.resetAdSelectionOverrides { event: String? ->
      eventLog.writeEvent(
        event!!)
    }
    customAudienceWrapper.resetCAOverrides { event: String? ->
      eventLog.writeEvent(
        event!!)
    }
  }

  /**
   * Gets a given intent extra or returns the given default value
   * @param intent The intent to get
   * @param defaultValue The default value to return if intent doesn't exist
   */
  private fun getIntentOrDefault(intent: String, defaultValue: String) : String {
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
   * @param uri Uri to resolve
   */
  private fun resolveAdTechIdentifier(uri: Uri) : AdTechIdentifier{
    return AdTechIdentifier.fromString(uri.host!!)
  }

  /**
   * Reads a file into a string, to be used to read the .js files into a string.
   */
  @Throws(IOException::class)
  private fun assetFileToString(location: String): String {
    return BufferedReader(InputStreamReader(applicationContext.assets.open(location)))
      .lines().collect(Collectors.joining("\n"))
  }

  /**
   * Replaces the override URI in the .js files with an actual reporting URI
   */
  private fun replaceReportingURI(js: String, reportingUri: String): String {
    return js.replace("https://reporting.example.com", reportingUri)
  }

  private fun calcExpiry(duration: Duration) : Instant {
    return Instant.now().plus(duration)
  }

  private fun getAppInstallFilterForPackage(packageName: String): AdFilters? {
    return AdFilters.Builder()
      .setAppInstallFilters(AppInstallFilters.Builder()
                              .setPackageNames(setOf(packageName))
                              .build())
      .build()
  }
}