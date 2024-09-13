/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.example.adservices.samples.fledge.sampleapp;

import static com.example.adservices.samples.fledge.SdkExtensionsHelpers.VersionCompatUtil.isTestableVersion;
import static com.example.adservices.samples.fledge.sampleapp.MainActivity.TAG;

import android.adservices.common.AdData;
import android.adservices.common.AdFilters;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.adservices.common.FrequencyCapFilters;
import android.adservices.common.KeyedFrequencyCap;
import android.adservices.customaudience.AddCustomAudienceOverrideRequest;
import android.adservices.customaudience.CustomAudience;
import android.adservices.customaudience.FetchAndJoinCustomAudienceRequest;
import android.adservices.customaudience.TrustedBiddingData;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** CLass to read the contents of the config file for Custom Audiences and Remote Overrides. */
@SuppressLint("NewApi")
public class ConfigFileLoader {
    private static final String CUSTOM_AUDIENCES_FIELD = "customAudiences";
    private static final String CUSTOM_AUDIENCE_DEV_OVERRIDES_FIELD = "customAudienceDevOverrides";
    private static final String FETCH_CUSTOM_AUDIENCES_FIELD = "fetchAndJoinCustomAudiences";
    private static final String FETCH_URI = "fetch_uri";
    private static final String LABEL_NAME = "label_name";
    private static final String NAME = "name";
    private static final String BUYER = "buyer";
    private static final String ACTIVATION_TIME = "activation_time_from_now_in_sec";
    private static final String EXPIRATION_TIME = "expiration_time_from_now_in_sec";
    private static final String BIDDING_LOGIC_URI = "bidding_logic_uri";
    private static final String USER_BIDDING_SIGNALS = "user_bidding_signals";
    private static final String TRUSTED_BIDDING_DATA = "trusted_bidding_data";
    private static final String DAILY_UPDATE_URI = "daily_update_uri";
    private static final String ADS = "ads";
    private static final String ADS_URI = "uri";
    private static final String ADS_KEYS = "keys";
    private static final String ADS_AD_COUNTER_KEYS = "ad_counter_keys";
    private static final String ADS_AD_FILTERS = "ad_filters";
    private static final String AD_AD_RENDER_URI = "render_uri";
    private static final String AD_METADATA = "metadata";
    private static final String AD_AD_RENDER_ID = "ad_render_id";
    private static final String DEV_OVERRIDES_TRUSTED_BIDDING_SIGNALS = "trusted_bidding_signals";
    private static final String DEV_OVERRIDES_BIDDING_LOGIC_JS = "bidding_logic_js_asset";
    private static final String DEV_OVERRIDES_TRUSTED_SCORING_SIGNALS = "trusted_scoring_signals";
    private static final String DEV_OVERRIDES_SCORING_LOGIC_JS = "scoring_logic_js_asset";
    public static final String EXAMPLE_REPORTING_URL = "https://reporting.example.com";
    public static final String FCAP = "frequency_cap";
    public static final String FCAP_CLICK_EVENTS = "for_click_events";
    public static final String FCAP_VIEW_EVENTS = "for_view_events";
    public static final String FCAP_IMPRESSION_EVENTS = "for_impression_events";
    public static final String FCAP_WIN_EVENTS = "for_win_events";
    public static final String FCAP_AD_COUNTER_KEY = "ad_counter_key";
    public static final String FCAP_MAX_COUNT = "max_count";
    public static final String INTERVAL_IN_SEC_FIELD = "interval_in_sec";
    public static final String VARIABLE_BUYER = "{buyer}";
    public static final String VARIABLE_BASE_URI_BUYER = "{base_uri}";
    public static final String VARIABLE_SERVER_AUCTION_BUYER = "{server_auction_buyer}";
    public static final String VARIABLE_SERVER_AUCTION_BUYER_BASE_URI =
            "{server_auction_buyer_base_uri}";
    private final Context mContext;
    private final ConfigUris mConfig;
    private final Consumer<String> mStatusReceiver;

    /**
     * Default constructor,
     *
     * @param context the context
     * @param config configuration URLs.
     */
    public ConfigFileLoader(Context context, ConfigUris config, Consumer<String> statusReceiver) {
        this.mContext = context;
        this.mConfig = config;
        this.mStatusReceiver = statusReceiver;
    }

