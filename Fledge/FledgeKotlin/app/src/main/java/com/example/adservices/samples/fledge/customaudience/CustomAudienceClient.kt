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
package com.example.adservices.samples.fledge.customaudience

import android.util.Log
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudienceManager
import androidx.privacysandbox.ads.adservices.customaudience.FetchAndJoinCustomAudienceRequest
import androidx.privacysandbox.ads.adservices.customaudience.JoinCustomAudienceRequest
import androidx.privacysandbox.ads.adservices.customaudience.LeaveCustomAudienceRequest
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.appContext
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CustomAudienceClient {
  private val customAudienceManager: CustomAudienceManager =
    CustomAudienceManager.obtain(appContext)!!

  /**
   * Joins a custom audience.
   *
   * @param customAudience The CA to join.
   * @param statusReceiver A consumer function that is run after the API call and returns a string.
   */
  suspend fun joinCustomAudience(
    customAudience: CustomAudience,
    statusReceiver: Consumer<String>
  ) {
    try {
      val request = JoinCustomAudienceRequest(customAudience)

      withContext(Dispatchers.IO) {
        customAudienceManager.joinCustomAudience(request)
      }
      statusReceiver.accept("Joined ${customAudience.name} custom audience")
    } catch (e: Exception) {
      Log.e(TAG, "Exception calling joinCustomAudience", e)
      statusReceiver.accept("Got the following exception when trying to join ${customAudience.name} custom audience: $e")
    }
  }

  /**
   * Fetches and joins a custom audience.
   *
   * @param request The request for FetchAndJoinCustomAudience.
   * @param statusReceiver A consumer function that is run after the API call and returns a string.
   */
  @OptIn(ExperimentalFeatures.Ext10OptIn::class)
  suspend fun fetchAndJoinCustomAudience(
    request: FetchAndJoinCustomAudienceRequest,
    statusReceiver: Consumer<String>
  ) {
    try {
      withContext(Dispatchers.IO) {
        customAudienceManager.fetchAndJoinCustomAudience(request)
      }
      statusReceiver.accept("Fetched and joined ${request.name} custom audience from ${request.fetchUri}")
    } catch (e: Exception) {
      Log.e(TAG, "Exception calling fetchAndJoinCustomAudience", e)
      statusReceiver.accept("Got the following exception when trying to fetch and join ${request.name} custom audience from ${request.fetchUri}: $e")
    }
  }

  /**
   * Leaves a custom audience.
   *
   * @param name The name of the CA to leave.
   * @param statusReceiver A consumer function that is run after the API call and returns a string.
   */
  suspend fun leaveCustomAudience(
    name: String,
    buyer: AdTechIdentifier,
    statusReceiver: Consumer<String>
  ) {
    try {
      val request = LeaveCustomAudienceRequest(buyer, name)

      withContext(Dispatchers.IO) {
        customAudienceManager.leaveCustomAudience(request)
      }
      statusReceiver.accept("Left $name custom audience")
    } catch (e: Exception) {
      Log.e(TAG, "Exception calling leaveCustomAudience", e)
      statusReceiver.accept("Got the following exception when trying to leave $name custom audience: $e")
    }
  }
}
