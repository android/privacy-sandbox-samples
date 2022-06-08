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
import java.util.concurrent.Executor
import java.util.concurrent.Executors

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
      // Set URLS
      val biddingUrl = Uri.parse(getIntentOrError("biddingUrl", eventLog))
      val scoringUrl = Uri.parse(getIntentOrError("scoringUrl", eventLog))

      // Set up ad selection
      val adWrapper = AdSelectionWrapper(listOf(BUYER),
                                         SELLER, scoringUrl, context, EXECUTOR)
      binding.runAdsButton.setOnClickListener { v: View? ->
        adWrapper.runAdSelection(
          { event: String? ->
            eventLog.writeEvent(
              event!!)
          }) { text: String? -> binding.adSpace.text = text }
      }

      // Set up Custom Audiences (CAs)
      val owner = context.packageName
      val caWrapper = CustomAudienceWrapper(owner, BUYER, context, EXECUTOR)
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
    } catch (e: Exception) {
      Log.e(TAG, "Error when setting up app", e)
    }
  }

  /**
   * Gets a given intent extra or notifies the user that it is missing
   * @param intent The intent to get
   * @param eventLog An eventlog to write the error to
   * @return The string value of the intent specified.
   */
  private fun getIntentOrError(intent: String, eventLog: EventLogManager): String {
    val toReturn = getIntent().getStringExtra(intent)
    if (toReturn == null) {
      val message = String.format(MISSING_FIELD_STRING_FORMAT, intent)
      eventLog.writeEvent(message)
      throw RuntimeException(message)
    }
    return toReturn
  }

  companion object {
    // Log tag
    const val TAG = "FledgeSample"

    // The sample buyer and seller for the custom audiences
    const val BUYER = "sample-buyer.sampleapp"
    const val SELLER = "sample-seller.sampleapp"

    // The names for the shirts and shoes custom audience
    private const val SHOES_NAME = "shoes"
    private const val SHIRTS_NAME = "shirts"

    // Shirts and shoes render URLS
    private val SHOES_RENDER_URL = Uri.parse("shoes-url.shoestld")
    private val SHIRTS_RENDER_URL = Uri.parse("shirts-url.shirtstld")

    // Executor to be used for API calls
    private val EXECUTOR: Executor = Executors.newCachedThreadPool()

    // String to inform user a field in missing
    private const val MISSING_FIELD_STRING_FORMAT = "ERROR: %s is missing, " +
      "restart the activity using the directions in the README. The app will not be usable " +
      "until this is done"
  }
}