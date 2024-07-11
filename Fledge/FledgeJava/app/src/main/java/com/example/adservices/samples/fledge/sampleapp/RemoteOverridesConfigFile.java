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

import android.adservices.common.AdSelectionSignals;
import android.adservices.customaudience.AddCustomAudienceOverrideRequest;
import android.annotation.SuppressLint;

import com.google.common.collect.ImmutableList;

@SuppressLint("NewApi")
public class RemoteOverridesConfigFile {

    private final ImmutableList<AddCustomAudienceOverrideRequest> mCustomAudienceWithDevOverrides;
    private final AdSelectionSignals mTrustedOverrideScoringSignals;
    private final String mTrustedOverrideScoringLogic;

    public RemoteOverridesConfigFile(
            ImmutableList<AddCustomAudienceOverrideRequest> customAudienceWithDevOverrides,
            AdSelectionSignals trustedOverrideScoringSignals,
            String trustedOverrideScoringLogic) {
        this.mCustomAudienceWithDevOverrides = customAudienceWithDevOverrides;
        this.mTrustedOverrideScoringSignals = trustedOverrideScoringSignals;
        this.mTrustedOverrideScoringLogic = trustedOverrideScoringLogic;
    }

    public boolean hasOverrides() {
        return !getOverrides().isEmpty() && getTrustedScoringSignals() != AdSelectionSignals.EMPTY;
    }

    public String getScoringLogic() {
        return mTrustedOverrideScoringLogic;
    }

    public AdSelectionSignals getTrustedScoringSignals() {
        return mTrustedOverrideScoringSignals;
    }

    public ImmutableList<AddCustomAudienceOverrideRequest> getOverrides() {
        return mCustomAudienceWithDevOverrides;
    }
}
