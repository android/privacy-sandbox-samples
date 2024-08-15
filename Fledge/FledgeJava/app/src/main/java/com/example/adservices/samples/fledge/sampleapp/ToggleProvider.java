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
import android.adservices.customaudience.FetchAndJoinCustomAudienceRequest;
import android.annotation.SuppressLint;
import android.content.Context;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Provides all the toggles for Custom audiences. */
@SuppressLint("NewApi")
public class ToggleProvider {

    // Replace these with different file paths if desired.
    public static final String REMOTE_OVERRIDES_JSON = "RemoteOverrides.json";
    public static final String CONFIG_JSON = "DefaultConfig.json";

    private final Context mContext;
    private final EventLogManager mEventLog;
    private final CustomAudienceWrapper mCustomAudienceWrapper;
    private final ConfigFileLoader mConfigFileLoader;
    private final AdSelectionWrapper mAdSelectionWrapper;

    public ToggleProvider(
            Context context,
            EventLogManager eventLog,
            CustomAudienceWrapper customAudienceWrapper,
            AdSelectionWrapper adSelectionWrapper,
            ConfigUris config) {
        this.mContext = context;
        this.mEventLog = eventLog;
        this.mCustomAudienceWrapper = customAudienceWrapper;
        this.mAdSelectionWrapper = adSelectionWrapper;
        this.mConfigFileLoader = new ConfigFileLoader(mContext, config, eventLog::writeEvent);
    }

    /**
     * @return List of Toggle for Custom Audiences.
     */
    public List<Toggle> getToggles() throws JSONException, IOException {
        List<Toggle> toggles = new ArrayList<>();

        CustomAudienceConfigFile data = mConfigFileLoader.loadCustomAudienceConfigFile(CONFIG_JSON);
        RemoteOverridesConfigFile remoteOverridesConfigFile =
                mConfigFileLoader.loadRemoteOverridesConfigFile(REMOTE_OVERRIDES_JSON);
        if (remoteOverridesConfigFile.hasOverrides()) {
            toggles.add(
                    new RemoteOverridesToggle(
                            remoteOverridesConfigFile.getScoringLogic(),
                            remoteOverridesConfigFile.getTrustedScoringSignals(),
                            remoteOverridesConfigFile.getOverrides(),
                            mAdSelectionWrapper,
                            mCustomAudienceWrapper,
                            mEventLog,
                            mContext));
        }

        for (Map.Entry<String, CustomAudience> customAudience :
                data.getCustomAudiences().entrySet()) {
            toggles.add(
                    new CustomAudienceToggle(
                            customAudience.getKey(),
                            customAudience.getValue(),
                            mCustomAudienceWrapper,
                            mEventLog,
                            mContext));
        }
        for (Map.Entry<String, FetchAndJoinCustomAudienceRequest>
                fetchAndJoinCustomAudienceRequest :
                        data.getFetchAndJoinCustomAudiences().entrySet()) {
            toggles.add(
                    new FetchAndJoinCustomAudienceToggle(
                            fetchAndJoinCustomAudienceRequest.getKey(),
                            fetchAndJoinCustomAudienceRequest.getValue(),
                            mCustomAudienceWrapper,
                            mEventLog,
                            mContext));
        }
        return toggles;
    }
}
