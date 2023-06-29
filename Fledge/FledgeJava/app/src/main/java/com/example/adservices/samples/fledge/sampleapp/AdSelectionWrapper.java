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

import static android.adservices.adselection.ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER;
import static android.adservices.adselection.ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER;
import static android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_CLICK;
import static android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION;
import static android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_VIEW;
import static android.adservices.common.FrequencyCapFilters.AD_EVENT_TYPE_WIN;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.TAG;

import android.adservices.adselection.AdSelectionConfig;
import android.adservices.adselection.AdSelectionOutcome;
import android.adservices.adselection.AddAdSelectionOverrideRequest;
import android.adservices.adselection.BuyersDecisionLogic;
import android.adservices.adselection.ContextualAds;
import android.adservices.adselection.ReportImpressionRequest;
import android.adservices.adselection.ReportEventRequest;
import android.adservices.adselection.SetAppInstallAdvertisersRequest;
import android.adservices.adselection.UpdateAdCounterHistogramRequest;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.example.adservices.samples.fledge.clients.AdSelectionClient;
import com.example.adservices.samples.fledge.clients.TestAdSelectionClient;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Wrapper for the FLEDGE Ad Selection API. This wrapper is opinionated and makes several
 * choices such as running impression reporting immediately after every successful ad auction or leaving
 * the ad signals empty to limit the complexity that is exposed the user.
 */
@RequiresApi(api = 34)
public class AdSelectionWrapper {

  private AdSelectionConfig mAdSelectionConfig;
  private final AdSelectionClient mAdClient;
  private final TestAdSelectionClient mOverrideClient;
  private final boolean mUsePrebuiltForScoring;
  private final Uri mOriginalScoringUri;
  private final Executor mExecutor;

  /**
   * Initializes the ad selection wrapper with a specific seller, list of buyers, and decision
   * endpoint.
   * @param buyers A list of buyers for the auction.
   * @param seller The name of the seller for the auction
   * @param decisionUri The URI to retrieve the seller scoring and reporting logic from
   * @param context The application context.
   * @param executor An executor to use with the FLEDGE API calls.
   */
  public AdSelectionWrapper(List<AdTechIdentifier> buyers, AdTechIdentifier seller, Uri decisionUri, Uri trustedDataUri, ContextualAds contextualAds,
      boolean usePrebuiltForScoring, Uri originalScoringUri, Context context, Executor executor) {

    mAdSelectionConfig = new AdSelectionConfig.Builder()
        .setSeller(seller)
        .setDecisionLogicUri(decisionUri)
        .setCustomAudienceBuyers(buyers)
        .setAdSelectionSignals(AdSelectionSignals.EMPTY)
        .setSellerSignals(AdSelectionSignals.EMPTY)
        .setPerBuyerSignals(buyers.stream()
            .collect(Collectors.toMap(buyer -> buyer, buyer -> AdSelectionSignals.EMPTY)))
        .setTrustedScoringSignalsUri((buyers.isEmpty()) ? Uri.EMPTY : trustedDataUri)
        .setBuyerContextualAds(Collections.singletonMap(contextualAds.getBuyer(), contextualAds))
        .build();
    mAdClient = new AdSelectionClient.Builder().setContext(context).setExecutor(executor).build();
    mOverrideClient = new TestAdSelectionClient.Builder().setContext(context).setExecutor(executor).build();
    mUsePrebuiltForScoring = usePrebuiltForScoring;
    mOriginalScoringUri = originalScoringUri;
    mExecutor = executor;
  }

  /**
   * Resets the {@code AdSelectionConfig} with the new decisionUri associated with this {@code AdSelectionWrapper}.
   * To be used when switching back and forth between dev overrides/mock server states.
   *
   * @param decisionUri the new {@code Uri} to be used
   */
  public void resetAdSelectionConfig(List<AdTechIdentifier> buyers, AdTechIdentifier seller,  Uri decisionUri, Uri trustedScoringUri,
      ContextualAds contextualAds) {
    mAdSelectionConfig = new AdSelectionConfig.Builder()
        .setSeller(seller)
        .setDecisionLogicUri(decisionUri)
        .setCustomAudienceBuyers(buyers)
        .setAdSelectionSignals(AdSelectionSignals.EMPTY)
        .setSellerSignals(AdSelectionSignals.EMPTY)
        .setPerBuyerSignals(buyers.stream()
            .collect(Collectors.toMap(buyer -> buyer, buyer -> AdSelectionSignals.EMPTY)))
        .setTrustedScoringSignalsUri(trustedScoringUri)
        .setBuyerContextualAds(Collections.singletonMap(contextualAds.getBuyer(), contextualAds))
        .build();
  }

