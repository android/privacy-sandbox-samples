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

import android.adservices.adselection.AdSelectionConfig;
import android.adservices.adselection.AdSelectionOutcome;
import android.adservices.adselection.AddAdSelectionOverrideRequest;
import android.adservices.adselection.ReportImpressionRequest;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.example.adservices.samples.fledge.clients.AdSelectionClient;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * Wrapper for the FLEDGE Ad Selection API. This wrapper is opinionated and makes several
 * choices such as running impression reporting immediately after every successful ad auction or leaving
 * the ad signals empty to limit the complexity that is exposed the user.
 */
@RequiresApi(api = 34)
public class AdSelectionWrapper {

  private AdSelectionConfig mAdSelectionConfig;
  private final AdSelectionClient mAdClient;
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
  public AdSelectionWrapper(List<String> buyers, String seller, Uri decisionUri, Uri trustedDataUri, Context context,
      Executor executor) {

    mAdSelectionConfig = new AdSelectionConfig.Builder()
        .setSeller(seller)
        .setDecisionLogicUri(decisionUri)
        .setCustomAudienceBuyers(buyers)
        .setAdSelectionSignals(new JSONObject().toString())
        .setSellerSignals(new JSONObject().toString())
        .setPerBuyerSignals(buyers.stream()
            .collect(Collectors.toMap(buyer -> buyer, buyer -> new JSONObject().toString())))
        .setContextualAds(new ArrayList<>())
        .setTrustedScoringSignalsUri(trustedDataUri)
        .build();
    mAdClient = new AdSelectionClient.Builder().setContext(context).setExecutor(executor).build();
    mExecutor = executor;
  }

  /**
   * Resets the {@code AdSelectionConfig} with the new decisionUri associated with this {@code AdSelectionWrapper}.
   * To be used when switching back and forth between dev overrides/mock server states.
   *
   * @param decisionUri the new {@code Uri} to be used
   */
  public void resetAdSelectionConfig(Uri decisionUri) {
    mAdSelectionConfig = new AdSelectionConfig.Builder()
        .setSeller(mAdSelectionConfig.getSeller())
        .setDecisionLogicUri(decisionUri)
        .setCustomAudienceBuyers(mAdSelectionConfig.getCustomAudienceBuyers())
        .setAdSelectionSignals(new JSONObject().toString())
        .setSellerSignals(new JSONObject().toString())
        .setPerBuyerSignals(mAdSelectionConfig.getPerBuyerSignals())
        .setContextualAds(new ArrayList<>())
        .build();
  }


  /**
   * Runs ad selection and passes a string describing its status to the input receivers. If ad
   * selection succeeds, also report impressions.
   * @param statusReceiver A consumer function that is run after ad selection and impression reporting
   * with a string describing how the auction and reporting went.
   * @param renderUriReceiver A consumer function that is run after ad selection with a message describing the render URI
   * or lack thereof.
   */
  public void runAdSelection(Consumer<String> statusReceiver, Consumer<String> renderUriReceiver) {
    try {
      Futures.addCallback(mAdClient.runAdSelection(mAdSelectionConfig),
          new FutureCallback<AdSelectionOutcome>() {
            public void onSuccess(AdSelectionOutcome adSelectionOutcome) {
              statusReceiver.accept("Ran ad selection");
              renderUriReceiver.accept("Would display ad from " + adSelectionOutcome.getRenderUri());

              reportImpression(adSelectionOutcome.getAdSelectionId(), mAdSelectionConfig, statusReceiver);
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
   * Helper function of {@link #runAdSelection}. Runs impression reporting.
   *
   * @param adSelectionId The auction to report impression on.
   * @param statusReceiver A consumer function that is run after impression reporting
   * with a string describing how the auction and reporting went.
   */
  private void reportImpression(long adSelectionId, AdSelectionConfig config, Consumer<String> statusReceiver) {
    ReportImpressionRequest request = new ReportImpressionRequest.Builder()
        .setAdSelectionConfig(config)
        .setAdSelectionId(adSelectionId)
        .build();

    Futures.addCallback(mAdClient.reportImpression(request),
        new FutureCallback<Void>() {
          public void onSuccess(Void unused) {
            statusReceiver.accept("Reported impressions from ad selection");
          }

          public void onFailure(@NonNull Throwable e) {
            statusReceiver.accept("Error when reporting impressions: " + e.getMessage());
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
  public void overrideAdSelection(Consumer<String> statusReceiver, String decisionLogicJS, String trustedScoringSignals) {
    AddAdSelectionOverrideRequest request =
        new AddAdSelectionOverrideRequest(mAdSelectionConfig, decisionLogicJS, trustedScoringSignals);
    try {
      Futures.addCallback(mAdClient.overrideAdSelectionConfigRemoteInfo(request),
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
      Futures.addCallback(mAdClient.resetAllAdSelectionConfigRemoteOverrides(),
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
}
