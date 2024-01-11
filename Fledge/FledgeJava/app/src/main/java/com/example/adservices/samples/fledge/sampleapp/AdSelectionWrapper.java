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

import android.adservices.adselection.AdSelectionConfig;
import android.adservices.adselection.AdSelectionOutcome;
import android.adservices.adselection.AddAdSelectionOverrideRequest;
import android.adservices.adselection.GetAdSelectionDataRequest;
import android.adservices.adselection.PersistAdSelectionResultRequest;
import android.adservices.adselection.ReportEventRequest;
import android.adservices.adselection.ReportImpressionRequest;
import android.adservices.adselection.UpdateAdCounterHistogramRequest;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.adservices.common.FrequencyCapFilters;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.example.adservices.samples.fledge.ServerAuctionHelpers.BiddingAuctionServerClient;
import com.example.adservices.samples.fledge.ServerAuctionHelpers.SelectAdsResponse;
import com.example.adservices.samples.fledge.clients.AdSelectionClient;
import com.example.adservices.samples.fledge.clients.TestAdSelectionClient;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Wrapper for the FLEDGE Ad Selection API. This wrapper is opinionated and makes several choices
 * such as running impression reporting immediately after every successful ad auction or leaving the
 * ad signals empty to limit the complexity that is exposed the user.
 */
@RequiresApi(api = 34)
public class AdSelectionWrapper {
  private static final String TAG = "adservices";

  private AdSelectionConfig mAdSelectionConfig;
  private final AdSelectionClient mAdClient;
  private final TestAdSelectionClient mOverrideClient;
  private final Executor mExecutor;
  private final Context mContext;

  /**
   * Initializes the ad selection wrapper with a specific seller, list of buyers, and decision
   * endpoint.
   *
   * @param buyers A list of buyers for the auction.
   * @param seller The name of the seller for the auction
   * @param decisionUri The URI to retrieve the seller scoring and reporting logic from
   * @param context The application context.
   * @param executor An executor to use with the FLEDGE API calls.
   */
  public AdSelectionWrapper(
      List<AdTechIdentifier> buyers,
      AdTechIdentifier seller,
      Uri decisionUri,
      Uri trustedDataUri,
      Context context,
      Executor executor) {

    mAdSelectionConfig =
        new AdSelectionConfig.Builder()
            .setSeller(seller)
            .setDecisionLogicUri(decisionUri)
            .setCustomAudienceBuyers(buyers)
            .setAdSelectionSignals(AdSelectionSignals.EMPTY)
            .setSellerSignals(AdSelectionSignals.EMPTY)
            .setPerBuyerSignals(
                buyers.stream()
                    .collect(Collectors.toMap(buyer -> buyer, buyer -> AdSelectionSignals.EMPTY)))
            .setTrustedScoringSignalsUri(trustedDataUri)
            .build();
    mAdClient = new AdSelectionClient.Builder().setContext(context).setExecutor(executor).build();
    mOverrideClient =
        new TestAdSelectionClient.Builder().setContext(context).setExecutor(executor).build();
    mExecutor = executor;
    mContext = context;
  }

  /**
   * Resets the {@code AdSelectionConfig} with the new decisionUri associated with this {@code
   * AdSelectionWrapper}. To be used when switching back and forth between dev overrides/mock server
   * states.
   *
   * @param decisionUri the new {@code Uri} to be used
   */
  public void resetAdSelectionConfig(
      List<AdTechIdentifier> buyers,
      AdTechIdentifier seller,
      Uri decisionUri,
      Uri trustedScoringUri) {
    mAdSelectionConfig =
        new AdSelectionConfig.Builder()
            .setSeller(seller)
            .setDecisionLogicUri(decisionUri)
            .setCustomAudienceBuyers(buyers)
            .setAdSelectionSignals(AdSelectionSignals.EMPTY)
            .setSellerSignals(AdSelectionSignals.EMPTY)
            .setPerBuyerSignals(
                buyers.stream()
                    .collect(Collectors.toMap(buyer -> buyer, buyer -> AdSelectionSignals.EMPTY)))
            .setTrustedScoringSignalsUri(trustedScoringUri)
            .build();
  }

