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

import android.adservices.common.AdTechIdentifier;
import android.annotation.SuppressLint;
import android.net.Uri;

import com.google.auto.value.AutoValue;

import java.util.Objects;

@SuppressLint("NewApi")
@AutoValue
public abstract class ConfigUris {

    private static final AdTechIdentifier EMPTY_AD_TECH = AdTechIdentifier.fromString("");

    static Builder newBuilder() {
        return new AutoValue_ConfigUris.Builder();
    }

    abstract Uri getBaseUri();

    AdTechIdentifier getBuyer() {
        return AdTechIdentifier.fromString(Objects.requireNonNull(getBaseUri().getHost()));
    }

    AdTechIdentifier getSeller() {
        return AdTechIdentifier.fromString(Objects.requireNonNull(getBaseUri().getHost()));
    }

    abstract Uri getAuctionServerSellerSfeUri();

    abstract AdTechIdentifier getAuctionServerSeller();

    abstract AdTechIdentifier getAuctionServerBuyer();

    /**
     * Check if a server auction is likely given input.
     *
     * @return True if any of the server auction values have been passed.
     */
    boolean isMaybeServerAuction() {
        return getAuctionServerBuyer() != EMPTY_AD_TECH
                && getAuctionServerSeller() != EMPTY_AD_TECH
                && getAuctionServerSellerSfeUri() != Uri.EMPTY;
    }

    @AutoValue.Builder
    public abstract static class Builder {
        abstract Builder setBaseUri(Uri baseUri);

        abstract Builder setAuctionServerSellerSfeUri(Uri uri);

        abstract Builder setAuctionServerSeller(AdTechIdentifier adTechIdentifier);

        abstract Builder setAuctionServerBuyer(AdTechIdentifier adTechIdentifier);

        abstract ConfigUris build();
    }
}
