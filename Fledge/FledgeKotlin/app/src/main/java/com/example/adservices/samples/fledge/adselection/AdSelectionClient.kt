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
package com.example.adservices.samples.fledge.adselection

import android.util.Log
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionManager
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome
import androidx.privacysandbox.ads.adservices.adselection.GetAdSelectionDataOutcome
import androidx.privacysandbox.ads.adservices.adselection.GetAdSelectionDataRequest
import androidx.privacysandbox.ads.adservices.adselection.PersistAdSelectionResultRequest
import androidx.privacysandbox.ads.adservices.adselection.ReportEventRequest
import androidx.privacysandbox.ads.adservices.adselection.ReportImpressionRequest
import androidx.privacysandbox.ads.adservices.adselection.UpdateAdCounterHistogramRequest
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.appContext
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdSelectionClient {
  private val adSelectionManager: AdSelectionManager = AdSelectionManager.obtain(appContext)!!

  /**
   * Runs ad selection.
   */
  suspend fun selectAds(
    adSelectionConfig: AdSelectionConfig,
    statusReceiver: Consumer<String>
  ): AdSelectionOutcome {
    return try {
      withContext(Dispatchers.IO) {
        val adSelectionOutcome = adSelectionManager.selectAds(adSelectionConfig)
        statusReceiver.accept("Ad selection successful for " + adSelectionOutcome.adSelectionId)
        adSelectionOutcome
      }
    } catch (e: Exception) {
      statusReceiver.accept("Exception calling runAdSelection: $e")
      Log.e(TAG, "Exception calling runAdSelection", e)
      throw e
    }
  }

  /**
   * Runs impression reporting.
   */
  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  suspend fun reportImpression(
    request: ReportImpressionRequest,
    statusReceiver: Consumer<String>,
    onSuccess: (suspend () -> Unit)? = null
  ) {
    try {
      withContext(Dispatchers.IO) {
        adSelectionManager.reportImpression(request)
      }
      statusReceiver.accept("Reported impression from ad selection")
      Log.v(TAG, "Reported impression from ad selection")
      onSuccess?.invoke()
    } catch (e: Exception) {
      statusReceiver.accept("Exception calling reportImpression: ${e.message}")
      Log.e(TAG, "Exception calling reportImpression: ${e.message}")
    }
  }

  /**
   * Runs event reporting.
   */
  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  suspend fun reportEvent(
    request: ReportEventRequest,
    statusReceiver: Consumer<String>,
  ) {
    try {
      withContext(Dispatchers.IO) {
        adSelectionManager.reportEvent(request)
      }
      statusReceiver.accept("Reported event with ${request.eventKey} key from ad selection")
    } catch (e: Exception) {
      statusReceiver.accept("Exception calling reportEvent with ${request.eventKey} key and ${request.eventData} data: ${e.message}")
      Log.e(
        TAG,
        "Exception calling reportEvent with ${request.eventKey} key and ${request.eventData} data: ${e.message}"
      )
    }
  }

  /**
   * Updates the counter histogram for an ad.
   */
  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  suspend fun updateAdCounterHistogram(
    request: UpdateAdCounterHistogramRequest,
    statusReceiver: Consumer<String>
  ) {

    try {
      withContext(Dispatchers.IO) {
        adSelectionManager.updateAdCounterHistogram(request)
      }
      statusReceiver.accept(
        "Updated ad counter histogram with ${fCapEventToString(request.adEventType)} event for adtech ${request.callerAdTech}"
      )
    } catch (e: Exception) {
      statusReceiver.accept(
        "Exception calling updateAdCounterHistogram with ${fCapEventToString(request.adEventType)} event for adtech ${request.callerAdTech}: ${e.message}"
      )
      Log.e(
        TAG,
        "Exception calling updateAdCounterHistogram with ${fCapEventToString(request.adEventType)} event for adtech ${request.callerAdTech}: ${e.message}"
      )
    }
  }

  /**
   * Invokes the `getAdSelectionData` method of [AdSelectionManager], and returns an GetAdSelectionDataOutcome.
   */
  @ExperimentalFeatures.Ext10OptIn
  suspend fun getAdSelectionData(
    request: GetAdSelectionDataRequest,
    statusReceiver: Consumer<String>
  ): GetAdSelectionDataOutcome {
    return try {
      withContext(Dispatchers.IO) {
        adSelectionManager.getAdSelectionData(request)
      }
    } catch (e: Exception) {
      statusReceiver.accept("Got the following exception when trying to run getAdSelectionData: $e")
      Log.e(TAG, "Exception calling getAdSelectionData: ${e.message}", e)
      throw e
    }
  }

  /**
   * Invokes the `persistAdSelectionResult` method of [AdSelectionManager], and returns an AdSelectionOutcome.
   */
  @OptIn(ExperimentalFeatures.Ext10OptIn::class)
  suspend fun persistAdSelectionResult(
    request: PersistAdSelectionResultRequest,
    statusReceiver: Consumer<String>
  ): AdSelectionOutcome {
    return try {

      withContext(Dispatchers.IO) {
        val adSelectionOutcome = adSelectionManager.persistAdSelectionResult(request)
        statusReceiver.accept("Auction Result is persisted for ${adSelectionOutcome.adSelectionId}")
        Log.v(TAG, "Auction Result is persisted for ${adSelectionOutcome.adSelectionId}")
        adSelectionOutcome
      }
    } catch (e: Exception) {
      statusReceiver.accept("Exception calling persistAdSelectionResult: $e")
      Log.e(TAG, "Exception calling persistAdSelectionResult: ${e.message}", e)
      throw e
    }
  }

  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  private fun fCapEventToString(eventType: Int): String {
    val result: String =
      when (eventType) {
        FrequencyCapFilters.AD_EVENT_TYPE_WIN -> "win"
        FrequencyCapFilters.AD_EVENT_TYPE_CLICK -> "click"
        FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION -> "impression"
        FrequencyCapFilters.AD_EVENT_TYPE_VIEW -> "view"
        else -> "unknown"
      }
    return result
  }
}