  /**
   * Runs ad selection and passes a string describing its status to the input receivers. If ad
   * selection succeeds, updates the ad histogram with an impression event and reports the
   * impression.
   *
   * @param statusReceiver A consumer function that is run after ad selection, histogram updating,
   *     and impression reporting with a string describing how the auction, histogram updating, and
   *     reporting went.
   * @param renderUriReceiver A consumer function that is run after ad selection with a message
   *     describing the render URI or lack thereof.
   */
  @SuppressLint("NewApi")
  public void runAdSelection(Consumer<String> statusReceiver, Consumer<String> renderUriReceiver) {
    Log.i(TAG,
        "Running ad selection with scoring uri: " + mAdSelectionConfig.getDecisionLogicUri());
    Log.i(TAG,
        "Running ad selection with buyers: " + mAdSelectionConfig.getCustomAudienceBuyers());
    Log.i(TAG, "Running ad selection with seller: " + mAdSelectionConfig.getSeller());
    try {
      Futures.addCallback(
          mAdClient.selectAds(mAdSelectionConfig),
          new FutureCallback<AdSelectionOutcome>() {
            public void onSuccess(AdSelectionOutcome adSelectionOutcome) {
              statusReceiver.accept(
                  "Ran ad selection! Id: " + adSelectionOutcome.getAdSelectionId());
              renderUriReceiver.accept(
                  "Would display ad from " + adSelectionOutcome.getRenderUri());
              updateAdCounterHistogram(
                  adSelectionOutcome.getAdSelectionId(),
                  FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION,
                  statusReceiver);
              reportImpression(adSelectionOutcome.getAdSelectionId(), statusReceiver);
            }

            public void onFailure(@NonNull Throwable e) {
              statusReceiver.accept("Error when running ad selection: " + e.getMessage());
              renderUriReceiver.accept("Ad selection failed -- no ad to display");
              Log.e(MainActivity.TAG, "Exception during ad selection", e);
            }
          },
          mExecutor);
    } catch (Exception e) {
      statusReceiver.accept("Got the following exception when trying to run ad selection: " + e);
      renderUriReceiver.accept("Ad selection failed -- no ad to display");
      Log.e(MainActivity.TAG, "Exception calling runAdSelection", e);
    }
  }

  /**
   * Runs ad selection on Auction Servers and passes a string describing its status to the input
   * receivers. If ad selection succeeds, updates the ad histogram with an impression event and
   * reports the impression.
   *
   * @param statusReceiver A consumer function that is run after ad selection, histogram updating,
   * and impression reporting with a string describing how the auction, histogram updating, and
   * reporting went.
   *
   * @param renderUriReceiver A consumer function that is run after ad selection with a message
   * describing the render URI or lack thereof.
   */
  @SuppressWarnings("UnstableApiUsage") /* FluentFuture */
  @SuppressLint("NewApi")
  public void runAdSelectionOnAuctionServer(
      Uri sellerSfeUri,
      AdTechIdentifier seller,
      AdTechIdentifier buyer,
      Consumer<String> statusReceiver,
      Consumer<String> renderUriReceiver) {
    if (!isTestableVersion(10, 10)) {
      statusReceiver.accept(
          "Unsupported SDK Extension: Running Server-side auction requires 10, skipping");
      Log.w(
          MainActivity.TAG,
          "Unsupported SDK Extension: Running Server-side auction  requires 10, skipping");
      return;
    }

    Log.i(TAG, "Running ad selection on Auction Servers GetAdSelectionData");
    try {
      Log.i(TAG, "Auction Server ad selection seller:" + seller);
      Log.i(TAG, "Auction Server ad selection seller SFE URI:" + sellerSfeUri);
      GetAdSelectionDataRequest getDataRequest = new GetAdSelectionDataRequest.Builder()
          .setSeller(seller).build();
      ListenableFuture<AdSelectionOutcome> adSelectionOutcome =
          FluentFuture.from(mAdClient.getAdSelectionData(getDataRequest))
              .transform(
                  outcome -> {
                    statusReceiver.accept(
                        "CA data collected from device! Id: " + outcome.getAdSelectionId());
                    try {
                      BiddingAuctionServerClient auctionServerClient =
                          new BiddingAuctionServerClient(mContext);
                      SelectAdsResponse actualResponse = auctionServerClient.runServerAuction(
                          sellerSfeUri.toString(),
                          seller.toString(),
                          buyer.toString(),
                          outcome.getAdSelectionData());

                      Pair<Long, SelectAdsResponse> serverAuctionResult =
                          new Pair<>(outcome.getAdSelectionId(), actualResponse);
                      statusReceiver.accept(
                          "Server auction run successfully for " + serverAuctionResult.first);
                      return serverAuctionResult;
                    } catch (IOException e) {
                      statusReceiver.accept(
                          "Something went wrong when calling bidding auction server");
                      throw new UncheckedIOException(e);
                    }
                  }, mExecutor)
              .transformAsync(
                  pair -> {
                    Objects.requireNonNull(pair);
                    long adSelectionId = pair.first;
                    SelectAdsResponse response = pair.second;
                    Objects.requireNonNull(response);
                    Objects.requireNonNull(response.auctionResultCiphertext);
                    PersistAdSelectionResultRequest persistResultRequest =
                        new PersistAdSelectionResultRequest.Builder()
                            .setSeller(seller)
                            .setAdSelectionId(adSelectionId)
                            .setAdSelectionResult(
                                BaseEncoding.base64().decode(response.auctionResultCiphertext))
                            .build();
                    return mAdClient.persistAdSelectionResult(persistResultRequest);
                  }, mExecutor);
      Futures.addCallback(
          adSelectionOutcome,
          new FutureCallback<AdSelectionOutcome>() {
            public void onSuccess(AdSelectionOutcome adSelectionOutcome) {
              statusReceiver.accept(
                  "Auction Result is persisted for : " + adSelectionOutcome.getAdSelectionId());
              if (adSelectionOutcome.hasOutcome()) {
                renderUriReceiver.accept(
                    "Would display ad from " + adSelectionOutcome.getRenderUri());
                reportImpression(adSelectionOutcome.getAdSelectionId(), statusReceiver);
              } else {
                renderUriReceiver.accept("Would display ad from contextual winner");
              }
            }

            public void onFailure(@NonNull Throwable e) {
              statusReceiver.accept("Error when running ad selection: " + e.getMessage());
              renderUriReceiver.accept("Ad selection failed -- no ad to display");
              Log.e(MainActivity.TAG, "Exception during ad selection", e);
            }
          }, mExecutor);
    } catch (Exception e) {
      statusReceiver.accept("Got the following exception when trying to run ad selection: " + e);
      renderUriReceiver.accept("Ad selection failed -- no ad to display");
      Log.e(MainActivity.TAG, "Exception calling runAdSelection", e);
    }
  }

