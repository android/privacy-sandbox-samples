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

import android.adservices.adselection.AdSelectionConfig
import android.adservices.adselection.AdSelectionManager
import android.adservices.adselection.AdSelectionOutcome
import android.adservices.adselection.AddAdSelectionOverrideRequest
import android.adservices.adselection.ReportImpressionRequest
import android.adservices.exceptions.AdServicesException
import android.content.Context
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.Objects
import java.util.concurrent.Executor

/**
 * The ad selection client.
 */
@RequiresApi(api = 34)
class AdSelectionClient private constructor(
  mContext: Context,
  private val executor: Executor
) {
  private val adSelectionManager: AdSelectionManager

  /**
   * Invokes the `runAdSelection` method of [AdSelectionManager], and returns a future
   * with [AdSelectionOutcome] if succeeds, or an [AdServicesException] if fails.
   */
  fun runAdSelection(
    adSelectionConfig: AdSelectionConfig
  ): ListenableFuture<AdSelectionOutcome?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<AdSelectionOutcome?> ->
      adSelectionManager.runAdSelection(
        adSelectionConfig,
        executor,
        object : OutcomeReceiver<AdSelectionOutcome, AdServicesException> {
          override fun onResult(result: AdSelectionOutcome) {
            completer.set(result)
          }

          override fun onError(error: AdServicesException) {
            completer.setException(error)
          }
        })
      "Ad Selection"
    }
  }

  /**
   * Invokes the `reportImpression` method of [AdSelectionManager], and returns a Void
   * future
   */
  fun reportImpression(
    input: ReportImpressionRequest
  ): ListenableFuture<Void?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
      adSelectionManager.reportImpression(
        input,
        executor,
        object : NullableOutcomeReceiver<Void?, AdServicesException?> {
          override fun onResult(result: Void?) {
            completer.set(result)
          }

          override fun onError(error: AdServicesException?) {
            completer.setException(error!!)
          }
        })
      "reportImpression"
    }
  }

  /**
   * Overrides remote info for a given `AdSelectionConfig`
   */
  fun overrideAdSelectionConfigRemoteInfo(
    request: AddAdSelectionOverrideRequest
  ): ListenableFuture<Void?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
      adSelectionManager.overrideAdSelectionConfigRemoteInfo(
        request,
        executor,
        object : NullableOutcomeReceiver<Void?, AdServicesException> {
          override fun onResult(result: Void?) {
            completer.set(result)
          }

          override fun onError(error: AdServicesException) {
            completer.setException(error)
          }
        })
      "overrideAdSelectionConfigRemoteInfo"
    }
  }

  /**
   * Resets all ad selection config overrides.
   */
  fun resetAllAdSelectionConfigRemoteOverrides(): ListenableFuture<Void?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
      adSelectionManager.resetAllAdSelectionConfigRemoteOverrides(
        executor,
        object : NullableOutcomeReceiver<Void?, AdServicesException> {
          override fun onResult(result: Void?) {
            completer.set(result)
          }

          override fun onError(error: AdServicesException) {
            completer.setException(error)
          }
        })
      "resetAllAdSelectionConfigRemoteOverrides"
    }
  }

  /** Builder class.  */
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
     * Builds the Ad Selection Client.
     *
     * @throws NullPointerException if `mContext` is null or if `mExecutor` is null
     */
    fun build(): AdSelectionClient {
      Objects.requireNonNull(mContext)
      Objects.requireNonNull(mExecutor)
      return AdSelectionClient(mContext!!, mExecutor!!)
    }
  }

  init {
    adSelectionManager = mContext.getSystemService(AdSelectionManager::class.java)
  }
}