    /**
     * @param filePath the full path of the configuration file for custom audiences.
     * @return CustomAudienceConfigFile which consists of all parsed custom audience data.
     */
    public CustomAudienceConfigFile loadCustomAudienceConfigFile(String filePath)
            throws IOException, JSONException {
        InputStream inputStream = mContext.getAssets().open(filePath);
        JSONObject jsonObject = parseToJsonObject(inputStream);

        LinkedHashMap<String, CustomAudience> customAudienceHashMap = new LinkedHashMap<>();
        if (jsonObject.has(CUSTOM_AUDIENCES_FIELD)) {
            JSONArray jsonArray = jsonObject.getJSONArray(CUSTOM_AUDIENCES_FIELD);
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    Pair<String, CustomAudience> loadedCaData =
                            loadCustomAudience(jsonArray.getJSONObject(i));
                    customAudienceHashMap.put(loadedCaData.first, loadedCaData.second);
                } catch (RuntimeException e) {
                    mStatusReceiver.accept(e.getMessage());
                    Log.w(TAG, Objects.requireNonNull(e.getMessage()));
                }
            }
        }

        LinkedHashMap<String, FetchAndJoinCustomAudienceRequest>
                fetchAndJoinCustomAudienceRequestMap = new LinkedHashMap<>();
        if (jsonObject.has(FETCH_CUSTOM_AUDIENCES_FIELD)) {
            JSONArray jsonArray = jsonObject.getJSONArray(FETCH_CUSTOM_AUDIENCES_FIELD);
            for (int i = 0; i < jsonArray.length(); i++) {
                Pair<String, FetchAndJoinCustomAudienceRequest> loadedFetchCaData =
                        loadFetchCustomAudience(jsonArray.getJSONObject(i));
                fetchAndJoinCustomAudienceRequestMap.put(
                        loadedFetchCaData.first, loadedFetchCaData.second);
            }
        }

        return new CustomAudienceConfigFile(
                customAudienceHashMap, fetchAndJoinCustomAudienceRequestMap);
    }

    /**
     * @param filePath the full path of the configuration file fir remote overrides.
     * @return RemoteOverridesConfigFile which consists of all parsed remote overrides data.
     */
    public RemoteOverridesConfigFile loadRemoteOverridesConfigFile(String filePath)
            throws IOException, JSONException {
        if (!Arrays.asList(Objects.requireNonNull(mContext.getAssets().list("")))
                .contains(filePath)) {
            return new RemoteOverridesConfigFile(ImmutableList.of(), AdSelectionSignals.EMPTY, "");
        }

        InputStream inputStream = mContext.getAssets().open(filePath);
        JSONObject jsonObject = parseToJsonObject(inputStream);

        AdSelectionSignals overrideScoringSignals;
        String overrideScoringJs;
        ImmutableList.Builder<AddCustomAudienceOverrideRequest> customAudienceOverrides =
                ImmutableList.builder();
        JSONArray jsonArray = jsonObject.getJSONArray(CUSTOM_AUDIENCE_DEV_OVERRIDES_FIELD);
        for (int i = 0; i < jsonArray.length(); i++) {
            customAudienceOverrides.add(
                    loadCustomAudienceWithDevOverrides(jsonArray.getJSONObject(i)));
        }
        overrideScoringSignals =
                AdSelectionSignals.fromString(
                        jsonObject.getString(DEV_OVERRIDES_TRUSTED_SCORING_SIGNALS));
        overrideScoringJs = readAssetToString(jsonObject.getString(DEV_OVERRIDES_SCORING_LOGIC_JS));

        return new RemoteOverridesConfigFile(
                customAudienceOverrides.build(), overrideScoringSignals, overrideScoringJs);
    }

    private Pair<String, CustomAudience> loadCustomAudience(@NonNull JSONObject jsonObject)
            throws JSONException {
        String labelName = jsonObject.getString(LABEL_NAME);
        if (labelName.isEmpty()) {
            throw new IllegalStateException(
                    String.format(
                            "%s should be present in the configuration file to parse custom"
                                + " audience data",
                            LABEL_NAME));
        }
        CustomAudience.Builder builder =
                new CustomAudience.Builder()
                        .setName(jsonObject.getString(NAME))
                        .setBuyer(AdTechIdentifier.fromString(jsonObject.getString(BUYER)))
                        .setBiddingLogicUri(Uri.parse(jsonObject.getString(BIDDING_LOGIC_URI)))
                        .setDailyUpdateUri(Uri.parse(jsonObject.getString(DAILY_UPDATE_URI)));
        if (jsonObject.has(TRUSTED_BIDDING_DATA)) {
            builder.setTrustedBiddingData(
                    getTrustedBiddingDataFromJson(jsonObject.getJSONObject(TRUSTED_BIDDING_DATA)));
        }
        if (jsonObject.has(USER_BIDDING_SIGNALS)) {
            builder.setUserBiddingSignals(
                    AdSelectionSignals.fromString(jsonObject.getString(USER_BIDDING_SIGNALS)));
        }
        if (jsonObject.has(ADS)) {
            builder.setAds(getAdsFromJsonArray(jsonObject.getJSONArray(ADS)));
        }
        if (jsonObject.has(EXPIRATION_TIME)) {
            builder.setExpirationTime(
                    calculateRelativeTime(Duration.ofSeconds(jsonObject.getInt(EXPIRATION_TIME))));
        }
        if (jsonObject.has(ACTIVATION_TIME)) {
            builder.setActivationTime(
                    calculateRelativeTime(Duration.ofSeconds(jsonObject.getInt(ACTIVATION_TIME))));
        }
        return Pair.create(labelName, builder.build());
    }

    private TrustedBiddingData getTrustedBiddingDataFromJson(JSONObject jsonObject)
            throws JSONException {
        return new TrustedBiddingData.Builder()
                .setTrustedBiddingUri(Uri.parse(jsonObject.getString(ADS_URI)))
                .setTrustedBiddingKeys(getStringsFromJsonArray(jsonObject.getJSONArray(ADS_KEYS)))
                .build();
    }

    private ImmutableList<String> getStringsFromJsonArray(JSONArray jsonArray)
            throws JSONException {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (int i = 0; i < jsonArray.length(); i++) {
            builder.add(jsonArray.getString(i));
        }
        return builder.build();
    }

    private ImmutableList<AdData> getAdsFromJsonArray(JSONArray jsonArray) throws JSONException {
        ImmutableList.Builder<AdData> builder = ImmutableList.builder();
        for (int i = 0; i < jsonArray.length(); i++) {
            builder.add(getAdFromJson(jsonArray.getJSONObject(i)));
        }
        return builder.build();
    }

    private AdData getAdFromJson(JSONObject jsonObject) throws JSONException {
        AdData.Builder builder =
                new AdData.Builder()
                        .setRenderUri(Uri.parse(jsonObject.getString(AD_AD_RENDER_URI)))
                        .setMetadata(jsonObject.getString(AD_METADATA));
        if (jsonObject.has(AD_AD_RENDER_ID)) {
            if (isTestableVersion(10, 10)) {
                builder.setAdRenderId(jsonObject.getString(AD_AD_RENDER_ID));
            } else {
                throw new RuntimeException(
                        "Unsupported SDK Extension: Ad render id (and server auction) requires 10,"
                            + " skipping");
            }
        }
        if (jsonObject.has(ADS_AD_COUNTER_KEYS)) {
            builder.setAdCounterKeys(
                    getIntegersFromJsonArray(jsonObject.getJSONArray(ADS_AD_COUNTER_KEYS)));
        }
        if (jsonObject.has(ADS_AD_FILTERS) && jsonObject.getJSONObject(ADS_AD_FILTERS).has(FCAP)) {
            if (isTestableVersion(8, 9)) {
                builder.setAdFilters(
                        new AdFilters.Builder()
                                .setFrequencyCapFilters(
                                        getFrequencyCapFilters(
                                                jsonObject
                                                        .getJSONObject(ADS_AD_FILTERS)
                                                        .getJSONObject("frequency_cap")))
                                .build());
            } else {
                throw new RuntimeException(
                        "Unsupported SDK Extension: Ad filters require 8 for T+ or 9 for S-,"
                            + " skipping");
            }
        }
        return builder.build();
    }

    private FrequencyCapFilters getFrequencyCapFilters(JSONObject jsonObject) throws JSONException {
        FrequencyCapFilters.Builder builder = new FrequencyCapFilters.Builder();
        if (jsonObject.has(FCAP_CLICK_EVENTS)) {
            builder.setKeyedFrequencyCapsForClickEvents(
                    getKeyedFrequencyCapList(jsonObject.getJSONArray(FCAP_CLICK_EVENTS)));
        }
        if (jsonObject.has(FCAP_VIEW_EVENTS)) {
            builder.setKeyedFrequencyCapsForViewEvents(
                    getKeyedFrequencyCapList(jsonObject.getJSONArray(FCAP_VIEW_EVENTS)));
        }
        if (jsonObject.has(FCAP_IMPRESSION_EVENTS)) {
            builder.setKeyedFrequencyCapsForImpressionEvents(
                    getKeyedFrequencyCapList(jsonObject.getJSONArray(FCAP_IMPRESSION_EVENTS)));
        }
        if (jsonObject.has(FCAP_WIN_EVENTS)) {
            builder.setKeyedFrequencyCapsForWinEvents(
                    getKeyedFrequencyCapList(jsonObject.getJSONArray(FCAP_WIN_EVENTS)));
        }
        return builder.build();
    }

    private List<KeyedFrequencyCap> getKeyedFrequencyCapList(JSONArray jsonArray)
            throws JSONException {
        ImmutableList.Builder<KeyedFrequencyCap> builder = ImmutableList.builder();
        for (int i = 0; i < jsonArray.length(); i++) {
            builder.add(getKeyedFrequencyCap(jsonArray.getJSONObject(i)));
        }
        return builder.build();
    }

    private KeyedFrequencyCap getKeyedFrequencyCap(JSONObject jsonObject) throws JSONException {
        return new KeyedFrequencyCap.Builder(
                        jsonObject.getInt(FCAP_AD_COUNTER_KEY),
                        jsonObject.getInt(FCAP_MAX_COUNT),
                        Duration.ofSeconds(jsonObject.getInt(INTERVAL_IN_SEC_FIELD)))
                .build();
    }

    private ImmutableSet<Integer> getIntegersFromJsonArray(JSONArray jsonArray)
            throws JSONException {
        ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
        for (int i = 0; i < jsonArray.length(); i++) {
            builder.add(jsonArray.getInt(i));
        }
        return builder.build();
    }

    /**
     * Returns a point in time at {@code duration} in the future from now
     *
     * @param duration Amount of time in the future
     */
    private Instant calculateRelativeTime(Duration duration) {
        return Instant.now().plus(duration);
    }

    private JSONObject parseToJsonObject(InputStream inputStream)
            throws IOException, JSONException {
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        inputStream.read(buffer);
        inputStream.close();
        String fileString = new String(buffer, StandardCharsets.UTF_8);
        // Before parsing to JSON, make substitutions for base_url and server urls.
        fileString =
                fileString.replace(
                        VARIABLE_BASE_URI_BUYER,
                        Objects.requireNonNull(mConfig.getBaseUri().toString()));
        fileString =
                fileString.replace(
                        VARIABLE_BUYER, Objects.requireNonNull(mConfig.getBaseUri().getHost()));
        fileString =
                fileString.replace(
                        VARIABLE_SERVER_AUCTION_BUYER,
                        mConfig.isMaybeServerAuction()
                                ? mConfig.getAuctionServerBuyer().toString()
                                : mConfig.getBaseUri().getHost());
        fileString =
                fileString.replace(
                        VARIABLE_SERVER_AUCTION_BUYER_BASE_URI,
                        mConfig.isMaybeServerAuction()
                                ? "https://" + mConfig.getAuctionServerBuyer()
                                : Objects.requireNonNull(mConfig.getBaseUri().toString()));
        Log.v(MainActivity.TAG, "Loaded JSON file: " + fileString);
        return new JSONObject(fileString);
    }

    private AddCustomAudienceOverrideRequest loadCustomAudienceWithDevOverrides(
            @NonNull JSONObject jsonObject) throws JSONException, IOException {
        return new AddCustomAudienceOverrideRequest.Builder()
                .setName(jsonObject.getString(NAME))
                .setBuyer(AdTechIdentifier.fromString(jsonObject.getString(BUYER)))
                .setBiddingLogicJs(
                        readAssetToString(jsonObject.getString(DEV_OVERRIDES_BIDDING_LOGIC_JS)))
                .setTrustedBiddingSignals(
                        AdSelectionSignals.fromString(
                                jsonObject
                                        .getJSONObject(DEV_OVERRIDES_TRUSTED_BIDDING_SIGNALS)
                                        .toString()))
                .build();
    }

    private Pair<String, FetchAndJoinCustomAudienceRequest> loadFetchCustomAudience(
            JSONObject jsonObject) throws JSONException {
        FetchAndJoinCustomAudienceRequest.Builder builder =
                new FetchAndJoinCustomAudienceRequest.Builder(
                        Uri.parse(jsonObject.getString(FETCH_URI)));
        String labelName = jsonObject.getString(LABEL_NAME);
        if (labelName.isEmpty()) {
            throw new IllegalStateException(
                    String.format(
                            "%s should be present in the configuration file to parse custom"
                                + " audience data",
                            LABEL_NAME));
        }
        if (jsonObject.has(NAME)) {
            builder.setName(jsonObject.getString(NAME));
        }
        if (jsonObject.has(ACTIVATION_TIME)) {
            builder.setActivationTime(
                    calculateRelativeTime(Duration.ofSeconds(jsonObject.getInt(ACTIVATION_TIME))));
        }
        if (jsonObject.has(EXPIRATION_TIME)) {
            builder.setExpirationTime(
                    calculateRelativeTime(Duration.ofSeconds(jsonObject.getInt(EXPIRATION_TIME))));
        }
        if (jsonObject.has(USER_BIDDING_SIGNALS)) {
            builder.setUserBiddingSignals(
                    AdSelectionSignals.fromString(jsonObject.getString(USER_BIDDING_SIGNALS)));
        }
        return Pair.create(labelName, builder.build());
    }

    private String readAssetToString(String assetFileName) throws IOException {
        InputStream inputStream = mContext.getAssets().open(assetFileName);
        int size = inputStream.available();
        byte[] buffer = new byte[size];
        inputStream.read(buffer);
        inputStream.close();
        String fileString = new String(buffer, StandardCharsets.UTF_8);
        fileString = fileString.replace(EXAMPLE_REPORTING_URL, mConfig.getBaseUri().toString());
        return fileString;
    }
}
