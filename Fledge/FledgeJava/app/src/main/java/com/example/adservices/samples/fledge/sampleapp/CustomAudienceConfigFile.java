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

import com.google.common.collect.ImmutableList;

/** POJO to capture the parsed custom audiences from config file. */
public class CustomAudienceConfigFile {
    private final ImmutableList<CustomAudience> mCustomAudiences;
    private final ImmutableList<FetchAndJoinCustomAudienceRequest> mFetchAndJoinCustomAudiences;

    /**
     * Default constructor
     *
     * @param customAudiences immutable list of custom audiences.
     * @param fetchAndJoinCustomAudiences immutable list of fetch and join custom audiences.
     */
    public CustomAudienceConfigFile(
            ImmutableList<CustomAudience> customAudiences,
            ImmutableList<FetchAndJoinCustomAudienceRequest> fetchAndJoinCustomAudiences) {
        this.mCustomAudiences = customAudiences;
        this.mFetchAndJoinCustomAudiences = fetchAndJoinCustomAudiences;
    }

    /**
     * @return list of custom audiences.
     */
    public ImmutableList<CustomAudience> getCustomAudiences() {
        return mCustomAudiences;
    }

    /**
     * @return list of fetch and join custom audience requests.
     */
    public ImmutableList<FetchAndJoinCustomAudienceRequest> getFetchAndJoinCustomAudiences() {
        return mFetchAndJoinCustomAudiences;
    }
}
