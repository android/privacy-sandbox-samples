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
package com.example.adservices.samples.fledge.clients

import android.content.Context
import android.net.Uri
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ads.adservices.common.AdSelectionSignals
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudienceManager
import androidx.privacysandbox.ads.adservices.customaudience.FetchAndJoinCustomAudienceRequest
import androidx.privacysandbox.ads.adservices.customaudience.JoinCustomAudienceRequest
import androidx.privacysandbox.ads.adservices.customaudience.LeaveCustomAudienceRequest
import java.time.Instant
import java.util.Objects
import java.util.concurrent.Executor

/** The custom audience client. */
@ExperimentalFeatures.Ext10OptIn
@OptIn(ExperimentalFeatures.Ext8OptIn::class)
@RequiresApi(api = 34)
class CustomAudienceClient private constructor(context: Context, private val executor: Executor) {
    private val customAudienceManager: CustomAudienceManager = CustomAudienceManager.obtain(context)!!

    /** Join custom audience. */
    suspend fun joinCustomAudience(customAudience: CustomAudience?) {
        val request = JoinCustomAudienceRequest(customAudience!!)
        customAudienceManager.joinCustomAudience(request)
    }

    /** Fetch and Join custom audience. */
    suspend fun fetchAndJoinCustomAudience(
            fetchUri: Uri,
            name: String?,
            activationTime: Instant?,
            expirationTime: Instant?,
            userBiddingSignals: AdSelectionSignals?
    ) {
        val request =
                FetchAndJoinCustomAudienceRequest(fetchUri, name, activationTime, expirationTime, userBiddingSignals)
      customAudienceManager.fetchAndJoinCustomAudience(request)
    }

    /** Leave custom audience. */
    suspend fun leaveCustomAudience(
            owner: String,
            buyer: AdTechIdentifier,
            name: String
    ) {
        val request = LeaveCustomAudienceRequest(buyer, name)
        customAudienceManager.leaveCustomAudience(request)
    }

    /** Builder class. */
    class Builder
    /** Empty-arg constructor with an empty body for Builder */
    {
        private var mContext: Context? = null
        private var mExecutor: Executor? = null

        /** Sets the context. */
        fun setContext(context: Context): Builder {
            Objects.requireNonNull(context)
            mContext = context
            return this
        }

        /**
         * Sets the worker executor.
         *
         * @param executor the worker executor used to run heavy background tasks.
         */
        fun setExecutor(executor: Executor): Builder {
            Objects.requireNonNull(executor)
            mExecutor = executor
            return this
        }

        /** Builds a [CustomAudienceClient] instance */
        fun build(): CustomAudienceClient {
            Objects.requireNonNull(mContext)
            Objects.requireNonNull(mExecutor)
            return CustomAudienceClient(mContext!!, mExecutor!!)
        }
    }
}