  /**
   * Runs impression reporting and reports a view event upon success.
   *
   * @param adSelectionId The auction to report impression on.
   * @param statusReceiver A consumer function that is run after impression reporting with a string
   *     describing how the auction and reporting went.
   */
  @SuppressLint("InlinedApi")
  public void reportImpression(long adSelectionId, Consumer<String> statusReceiver) {
    ReportImpressionRequest request =
        new ReportImpressionRequest(adSelectionId, mAdSelectionConfig);

    Futures.addCallback(
        mAdClient.reportImpression(request),
        new FutureCallback<Void>() {
          public void onSuccess(Void unused) {
            statusReceiver.accept("Reported impressions from ad selection.");

            if (!isTestableVersion(8, 9)) {
              statusReceiver.accept(
                  "Unsupported SDK Extension: Event reporting requires 8 for T+ or 9 for S-, skipping");
              Log.w(
                  MainActivity.TAG,
                  "Unsupported SDK Extension: Event reporting requires 8 for T+ or 9 for S-, skipping");
            } else {
              statusReceiver.accept("Registered beacons successfully.");
              String viewInteraction = "view";
              String interactionData = "{\"viewTimeSeconds\":1}";
              reportEvent(
                  adSelectionId,
                  viewInteraction,
                  interactionData,
                  ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER
                      | ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER,
                  statusReceiver);
            }
          }

          public void onFailure(@NonNull Throwable e) {
            statusReceiver.accept("Error when reporting impressions: " + e.getMessage());
            Log.e(MainActivity.TAG, e.toString(), e);
          }
        },
        mExecutor);
  }

  /**
   * Runs interaction reporting.
   *
   * @param adSelectionId The auction associated with the ad.
   * @param eventKey The type of event to be reported.
   * @param eventData Data associated with the event.
   * @param reportingDestinations the destinations to report to, (buyer/seller)
   * @param statusReceiver A consumer function that is run after event reporting with a string
   *     describing how the reporting went.
   */
  @SuppressLint("NewApi")
  public void reportEvent(
      long adSelectionId,
      String eventKey,
      String eventData,
      int reportingDestinations,
      Consumer<String> statusReceiver) {
    if (!isTestableVersion(8, 9)) {
      statusReceiver.accept(
          "Unsupported SDK Extension: Event reporting requires 8 for T+ or 9 for S-, skipping");
      Log.w(
          MainActivity.TAG,
          "Unsupported SDK Extension: Event reporting requires 8 for T+ or 9 for S-, skipping");
      return;
    }

    ReportEventRequest request =
        new ReportEventRequest.Builder(adSelectionId, eventKey, eventData, reportingDestinations)
            .build();

    Futures.addCallback(
        mAdClient.reportEvent(request),
        new FutureCallback<Void>() {
          public void onSuccess(Void unused) {
            statusReceiver.accept(String.format("Reported %s event.", eventKey));
          }

          public void onFailure(@NonNull Throwable e) {
            statusReceiver.accept("Error when reporting event: " + e.getMessage());
            Log.e(MainActivity.TAG, e.toString(), e);
          }
        },
        mExecutor);
  }

