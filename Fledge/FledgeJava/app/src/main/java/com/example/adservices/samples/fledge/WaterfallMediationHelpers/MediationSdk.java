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

package com.example.adservices.samples.fledge.WaterfallMediationHelpers;

import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.OUTCOME_SELECTION_URI_SUFFIX;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.TAG;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.WATERFALL_MEDIATION_LOGIC_JS;

import android.adservices.adselection.AdSelectionFromOutcomesConfig;
import android.adservices.adselection.AdSelectionOutcome;
import android.adservices.adselection.AddAdSelectionFromOutcomesOverrideRequest;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.RequiresApi;
import com.example.adservices.samples.fledge.sampleapp.EventLogManager;
import com.example.adservices.samples.fledge.sampleapp.MainActivity;
import com.example.adservices.samples.fledge.sampleapp.databinding.WaterfallMediationActivityBinding;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Represents an Ad Network SDK's wrapper class that can run Waterfall Mediation flow.
 *
 * <p>{@code An Ad Network} who is {@code a Mediation SDK} (first-party or 1P) can run mediation
 * flows on FLEDGE additional to ad selection and reporting.</p>
 *
 * <p>{@code Mediation SDK} uses {@link NetworkAdapter}s of participating SDKs (third-party or 3P
 * SDKs) to trigger their ad selection and reporting flows.</p>
 *
 * <p>This class is expected to be implemented by SDKs who are willing to run mediation flows.</p>
 */
@RequiresApi(api = 34)
public class MediationSdk extends NetworkAdapter {
  private static final String AD_SELECTION_PREBUILT_SCHEMA = "ad-selection-prebuilt";
  private static final String AD_SELECTION_FROM_OUTCOMES_USE_CASE = "ad-selection-from-outcomes";
  private static final String AD_OUTCOME_SELECTION_WATERFALL_MEDIATION_TRUNCATION = "waterfall-mediation-truncation";
  private static final String BID_FLOOR_SIGNALS_FORMAT = "{%s:%s}";
  private static final String BID_FLOOR_PARAM_KEY = "bidFloor";
  private static final String BID_FLOOR_SIGNAL_KEY = "bid_floor";
  private final WaterfallMediationActivityBinding binding;
  private final boolean useOnlyAdditionalIds;

  public MediationSdk(String networkName, AdTechIdentifier buyer, Uri baseUri, boolean useOverrides, Executor executor,
      Context context, WaterfallMediationActivityBinding binding, EventLogManager eventLog, boolean useOnlyAdditionalIds) {
    super(networkName, buyer, baseUri, useOverrides, executor, context, eventLog);
    this.binding = binding;
    this.useOnlyAdditionalIds = useOnlyAdditionalIds;
  }

  @SuppressLint("NewApi")
  public Pair<AdSelectionOutcome, NetworkAdapter> orchestrateMediation(List<NetworkAdapter> mediationChain) throws Exception  {
    writeEvent("Mediation chain:\n%s", Joiner.on("\n").skipNulls().join(mediationChain));

    AdSelectionOutcome outcome1p = runAdSelection();
    if (outcome1p.hasOutcome()) {
      writeEvent("%s auction result (1P ad): %s", getNetworkName(), outcome1p.getAdSelectionId());
    } else {
      writeEvent("%s auction not returned an ad. No 1P ad. Will iterate the chain without 1P ad anyways.", getNetworkName());
    }

    AdSelectionOutcome outcome;
    for(NetworkAdapter network3p: mediationChain) {
      if (outcome1p.hasOutcome()) {
        writeEvent("Try to place %s before %s", getNetworkName(), network3p.getNetworkName());
        if ((outcome = runSelectOutcome(outcome1p, network3p)).hasOutcome()) {
          writeEvent("%s placed before! 1P ad wins: %s", getNetworkName(), outcome.getAdSelectionId());
          return new Pair<>(outcome, this);
        }
        writeEvent("%s isn't placed before %s, running ad selection for it!", getNetworkName(), network3p.getNetworkName());
      } else {
        writeEvent("No 1P ad, continue without comparing");
      }

      if((outcome = network3p.runAdSelection()).hasOutcome()) {
        writeEvent("%s's auction returned an ad: Winner: %s", network3p.getNetworkName(), outcome.getAdSelectionId());
        return new Pair<>(outcome, network3p);
      }
      writeEvent("%s's auction not returned an ad. Moving to the next network", network3p.getNetworkName());
    }
    writeEvent("Mediation Chain is exhausted but no ads found! Returning the 1P auction result");
    return new Pair<>(outcome1p, this);
  }

