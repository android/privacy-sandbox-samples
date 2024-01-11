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
import android.adservices.customaudience.TrustedBiddingData;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.example.adservices.samples.fledge.clients.CustomAudienceClient;
import com.example.adservices.samples.fledge.clients.TestCustomAudienceClient;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.json.JSONObject;

/**
 * Wrapper for the FLEDGE Custom Audience (CA) API. Creating the wrapper locks the user into a given
 * owner and buyer. In order to interact with the wrapper they will first need to call the create
 * method to create a CA object. After that they can call joinCA and leaveCA.
 */
@RequiresApi(api = 34)
public class CustomAudienceWrapper {
  private final Executor mExecutor;
  private final CustomAudienceClient mCaClient;
  private final TestCustomAudienceClient mCaOverrideClient;

  /**
   * Initialize the custom audience wrapper and set the owner and buyer.
   *
   * @param context The application context.
   * @param executor An executor to use with the FLEDGE API calls.
   */
  public CustomAudienceWrapper(Context context, Executor executor) {
    mExecutor = executor;
    mCaClient =
        new CustomAudienceClient.Builder().setContext(context).setExecutor(executor).build();
    mCaOverrideClient =
        new TestCustomAudienceClient.Builder().setContext(context).setExecutor(executor).build();
  }


  /**
   * Joins a CA.
   *
   * @param name The name of the CA to join.
   * @param owner The owner of the CA
   * @param buyer The buyer of ads
   * @param biddingUri The URL to retrieve the bidding logic
   * @param renderUri The URL to render the ad
   * @param dailyUpdateUri The URL for daily updates for the CA
   * @param trustedBiddingUri The URL to retrieve trusted bidding data
   * @param statusReceiver A consumer function that is run after the API call and returns a string
   *     indicating the outcome of the call.
   */
  public void joinCa(
      String name,
      String owner,
      AdTechIdentifier buyer,
      Uri biddingUri,
      Uri renderUri,
      Uri dailyUpdateUri,
      Uri trustedBiddingUri,
      Consumer<String> statusReceiver,
      Instant expiry) {
    try {
      AdData adData = getAdDataBuilder(renderUri).build();
      joinCustomAudience(
          getCustomAudienceBuilder(
              name, buyer, biddingUri, adData, dailyUpdateUri, trustedBiddingUri, expiry).build(),
          statusReceiver);
    } catch (Exception e) {
      statusReceiver.accept(
          "Got the following exception when trying to join " + name + " custom audience: " + e);
      Log.e(TAG, "Exception calling joinCustomAudience", e);
    }
  }

  @SuppressLint("NewApi")
  public void joinServerAuctionCa(String name, AdTechIdentifier buyer, Uri biddingUri,
      Uri renderUri, Uri dailyUpdateUri, Uri trustedBiddingUri, Consumer<String> statusReceiver,
      Instant expiry, int adRenderId) {
    try {
      if (!isTestableVersion(10, 10)) {
        statusReceiver.accept(
            "Unsupported SDK Extension: Joining Server Auction CAs requires 10, skipping");
        Log.w(
            MainActivity.TAG,
            "Unsupported SDK Extension: Joining Server Auction CAs requires 10, skipping");
        return;
      }

      int clickFilterMax = 1;
      Duration oneDayDuration = Duration.ofDays(1);
      KeyedFrequencyCap clickCap =
          new KeyedFrequencyCap.Builder(adRenderId, clickFilterMax, oneDayDuration).build();
      FrequencyCapFilters fCapFilters =
          new FrequencyCapFilters.Builder().setKeyedFrequencyCapsForClickEvents(Collections.singletonList(clickCap)).build();
      AdData adData = getAdDataBuilder(renderUri)
          .setAdRenderId(String.valueOf(adRenderId))
          .setAdCounterKeys(Collections.singleton(adRenderId))
          .setAdFilters(new AdFilters.Builder().setFrequencyCapFilters(fCapFilters).build())
          .build();
      Log.v(TAG, String.format("AdRenderId %s is set for ad render uri %s", adRenderId, renderUri));
      joinCustomAudience(
          getCustomAudienceBuilder(
              name, buyer, biddingUri, adData, dailyUpdateUri, trustedBiddingUri, expiry)
              .build(),
          statusReceiver);
    } catch (Exception e) {
      statusReceiver.accept("Got the following exception when trying to join " + name
          + " custom audience: " + e);
      Log.e(TAG, "Exception calling joinCustomAudience", e);
    }
  }

