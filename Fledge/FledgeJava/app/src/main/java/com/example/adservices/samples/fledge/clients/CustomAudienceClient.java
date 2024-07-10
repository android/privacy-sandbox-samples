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
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals;
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier;
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.Ext10OptIn;
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience;
import androidx.privacysandbox.ads.adservices.customaudience.FetchAndJoinCustomAudienceRequest;
import androidx.privacysandbox.ads.adservices.customaudience.JoinCustomAudienceRequest;
import androidx.privacysandbox.ads.adservices.customaudience.LeaveCustomAudienceRequest;
import androidx.privacysandbox.ads.adservices.java.customaudience.CustomAudienceManagerFutures;

import com.google.common.util.concurrent.ListenableFuture;

import kotlin.Unit;

import java.time.Instant;

/** The custom audience client. */
@RequiresApi(api = 34)
public class CustomAudienceClient {

    private final CustomAudienceManagerFutures mCustomAudienceManager;

    public CustomAudienceClient(@NonNull Context context) {
        mCustomAudienceManager = CustomAudienceManagerFutures.from(context);
    }

    /** Join custom audience. */
    @NonNull
    public ListenableFuture<Unit> joinCustomAudience(CustomAudience customAudience) {
        JoinCustomAudienceRequest request = new JoinCustomAudienceRequest(customAudience);
        return mCustomAudienceManager.joinCustomAudienceAsync(request);
    }

    /** Leave custom audience. */
    @NonNull
    public ListenableFuture<Unit> leaveCustomAudience(
            @NonNull String owner, @NonNull AdTechIdentifier buyer, @NonNull String name) {
        LeaveCustomAudienceRequest request = new LeaveCustomAudienceRequest(buyer, name);
        return mCustomAudienceManager.leaveCustomAudienceAsync(request);
    }

    /** Fetch and Join custom audience. */
    @OptIn(markerClass = Ext10OptIn.class)
    @NonNull
    @SuppressLint("NewApi")
    public ListenableFuture<Unit> fetchAndJoinCustomAudience(
            Uri fetchUri,
            String name,
            Instant activationTime,
            Instant expirationTime,
            AdSelectionSignals userBiddingSignals) {
        return mCustomAudienceManager.fetchAndJoinCustomAudienceAsync(
                new FetchAndJoinCustomAudienceRequest(
                        fetchUri, name, activationTime, expirationTime, userBiddingSignals));
    }
}
