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

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.adservices.samples.fledge.sampleapp.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.stream.Collectors


// Log tag
const val TAG = "FledgeSample"

// The sample buyer and seller for the custom audiences
private const val BUYER = "sample-buyer.com"
private const val SELLER = "sample-seller.com"

// The names for the shirts and shoes custom audience
private const val SHOES_NAME = "shoes"
private const val SHIRTS_NAME = "shirts"

// Set override URIs
private const val BIDDING_LOGIC_OVERRIDE_URI = "https://sample-buyer.com/bidding"
private const val SCORING_LOGIC_OVERRIDE_URI = "https://sample-seller.com/scoring/js"
private const val TRUSTED_SCORING_OVERRIDE_URI = "https://sample-seller.com/scoring/trusted"

// JSON string objects that will be used during ad selection
private const val TRUSTED_SCORING_SIGNALS = ("{\n"
  + "\t\"render_uri_1\": \"signals_for_1\",\n"
  + "\t\"render_uri_2\": \"signals_for_2\"\n"
  + "}")
private const val TRUSTED_BIDDING_SIGNALS = ("{\n"
  + "\t\"example\": \"example\",\n"
  + "\t\"valid\": \"Also valid\",\n"
  + "\t\"list\": \"list\",\n"
  + "\t\"of\": \"of\",\n"
  + "\t\"keys\": \"trusted bidding signal Values\"\n"
  + "}")

// JS files
private const val BIDDING_LOGIC_FILE = "BiddingLogic.js"
private const val DECISION_LOGIC_FILE = "DecisionLogic.js"

// Shirts and shoes render URIS
private val SHOES_RENDER_URI = Uri.parse("shoes-uri.shoestld")
private val SHIRTS_RENDER_URI = Uri.parse("shirts-uri.shirtstld")

// Executor to be used for API calls
private val EXECUTOR: Executor = Executors.newCachedThreadPool()

// Strings to inform user a field in missing
private const val MISSING_FIELD_STRING_FORMAT_RESTART_APP = "ERROR: %s is missing, " +
  "restart the activity using the directions in the README. The app will not be usable " +
  "until this is done."
private const val MISSING_FIELD_STRING_FORMAT_USE_OVERRIDES = ("ERROR: %s is missing, " +
  "restart the activity using the directions in the README. You may still use the dev overrides "
  + "without restarting.")

/**
 * Android application activity for testing FLEDGE API
 */