  /**
   * Joins a CA.
   *
   * @param name The name of the CA to join.
   * @param buyer The buyer of ads
   * @param biddingUri The URL to retrieve the bidding logic
   * @param renderUri The URL to render the ad
   * @param dailyUpdateUri The URL for daily updates for the CA
   * @param trustedBiddingUri The URL to retrieve trusted bidding data
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * @param expiry The time when the CA will expire
   * @param filters {@link AdFilters} that should be applied to the ad in the CA
   * @param adCounterKeys set of keys used in counting events string indicating the outcome of the
   *     call.
   */
  @SuppressLint("NewApi")
  public void joinFilteringCa(
      String name,
      AdTechIdentifier buyer,
      Uri biddingUri,
      Uri renderUri,
      Uri dailyUpdateUri,
      Uri trustedBiddingUri,
      Consumer<String> statusReceiver,
      Instant expiry,
      AdFilters filters,
      Set<Integer> adCounterKeys) {
    if (!isTestableVersion(8, 9)) {
      statusReceiver.accept("Unsupported SDK Extension: Frequency cap CA requires 8 for T+ or 9 for S-, skipping");
      Log.w(TAG, "Unsupported SDK Extension: Frequency cap CA requires 8 for T+ or 9 for S-, skipping");
      return;
    }

    try {
      joinCustomAudience(
          new CustomAudience.Builder()
              .setBuyer(buyer)
              .setName(name)
              .setDailyUpdateUri(dailyUpdateUri)
              .setBiddingLogicUri(biddingUri)
              .setAds(
                  Collections.singletonList(
                      new AdData.Builder()
                          .setRenderUri(renderUri)
                          .setMetadata(new JSONObject().toString())
                          .setAdFilters(filters)
                          .setAdCounterKeys(adCounterKeys)
                          .build()))
              .setActivationTime(Instant.now())
              .setExpirationTime(expiry)
              .setTrustedBiddingData(
                  new TrustedBiddingData.Builder()
                      .setTrustedBiddingKeys(Collections.singletonList("key"))
                      .setTrustedBiddingUri(trustedBiddingUri)
                      .build())
              .setUserBiddingSignals(AdSelectionSignals.EMPTY)
              .build(),
          statusReceiver);
    } catch (Exception e) {
      statusReceiver.accept(
          "Got the following exception when trying to join " + name + " custom audience: " + e);
      Log.e(TAG, "Exception calling joinCustomAudience", e);
    }
  }

  /**
   * Creates a CA with empty user bidding signals, trusted bidding data, and ads.
   *
   * @param name The name of the CA to join.
   * @param owner The owner of the CA
   * @param buyer The buyer of ads
   * @param biddingUri The URL to retrieve the bidding logic
   * @param dailyUpdateUri The URL for daily updates for the CA
   * @param statusReceiver A consumer function that is run after the API call and returns a
   */
  public void joinEmptyFieldsCa(
      String name,
      String owner,
      AdTechIdentifier buyer,
      Uri biddingUri,
      Uri dailyUpdateUri,
      Consumer<String> statusReceiver,
      Instant expiry) {
    try {
      joinCustomAudience(
          new CustomAudience.Builder()
              .setBuyer(buyer)
              .setName(name)
              .setDailyUpdateUri(dailyUpdateUri)
              .setBiddingLogicUri(biddingUri)
              .setActivationTime(Instant.now())
              .setExpirationTime(expiry)
              .build(),
          statusReceiver);
    } catch (Exception e) {
      statusReceiver.accept(
          "Got the following exception when trying to join " + name + " custom audience: " + e);
      Log.e(TAG, "Exception calling joinCustomAudience", e);
    }
  }

  /**
   * Fetches and joins a CA with from an URI.
   *
   * @param fetchUri The URL to retrieve the CA from.
   * @param name The name of the CA to join.
   * @param activationTime The time when the CA will activate.
   * @param expirationTime The time when the CA will expire.
   * @param userBiddingSignals The user bidding signals used at auction.
   * @param statusReceiver A consumer function that is run after the API call and returns a string.
   */
  public void fetchAndJoinCa(
      Uri fetchUri,
      String name,
      Instant activationTime,
      Instant expirationTime,
      AdSelectionSignals userBiddingSignals,
      Consumer<String> statusReceiver) {
    if (!isTestableVersion(10, 10)) {
      statusReceiver.accept(
          "Unsupported SDK Extension: The fetchAndJoinCustomAudience API requires 10, skipping");
      Log.w(
          MainActivity.TAG,
          "Unsupported SDK Extension: The fetchAndJoinCustomAudience API requires 10, skipping");
      return;
    }
    try {
      Futures.addCallback(
          mCaClient.fetchAndJoinCustomAudience(
              fetchUri, name, activationTime, expirationTime, userBiddingSignals),
          new FutureCallback<Void>() {
            public void onSuccess(Void unused) {
              statusReceiver.accept("Fetched and joined " + name + " custom audience.");
            }

            public void onFailure(@NonNull Throwable e) {
              statusReceiver.accept(
                  "Error when fetching and joining "
                      + name
                      + " custom audience: "
                      + e.getMessage());
            }
          },
          mExecutor);
    } catch (Exception e) {
      statusReceiver.accept(
          "Got the following exception when trying to fetch and join"
              + name
              + " custom audience: "
              + e);
      Log.e(MainActivity.TAG, "Exception calling fetchAndJoinCustomAudience", e);
    }
  }

