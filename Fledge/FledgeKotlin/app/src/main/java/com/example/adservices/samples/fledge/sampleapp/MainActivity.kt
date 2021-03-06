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
private const val BUYER = "sample-buyer.sampleapp"
private const val SELLER = "sample-seller.sampleapp"

// The names for the shirts and shoes custom audience
private const val SHOES_NAME = "shoes"
private const val SHIRTS_NAME = "shirts"

// Set override URLs
private const val BIDDING_OVERRIDE_URL = "https://override_url.com/bidding"
private const val SCORING_OVERRIDE_URL = "https://override_url.com/scoring"

// JS files
private const val BIDDING_LOGIC_FILE = "BiddingLogic.js"
private const val DECISION_LOGIC_FILE = "DecisionLogic.js"

// Shirts and shoes render URLS
private val SHOES_RENDER_URL = Uri.parse("shoes-url.shoestld")
private val SHIRTS_RENDER_URL = Uri.parse("shirts-url.shirtstld")

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
   * Does the initial setup for the app. This includes reading the Javascript server URLs from the
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
      // Set override URLS since overrides are on by default
      var biddingUrl = Uri.parse(BIDDING_OVERRIDE_URL)
      var scoringUrl = Uri.parse(SCORING_OVERRIDE_URL)

      // Get override reporting URL
      val reportingUrl = getIntentOrError("reportingUrl", eventLog, MISSING_FIELD_STRING_FORMAT_RESTART_APP)

      // Replace override URLs in JS
      val overrideDecisionJS =
        replaceReportingURL(assetFileToString(DECISION_LOGIC_FILE), reportingUrl)
      val overrideBiddingJs =
        replaceReportingURL(assetFileToString(BIDDING_LOGIC_FILE), reportingUrl)

      // Set up ad selection
      val adWrapper = AdSelectionWrapper(listOf(BUYER),
                                         SELLER, scoringUrl, context, EXECUTOR)
      binding.runAdsButton.setOnClickListener {
        adWrapper.runAdSelection(
          { event: String -> eventLog.writeEvent(event)
          }) { text: String? -> binding.adSpace.text = text }
      }

      // Set up Custom Audiences (CAs)
      val owner = context.packageName
      val caWrapper = CustomAudienceWrapper(owner, BUYER, EXECUTOR, context)

      // Set up CA buttons
      setupJoinCAButtons(caWrapper, eventLog, binding, biddingUrl)
      setupLeaveCAButtons(caWrapper, eventLog, binding)

      // Set up remote overrides by default
      useOverrides(eventLog, adWrapper, caWrapper, overrideDecisionJS, overrideBiddingJs)

      // Set up Override Switch
      binding.overrideSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
        if (isChecked) {
          useOverrides(eventLog, adWrapper, caWrapper, overrideDecisionJS, overrideBiddingJs)
        } else {
          try {
            biddingUrl = Uri.parse(getIntentOrError("biddingUrl", eventLog, MISSING_FIELD_STRING_FORMAT_USE_OVERRIDES))
            scoringUrl = Uri.parse(getIntentOrError("scoringUrl", eventLog, MISSING_FIELD_STRING_FORMAT_USE_OVERRIDES))
            // Set with new scoring url
            adWrapper.resetAdSelectionConfig(scoringUrl)

            // Reset join custom audience buttons as they rely on different biddingUrl
            setupJoinCAButtons(caWrapper, eventLog, binding, biddingUrl)

            // Resetting overrides
            resetOverrides(eventLog, adWrapper, caWrapper)
          } catch (e: java.lang.Exception) {
            binding.overrideSwitch.isChecked = true
            Log.e(TAG,"Error getting mock server urls", e)
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
    biddingUrl: Uri,
  ) {
    binding.joinShoesButton.setOnClickListener { v: View? ->
      caWrapper.joinCa(SHOES_NAME, biddingUrl, SHOES_RENDER_URL) { event: String? ->
        eventLog.writeEvent(
          event!!)
      }
    }
    binding.joinShirtsButton.setOnClickListener { v: View? ->
      caWrapper.joinCa(SHIRTS_NAME, biddingUrl, SHIRTS_RENDER_URL) { event: String? ->
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
  ) {
    adSelectionWrapper.overrideAdSelection({ event: String? ->
                                             eventLog.writeEvent(
                                               event!!)
                                           }, decisionLogicJs)
    customAudienceWrapper.addCAOverride(SHOES_NAME, biddingLogicJs, "") { event: String? ->
      eventLog.writeEvent(
        event!!)
    }
    customAudienceWrapper.addCAOverride(SHIRTS_NAME, biddingLogicJs, "") { event: String? ->
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
   * Replaces the override URL in the .js files with an actual reporting URL
   */
  private fun replaceReportingURL(js: String, reportingUrl: String): String {
    return js.replace("https://reporting.example.com", reportingUrl)
  }
}