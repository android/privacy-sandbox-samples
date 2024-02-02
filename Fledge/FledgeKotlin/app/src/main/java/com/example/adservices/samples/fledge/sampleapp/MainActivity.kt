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
import android.adservices.adselection.ReportEventRequest
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
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.adservices.samples.fledge.sampleapp.databinding.ActivityMainBinding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.Duration
import java.time.Instant
import java.util.Objects
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.stream.Collectors
import org.json.JSONObject


// Log tag
const val TAG = "FledgeSample"

// Intents
private const val BASE_URL_INTENT = "baseUrl"
private const val AUCTION_SERVER_SELLER_SFE_URL_INTENT = "auctionServerSellerSfeUrl"
private const val AUCTION_SERVER_SELLER_INTENT = "auctionServerSeller"
private const val AUCTION_SERVER_BUYER_INTENT = "auctionServerBuyer"

const val AD_SELECTION_PREBUILT_SCHEMA = "ad-selection-prebuilt"
const val AD_SELECTION_USE_CASE = "ad-selection"
const val AD_SELECTION_HIGHEST_BID_WINS = "highest-bid-wins"

// The custom audience names
private const val SHOES_CA_NAME = "shoes"
private const val SHIRTS_CA_NAME = "shirts"
private const val HATS_CA_NAME = "hats"
private const val SHORT_EXPIRING_CA_NAME = "short_expiring"
private const val INVALID_FIELD_CA_NAME = "invalid_fields"
private const val APP_INSTALL_CA_NAME = "app_install"
private const val FREQ_CAP_CA_NAME = "freq_cap"
private const val SHOES_SERVER_AUCTION_CA_NAME = "shoes_server"
private const val SHIRTS_SERVER_AUCTION_CA_NAME = "shirts_server"

