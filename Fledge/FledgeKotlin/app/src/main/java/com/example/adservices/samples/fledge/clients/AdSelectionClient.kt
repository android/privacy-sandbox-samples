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
import android.adservices.adselection.GetAdSelectionDataOutcome
import android.adservices.adselection.GetAdSelectionDataRequest
import android.adservices.adselection.PersistAdSelectionResultRequest
import android.adservices.adselection.ReportEventRequest
import android.adservices.adselection.ReportImpressionRequest
import android.adservices.adselection.UpdateAdCounterHistogramRequest
import android.annotation.SuppressLint
import android.content.Context
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.example.adservices.samples.fledge.sdkExtensionsHelpers.VersionCompatUtil.isTestableVersion
import com.example.adservices.samples.fledge.sampleapp.TAG
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.Objects
import java.util.concurrent.Executor


/** The ad selection client. */
@RequiresApi(api = 34)
class AdSelectionClient private constructor(mContext: Context, private val executor: Executor) {
  private val adSelectionManager: AdSelectionManager

  /**
   * Invokes the {@code selectAds} method of {@link AdSelectionManager}, and returns a future with
   * {@link AdSelectionOutcome} if succeeds, or an {@link Exception} if fails.
   */
  fun selectAds(adSelectionConfig: AdSelectionConfig): ListenableFuture<AdSelectionOutcome?> {
    return CallbackToFutureAdapter.getFuture {
      completer: CallbackToFutureAdapter.Completer<AdSelectionOutcome?> ->
      adSelectionManager.selectAds(
        adSelectionConfig,
        executor,
        object : OutcomeReceiver<AdSelectionOutcome, Exception> {
          override fun onResult(result: AdSelectionOutcome) {
            completer.set(
              AdSelectionOutcome.Builder()
                .setAdSelectionId(result.adSelectionId)
                .setRenderUri(result.renderUri)
                .build()
            )
          }

          override fun onError(error: Exception) {
            completer.setException(error)
          }
        }
      )
      "Ad Selection"
    }
  }

  /** Invokes the `reportImpression` method of [AdSelectionManager], and returns a Void future */
  fun reportImpression(input: ReportImpressionRequest): ListenableFuture<Void?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?>
      ->
      adSelectionManager.reportImpression(
        input,
        executor,
        object : NullableOutcomeReceiver<Any?, java.lang.Exception?> {
          override fun onResult(result: Any?) {
            completer.set(null)
          }

          override fun onError(error: java.lang.Exception?) {
            completer.setException(error!!)
          }
        }
      )
      "reportImpression"
    }
  }
  @SuppressLint("NewApi")
  fun reportEvent(request: ReportEventRequest): ListenableFuture<Void?> {
    if (!isTestableVersion(8, 9)) {
      Log.w(TAG, "Unsupported SDK Extension: Event reporting requires 8 for T+ or 9 for S-, skipping")
      return Futures.immediateVoidFuture()
    }

    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?>
      ->
      adSelectionManager.reportEvent(
        request,
        executor,
        object : OutcomeReceiver<Any, java.lang.Exception> {
          override fun onResult(ignoredResult: Any) {
            completer.set(null)
          }

          override fun onError(error: java.lang.Exception) {
            completer.setException(error)
          }
        }
      )
      "reportEvent"
    }
  }

  /**
   * Invokes the `updateAdCounterHistogram` method of [AdSelectionManager], and returns a Void
   * future.
   */
  @SuppressLint("NewApi")
  fun updateAdCounterHistogram(
    updateAdCounterHistogramRequest: UpdateAdCounterHistogramRequest
  ): ListenableFuture<Void?> {
    if (!isTestableVersion(8, 9)) {
      Log.w(TAG, "Unsupported SDK Extension: Ad counter histogram update requires 8 for T+ or 9 for S-, skipping")
      return Futures.immediateVoidFuture()
    }

    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?>
      ->
      adSelectionManager.updateAdCounterHistogram(
        updateAdCounterHistogramRequest,
        executor,
        object : OutcomeReceiver<Any, java.lang.Exception> {
          override fun onResult(ignoredResult: Any) {
            completer.set(null)
          }

          override fun onError(error: java.lang.Exception) {
            completer.setException(error)
          }
        }
      )
      "updateAdCounterHistogram"
    }
  }

  /**
   * Invokes the `getAdSelectionData` method of [AdSelectionManager], and returns a
   * GetAdSelectionDataOutcome future.
   */
  @SuppressLint("NewApi")
  fun getAdSelectionData(
          request: GetAdSelectionDataRequest): ListenableFuture<GetAdSelectionDataOutcome?> {
    if (!isTestableVersion(10, 10)) {
      Log.w(
          TAG,
          "Unsupported SDK Extension: Get Ad Selection Data requires 10, skipping")
      return Futures.immediateFailedFuture(
          IllegalStateException("Unsupported SDK Extension: Get Ad Selection Data requires 10, skipping")
      )
    }
    return CallbackToFutureAdapter.getFuture {
      completer: CallbackToFutureAdapter.Completer<GetAdSelectionDataOutcome?> ->
              adSelectionManager.getAdSelectionData(
                      request,
                      executor,
                      object : OutcomeReceiver<GetAdSelectionDataOutcome, Exception> {
                        override fun onResult(result: GetAdSelectionDataOutcome) {
                          completer.set(result)
                        }

                        override fun onError(error: Exception) {
                          completer.setException(error)
                        }
                      })
              "getAdSelectionData"
            }
  }

  /**
   * Invokes the `persistAdSelectionResult` method of [AdSelectionManager], and returns a
   * AdSelectionOutcome future.
   */
  @SuppressLint("NewApi")
  fun persistAdSelectionResult(
          request: PersistAdSelectionResultRequest): ListenableFuture<AdSelectionOutcome?> {
    if (!isTestableVersion(10, 10)) {
      Log.w(
          TAG,
          "Unsupported SDK Extension: Persist Ad Selection Result requires 10, skipping")
      return Futures.immediateFailedFuture(
          IllegalStateException("Unsupported SDK Extension: Persist Ad Selection Result requires 10, skipping")
      )
    }
    return CallbackToFutureAdapter.getFuture {
        completer: CallbackToFutureAdapter.Completer<AdSelectionOutcome?> ->
              adSelectionManager.persistAdSelectionResult(
                      request,
                      executor,
                      object : OutcomeReceiver<AdSelectionOutcome, Exception> {
                        override fun onResult(result: AdSelectionOutcome) {
                          completer.set(result)
                        }

                        override fun onError(error: Exception) {
                          completer.setException(error)
                        }
                      })
              "persistAdSelectionResult"
            }
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
    adSelectionManager = AdSelectionManager.get(mContext)
  }
}