  @SuppressLint("NewApi")
  public AdSelectionOutcome runSelectOutcome(AdSelectionOutcome outcome1p, NetworkAdapter network3p)
      throws Exception {
    AdSelectionFromOutcomesConfig config = prepareWaterfallConfig(outcome1p.getAdSelectionId(), network3p.getBidFloor());

    if (useOverrides) {
      addAdSelectionFromOutcomesOverride(config);
    }

    AdSelectionOutcome result;
    try {
      result = adSelectionClient.selectAds(config).get(10, TimeUnit.SECONDS);
      Thread.sleep(1000);
    } catch (Exception e) {
      Log.e(MainActivity.TAG, "Exception calling selectAds(AdSelectionFromOutcomesConfig)", e);
      throw e;
    }
    return result;
  }

  @SuppressLint("NewApi")
  private void addAdSelectionFromOutcomesOverride(AdSelectionFromOutcomesConfig config) {
    try {
      testAdSelectionClient
          .overrideAdSelectionFromOutcomesConfigRemoteInfo(
              new AddAdSelectionFromOutcomesOverrideRequest(config, WATERFALL_MEDIATION_LOGIC_JS,
                  AdSelectionSignals.EMPTY))
          .get(10, TimeUnit.SECONDS);
      Log.i(TAG, networkName + " adSelection overrides success!");
      writeEvent("Adds AdSelectionFromOutcomesConfig overrides");
    } catch (Exception e) {
      Log.e(MainActivity.TAG, "Exception adding overrides for " + networkName + ": " + e);
    }
  }

  @SuppressLint("NewApi")
  private AdSelectionFromOutcomesConfig prepareWaterfallConfig(Long outcome1pId, double bidFloor) {
    // inject a flag to run only with "Additional ad selection ids from the UX"
    List<Long> outcomeIds = new ArrayList<>();
    writeEvent("useOnlyAdditionalIds flag: " + useOnlyAdditionalIds);
    if (!useOnlyAdditionalIds)
      outcomeIds.add(outcome1pId);
    outcomeIds.addAll(getAdditionalIdOrNothing());
    return new AdSelectionFromOutcomesConfig.Builder()
        .setSeller(AdTechIdentifier.fromString(getSelectionLogicUri().getHost()))
        .setAdSelectionIds(outcomeIds)
        .setSelectionSignals(getSignalsForPrebuiltUri(bidFloor))
        .setSelectionLogicUri(getPrebuiltUriForWaterfallTruncation())
        .build();
  }

  @Override
  public void resetAdSelectionOverrides() {
    super.resetAdSelectionOverrides();
    testAdSelectionClient.resetAllAdSelectionFromOutcomesConfigRemoteOverrides();
  }

  private List<Long> getAdditionalIdOrNothing() {
    String additionalIdText;
    if (!(additionalIdText = binding.adSelectionIdsToInclude.getText().toString()).isEmpty()) {
      List<Long> additionalIds = new ArrayList<>();
      for (String longIdString: additionalIdText.split(",")) {
        additionalIds.add(Long.parseLong(longIdString.replace(" ", "")));
      }
      return additionalIds;
    } else {
      return Collections.emptyList();
    }
  }

  private Uri getSelectionLogicUri() {
    return baseUri.buildUpon().appendPath(OUTCOME_SELECTION_URI_SUFFIX).build();
  }

  private AdSelectionSignals getSignalsForPrebuiltUri(double bidFloor) {
    return AdSelectionSignals.fromString(String.format(BID_FLOOR_SIGNALS_FORMAT, BID_FLOOR_SIGNAL_KEY, bidFloor));
  }

  private Uri getPrebuiltUriForWaterfallTruncation() {
    return Uri.parse(
        String.format(
            "%s://%s/%s/?%s=%s",
            AD_SELECTION_PREBUILT_SCHEMA,
            AD_SELECTION_FROM_OUTCOMES_USE_CASE,
            AD_OUTCOME_SELECTION_WATERFALL_MEDIATION_TRUNCATION,
            BID_FLOOR_PARAM_KEY,
            BID_FLOOR_SIGNAL_KEY));
  }
}