  /**
   * Helper function of {@link AdSelectionClient#updateAdCounterHistogram}. Updates the counter
   * histograms for an ad.
   *
   * @param adSelectionId The identifier associated with the winning ad.
   * @param adEventType identifies which histogram should be updated
   * @param statusReceiver A consumer function that is run after that reports how the call went
   *     after it is completed
   */
  @SuppressLint("NewApi")
  public void updateAdCounterHistogram(
      long adSelectionId, int adEventType, Consumer<String> statusReceiver) {
    if (!isTestableVersion(8, 9)) {
      statusReceiver.accept(
          "Unsupported SDK Extension: Ad counter histogram update requires 8 for T+ or 9 for S-, skipping");
      Log.w(
          MainActivity.TAG,
          "Unsupported SDK Extension: Ad counter histogram update requires 8 for T+ or 9 for S-, skipping");
      return;
    }

    AdTechIdentifier callerAdTech = mAdSelectionConfig.getSeller();

    UpdateAdCounterHistogramRequest request =
        new UpdateAdCounterHistogramRequest.Builder(adSelectionId, adEventType, callerAdTech)
            .build();

    Futures.addCallback(
        mAdClient.updateAdCounterHistogram(request),
        new FutureCallback<Void>() {
          public void onSuccess(Void unused) {
            statusReceiver.accept(
                String.format(
                    "Updated ad counter histogram with %s event for adtech: %s",
                    fCapEventToString(adEventType), callerAdTech.toString()));
          }

          public void onFailure(@NonNull Throwable e) {
            statusReceiver.accept("Error when updating ad counter histogram: " + e.toString());
            Log.e(MainActivity.TAG, e.toString(), e);
          }
        },
        mExecutor);
  }

  /**
   * Overrides remote info for an ad selection config.
   *
   * @param decisionLogicJS The overriding decision logic javascript
   * @param statusReceiver A consumer function that is run after the API call and returns a string
   *     indicating the outcome of the call.
   */
  public void overrideAdSelection(
      Consumer<String> statusReceiver,
      String decisionLogicJS,
      AdSelectionSignals trustedScoringSignals) {
    AddAdSelectionOverrideRequest request =
        new AddAdSelectionOverrideRequest(
            mAdSelectionConfig, decisionLogicJS, trustedScoringSignals);
    try {
      Futures.addCallback(
          mOverrideClient.overrideAdSelectionConfigRemoteInfo(request),
          new FutureCallback<Void>() {
            public void onSuccess(Void unused) {
              statusReceiver.accept("Added override for ad selection");
            }

            public void onFailure(@NonNull Throwable e) {
              statusReceiver.accept(
                  "Error when adding override for ad selection " + e.getMessage());
            }
          },
          mExecutor);
    } catch (Exception e) {
      statusReceiver.accept(
          "Got the following exception when trying to override remote info for ad"
              + " selection: "
              + e);
      Log.e(MainActivity.TAG, "Exception calling overrideAdSelectionConfigRemoteInfo", e);
    }
  }

  /**
   * Resets all ad selection overrides.
   *
   * @param statusReceiver A consumer function that is run after the API call and returns a string
   *     indicating the outcome of the call.
   */
  public void resetAdSelectionOverrides(Consumer<String> statusReceiver) {
    try {
      Futures.addCallback(
          mOverrideClient.resetAllAdSelectionConfigRemoteOverrides(),
          new FutureCallback<Void>() {
            public void onSuccess(Void unused) {
              statusReceiver.accept("Reset ad selection overrides");
            }

            public void onFailure(@NonNull Throwable e) {
              statusReceiver.accept(
                  "Error when resetting all ad selection overrides " + e.getMessage());
            }
          },
          mExecutor);
    } catch (Exception e) {
      statusReceiver.accept(
          "Got the following exception when trying to reset all ad selection overrides: " + e);
      Log.e(MainActivity.TAG, "Exception calling resetAllAdSelectionConfigRemoteOverrides", e);
    }
  }

  private String fCapEventToString(int eventType) {
    String result;
    switch (eventType) {
      case FrequencyCapFilters.AD_EVENT_TYPE_WIN:
        result = "win";
        break;
      case FrequencyCapFilters.AD_EVENT_TYPE_CLICK:
        result = "click";
        break;
      case FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION:
        result = "impression";
        break;
      case FrequencyCapFilters.AD_EVENT_TYPE_VIEW:
        result = "view";
        break;
      default:
        result = "unknown";
    }
    return result;
  }
}