  /**
   * Runs ad selection and passes a string describing its status to the input receivers. If ad
   * selection succeeds, updates the ad histogram with an impression event and reports the impression.
   * @param statusReceiver A consumer function that is run after ad selection, histogram updating, and impression reporting
   * with a string describing how the auction, histogram updating, and reporting went.
   * @param renderUriReceiver A consumer function that is run after ad selection with a message describing the render URI
   * or lack thereof.
   */
  public void runAdSelection(Consumer<String> statusReceiver, Consumer<String> renderUriReceiver) {
    Log.i(TAG, "Running ad selection with scoring uri: " + mAdSelectionConfig.getDecisionLogicUri());
    Log.i(TAG, "Running ad selection with buyers: " + mAdSelectionConfig.getCustomAudienceBuyers());
    try {
      Futures.addCallback(mAdClient.selectAds(mAdSelectionConfig),
          new FutureCallback<AdSelectionOutcome>() {
            public void onSuccess(AdSelectionOutcome adSelectionOutcome) {
              statusReceiver.accept("Ran ad selection! Id: " + adSelectionOutcome.getAdSelectionId());
              renderUriReceiver.accept("Would display ad from " + adSelectionOutcome.getRenderUri());
              updateAdCounterHistogram(adSelectionOutcome.getAdSelectionId(), AD_EVENT_TYPE_IMPRESSION, statusReceiver);
              reportImpression(adSelectionOutcome.getAdSelectionId(), statusReceiver);
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
   * Runs impression reporting and reports a view interaction upon success.
   *
   * @param adSelectionId The auction to report impression on.
   * @param statusReceiver A consumer function that is run after impression reporting
   * with a string describing how the auction and reporting went.
   */
  public void reportImpression(long adSelectionId, Consumer<String> statusReceiver) {
    // If a prebuilt uri is used for scoring during the ad selection, we will replace that with the
    // original uri for reporting purposes to get the original reportResult function
    if (mUsePrebuiltForScoring) {
      mAdSelectionConfig = new AdSelectionConfig.Builder()
          .setAdSelectionSignals(mAdSelectionConfig.getAdSelectionSignals())
          .setBuyerContextualAds(mAdSelectionConfig.getBuyerContextualAds())
          .setCustomAudienceBuyers(mAdSelectionConfig.getCustomAudienceBuyers())
          .setDecisionLogicUri(mOriginalScoringUri)
          .setPerBuyerSignals(mAdSelectionConfig.getPerBuyerSignals())
          .setSeller(mAdSelectionConfig.getSeller())
          .setSellerSignals(mAdSelectionConfig.getSellerSignals())
          .setTrustedScoringSignalsUri(mAdSelectionConfig.getTrustedScoringSignalsUri())
          .build();
    }
    ReportImpressionRequest request = new ReportImpressionRequest(adSelectionId, mAdSelectionConfig);

    Futures.addCallback(mAdClient.reportImpression(request),
        new FutureCallback<Void>() {
          public void onSuccess(Void unused) {
            statusReceiver.accept("Reported impressions from ad selection.");
            statusReceiver.accept("Registered beacons successfully.");
            String viewInteraction = "view";
            String interactionData = "{\"viewTimeSeconds\":1}";
            reportInteraction(adSelectionId, viewInteraction, interactionData,
                FLAG_REPORTING_DESTINATION_SELLER | FLAG_REPORTING_DESTINATION_BUYER, statusReceiver);
          }

          public void onFailure(@NonNull Throwable e) {
            statusReceiver.accept("Error when reporting impressions: " + e.getMessage());
            Log.e(MainActivity.TAG, e.toString(), e);
          }
        }, mExecutor);
  }

  /**
   * Helper function of {@link  AdSelectionClient#setAppInstallAdvertisers}. Set which adtechs
   * can filter on this app's presence.
   *
   * @param adtechs The set of adtechs that can filter
   * @param statusReceiver A consumer function that is run after that reports how the call went
   * after it is completed
   */
  public void setAppInstallAdvertisers(Set<AdTechIdentifier> adtechs, Consumer<String> statusReceiver) {
    SetAppInstallAdvertisersRequest request = new SetAppInstallAdvertisersRequest(adtechs);

    Futures.addCallback(mAdClient.setAppInstallAdvertisers(request),
        new FutureCallback<Void>() {
          public void onSuccess(Void unused) {
            statusReceiver.accept("Set this app's app install advertisers to: " + adtechs);
          }

          public void onFailure(@NonNull Throwable e) {
            statusReceiver.accept("Error when setting app install advertisers: "
                + e.getMessage());
          }
        }, mExecutor);
  }

  /*
   * Runs interaction reporting.
   *
   * @param adSelectionId The auction associated with the ad.
   * @param interactionKey The type of interaction to be reported.
   * @param interactionData Data associated with the interaction.
   * @param reportingDestinations the destinations to report to, (buyer/seller)
   * @param statusReceiver A consumer function that is run after interaction reporting
   * with a string describing how the reporting went.
   */
  public void reportInteraction(long adSelectionId, String interactionKey, String interactionData, int reportingDestinations, Consumer<String> statusReceiver) {
    ReportEventRequest request = new ReportEventRequest.Builder(adSelectionId, interactionKey, interactionData, reportingDestinations).build();

    Futures.addCallback(mAdClient.reportInteraction(request),
        new FutureCallback<Void>() {
          public void onSuccess(Void unused) {
            statusReceiver.accept(String.format("Reported %s interaction.", interactionKey));
          }

          public void onFailure(@NonNull Throwable e) {
            statusReceiver.accept("Error when reporting interaction: " + e.getMessage());
            Log.e(MainActivity.TAG, e.toString(), e);
          }
        }, mExecutor);
  }

  /**
   * Helper function of {@link  AdSelectionClient#updateAdCounterHistogram}.
   * Updates the counter histograms for an ad.
   *
   * @param adSelectionId The identifier associated with the winning ad.
   * @param adEventType identifies which histogram should be updated
   * @param statusReceiver A consumer function that is run after that reports how the call went
   * after it is completed
   */
  public void updateAdCounterHistogram(long adSelectionId, int adEventType, Consumer<String> statusReceiver) {
    AdTechIdentifier callerAdTech = mAdSelectionConfig.getSeller();

    UpdateAdCounterHistogramRequest request = new UpdateAdCounterHistogramRequest.Builder(adSelectionId, adEventType, callerAdTech)
        .build();

    Futures.addCallback(mAdClient.updateAdCounterHistogram(request),
        new FutureCallback<Void>() {
          public void onSuccess(Void unused) {
            statusReceiver.accept(String.format("Updated ad counter histogram with %s event for adtech:%s", fCapEventToString(adEventType), callerAdTech.toString()));
          }

          public void onFailure(@NonNull Throwable e) {
            statusReceiver.accept("Error when updating ad counter histogram: "
                + e.toString());
            Log.e(MainActivity.TAG, e.toString(), e);
          }
        }, mExecutor);
  }

  /**
   * Overrides remote info for an ad selection config.
   *
   * @param decisionLogicJS The overriding decision logic javascript
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  public void overrideAdSelection(Consumer<String> statusReceiver, String decisionLogicJS,
      AdSelectionSignals trustedScoringSignals, BuyersDecisionLogic contextualLogic) {
    AddAdSelectionOverrideRequest request = new AddAdSelectionOverrideRequest(mAdSelectionConfig, decisionLogicJS,
          trustedScoringSignals, contextualLogic);
    try {
      Futures.addCallback(mOverrideClient.overrideAdSelectionConfigRemoteInfo(request),
          new FutureCallback<Void>() {
            public void onSuccess(Void unused) {
              statusReceiver.accept("Added override for ad selection");
            }

            public void onFailure(@NonNull Throwable e) {
              statusReceiver.accept("Error when adding override for ad selection " + e.getMessage());
            }
          }, mExecutor);
    } catch (Exception e) {
      statusReceiver.accept("Got the following exception when trying to override remote info for ad selection: " + e);
      Log.e(MainActivity.TAG, "Exception calling overrideAdSelectionConfigRemoteInfo", e);
    }
  }

  /**
   * Resets all ad selection overrides.
   *
   * @param statusReceiver A consumer function that is run after the API call and returns a
   * string indicating the outcome of the call.
   */
  public void resetAdSelectionOverrides(Consumer<String> statusReceiver) {
    try {
      Futures.addCallback(mOverrideClient.resetAllAdSelectionConfigRemoteOverrides(),
          new FutureCallback<Void>() {
            public void onSuccess(Void unused) {
              statusReceiver.accept("Reset ad selection overrides");
            }

            public void onFailure(@NonNull Throwable e) {
              statusReceiver.accept("Error when resetting all ad selection overrides " + e.getMessage());
            }
          }, mExecutor);
    } catch (Exception e) {
      statusReceiver.accept("Got the following exception when trying to reset all ad selection overrides: " + e);
      Log.e(MainActivity.TAG, "Exception calling resetAllAdSelectionConfigRemoteOverrides", e);
    }
  }

  private String fCapEventToString(int eventType) {
    String result;
    switch (eventType) {
      case AD_EVENT_TYPE_WIN:
        result = "win";
        break;
      case AD_EVENT_TYPE_CLICK:
        result = "click";
        break;
      case AD_EVENT_TYPE_IMPRESSION:
        result = "impression";
        break;
      case AD_EVENT_TYPE_VIEW:
        result = "view";
        break;
      default: result = "unknown";
    }
    return result;
  }
}
