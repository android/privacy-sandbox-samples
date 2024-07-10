package com.example.adservices.samples.fledge.sampleapp

import android.adservices.common.AdData
import android.adservices.common.AdFilters
import android.adservices.common.AdSelectionSignals
import android.adservices.common.AdTechIdentifier
import android.adservices.common.FrequencyCapFilters
import android.adservices.common.KeyedFrequencyCap
import android.adservices.customaudience.AddCustomAudienceOverrideRequest
import android.adservices.customaudience.CustomAudience
import android.adservices.customaudience.FetchAndJoinCustomAudienceRequest
import android.adservices.customaudience.TrustedBiddingData
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.util.Consumer
import com.example.adservices.samples.fledge.sdkExtensionsHelpers.VersionCompatUtil.isTestableVersion
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Arrays
import java.util.Objects

@SuppressLint("NewApi")
class ConfigFileLoader(
    private val mContext: Context,
    private val mConfig: ConfigUris,
    private val mStatusReceiver: Consumer<String>
) {
    @Throws(IOException::class, JSONException::class)
    private fun parseToJsonObject(inputStream: InputStream): JSONObject {
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        var fileString = String(buffer, StandardCharsets.UTF_8)
        // Before parsing to JSON, make substitutions for base_url and server urls.
        fileString =
            fileString.replace(
                VARIABLE_BUYER,
                Objects.requireNonNull(mConfig.baseUri.host).toString()
            )
        fileString =
            fileString.replace(
                VARIABLE_BASE_URI,
                Objects.requireNonNull(mConfig.baseUri).toString()
            )
        fileString =
            fileString.replace(
                VARIABLE_SERVER_AUCTION_BUYER,
                if (mConfig.isMaybeServerAuction
                ) mConfig.auctionServerBuyer.toString()
                else mConfig.baseUri.host.toString()
            )
        fileString =
            fileString.replace(
                VARIABLE_SERVER_AUCTION_BUYER_BASE_URI,
                Objects.requireNonNull(Uri.parse("https://" + mConfig.auctionServerBuyer))
                    .toString()
            )
        Log.v(MainActivity.TAG, "Loaded JSON file: $fileString")
        return JSONObject(fileString)
    }

    @Throws(JSONException::class, IOException::class)
    fun loadCustomAudienceWithDevOverrides(
        jsonObject: JSONObject
    ): AddCustomAudienceOverrideRequest {
        return AddCustomAudienceOverrideRequest.Builder()
            .setName(jsonObject.getString(NAME))
            .setBuyer(AdTechIdentifier.fromString(jsonObject.getString(BUYER)))
            .setBiddingLogicJs(
                readAssetToString(jsonObject.getString(DEV_OVERRIDES_BIDDING_LOGIC_JS))
            )
            .setTrustedBiddingSignals(
                AdSelectionSignals.fromString(
                    jsonObject
                        .getJSONObject(DEV_OVERRIDES_TRUSTED_BIDDING_SIGNALS)
                        .toString()
                )
            )
            .build()
    }

    @Throws(IOException::class, JSONException::class)
    fun loadCustomAudienceConfigFile(filePath: String?): CustomAudienceConfigFile {
        val inputStream = mContext.assets.open(filePath!!)
        val jsonObject = parseToJsonObject(inputStream)

        val customAudiences = ImmutableList.builder<CustomAudience>()
        if (jsonObject.has(CUSTOM_AUDIENCES_FIELD)) {
            val jsonArray = jsonObject.getJSONArray(CUSTOM_AUDIENCES_FIELD)
            for (i in 0 until jsonArray.length()) {
                try {
                    customAudiences.add(loadCustomAudience(jsonArray.getJSONObject(i)))
                } catch (e: java.lang.RuntimeException) {
                    mStatusReceiver.accept(e.message)
                    Log.w(MainActivity.TAG, e.message!!)
                }
            }
        }

        val fetchCustomAudiences =
            ImmutableList.builder<FetchAndJoinCustomAudienceRequest>()
        if (jsonObject.has(FETCH_CUSTOM_AUDIENCES_FIELD)) {
            val jsonArray = jsonObject.getJSONArray(FETCH_CUSTOM_AUDIENCES_FIELD)
            for (i in 0 until jsonArray.length()) {
                fetchCustomAudiences.add(loadFetchCustomAudience(jsonArray.getJSONObject(i)))
            }
        }

        return CustomAudienceConfigFile(customAudiences.build(), fetchCustomAudiences.build())
    }

    @Throws(IOException::class, JSONException::class)
    fun loadRemoteOverridesConfigFile(filePath: String): RemoteOverridesConfigFile {
        if (!Arrays.asList(*Objects.requireNonNull(mContext.assets.list("")))
                .contains(filePath)
        ) {
            return RemoteOverridesConfigFile(ImmutableList.of(), AdSelectionSignals.EMPTY, "")
        }

        val inputStream = mContext.assets.open(filePath)
        val jsonObject = parseToJsonObject(inputStream)
        val customAudienceOverrides =
            ImmutableList.builder<AddCustomAudienceOverrideRequest>()
        val jsonArray = jsonObject.getJSONArray(CUSTOM_AUDIENCE_DEV_OVERRIDES_FIELD)
        for (i in 0 until jsonArray.length()) {
            customAudienceOverrides.add(
                loadCustomAudienceWithDevOverrides(jsonArray.getJSONObject(i))
            )
        }
        val overrideScoringSignals = AdSelectionSignals.fromString(
            jsonObject.getString(DEV_OVERRIDES_TRUSTED_SCORING_SIGNALS)
        )
        val overrideScoringJs = readAssetToString(
            jsonObject.getString(
                DEV_OVERRIDES_SCORING_LOGIC_JS
            )
        )

        return RemoteOverridesConfigFile(
            customAudienceOverrides.build(), overrideScoringSignals, overrideScoringJs
        )
    }

    @Throws(JSONException::class)
    private fun loadFetchCustomAudience(jsonObject: JSONObject): FetchAndJoinCustomAudienceRequest {
        val builder =
            FetchAndJoinCustomAudienceRequest.Builder(
                Uri.parse(jsonObject.getString(FETCH_URI))
            )
        if (jsonObject.has(NAME)) {
            builder.setName(jsonObject.getString(NAME))
        }
        if (jsonObject.has(ACTIVATION_TIME)) {
            builder.setActivationTime(Instant.parse(jsonObject.getString(ACTIVATION_TIME)))
        }
        if (jsonObject.has(EXPIRATION_TIME)) {
            builder.setExpirationTime(
                Instant.now().plusSeconds(
                    jsonObject.getInt(EXPIRATION_TIME).toLong()
                )
            )
        }
        if (jsonObject.has(USER_BIDDING_SIGNALS)) {
            builder.setUserBiddingSignals(
                AdSelectionSignals.fromString(jsonObject.getString(USER_BIDDING_SIGNALS))
            )
        }
        return builder.build()
    }

    @Throws(IOException::class)
    private fun readAssetToString(assetFileName: String): String {
        val inputStream = mContext.assets.open(assetFileName)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        var fileString = String(buffer, StandardCharsets.UTF_8)
        fileString = fileString.replace(EXAMPLE_REPORTING_URL, mConfig.baseUri.toString())
        return fileString
    }

    @Throws(JSONException::class)
    private fun getAdFromJson(jsonObject: JSONObject): AdData {
        val builder =
            AdData.Builder()
                .setRenderUri(Uri.parse(jsonObject.getString(AD_AD_RENDER_URI)))
                .setMetadata(jsonObject.getString(AD_METADATA))
        if (jsonObject.has(AD_AD_RENDER_ID)) {
            if (isTestableVersion(10, 10)) {
                builder.setAdRenderId(jsonObject.getString(AD_AD_RENDER_ID))
            } else {
                throw RuntimeException(
                    "Unsupported SDK Extension: Ad render id (and server auction) requires 10, skipping")
            }
        }
        if (jsonObject.has(ADS_AD_COUNTER_KEYS)) {
            builder.setAdCounterKeys(
                getIntegersFromJsonArray(jsonObject.getJSONArray(ADS_AD_COUNTER_KEYS))
            )
        }
        if (jsonObject.has(ADS_AD_FILTERS) && jsonObject.getJSONObject(ADS_AD_FILTERS)
                .has(FCAP)
        ) {
            if (isTestableVersion(8, 9)) {
                builder.setAdFilters(
                    AdFilters.Builder()
                        .setFrequencyCapFilters(
                            getFrequencyCapFilters(
                                jsonObject
                                    .getJSONObject(ADS_AD_FILTERS)
                                    .getJSONObject("frequency_cap")
                            )
                        )
                        .build()
                )
            } else {
                throw RuntimeException(
                    "Unsupported SDK Extension: Ad filters require 8 for T+ or 9 for S-, skipping")
            }
        }
        return builder.build()
    }

    @Throws(JSONException::class)
    fun loadCustomAudience(jsonObject: JSONObject): CustomAudience {
        val builder =
            CustomAudience.Builder()
                .setName(jsonObject.getString(NAME))
                .setBuyer(AdTechIdentifier.fromString(jsonObject.getString(BUYER)))
                .setBiddingLogicUri(Uri.parse(jsonObject.getString(BIDDING_LOGIC_URI)))
                .setDailyUpdateUri(Uri.parse(jsonObject.getString(DAILY_UPDATE_URI)))
        if (jsonObject.has(TRUSTED_BIDDING_DATA)) {
            builder.setTrustedBiddingData(
                getTrustedBiddingDataFromJson(jsonObject.getJSONObject(TRUSTED_BIDDING_DATA))
            )
        }
        if (jsonObject.has(USER_BIDDING_SIGNALS)) {
            builder.setUserBiddingSignals(
                AdSelectionSignals.fromString(jsonObject.getString(USER_BIDDING_SIGNALS))
            )
        }
        if (jsonObject.has(ADS)) {
            builder.setAds(getAdsFromJsonArray(jsonObject.getJSONArray(ADS)))
        }
        if (jsonObject.has(EXPIRATION_TIME)) {
            builder.setExpirationTime(
                Instant.now().plusSeconds(
                    jsonObject.getInt(EXPIRATION_TIME).toLong()
                )
            )
        }
        if (jsonObject.has(ACTIVATION_TIME)) {
            builder.setActivationTime(
                Instant.now().plusSeconds(
                    jsonObject.getInt(ACTIVATION_TIME).toLong()
                )
            )
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

    companion object {
        private const val CUSTOM_AUDIENCES_FIELD = "customAudiences"
        private const val CUSTOM_AUDIENCE_DEV_OVERRIDES_FIELD = "customAudienceDevOverrides"
        private const val FETCH_CUSTOM_AUDIENCES_FIELD = "fetchAndJoinCustomAudiences"
        private const val FETCH_URI = "fetch_uri"
        private const val NAME = "name"
        private const val BUYER = "buyer"
        private const val ACTIVATION_TIME = "activation_time_from_now_in_sec"
        private const val EXPIRATION_TIME = "expiration_time_from_now_in_sec"
        private const val BIDDING_LOGIC_URI = "bidding_logic_uri"
        private const val USER_BIDDING_SIGNALS = "user_bidding_signals"
        private const val TRUSTED_BIDDING_DATA = "trusted_bidding_data"
        private const val DAILY_UPDATE_URI = "daily_update_uri"
        private const val ADS = "ads"
        private const val ADS_URI = "uri"
        private const val ADS_KEYS = "keys"
        private const val ADS_AD_COUNTER_KEYS = "ad_counter_keys"
        private const val ADS_AD_FILTERS = "ad_filters"
        private const val AD_AD_RENDER_URI = "render_uri"
        private const val AD_METADATA = "metadata"
        private const val AD_AD_RENDER_ID = "ad_render_id"
        private const val DEV_OVERRIDES_TRUSTED_BIDDING_SIGNALS = "trusted_bidding_signals"
        private const val DEV_OVERRIDES_BIDDING_LOGIC_JS = "bidding_logic_js_asset"
        private const val DEV_OVERRIDES_TRUSTED_SCORING_SIGNALS = "trusted_scoring_signals"
        private const val DEV_OVERRIDES_SCORING_LOGIC_JS = "scoring_logic_js_asset"
        const val EXAMPLE_REPORTING_URL: String = "https://reporting.example.com"
        private const val FCAP: String = "frequency_cap"
        private const val FCAP_CLICK_EVENTS: String = "for_click_events"
        private const val FCAP_VIEW_EVENTS: String = "for_view_events"
        private const val FCAP_IMPRESSION_EVENTS: String = "for_impression_events"
        private const val FCAP_WIN_EVENTS: String = "for_win_events"
        private const val FCAP_AD_COUNTER_KEY: String = "ad_counter_key"
        private const val FCAP_MAX_COUNT: String = "max_count"
        private const val INTERVAL_IN_SEC_FIELD: String = "interval_in_sec"
        private const val VARIABLE_BUYER: String = "{buyer}"
        private const val VARIABLE_BASE_URI: String = "{base_uri}"
        private const val VARIABLE_SERVER_AUCTION_BUYER: String = "{server_auction_buyer}"
        private const val VARIABLE_SERVER_AUCTION_BUYER_BASE_URI: String =
            "{server_auction_buyer_base_uri}"


        @Throws(JSONException::class)
        private fun getTrustedBiddingDataFromJson(jsonObject: JSONObject): TrustedBiddingData {
            return TrustedBiddingData.Builder()
                .setTrustedBiddingUri(Uri.parse(jsonObject.getString(ADS_URI)))
                .setTrustedBiddingKeys(getStringsFromJsonArray(jsonObject.getJSONArray(ADS_KEYS)))
                .build()
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
        private fun getFrequencyCapFilters(jsonObject: JSONObject): FrequencyCapFilters {
            val builder = FrequencyCapFilters.Builder()
            if (jsonObject.has(FCAP_CLICK_EVENTS)) {
                builder.setKeyedFrequencyCapsForClickEvents(
                    getKeyedFrequencyCapList(jsonObject.getJSONArray(FCAP_CLICK_EVENTS))
                )
            }
            if (jsonObject.has(FCAP_VIEW_EVENTS)) {
                builder.setKeyedFrequencyCapsForViewEvents(
                    getKeyedFrequencyCapList(jsonObject.getJSONArray(FCAP_VIEW_EVENTS))
                )
            }
            if (jsonObject.has(FCAP_IMPRESSION_EVENTS)) {
                builder.setKeyedFrequencyCapsForImpressionEvents(
                    getKeyedFrequencyCapList(jsonObject.getJSONArray(FCAP_IMPRESSION_EVENTS))
                )
            }
            if (jsonObject.has(FCAP_WIN_EVENTS)) {
                builder.setKeyedFrequencyCapsForWinEvents(
                    getKeyedFrequencyCapList(jsonObject.getJSONArray(FCAP_WIN_EVENTS))
                )
            }
            return builder.build()
        }

        @Throws(JSONException::class)
        private fun getKeyedFrequencyCapList(jsonArray: JSONArray): List<KeyedFrequencyCap> {
            val builder = ImmutableList.builder<KeyedFrequencyCap>()
            for (i in 0 until jsonArray.length()) {
                builder.add(getKeyedFrequencyCap(jsonArray.getJSONObject(i)))
            }
            return builder.build()
        }

        @Throws(JSONException::class)
        private fun getKeyedFrequencyCap(jsonObject: JSONObject): KeyedFrequencyCap {
            return KeyedFrequencyCap.Builder(
                jsonObject.getInt(FCAP_AD_COUNTER_KEY),
                jsonObject.getInt(FCAP_MAX_COUNT),
                Duration.ofSeconds(jsonObject.getInt(INTERVAL_IN_SEC_FIELD).toLong())
            )
                .build()
        }

        @Throws(JSONException::class)
        private fun getIntegersFromJsonArray(jsonArray: JSONArray): ImmutableSet<Int> {
            val builder = ImmutableSet.builder<Int>()
            for (i in 0 until jsonArray.length()) {
                builder.add(jsonArray.getInt(i))
            }
            return builder.build()
        }
    }
}
