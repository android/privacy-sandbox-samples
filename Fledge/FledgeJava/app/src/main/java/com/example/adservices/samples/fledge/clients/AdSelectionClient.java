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

package com.example.adservices.samples.fledge.clients;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionFromOutcomesConfig;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionManager;
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionOutcome;
import androidx.privacysandbox.ads.adservices.adselection.GetAdSelectionDataOutcome;
import androidx.privacysandbox.ads.adservices.adselection.GetAdSelectionDataRequest;
import androidx.privacysandbox.ads.adservices.adselection.PersistAdSelectionResultRequest;
import androidx.privacysandbox.ads.adservices.adselection.ReportEventRequest;
import androidx.privacysandbox.ads.adservices.adselection.ReportImpressionRequest;
import androidx.privacysandbox.ads.adservices.adselection.UpdateAdCounterHistogramRequest;
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.Ext10OptIn;
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.Ext8OptIn;
import androidx.privacysandbox.ads.adservices.java.adselection.AdSelectionManagerFutures;

import com.google.common.util.concurrent.ListenableFuture;

import kotlin.Unit;

/** The ad selection client. */
@RequiresApi(api = 34)
public class AdSelectionClient {

    private final AdSelectionManagerFutures mAdSelectionManager;

    public AdSelectionClient(@NonNull Context context) {
        mAdSelectionManager = AdSelectionManagerFutures.from(context);
    }

    /**
     * Invokes the {@code selectAdsAsync} method of {@link AdSelectionManagerFutures}, and returns a
     * future with {@link AdSelectionOutcome} if succeeds, or an {@link Exception} if fails.
     */
    @NonNull
    public ListenableFuture<AdSelectionOutcome> selectAds(
            @NonNull AdSelectionConfig adSelectionConfig) {
        return mAdSelectionManager.selectAdsAsync(adSelectionConfig);
    }

    @OptIn(markerClass = Ext10OptIn.class)
    @NonNull
    public ListenableFuture<AdSelectionOutcome> selectAdsWithOutcomes(
            @NonNull AdSelectionFromOutcomesConfig adSelectionConfig) {
        return mAdSelectionManager.selectAdsAsync(adSelectionConfig);
    }

    /**
     * Invokes the {@code reportImpression} method of {@link AdSelectionManagerFutures}, and returns
     * a Unit future
     */
    @NonNull
    public ListenableFuture<Unit> reportImpression(@NonNull ReportImpressionRequest input) {
        return mAdSelectionManager.reportImpressionAsync(input);
    }

    /**
     * Invokes the {@code reportEvent} method of {@link AdSelectionManagerFutures}, and returns a
     * Void future
     */
    @OptIn(markerClass = Ext8OptIn.class)
    @NonNull
    public ListenableFuture<Unit> reportEvent(@NonNull ReportEventRequest request) {
        return mAdSelectionManager.reportEventAsync(request);
    }

    /**
     * Invokes the {@code updateAdCounterHistogram} method of {@link AdSelectionManager}, and
     * returns a Void future.
     */
    @OptIn(markerClass = Ext8OptIn.class)
    @NonNull
    public ListenableFuture<Unit> updateAdCounterHistogram(
            @NonNull UpdateAdCounterHistogramRequest updateAdCounterHistogramRequest) {
        return mAdSelectionManager.updateAdCounterHistogramAsync(updateAdCounterHistogramRequest);
    }

    /**
     * Invokes {@link AdSelectionManager#getAdSelectionData}, and returns a
     * GetAdSelectionDataOutcome future.
     */
    @OptIn(markerClass = Ext10OptIn.class)
    @NonNull
    public ListenableFuture<GetAdSelectionDataOutcome> getAdSelectionData(
            @NonNull GetAdSelectionDataRequest request) {
        return mAdSelectionManager.getAdSelectionDataAsync(request);
    }

    /**
     * Invokes {@link AdSelectionManager#persistAdSelectionResult} and returns an AdSelectionOutcome
     * future.
     */
    @OptIn(markerClass = Ext10OptIn.class)
    @NonNull
    public ListenableFuture<AdSelectionOutcome> persistAdSelectionResult(
            @NonNull PersistAdSelectionResultRequest request) {
        return mAdSelectionManager.persistAdSelectionResultAsync(request);
    }
}
