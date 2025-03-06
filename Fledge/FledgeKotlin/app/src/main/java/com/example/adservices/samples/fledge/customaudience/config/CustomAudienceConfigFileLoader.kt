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
package com.example.adservices.samples.fledge.customaudience.config

import android.net.Uri
import android.util.Log
import android.util.Pair
import androidx.privacysandbox.ads.adservices.common.AdData
import androidx.privacysandbox.ads.adservices.common.AdFilters
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters
import androidx.privacysandbox.ads.adservices.common.KeyedFrequencyCap
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience
import androidx.privacysandbox.ads.adservices.customaudience.FetchAndJoinCustomAudienceRequest
import androidx.privacysandbox.ads.adservices.customaudience.TrustedBiddingData
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.appContext
import com.example.adservices.samples.fledge.util.CommonConstants
import com.google.common.collect.ImmutableBiMap
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.function.Consumer
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class CustomAudienceConfigFileLoader {
  @OptIn(ExperimentalFeatures.Ext10OptIn::class)
  @Throws(IOException::class, JSONException::class)
  fun load(
    buyerBaseUri: Uri,
    statusReceiver: Consumer<String>,
    fileName: String = DEFAULT_CUSTOM_AUDIENCE_CONFIG_FILE,
  ): CustomAudienceConfig {
    val inputStream = appContext.assets.open(fileName)
    val fileString = inputStream.bufferedReader().use { it.readText() }

    val jsonObject = JSONObject(
      fileString
        .replace(VARIABLE_BUYER, buyerBaseUri.host.toString())
        .replace(VARIABLE_BUYER_BASE_URI, buyerBaseUri.toString())
    )

    val customAudienceHashMap = LinkedHashMap<String, CustomAudience>()
    if (jsonObject.has(FIELD_CUSTOM_AUDIENCES)) {
      val jsonArray = jsonObject.getJSONArray(FIELD_CUSTOM_AUDIENCES)
      for (i in 0 until jsonArray.length()) {
        try {
          val loadedCaData = loadCustomAudience(jsonArray.getJSONObject(i))
          customAudienceHashMap[loadedCaData.first] = loadedCaData.second
        } catch (e: RuntimeException) {
          statusReceiver.accept(e.message!!)
          Log.w(TAG, e.message!!)
        }
      }
    }
    val fetchAndJoinCustomAudienceRequestMap =
      LinkedHashMap<String, FetchAndJoinCustomAudienceRequest>()
    if (jsonObject.has(FIELD_FETCH_AND_JOIN_CUSTOM_AUDIENCES)) {
      val jsonArray = jsonObject.getJSONArray(FIELD_FETCH_AND_JOIN_CUSTOM_AUDIENCES)
      for (i in 0 until jsonArray.length()) {
        val loadedFetchCaData = loadFetchCustomAudience(jsonArray.getJSONObject(i))
        fetchAndJoinCustomAudienceRequestMap[loadedFetchCaData.first] = loadedFetchCaData.second
      }
    }

    Log.v(TAG, "Loaded custom audience config file: $fileName")
    return CustomAudienceConfig(
      ImmutableBiMap.copyOf(customAudienceHashMap),
      ImmutableBiMap.copyOf(fetchAndJoinCustomAudienceRequestMap)
    )
  }

  @Throws(JSONException::class)
  private fun loadCustomAudience(jsonObject: JSONObject): Pair<String, CustomAudience> {
    val labelName = jsonObject.getString(KEY_LABEL_NAME)
    if (labelName.isEmpty()) {
      throw IllegalStateException(
        "$labelName should be present in the configuration file to parse custom audience data"
      )
    }

    val name = jsonObject.getString(KEY_NAME)
    val buyer = AdTechIdentifier(jsonObject.getString(KEY_BUYER))
    val biddingLogicUri = Uri.parse(jsonObject.getString(KEY_BIDDING_LOGIC_URI))
    val dailyUpdateUri = Uri.parse(jsonObject.getString(KEY_DAILY_UPDATE_URI))
    var trustedBiddingData: TrustedBiddingData? = null
    if (jsonObject.has(KEY_TRUSTED_BIDDING_DATA)) {
      trustedBiddingData = getTrustedBiddingDataFromJson(
        jsonObject.getJSONObject(KEY_TRUSTED_BIDDING_DATA)
      )
    }
    var userBiddingSignals = CommonConstants.emptyAdSelectionSignals
    if (jsonObject.has(KEY_USER_BIDDING_SIGNALS)) {
      userBiddingSignals = AdSelectionSignals(
        jsonObject.getString(KEY_USER_BIDDING_SIGNALS)
      )
    }
    var adData: List<AdData> = ArrayList()
    if (jsonObject.has(KEY_ADS)) {
      adData = getAdsFromJsonArray(jsonObject.getJSONArray(KEY_ADS))
    }
    var expirationTime: Instant? = null
    if (jsonObject.has(KEY_EXPIRATION_TIME_FROM_NOW_IN_SEC)) {
      expirationTime = calculateRelativeTime(
        Duration.ofSeconds(jsonObject.getInt(KEY_EXPIRATION_TIME_FROM_NOW_IN_SEC).toLong())
      )
    }
    var activationTime: Instant? = null
    if (jsonObject.has(KEY_ACTIVATION_TIME_FROM_NOW_IN_SEC)) {
      activationTime = calculateRelativeTime(
        Duration.ofSeconds(jsonObject.getInt(KEY_ACTIVATION_TIME_FROM_NOW_IN_SEC).toLong())
      )
    }
    val customAudience = CustomAudience(
      buyer,
      name,
      dailyUpdateUri,
      biddingLogicUri,
      adData,
      activationTime,
      expirationTime,
      userBiddingSignals,
      trustedBiddingData
    )

    return Pair.create(labelName, customAudience)
  }

  @Throws(JSONException::class)
  private fun getTrustedBiddingDataFromJson(jsonObject: JSONObject): TrustedBiddingData {
    return TrustedBiddingData(
      Uri.parse(jsonObject.getString(KEY_ADS_URI)),
      getStringsFromJsonArray(jsonObject.getJSONArray(KEY_ADS_KEYS))
    )
  }

  @Throws(JSONException::class)
  private fun getStringsFromJsonArray(jsonArray: JSONArray): ImmutableList<String> {
    val builder = ImmutableList.builder<String>()
    for (i in 0 until jsonArray.length()) {
      builder.add(jsonArray.getString(i))
    }
    return builder.build()
  }

  @Throws(JSONException::class)
  private fun getAdsFromJsonArray(jsonArray: JSONArray): ImmutableList<AdData> {
    val builder = ImmutableList.builder<AdData>()
    for (i in 0 until jsonArray.length()) {
      builder.add(getAdFromJson(jsonArray.getJSONObject(i)))
    }
    return builder.build()
  }

  @OptIn(ExperimentalFeatures.Ext10OptIn::class, ExperimentalFeatures.Ext8OptIn::class)
  @Throws(JSONException::class)
  private fun getAdFromJson(jsonObject: JSONObject): AdData {
    var adRenderId: String? = null
    if (jsonObject.has(KEY_ADS_AD_RENDER_ID)) {
      adRenderId = jsonObject.getString(KEY_ADS_AD_RENDER_ID)
    }
    var adCounterKeys: Set<Int> = HashSet()
    if (jsonObject.has(KEY_ADS_AD_COUNTER_KEYS)) {
      adCounterKeys = getIntegersFromJsonArray(jsonObject.getJSONArray(KEY_ADS_AD_COUNTER_KEYS))
    }
    var adFilters: AdFilters? = null
    if (jsonObject.has(KEY_ADS_AD_FILTERS) && jsonObject.getJSONObject(KEY_ADS_AD_FILTERS)
        .has(KEY_FCAP)
    ) {
      adFilters = AdFilters(
        getFrequencyCapFilters(
          jsonObject.getJSONObject(KEY_ADS_AD_FILTERS).getJSONObject("frequency_cap")
        )
      )
    }
    return AdData(
      Uri.parse(jsonObject.getString(KEY_ADS_RENDER_URI)),
      jsonObject.getString(KEY_ADS_METADATA),
      adCounterKeys,
      adFilters,
      adRenderId
    )
  }

  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  @Throws(JSONException::class)
  private fun getFrequencyCapFilters(jsonObject: JSONObject): FrequencyCapFilters {
    var fCapsForWinEvents: List<KeyedFrequencyCap> = ArrayList()
    var fCapsForImpressionEvents: List<KeyedFrequencyCap> = ArrayList()
    var fCapsForViewEvents: List<KeyedFrequencyCap> = ArrayList()
    var fCapsForClickEvents: List<KeyedFrequencyCap> = ArrayList()
    if (jsonObject.has(KEY_FCAP_FOR_WIN_EVENTS)) {
      fCapsForWinEvents = getKeyedFrequencyCapList(jsonObject.getJSONArray(KEY_FCAP_FOR_WIN_EVENTS))
    }
    if (jsonObject.has(KEY_FCAP_FOR_IMPRESSION_EVENTS)) {
      fCapsForImpressionEvents = getKeyedFrequencyCapList(
        jsonObject.getJSONArray(KEY_FCAP_FOR_IMPRESSION_EVENTS)
      )
    }
    if (jsonObject.has(KEY_FCAP_FOR_VIEW_EVENTS)) {
      fCapsForViewEvents =
        getKeyedFrequencyCapList(jsonObject.getJSONArray(KEY_FCAP_FOR_VIEW_EVENTS))
    }
    if (jsonObject.has(KEY_FCAP_FOR_CLICK_EVENTS)) {
      fCapsForClickEvents =
        getKeyedFrequencyCapList(jsonObject.getJSONArray(KEY_FCAP_FOR_CLICK_EVENTS))
    }
    return FrequencyCapFilters(
      fCapsForWinEvents, fCapsForImpressionEvents, fCapsForViewEvents, fCapsForClickEvents
    )
  }

  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  @Throws(JSONException::class)
  private fun getKeyedFrequencyCapList(jsonArray: JSONArray): List<KeyedFrequencyCap> {
    val builder = ImmutableList.builder<KeyedFrequencyCap>()
    for (i in 0 until jsonArray.length()) {
      builder.add(getKeyedFrequencyCap(jsonArray.getJSONObject(i)))
    }
    return builder.build()
  }

  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  @Throws(JSONException::class)
  private fun getKeyedFrequencyCap(jsonObject: JSONObject): KeyedFrequencyCap {
    return KeyedFrequencyCap(
      jsonObject.getInt(KEY_FCAP_AD_COUNTER_KEY),
      jsonObject.getInt(KEY_FCAP_MAX_COUNT),
      Duration.ofSeconds(jsonObject.getInt(KEY_FCAP_INTERVAL_IN_SEC).toLong())
    )
  }

  @Throws(JSONException::class)
  private fun getIntegersFromJsonArray(jsonArray: JSONArray): ImmutableSet<Int> {
    val builder = ImmutableSet.builder<Int>()
    for (i in 0 until jsonArray.length()) {
      builder.add(jsonArray.getInt(i))
    }
    return builder.build()
  }

  /**
   * Returns a point in time at `duration` in the future from now
   *
   * @param duration Amount of time in the future
   */
  private fun calculateRelativeTime(duration: Duration): Instant {
    return Instant.now().plus(duration)
  }

  @OptIn(ExperimentalFeatures.Ext10OptIn::class)
  @Throws(JSONException::class)
  private fun loadFetchCustomAudience(
    jsonObject: JSONObject,
  ): Pair<String, FetchAndJoinCustomAudienceRequest> {
    val labelName = jsonObject.getString(KEY_LABEL_NAME)
    if (labelName.isEmpty()) {
      throw IllegalStateException(
        "$KEY_LABEL_NAME should be present in the configuration file to parse custom audience data"
      )
    }
    var name: String? = null
    if (jsonObject.has(KEY_NAME)) {
      name = jsonObject.getString(KEY_NAME)
    }
    var activationTime: Instant? = null
    if (jsonObject.has(KEY_ACTIVATION_TIME_FROM_NOW_IN_SEC)) {
      activationTime = calculateRelativeTime(
        Duration.ofSeconds(jsonObject.getInt(KEY_ACTIVATION_TIME_FROM_NOW_IN_SEC).toLong())
      )
    }
    var expirationTime: Instant? = null
    if (jsonObject.has(KEY_EXPIRATION_TIME_FROM_NOW_IN_SEC)) {
      expirationTime = calculateRelativeTime(
        Duration.ofSeconds(jsonObject.getInt(KEY_EXPIRATION_TIME_FROM_NOW_IN_SEC).toLong())
      )
    }
    var userBiddingSignals = CommonConstants.emptyAdSelectionSignals
    if (jsonObject.has(KEY_USER_BIDDING_SIGNALS)) {
      userBiddingSignals = AdSelectionSignals(
        jsonObject.getString(KEY_USER_BIDDING_SIGNALS)
      )
    }
    val request = FetchAndJoinCustomAudienceRequest(
      Uri.parse(jsonObject.getString(KEY_FETCH_URI)),
      name,
      activationTime,
      expirationTime,
      userBiddingSignals
    )
    return Pair.create(labelName, request)
  }

  private companion object {
    const val DEFAULT_CUSTOM_AUDIENCE_CONFIG_FILE: String = "CustomAudienceConfig.json"

    const val FIELD_CUSTOM_AUDIENCES = "customAudiences"
    const val FIELD_FETCH_AND_JOIN_CUSTOM_AUDIENCES = "fetchAndJoinCustomAudiences"

    const val KEY_FETCH_URI = "fetch_uri"
    const val KEY_LABEL_NAME = "label_name"
    const val KEY_NAME = "name"
    const val KEY_BUYER = "buyer"
    const val KEY_ACTIVATION_TIME_FROM_NOW_IN_SEC = "activation_time_from_now_in_sec"
    const val KEY_EXPIRATION_TIME_FROM_NOW_IN_SEC = "expiration_time_from_now_in_sec"
    const val KEY_BIDDING_LOGIC_URI = "bidding_logic_uri"
    const val KEY_USER_BIDDING_SIGNALS = "user_bidding_signals"
    const val KEY_TRUSTED_BIDDING_DATA = "trusted_bidding_data"
    const val KEY_DAILY_UPDATE_URI = "daily_update_uri"
    const val KEY_ADS = "ads"
    const val KEY_ADS_URI = "uri"
    const val KEY_ADS_KEYS = "keys"
    const val KEY_ADS_AD_COUNTER_KEYS = "ad_counter_keys"
    const val KEY_ADS_AD_FILTERS = "ad_filters"
    const val KEY_ADS_RENDER_URI = "render_uri"
    const val KEY_ADS_METADATA = "metadata"
    const val KEY_ADS_AD_RENDER_ID = "ad_render_id"
    const val KEY_FCAP = "frequency_cap"
    const val KEY_FCAP_FOR_CLICK_EVENTS = "for_click_events"
    const val KEY_FCAP_FOR_VIEW_EVENTS = "for_view_events"
    const val KEY_FCAP_FOR_IMPRESSION_EVENTS = "for_impression_events"
    const val KEY_FCAP_FOR_WIN_EVENTS = "for_win_events"
    const val KEY_FCAP_AD_COUNTER_KEY = "ad_counter_key"
    const val KEY_FCAP_MAX_COUNT = "max_count"
    const val KEY_FCAP_INTERVAL_IN_SEC = "interval_in_sec"

    const val VARIABLE_BUYER = "{buyer}"
    const val VARIABLE_BUYER_BASE_URI = "{buyer_base_uri}"
  }
}