// Server Auction Custom Audience Render Id
private const val SHOES_SERVER_AUCTION_AD_RENDER_ID = "1"
private const val SHIRTS_SERVER_AUCTION_AD_RENDER_ID = "2"

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

  private var baseUriString: String = ""
  private var biddingLogicUri: Uri = Uri.EMPTY
  private var scoringLogicUri: Uri = Uri.EMPTY
  private var trustedDataUri: Uri = Uri.EMPTY
  private var contextualLogicUri: Uri = Uri.EMPTY
  private var auctionServerSellerSfeUri: Uri? = null
  private var auctionServerSeller: AdTechIdentifier? = null
  private var auctionServerBuyer: AdTechIdentifier? = null
  private var buyer: AdTechIdentifier? = null
  private var seller: AdTechIdentifier? = null
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

    // Same for override and non-overrides flows

    // Same for override and non-overrides flows
    baseUriString = getIntentOrError(
      BASE_URL_INTENT,
      eventLog!!,
      MISSING_FIELD_STRING_FORMAT_RESTART_APP
    )
    biddingLogicUri = Uri.parse("$baseUriString/bidding")
    scoringLogicUri = Uri.parse("$baseUriString/scoring")
    trustedDataUri = Uri.parse("$biddingLogicUri/trusted")
    contextualLogicUri = Uri.parse("$baseUriString/contextual")
    buyer = resolveAdTechIdentifier(biddingLogicUri)
    seller = resolveAdTechIdentifier(scoringLogicUri)

    auctionServerSellerSfeUri = auctionServerSellerSfeUriOrEmpty()
    auctionServerSeller = auctionServerSellerOrEmpty()
    auctionServerBuyer = auctionServerBuyerOrEmpty()

    try {
      // Get override reporting URI
      val reportingUriString = baseUriString
      // Replace override URIs in JS
      overrideDecisionJS = replaceReportingURI(assetFileToString(DECISION_LOGIC_FILE),
              reportingUriString)
      overrideContextualJs = replaceReportingURI(assetFileToString(CONTEXTUAL_LOGIC_FILE),
              reportingUriString)
      overrideBiddingJsV2 = replaceReportingURI(assetFileToString(BIDDING_LOGIC_FILE_V2),
              reportingUriString)
      overrideBiddingJsV3 = replaceReportingURI(assetFileToString(BIDDING_LOGIC_FILE_V3),
              reportingUriString)

      contextualAds = ContextualAds.Builder()
        .setBuyer(AdTechIdentifier.fromString(biddingLogicUri.host!!))
        .setDecisionLogicUri(contextualLogicUri)
        .setAdsWithBid(ArrayList())
        .build()

      // Set up the contextual ads switches
      setupContextualAdsSwitches(baseUriString, eventLog!!);

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

      // Set up Prebuilt Switch
      binding!!.usePrebuiltForScoring.setOnCheckedChangeListener { compoundButton: CompoundButton?, isPrebuiltForScoringEnabled: Boolean ->
        this.togglePrebuiltUriForScoring(
          compoundButton!!,
          isPrebuiltForScoringEnabled
        )
      }

      // Set up No buyers ad selection
      binding!!.noBuyers.setOnCheckedChangeListener { ignored1, ignored2 -> toggleNoBuyersCheckbox() }

      // Set up Auction Server ad selection
      binding!!.auctionServer.setOnCheckedChangeListener { ignored1, ignored2 -> toggleAuctionServerCheckbox() }

      // Set package names
      setupPackageNames()
    } catch (e: Exception) {
      Log.e(TAG, "Error when setting up app", e)
    }
  }

  private fun togglePrebuiltUriForScoring(
    compoundButton: CompoundButton,
    isPrebuiltForScoringEnabled: Boolean,
  ) {
    if (!isPrebuiltForScoringEnabled) {
      scoringLogicUri = Uri.parse("$baseUriString/scoring")
      setAdSelectionWrapper()
      Log.i(TAG, "prebuilt is turned off for scoring. ScoringUri: $scoringLogicUri"
      )
      eventLog!!.writeEvent("Prebuilt is turned off for scoring!")
      return
    }
    if (!binding!!.overrideOff.isChecked) {
      compoundButton.isChecked = false
      Log.i(TAG, "Cant apply prebuilt URI when overrides are on.")
      eventLog!!.writeEvent("Cant use prebuilt when override is on!")
      return
    }
    scoringLogicUri = getPrebuiltUriForScoringPickHighest()!!
    Log.i(TAG, "Switched to using prebuilt uri for scoring that picks the highest as "
        + "winner. Scoring Uri: " + scoringLogicUri
    )
    setAdSelectionWrapper()
    eventLog!!.writeEvent("Set prebuilt uri for scoring: pick highest bid")
  }

  private fun toggleNoBuyersCheckbox() {
    Log.i(TAG, "No Buyers check toggled to " + binding!!.noBuyers.isChecked)
    setAdSelectionWrapper()
    eventLog!!.writeEvent("No Buyers check toggled to " + binding!!.noBuyers.isChecked)
  }

  private fun toggleAuctionServerCheckbox() {
    if (auctionServerSellerSfeUri === Uri.EMPTY) {
      eventLog!!.writeEvent(
        String.format(
          "To enable server auctions you have to pass %s intent when starting the app.",
          AUCTION_SERVER_SELLER_SFE_URL_INTENT
        )
      )
      binding!!.auctionServer.isChecked = false
      return
    }
    if (auctionServerSeller.toString().isEmpty()) {
      eventLog!!.writeEvent(
        String.format(
          "To enable server auctions you have to pass %s intent when starting the app.",
          AUCTION_SERVER_SELLER_INTENT
        )
      )
      binding!!.auctionServer.isChecked = false
      return
    }
    if (auctionServerBuyer.toString().isEmpty()) {
      eventLog!!.writeEvent(
        String.format(
          "To enable server auctions you have to pass %s intent when starting the app.",
          AUCTION_SERVER_BUYER_INTENT
        )
      )
      binding!!.auctionServer.isChecked = false
      return
    }
    Log.i(TAG, "Auction Server check toggled to " + binding!!.auctionServer.isChecked)
    setAdSelectionWrapper()
    eventLog!!.writeEvent("Auction Server check toggled to " + binding!!.auctionServer.isChecked)
  }

  private fun setupPackageNames() {
    binding!!.contextualAiDataInput.setText(context!!.packageName)
    binding!!.caAiDataInput.setText(context!!.packageName)
  }

  private fun setAdSelectionWrapper() {
    val buyers: List<AdTechIdentifier> = if (binding!!.noBuyers.isChecked) emptyList() else listOf(buyer!!)
    adWrapper = AdSelectionWrapper(
      buyers,
      seller!!,
      scoringLogicUri,
      trustedDataUri,
      contextualAds!!,
      binding!!.usePrebuiltForScoring.isChecked,
      Uri.parse("$baseUriString/scoring"),
      context!!,
      EXECUTOR
    )
    if (binding!!.auctionServer.isChecked) {
      binding!!.runAdsButton.setOnClickListener { v ->
        adWrapper!!.runAdSelectionOnAuctionServer(
          auctionServerSellerSfeUri!!,
          auctionServerSeller!!,
          auctionServerBuyer!!,
          { event: String? ->
            eventLog!!.writeEvent(
              event!!
            )
          }, binding!!.adSpace::setText
        )
      }
    } else {
      binding!!.runAdsButton.setOnClickListener { v ->
        adWrapper!!.runAdSelection({ event: String? ->
                                     eventLog!!.writeEvent(
                                       event!!
                                     )
                                   }, binding!!.adSpace::setText)
      }
    }
  }

  private fun toggleOverrideSwitch(
    radioGroup: RadioGroup,
    checkedId: Int,
  ) {
    if (binding!!.overrideOff.isChecked) {
      try {
        biddingLogicUri = Uri.parse("$baseUriString/bidding")
        scoringLogicUri = Uri.parse("$baseUriString/scoring")
        trustedDataUri = Uri.parse("$biddingLogicUri/trusted")
        contextualLogicUri = Uri.parse("$baseUriString/contextual")
        buyer = resolveAdTechIdentifier(biddingLogicUri)
        seller = resolveAdTechIdentifier(scoringLogicUri)

        // Set with new scoring uri
        adWrapper!!.resetAdSelectionConfig(listOf(buyer!!),
                                           seller!!,
                                           scoringLogicUri,
                                           trustedDataUri,
                                           contextualAds!!)

        // Reset CA switches as they rely on different biddingLogicUri
        setupCASwitches(caWrapper!!, eventLog!!, binding!!, biddingLogicUri, context!!)
        resetOverrides(eventLog!!, adWrapper!!, caWrapper!!)

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
    if (binding!!.usePrebuiltForScoring.isChecked) {
      binding!!.usePrebuiltForScoring.isChecked = false
    }

    setAdSelectionWrapper()

    // Uncheck prebuilt checkbox because prebuilt is not available when overrides are on yet
    binding!!.usePrebuiltForScoring.isChecked = false

    // Set up Custom Audience Wrapper
    caWrapper = CustomAudienceWrapper(EXECUTOR, context!!)

    // Set up the app install switch
    setupAppInstallSwitch(biddingLogicUri, eventLog!!)

    // Set up CA switches
    setupCASwitches(caWrapper!!, eventLog!!, binding!!, biddingLogicUri, context!!)

    // Decide which js file to use.
    val biddingLogicJs = if (biddingLogicVersion == 2L) overrideBiddingJsV2 else overrideBiddingJsV3

    // Setup remote overrides by default
    useOverrides(eventLog!!, adWrapper!!, caWrapper!!, overrideDecisionJS!!, biddingLogicJs!!, overrideContextualJs!!, biddingLogicVersion, TRUSTED_SCORING_SIGNALS, TRUSTED_BIDDING_SIGNALS, biddingLogicUri, context!!);
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
      setAdSelectionWrapper()
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

      setAdSelectionWrapper()
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
    val buyer = AdTechIdentifier.fromString(biddingUri.host!!)
    // Shoes
    binding.shoesCaSwitch.setOnCheckedChangeListener { _ , isChecked : Boolean ->
     if (isChecked) {
       caWrapper.joinCa(SHOES_CA_NAME,
                        buyer,
                        biddingUri,
                        Uri.parse(
                          "$biddingUri/render_$SHOES_CA_NAME"),
                        Uri.parse("$biddingUri/daily"),
                        Uri.parse("$biddingUri/trusted"),
                        eventLog::writeEvent,
                        calcExpiry(ONE_DAY_EXPIRY))
     } else {
       Log.i(TAG, "leaving SHOES CA with buyer: $buyer")
       caWrapper.leaveCa(SHOES_CA_NAME, context.packageName, buyer, eventLog::writeEvent)
     }
    }

    // Shirt
    binding.shirtsCaSwitch.setOnCheckedChangeListener { _ , isChecked: Boolean ->
      if (isChecked) {
        caWrapper.joinCa(SHIRTS_CA_NAME,
                         buyer,
                         biddingUri,
                         Uri.parse(
                           "$biddingUri/render_$SHIRTS_CA_NAME"),
                         Uri.parse("$biddingUri/daily"),
                         Uri.parse("$biddingUri/trusted"),
                         eventLog::writeEvent,
                         calcExpiry(ONE_DAY_EXPIRY))
      } else {
        caWrapper.leaveCa(SHIRTS_CA_NAME, context.packageName, buyer, eventLog::writeEvent)
      }
    }

    // Server Auction CA
    val serverAuctionBiddingUri =
      biddingUri.buildUpon().authority(auctionServerBuyer.toString()).build()
    binding.shoesServerAuctionCaSwitch.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        caWrapper.joinServerAuctionCa(
          SHOES_SERVER_AUCTION_CA_NAME,
          AdTechIdentifier.fromString(serverAuctionBiddingUri.host!!),
          serverAuctionBiddingUri,
          Uri.parse("$serverAuctionBiddingUri./render_$SHOES_SERVER_AUCTION_CA_NAME"),
          Uri.parse("$serverAuctionBiddingUri/daily"),
          Uri.parse("$serverAuctionBiddingUri/trusted"),
          { event: String? -> eventLog.writeEvent(event!!) },
          calcExpiry(ONE_DAY_EXPIRY),
          SHOES_SERVER_AUCTION_AD_RENDER_ID
        )
      } else {
        caWrapper.leaveCa(
          SHOES_SERVER_AUCTION_CA_NAME,
          context.packageName,
          AdTechIdentifier.fromString(serverAuctionBiddingUri.host!!)
        ) { event: String? -> eventLog.writeEvent(event!!) }
      }
    }
    binding.shirtsServerAuctionCaSwitch.setOnCheckedChangeListener { _, isChecked ->
      if (isChecked) {
        caWrapper.joinServerAuctionCa(
          SHIRTS_SERVER_AUCTION_CA_NAME,
          AdTechIdentifier.fromString(serverAuctionBiddingUri.host!!),
          serverAuctionBiddingUri,
          Uri.parse("$serverAuctionBiddingUri/render_$SHIRTS_SERVER_AUCTION_CA_NAME"),
          Uri.parse("$serverAuctionBiddingUri/daily"),
          Uri.parse("$serverAuctionBiddingUri/trusted"),
          { event: String? -> eventLog.writeEvent(event!!) },
          calcExpiry(ONE_DAY_EXPIRY),
          SHIRTS_SERVER_AUCTION_AD_RENDER_ID
        )
      } else {
        caWrapper.leaveCa(
          SHIRTS_SERVER_AUCTION_CA_NAME,
          context.packageName,
          AdTechIdentifier.fromString(serverAuctionBiddingUri.host!!)
        ) { event: String? -> eventLog.writeEvent(event!!) }
      }
    }

    // Short expiring CA
    binding.shortExpiryCaSwitch.setOnCheckedChangeListener{ _ , isChecked: Boolean ->
      if (isChecked) {
        caWrapper.joinCa(SHORT_EXPIRING_CA_NAME,
                         buyer,
                         biddingUri,
                         Uri.parse(
                           "$biddingUri/render_$SHOES_CA_NAME"),
                         Uri.parse("$biddingUri/daily"),
                         Uri.parse("$biddingUri/trusted"),
                         eventLog::writeEvent,
                         calcExpiry(THIRTY_SECONDS_EXPIRY))
      } else {
        caWrapper.leaveCa(SHORT_EXPIRING_CA_NAME, context.packageName, buyer, eventLog::writeEvent)
      }
    }

    // Invalid field CA
    binding.invalidFieldsCaSwitch.setOnCheckedChangeListener{ _ , isChecked: Boolean ->
      if (isChecked) {
        caWrapper.joinEmptyFieldCa(INVALID_FIELD_CA_NAME,
                                   buyer,
                                   biddingUri,
                                   Uri.parse(
                           "$biddingUri/render_$SHOES_CA_NAME"),
                                   eventLog::writeEvent,
                                   calcExpiry(THIRTY_SECONDS_EXPIRY))
      } else {
        caWrapper.leaveCa(SHORT_EXPIRING_CA_NAME, context.packageName, buyer, eventLog::writeEvent)
      }
    }

    // App Install CA
    binding.appInstallCaSwitch.setOnCheckedChangeListener{ _ , isChecked: Boolean ->
      if (isChecked) {
        caWrapper.joinCa(APP_INSTALL_CA_NAME,
                         buyer,
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
        caWrapper.leaveCa(APP_INSTALL_CA_NAME, context.packageName, buyer, eventLog::writeEvent)
        binding.caAiDataInput.isEnabled = true;
      }
    }

    // Frequency Capped CA
    binding.freqCapCaSwitch.setOnCheckedChangeListener { _, isChecked ->
      val adCounterKey = 1

      // Caps is exceeded after 2 impression events
      val keyedFrequencyCapImpression =
        KeyedFrequencyCap.Builder(adCounterKey, 2, Duration.ofSeconds(10))
          .build()

      // Caps is exceeded after 1 click event
      val keyedFrequencyCapClick =
        KeyedFrequencyCap.Builder(adCounterKey, 1, Duration.ofSeconds(10))
          .build()
      val filters = AdFilters.Builder()
        .setFrequencyCapFilters(FrequencyCapFilters.Builder()
                                  .setKeyedFrequencyCapsForImpressionEvents(ImmutableList.of(
                                    keyedFrequencyCapImpression))
                                  .setKeyedFrequencyCapsForClickEvents(ImmutableList.of(
                                    keyedFrequencyCapClick))
                                  .build()
        )
        .build()
      if (isChecked) {
        caWrapper.joinFilteringCa(FREQ_CAP_CA_NAME,
                                  buyer,
                                  biddingUri,
                                  Uri.parse("$biddingUri/render_$FREQ_CAP_CA_NAME"),
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
                          buyer
        ) { event: String? ->
          eventLog.writeEvent(
            event!!)
        }
      }
    }

    // Fetch CA
    val baseUri = getIntentOrError(BASE_URL_INTENT,
                                   eventLog,
                                   MISSING_FIELD_STRING_FORMAT_RESTART_APP)
    binding.fetchAndJoinCaSwitch.setOnCheckedChangeListener {
        _, isChecked: Boolean ->
      if (isChecked) {
        caWrapper.fetchAndJoinCa(
                Uri.parse("$baseUri/fetch/ca"),
                HATS_CA_NAME,
                Instant.now(),
                calcExpiry(ONE_DAY_EXPIRY),
                AdSelectionSignals.EMPTY) {
          event: String? -> eventLog.writeEvent(event!!)
        }
      } else {
        caWrapper.leaveCa(
          HATS_CA_NAME,
          context.packageName,
          buyer) {
          event: String? -> eventLog.writeEvent(event!!)
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
                                             ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER or ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER
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
    val buyersDecisionLogic = BuyersDecisionLogic(mapOf(Pair(buyer,
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

  private fun auctionServerSellerSfeUriOrEmpty(): Uri {
    var sfeUriString: String?
    return if (getIntentOrNull(AUCTION_SERVER_SELLER_SFE_URL_INTENT).also {
        sfeUriString = it
      } != null) {
      Uri.parse(sfeUriString)
    } else {
      Uri.EMPTY
    }
  }

  private fun auctionServerSellerOrEmpty(): AdTechIdentifier {
    var auctionServerSeller: String?
    return if (getIntentOrNull(AUCTION_SERVER_SELLER_INTENT).also {
        auctionServerSeller = it
      } != null) {
      AdTechIdentifier.fromString(auctionServerSeller!!)
    } else {
      AdTechIdentifier.fromString("")
    }
  }

  private fun auctionServerBuyerOrEmpty(): AdTechIdentifier {
    var auctionServerBuyer: String?
    return if (getIntentOrNull(AUCTION_SERVER_BUYER_INTENT).also {
        auctionServerBuyer = it
      } != null) {
      AdTechIdentifier.fromString(auctionServerBuyer!!)
    } else {
      AdTechIdentifier.fromString("")
    }
  }

  private fun getPrebuiltUriForScoringPickHighest(): Uri? {
    val paramKey = "reportingUrl"
    val paramValue = "$baseUriString/reporting"
    return Uri.parse(
      String.format(
        "%s://%s/%s/?%s=%s",
        AD_SELECTION_PREBUILT_SCHEMA,
        AD_SELECTION_USE_CASE,
        AD_SELECTION_HIGHEST_BID_WINS,
        paramKey,
        paramValue
      )
    )
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

  private fun getIntentOrNull(intent: String): String? {
    val value = getIntent().getStringExtra(intent)
    if (Objects.isNull(value)) {
      Log.e(
        TAG, String.format(
          "Intent %s is not available, returning null. This can cause problems later.",
          intent
        )
      )
    }
    return value
  }
}