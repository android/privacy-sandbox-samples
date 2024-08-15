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

import android.adservices.customaudience.CustomAudience;
import android.annotation.SuppressLint;
import android.content.Context;

@SuppressLint("NewApi")
public class CustomAudienceToggle implements Toggle {

    private final String mLabelName;
    private final CustomAudience mCustomAudience;
    private final CustomAudienceWrapper mCustomAudienceWrapper;
    private final EventLogManager mEventLog;
    private final Context mContext;

    CustomAudienceToggle(
            String labelName,
            CustomAudience customAudience,
            CustomAudienceWrapper customAudienceWrapper,
            EventLogManager eventLog,
            Context context) {
        this.mLabelName = labelName;
        this.mCustomAudience = customAudience;
        this.mCustomAudienceWrapper = customAudienceWrapper;
        this.mEventLog = eventLog;
        this.mContext = context;
    }

    @Override
    public String getLabel() {
        return mContext.getString(R.string.ca_toggle, mLabelName);
    }

    @Override
    public boolean onSwitchToggle(boolean newValue) {
        return newValue ? joinCustomAudience() : leaveCustomAudience();
    }

    boolean joinCustomAudience() {
        mCustomAudienceWrapper.joinCa(mCustomAudience, mEventLog::writeEvent);
        return true;
    }

    boolean leaveCustomAudience() {
        mCustomAudienceWrapper.leaveCa(
                mCustomAudience.getName(), mCustomAudience.getBuyer(), mEventLog::writeEvent);
        return true;
    }
}
