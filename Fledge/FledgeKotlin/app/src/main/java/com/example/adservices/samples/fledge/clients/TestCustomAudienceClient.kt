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

import android.adservices.customaudience.AddCustomAudienceOverrideRequest
import android.adservices.customaudience.CustomAudienceManager
import android.adservices.customaudience.RemoveCustomAudienceOverrideRequest
import android.adservices.customaudience.TestCustomAudienceManager
import android.content.Context
import android.os.Build
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.Objects
import java.util.concurrent.Executor

/**
 * Client for override APIs.
 */
@RequiresApi(api = 34)
class TestCustomAudienceClient private constructor(
        mContext: Context,
        private val mExecutor: Executor,
) {
    private val mTestCustomAudienceManager: TestCustomAudienceManager

    /**
     * Invokes the {@code overrideCustomAudienceRemoteInfo} method of {@link CustomAudienceManager},
     * and returns a Void future
     */
    fun overrideCustomAudienceRemoteInfo(
            request: AddCustomAudienceOverrideRequest,
    ): ListenableFuture<Void?> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
            mTestCustomAudienceManager.overrideCustomAudienceRemoteInfo(
                    request,
                    mExecutor,
                    object : OutcomeReceiver<Any?, java.lang.Exception?> {
                        override fun onResult(ignoredResult: Any?) {
                            completer.set(null)
                        }

                        override fun onError(error: java.lang.Exception) {
                            completer.setException(error)
                        }
                    })
            "overrideCustomAudienceRemoteInfo"
        }
    }

    /**
     * Invokes the {@code removeCustomAudienceRemoteInfoOverride} method of {@link
     * CustomAudienceManager}, and returns a Void future
     */
    fun removeCustomAudienceRemoteInfoOverride(
            request: RemoveCustomAudienceOverrideRequest,
    ): ListenableFuture<Void?> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
            mTestCustomAudienceManager.removeCustomAudienceRemoteInfoOverride(
                    request,
                    mExecutor,
                    object : OutcomeReceiver<Any?, java.lang.Exception?> {
                        override fun onResult(ignoredResult: Any?) {
                            completer.set(null)
                        }

                        override fun onError(error: java.lang.Exception) {
                            completer.setException(error)
                        }
                    })
            "removeCustomAudienceRemoteInfoOverride"
        }
    }

    /**
     * Invokes the {@code resetAllCustomAudienceOverrides} method of {@link CustomAudienceManager},
     * and returns a Void future
     */
    fun resetAllCustomAudienceOverrides(): ListenableFuture<Void?> {
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
            mTestCustomAudienceManager.resetAllCustomAudienceOverrides(
                    mExecutor,
                    object : OutcomeReceiver<Any?, Exception?> {
                        override fun onResult(ignoredResult: Any?) {
                            completer.set(null)
                        }

                        override fun onError(error: Exception) {
                            completer.setException(error)
                        }
                    })
            "resetAllCustomAudienceOverrides"
        }
    }

    /** Builder class */
    class Builder
    /** Empty-arg constructor with an empty body for Builder  */
    {
        private var mContext: Context? = null
        private var mExecutor: Executor? = null

        /** Sets the context.  */
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

        /**
         * Builds the Custom Audience Client
         *
         * @throws NullPointerException if {@code mContext} is null or if {@code mExecutor} is null
         */
        fun build(): TestCustomAudienceClient {
            Objects.requireNonNull(mContext)
            Objects.requireNonNull(mExecutor)
            return TestCustomAudienceClient(mContext!!, mExecutor!!)
        }
    }

    init {
        mTestCustomAudienceManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mContext.getSystemService(CustomAudienceManager::class.java).testCustomAudienceManager
        } else {
            CustomAudienceManager.get(mContext).testCustomAudienceManager
        }
    }
}