  /**
   * Leaves a CA.
   *
   * @param name The name of the CA to leave.
   * @param statusReceiver A consumer function that is run after the API call and returns a string
   *     indicating the outcome of the call.
   */
  public void leaveCa(
      String name, String owner, AdTechIdentifier buyer, Consumer<String> statusReceiver) {
    try {
      Futures.addCallback(
          mCaClient.leaveCustomAudience(owner, buyer, name),
          new FutureCallback<Void>() {
            public void onSuccess(Void unused) {
              statusReceiver.accept("Left " + name + " custom audience");
            }

            public void onFailure(@NonNull Throwable e) {
              statusReceiver.accept(
                  "Error when leaving " + name + " custom audience: " + e.getMessage());
            }
          },
          mExecutor);
    } catch (Exception e) {
      statusReceiver.accept(
          "Got the following exception when trying to leave " + name + " custom audience: " + e);
      Log.e(TAG, "Exception calling leaveCustomAudience", e);
    }
  }

  /**
   * Overrides remote info for a CA.
   *
   * @param name The name of the CA to override remote info.
   * @param biddingLogicJs The overriding bidding logic javascript
   * @param trustedBiddingSignals The overriding trusted bidding signals
   * @param statusReceiver A consumer function that is run after the API call and returns a string
   *     indicating the outcome of the call.
   */
  public void addCAOverride(
      String name,
      String owner,
      AdTechIdentifier buyer,
      String biddingLogicJs,
      AdSelectionSignals trustedBiddingSignals,
      Consumer<String> statusReceiver) {
    try {
      AddCustomAudienceOverrideRequest request =
          new AddCustomAudienceOverrideRequest.Builder()
              .setBuyer(buyer)
              .setName(name)
              .setBiddingLogicJs(biddingLogicJs)
              .setTrustedBiddingSignals(trustedBiddingSignals)
              .build();
      Futures.addCallback(
          mCaOverrideClient.overrideCustomAudienceRemoteInfo(request),
          new FutureCallback<Void>() {
            public void onSuccess(Void unused) {
              statusReceiver.accept("Added override for " + name + " custom audience");
            }

            public void onFailure(@NonNull Throwable e) {
              statusReceiver.accept(
                  "Error adding override for " + name + " custom audience: " + e.getMessage());
            }
          },
          mExecutor);
    } catch (Exception e) {
      statusReceiver.accept(
          "Got the following exception when trying to add override for "
              + name
              + " custom audience: "
              + e);
      Log.e(TAG, "Exception calling overrideCustomAudienceRemoteInfo", e);
    }
  }

  /**
   * Resets all custom audience overrides.
   *
   * @param statusReceiver A consumer function that is run after the API call and returns a string
   *     indicating the outcome of the call.
   */
  public void resetCAOverrides(Consumer<String> statusReceiver) {
    try {
      Futures.addCallback(
          mCaOverrideClient.resetAllCustomAudienceOverrides(),
          new FutureCallback<Void>() {
            public void onSuccess(Void unused) {
              statusReceiver.accept("Reset all CA overrides");
            }

            public void onFailure(@NonNull Throwable e) {
              statusReceiver.accept("Error while resetting all CA overrides");
            }
          },
          mExecutor);
    } catch (Exception e) {
      statusReceiver.accept(
          "Got the following exception when trying to reset all CA overrides: " + e);
      Log.e(TAG, "Exception calling resetAllCustomAudienceOverrides", e);
    }
  }

  private void joinCustomAudience(CustomAudience ca, Consumer<String> statusReceiver) {
    Futures.addCallback(
        mCaClient.joinCustomAudience(ca),
        new FutureCallback<Void>() {
          public void onSuccess(Void unused) {
            statusReceiver.accept(
                "Joined " + ca.getName() + " custom audience with buyer '" + ca.getBuyer() + "'");
          }

          public void onFailure(@NonNull Throwable e) {
            statusReceiver.accept(
                "Error when joining " + ca.getName() +
                    " custom audience with buyer '" + ca.getBuyer() + "': " + e.getMessage());
            Log.e(TAG, "Exception during CA join process ", e);
          }
        },
        mExecutor);
  }

  private AdData.Builder getAdDataBuilder(Uri renderUri) {
    return new AdData.Builder()
        .setRenderUri(renderUri)
        .setMetadata(new JSONObject().toString());
  }

  private CustomAudience.Builder getCustomAudienceBuilder(
      String name, AdTechIdentifier buyer, Uri biddingUri,
      AdData adData, Uri dailyUpdateUri, Uri trustedBiddingUri,
      Instant expiry) {
    return new CustomAudience.Builder()
        .setBuyer(buyer)
        .setName(name)
        .setDailyUpdateUri(dailyUpdateUri)
        .setBiddingLogicUri(biddingUri)
        .setAds(Collections.singletonList(adData))
        .setActivationTime(Instant.now())
        .setExpirationTime(expiry)
        .setTrustedBiddingData(new TrustedBiddingData.Builder()
            .setTrustedBiddingKeys(Arrays.asList(name, buyer.toString(), "key1", "key2"))
            .setTrustedBiddingUri(trustedBiddingUri).build())
        .setUserBiddingSignals(AdSelectionSignals.EMPTY);
  }
}
