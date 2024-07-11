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

import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.BID_FLOOR_SIGNALS_FORMAT;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.DECISION_URI_SUFFIX;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.DEFAULT_BASE_URI_FORMAT;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.SCORING_LOGIC_WITH_BID_FLOOR_JS;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.TAG;
import static com.example.adservices.samples.fledge.WaterfallMediationHelpers.Constants.TRUSTED_SCORING_SIGNALS_URI_SUFFIX;

import android.adservices.adselection.AdSelectionConfig;
import android.adservices.adselection.AdSelectionOutcome;
import android.adservices.adselection.AddAdSelectionOverrideRequest;
import android.adservices.adselection.ReportImpressionRequest;
import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.example.adservices.samples.fledge.clients.AdSelectionClient;
import com.example.adservices.samples.fledge.clients.TestAdSelectionClient;
import com.example.adservices.samples.fledge.sampleapp.EventLogManager;
import com.example.adservices.samples.fledge.sampleapp.MainActivity;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Represents an {@code Ad Network SDK's} wrapper class.
 *
 * <p>{@code An Ad Network} can run ad selection and reporting in FLEDGE</p>
 *
 * <p>To allow an {@code Ad Network} to be included in a mediation flow as a participant(third-party
 * or 3P) {@code Ad Network} also needs to implement a wrapper class called {@code NetworkAdapter}.
 * {@link NetworkAdapter} class is implemented as a combination of {@code Ad Network's SDK} + {@code
 * Network adapter}<p/>
 *
 * <p>This class is expected to be implemented by third-party SDKs participating in mediation flow.
 * {@link MediationSdk} (who orchestrates the mediation flow can load/import a and use it to run ad
 * selection and reporting for the owner Network SDK.</p>
 */
@RequiresApi(api = 34)
public class NetworkAdapter {

  private final List<AdTechIdentifier> buyers;
  private final AdSelectionConfig adSelectionConfig;
  private final double bidFloor;

  protected final String networkName;
  protected final boolean useOverrides;
  protected final Uri baseUri;
  protected final String uriFriendlyName;
  protected final String baseUriString;
  protected final AdSelectionClient adSelectionClient;
  protected final TestAdSelectionClient testAdSelectionClient;
  protected final EventLogManager eventLog;

  protected NetworkAdapter(String networkName, AdTechIdentifier buyer, Uri baseUri, boolean useOverrides, Executor executor, Context context, EventLogManager eventLog) {
    // If bid floor is not given we set it to 0
    //  (i.e. Mediation SDK don't have the bid floor concept so we set bid floor to zero to let any
    //  bid pass by scoring)
    this(networkName, buyer, 0.0, baseUri, useOverrides, executor, context, eventLog);
  }

  public NetworkAdapter(String networkName, AdTechIdentifier buyer, double bidFloor, Uri baseUri, boolean useOverrides,
      Executor executor, Context context, EventLogManager eventLog) {
    uriFriendlyName = Constants.uriFriendlyString(networkName);
    this.networkName = networkName;
    this.bidFloor = bidFloor;
    this.eventLog = eventLog;
    this.useOverrides = useOverrides;
    this.baseUri = (!useOverrides) ? baseUri : Uri.parse(String.format(DEFAULT_BASE_URI_FORMAT, uriFriendlyName));

    buyers = Collections.singletonList(buyer);
    baseUriString = String.format(DEFAULT_BASE_URI_FORMAT, uriFriendlyName);
    adSelectionConfig = prepareAdSelectionConfig();
    adSelectionClient = new AdSelectionClient.Builder()
        .setContext(context)
        .setExecutor(executor)
        .build();
    testAdSelectionClient = new TestAdSelectionClient.Builder()
        .setContext(context)
        .setExecutor(executor)
        .build();
  }

  @SuppressLint("NewApi")
  public AdSelectionOutcome runAdSelection() {
    if (useOverrides) {
      addAdSelectionOverrides();
    }

    AdSelectionOutcome adSelectionOutcome;
    try {
      adSelectionOutcome = adSelectionClient.selectAds(adSelectionConfig).get(10, TimeUnit.SECONDS);
      Log.i(TAG, networkName + " adSelection success!");
      Thread.sleep(1000);
    } catch (Exception e) {
      Log.e(MainActivity.TAG, "Exception running ad selection for " + networkName + " " + e);
      adSelectionOutcome = AdSelectionOutcome.NO_OUTCOME;
    }
    return adSelectionOutcome;
  }

  public void reportImpressions(Long adSelectionId) {
    ReportImpressionRequest request = new ReportImpressionRequest(adSelectionId, prepareAdSelectionConfig());
    try {
      adSelectionClient.reportImpression(request).get(10, TimeUnit.SECONDS);
      writeEvent("Report impression succeeded for %s", adSelectionId);
    } catch (Exception e) {
      writeEvent("Report impression failed: %s", e);
    }
  }

  public void resetAdSelectionOverrides() {
    testAdSelectionClient.resetAllAdSelectionConfigRemoteOverrides();
  }

  public AdSelectionSignals getBidFloorSignals() {
    return AdSelectionSignals.fromString(
        String.format(BID_FLOOR_SIGNALS_FORMAT, this.bidFloor));
  }

  public double getBidFloor() {
    return bidFloor;
  }

  public String getNetworkName() {
    return networkName;
  }

  @NonNull
  @Override
  public String toString() {
    return String.format("%s - %s", networkName, bidFloor);
  }

  protected void writeEvent(String eventFormat, Object... args) {
    eventLog.writeEvent(String.format(eventFormat, args));
  }

  private void addAdSelectionOverrides() {
    try {
      testAdSelectionClient.overrideAdSelectionConfigRemoteInfo(
          new AddAdSelectionOverrideRequest(
              adSelectionConfig,
              String.format(SCORING_LOGIC_WITH_BID_FLOOR_JS, uriFriendlyName),
              AdSelectionSignals.EMPTY)).get(10, TimeUnit.SECONDS);
      Log.i(TAG, networkName + " adSelection overrides success!");
      writeEvent("Adds AdSelectionConfig overrides");

    } catch (Exception e) {
      Log.e(MainActivity.TAG, "Exception adding overrides for " + networkName + ": " + e);
    }
  }

  private AdSelectionConfig prepareAdSelectionConfig() {
    return new AdSelectionConfig.Builder()
        .setSeller(AdTechIdentifier.fromString(getDecisionLogicUri().getHost()))
        .setDecisionLogicUri(getDecisionLogicUri())
        .setCustomAudienceBuyers(buyers)
        .setAdSelectionSignals(AdSelectionSignals.EMPTY)
        .setSellerSignals(getSellerSignals())
        .setPerBuyerSignals(buyers.stream()
            .collect(Collectors.toMap(buyer -> buyer, buyer -> AdSelectionSignals.EMPTY)))
        .setTrustedScoringSignalsUri(getTrustedScoringUri())
        .build();
  }

  private Uri getDecisionLogicUri() {
    return baseUri.buildUpon().appendPath(DECISION_URI_SUFFIX).build();
  }

  private Uri getTrustedScoringUri() {
    return baseUri.buildUpon().appendPath(TRUSTED_SCORING_SIGNALS_URI_SUFFIX).build();

  }

  private AdSelectionSignals getSellerSignals() {
    return AdSelectionSignals.fromString(String.format(BID_FLOOR_SIGNALS_FORMAT, bidFloor));
  }
}
