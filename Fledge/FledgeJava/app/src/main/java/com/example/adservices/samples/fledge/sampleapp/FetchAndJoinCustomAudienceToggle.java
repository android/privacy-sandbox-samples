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
import android.adservices.customaudience.FetchAndJoinCustomAudienceRequest;
import android.annotation.SuppressLint;
import android.content.Context;

@SuppressLint("NewApi")
public class FetchAndJoinCustomAudienceToggle implements Toggle {

    private final CustomAudienceWrapper mCustomAudienceWrapper;
    private final EventLogManager mEventLog;
    private final Context mContext;
    private final FetchAndJoinCustomAudienceRequest mFetchAndJoinCustomAudienceRequest;

    FetchAndJoinCustomAudienceToggle(
            FetchAndJoinCustomAudienceRequest fetchAndJoincustomAudienceRequest,
            CustomAudienceWrapper customAudienceWrapper,
            EventLogManager eventLog,
            Context context) {
        this.mFetchAndJoinCustomAudienceRequest = fetchAndJoincustomAudienceRequest;
        this.mCustomAudienceWrapper = customAudienceWrapper;
        this.mEventLog = eventLog;
        this.mContext = context;
    }

    @Override
    public String getLabel() {
        return mContext.getString(
                R.string.fetch_and_join_ca_toggle, mFetchAndJoinCustomAudienceRequest.getName());
    }

    @Override
    public boolean onSwitchToggle(boolean newValue) {
        return newValue ? joinCustomAudience() : leaveCustomAudience();
    }

    boolean joinCustomAudience() {
        mCustomAudienceWrapper.fetchAndJoinCa(
                mFetchAndJoinCustomAudienceRequest, mEventLog::writeEvent);
        return true;
    }

    boolean leaveCustomAudience() {
        mCustomAudienceWrapper.leaveCa(
                mFetchAndJoinCustomAudienceRequest.getName(),
                AdTechIdentifier.fromString(
                        mFetchAndJoinCustomAudienceRequest.getFetchUri().getHost()),
                mEventLog::writeEvent);
        return true;
    }
}
