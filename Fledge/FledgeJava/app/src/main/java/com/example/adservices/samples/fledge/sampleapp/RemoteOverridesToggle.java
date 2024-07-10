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
import android.content.Context;

import java.util.List;

@SuppressLint("NewApi")
public class RemoteOverridesToggle implements Toggle {

    private final String overrideScoringLogic;
    private final AdSelectionSignals overrideScoringSignals;
    private final List<AddCustomAudienceOverrideRequest> mCustomAudienceRemoteOverrides;
    private final AdSelectionWrapper adSelectionWrapper;
    private final CustomAudienceWrapper mCustomAudienceWrapper;
    private final EventLogManager mEventLog;
    private final Context mContext;

    public RemoteOverridesToggle(
            String overrideScoringLogic,
            AdSelectionSignals overrideScoringSignals,
            List<AddCustomAudienceOverrideRequest> customAudienceRemoteOverrides,
            AdSelectionWrapper adSelectionWrapper,
            CustomAudienceWrapper customAudienceWrapper,
            EventLogManager eventLog,
            Context context) {
        this.overrideScoringLogic = overrideScoringLogic;
        this.overrideScoringSignals = overrideScoringSignals;
        this.mCustomAudienceRemoteOverrides = customAudienceRemoteOverrides;
        this.adSelectionWrapper = adSelectionWrapper;
        this.mCustomAudienceWrapper = customAudienceWrapper;
        this.mEventLog = eventLog;
        this.mContext = context;
    }

    @Override
    public String getLabel() {
        return mContext.getString(R.string.override_switch_text);
    }

    @Override
    public boolean onSwitchToggle(boolean active) {
        if (active) {
            adSelectionWrapper.overrideAdSelection(
                    mEventLog::writeEvent, overrideScoringLogic, overrideScoringSignals);
            for (AddCustomAudienceOverrideRequest customAudienceOverrideRequest :
                    mCustomAudienceRemoteOverrides) {
                mCustomAudienceWrapper.addCAOverride(
                        customAudienceOverrideRequest.getName(),
                        customAudienceOverrideRequest.getBuyer(),
                        customAudienceOverrideRequest.getBiddingLogicJs(),
                        customAudienceOverrideRequest.getTrustedBiddingSignals(),
                        mEventLog::writeEvent);
            }

        } else {
            adSelectionWrapper.resetAdSelectionOverrides(mEventLog::writeEvent);
            mCustomAudienceWrapper.resetCAOverrides(mEventLog::writeEvent);
        }
        return true;
    }
}