@RequiresApi(api = 34)
class MainActivity : AppCompatActivity() {
  /**
   * Does the initial setup for the app. This includes reading the Javascript server URIs from the
   * start intent, creating the ad selection and custom audience wrappers to wrap the APIs, and
   * tying the UI elements to the wrappers so that button clicks trigger the appropriate methods.
   */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val context = applicationContext
    val binding = ActivityMainBinding.inflate(
      layoutInflater)
    val view: View = binding.root
    setContentView(view)
    val eventLog = EventLogManager(binding.eventLog)
    try {
      // Set override URIS since overrides are on by default
      var biddingLogicUri = Uri.parse(BIDDING_LOGIC_OVERRIDE_URI)
      var scoringLogicUri = Uri.parse(SCORING_LOGIC_OVERRIDE_URI)
      val trustedScoringUri = Uri.parse(TRUSTED_SCORING_OVERRIDE_URI)


      // Get override reporting URI
      val reportingUri = getIntentOrError("reportingUrl", eventLog, MISSING_FIELD_STRING_FORMAT_RESTART_APP)

      // Replace override URIs in JS
      val overrideDecisionJS =
        replaceReportingURI(assetFileToString(DECISION_LOGIC_FILE), reportingUri)
      val overrideBiddingJs =
        replaceReportingURI(assetFileToString(BIDDING_LOGIC_FILE), reportingUri)

      // Set up ad selection
      val adWrapper = AdSelectionWrapper(listOf(BUYER),
                                         SELLER, scoringLogicUri, trustedScoringUri, context, EXECUTOR)
      binding.runAdsButton.setOnClickListener {
        adWrapper.runAdSelection(
          { event: String -> eventLog.writeEvent(event)
          }) { text: String? -> binding.adSpace.text = text }
      }

      // Set up Custom Audiences (CAs)
      val owner = context.packageName
      val caWrapper = CustomAudienceWrapper(owner, BUYER, EXECUTOR, context)

      // Set up CA buttons
      setupJoinCAButtons(caWrapper, eventLog, binding, biddingLogicUri)
      setupLeaveCAButtons(caWrapper, eventLog, binding)

      // Set up remote overrides by default
      useOverrides(eventLog, adWrapper, caWrapper, overrideDecisionJS, overrideBiddingJs, TRUSTED_SCORING_SIGNALS, TRUSTED_BIDDING_SIGNALS)

      // Set up Override Switch
      binding.overrideSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
        if (isChecked) {
          useOverrides(eventLog, adWrapper, caWrapper, overrideDecisionJS, overrideBiddingJs, TRUSTED_SCORING_SIGNALS, TRUSTED_BIDDING_SIGNALS)
        } else {
          try {
            biddingLogicUri = Uri.parse(getIntentOrError("biddingUrl", eventLog, MISSING_FIELD_STRING_FORMAT_USE_OVERRIDES))
            scoringLogicUri = Uri.parse(getIntentOrError("scoringUrl", eventLog, MISSING_FIELD_STRING_FORMAT_USE_OVERRIDES))
            // Set with new scoring uri
            adWrapper.resetAdSelectionConfig(scoringLogicUri)

            // Reset join custom audience buttons as they rely on different biddingUri
            setupJoinCAButtons(caWrapper, eventLog, binding, biddingLogicUri)

            // Resetting overrides
            resetOverrides(eventLog, adWrapper, caWrapper)
          } catch (e: java.lang.Exception) {
            binding.overrideSwitch.isChecked = true
            Log.e(TAG,"Error getting mock server uris", e)
          }
        }
      }

    } catch (e: Exception) {
      Log.e(TAG, "Error when setting up app", e)
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

  private fun setupJoinCAButtons(
    caWrapper: CustomAudienceWrapper,
    eventLog: EventLogManager,
    binding: ActivityMainBinding,
    biddingUri: Uri,
  ) {
    binding.joinShoesButton.setOnClickListener { v: View? ->
      caWrapper.joinCa(SHOES_NAME, biddingUri, SHOES_RENDER_URI) { event: String? ->
        eventLog.writeEvent(
          event!!)
      }
    }
    binding.joinShirtsButton.setOnClickListener { v: View? ->
      caWrapper.joinCa(SHIRTS_NAME, biddingUri, SHIRTS_RENDER_URI) { event: String? ->
        eventLog.writeEvent(
          event!!)
      }
    }
  }

  private fun setupLeaveCAButtons(
    caWrapper: CustomAudienceWrapper,
    eventLog: EventLogManager,
    binding: ActivityMainBinding,
  ) {
    binding.leaveShoesButton.setOnClickListener { v: View? ->
      caWrapper.leaveCa(SHOES_NAME) { event: String? ->
        eventLog.writeEvent(
          event!!)
      }
    }
    binding.leaveShirtsButton.setOnClickListener { v: View? ->
      caWrapper.leaveCa(SHIRTS_NAME) { event: String? ->
        eventLog.writeEvent(
          event!!)
      }
    }
  }

  private fun useOverrides(
    eventLog: EventLogManager,
    adSelectionWrapper: AdSelectionWrapper,
    customAudienceWrapper: CustomAudienceWrapper,
    decisionLogicJs: String,
    biddingLogicJs: String,
    trustedScoringSignals: String,
    trustedBiddingSignals: String,
  ) {
    adSelectionWrapper.overrideAdSelection({ event: String? ->
                                             eventLog.writeEvent(
                                               event!!)
                                           }, decisionLogicJs, trustedScoringSignals)
    customAudienceWrapper.addCAOverride(SHOES_NAME, biddingLogicJs, trustedScoringSignals) { event: String? ->
      eventLog.writeEvent(
        event!!)
    }
    customAudienceWrapper.addCAOverride(SHIRTS_NAME, biddingLogicJs, trustedBiddingSignals) { event: String? ->
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
}