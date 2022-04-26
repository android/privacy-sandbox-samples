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
import android.adservices.adselection.AdSelectionManager;
import android.adservices.adselection.AdSelectionOutcome;
import android.adservices.adselection.ReportImpressionRequest;
import android.adservices.exceptions.AdServicesException;
import android.content.Context;
import android.net.Uri;
import android.os.OutcomeReceiver;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * Wrapper for the FLEDGE Ad Selection API. This wrapper is opinionated and makes several
 * choices such as running impression report immediately after every successful ad auction or leaving
 * the ad signals empty to limit the complexity that is exposed the user.
 */
@RequiresApi(api = 33)
public class AdSelectionClient {

  private final List<String> mBuyers;
  private final String mSeller;
  private final Supplier<Uri> mDecisionUrlSupplier;
  private final AdSelectionManager mAdManager;
  private final Executor mExecutor;

  /**
   * Initializes the ad selection client with a specific seller, list of buyers, and decision
   * endpoint.
   * @param buyers A list of buyers for the auction.
   * @param seller The name of the seller for the auction
   * @param decisionUrlSupplier A supplier that gives theURL to retrieve the seller scoring and
   * reporting logic from
   * @param context The application context.
   * @param executor An executor to use with the FLEDGE API calls.
   */
  public AdSelectionClient(List<String> buyers, String seller, Supplier<Uri> decisionUrlSupplier,
      Context context, Executor executor) {
    mBuyers = buyers;
    mSeller = seller;
    mDecisionUrlSupplier = decisionUrlSupplier;
    mAdManager = context.getSystemService(AdSelectionManager.class);
    mExecutor = executor;
  }

  /**
   * Runs add selection and passes a string describing it's status to the input receivers. If ad
   * selection succeeds, also report impressions.
   * @param statusReceiver A consumer function that is run after ad selection and impression reporting
   * with a string describing how the auction and reporting went.
   * @param renderUrlReceiver A consumer function that is run after ad selection with a message describing the render URL
   * or lack thereof.
   */
  public void runAdSelection(Consumer<String> statusReceiver, Consumer<String> renderUrlReceiver) {
    try {
      AdSelectionConfig config = generateAdSelectionConfig();
      OutcomeReceiver<AdSelectionOutcome, AdServicesException> adSelectionReceiver =
          new OutcomeReceiver<AdSelectionOutcome, AdServicesException>() {
            @Override
            public void onResult(@NonNull AdSelectionOutcome adSelectionOutcome) {
              statusReceiver.accept("Ran ad selection");
              renderUrlReceiver.accept("Would display ad from " + adSelectionOutcome.getRenderUrl());


              reportImpression(adSelectionOutcome.getAdSelectionId(), config, statusReceiver);
            }

            @Override
            public void onError(@NonNull AdServicesException e) {
              statusReceiver.accept("Error when running ad selection: " + e.getMessage());
              renderUrlReceiver.accept("Ad selection failed -- no ad to display");
              Log.e(MainActivity.TAG, "Exception during ad selection", e);
            }
          };
      mAdManager.runAdSelection(config, mExecutor, adSelectionReceiver);
    } catch (Exception e) {
      statusReceiver.accept("Got the following exception when trying to run ad selection: " + e);
      renderUrlReceiver.accept("Ad selection failed -- no ad to display");
      Log.e(MainActivity.TAG, "Exception calling runAdSelection", e);
    }

  }

  /**
   * Generates an ad selection config based on the fields in this class.
   * @return The generated ad selection config
   */
  private AdSelectionConfig generateAdSelectionConfig() {
    return new AdSelectionConfig.Builder()
        .setSeller(mSeller)
        .setDecisionLogicUrl(mDecisionUrlSupplier.get())
        .setCustomAudienceBuyers(mBuyers)
        .setAdSelectionSignals(new JSONObject().toString())
        .setSellerSignals(new JSONObject().toString())
        .setPerBuyerSignals(mBuyers.stream()
            .collect(Collectors.toMap(buyer -> buyer, buyer -> new JSONObject().toString())))
        .setContextualAds(new ArrayList<>())
        .build();
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
    OutcomeReceiver<Void, AdServicesException> impressionReceiver = new OutcomeReceiver<Void, AdServicesException>() {
      @Override
      public void onResult(@NonNull Void unused) {
        statusReceiver.accept("Reported impressions from ad selection");
      }

      @Override
      public void onError(@NonNull AdServicesException error) {
        statusReceiver.accept("Error when reporting impressions: " + error.getMessage());
        Log.e(MainActivity.TAG, error.toString(), error);
      }
    };
    mAdManager.reportImpression(request, mExecutor, impressionReceiver);
  }

}
