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
import android.adservices.adselection.AdSelectionFromOutcomesConfig
import android.adservices.adselection.AdSelectionManager
import android.adservices.adselection.AdSelectionOutcome
import android.adservices.adselection.GetAdSelectionDataOutcome
import android.adservices.adselection.GetAdSelectionDataRequest
import android.adservices.adselection.PersistAdSelectionResultRequest
import android.adservices.adselection.ReportEventRequest
import android.adservices.adselection.ReportImpressionRequest
import android.adservices.adselection.SetAppInstallAdvertisersRequest
import android.adservices.adselection.UpdateAdCounterHistogramRequest
import android.annotation.SuppressLint
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
  private val executor: Executor,
) {
  private val adSelectionManager: AdSelectionManager

  /**
   * Invokes the {@getAdSelectionData} method of [AdSelectionManager], and returns a
   * GetAdSelectionDataOutcome future.
   */
  @SuppressLint("MissingPermission")
  fun getAdSelectionData(
    request: GetAdSelectionDataRequest,
  ): ListenableFuture<GetAdSelectionDataOutcome?> {
    return CallbackToFutureAdapter.getFuture<GetAdSelectionDataOutcome?> { completer: CallbackToFutureAdapter.Completer<GetAdSelectionDataOutcome?> ->
      adSelectionManager.getAdSelectionData(
        request,
        executor,
        object : OutcomeReceiver<GetAdSelectionDataOutcome, java.lang.Exception> {
          override fun onResult(result: GetAdSelectionDataOutcome) {
            completer.set(result)
          }

          override fun onError(error: java.lang.Exception) {
            completer.setException(error)
          }
        })
      "getAdSelectionData"
    }
  }

  /**
   * Invokes the {@persistAdSelectionResult} method of [AdSelectionManager], and returns a
   * AdSelectionOutcome future.
   */
  @SuppressLint("MissingPermission")
  fun persistAdSelectionResult(
    request: PersistAdSelectionResultRequest,
  ): ListenableFuture<AdSelectionOutcome?> {
    return CallbackToFutureAdapter.getFuture<AdSelectionOutcome?> { completer: CallbackToFutureAdapter.Completer<AdSelectionOutcome?> ->
      adSelectionManager.persistAdSelectionResult(
        request,
        executor,
        object : OutcomeReceiver<AdSelectionOutcome, java.lang.Exception> {
          override fun onResult(result: AdSelectionOutcome) {
            completer.set(result)
          }

          override fun onError(error: java.lang.Exception) {
            completer.setException(error)
          }
        })
      "persistAdSelectionResult"
    }
  }

  /**
   * Invokes the {@code selectAds} method of {@link AdSelectionManager}, and returns a future with
   * {@link AdSelectionOutcome} if succeeds, or an {@link Exception} if fails.
   */
  fun selectAds(
    adSelectionConfig: AdSelectionConfig,
  ): ListenableFuture<AdSelectionOutcome?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<AdSelectionOutcome?> ->
      adSelectionManager.selectAds(
        adSelectionConfig,
        executor,
        object : OutcomeReceiver<AdSelectionOutcome, Exception> {
          override fun onResult(result: AdSelectionOutcome) {
            completer.set(
              AdSelectionOutcome.Builder()
                .setAdSelectionId(result.adSelectionId)
                .setRenderUri(result.renderUri)
                .build())
          }

          override fun onError(error: Exception) {
            completer.setException(error)
          }
        })
      "Ad Selection"
    }
  }

  /**
   * Invokes the {@code selectAds} method of {@link AdSelectionManager}, and returns a future with
   * {@link AdSelectionOutcome} if succeeds, or an {@link Exception} if fails.
   */
  fun selectAds(
    adSelectionFromOutcomesConfig: AdSelectionFromOutcomesConfig,
  ): ListenableFuture<AdSelectionOutcome?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<AdSelectionOutcome?> ->
      adSelectionManager.selectAds(
        adSelectionFromOutcomesConfig,
        executor,
        object : OutcomeReceiver<AdSelectionOutcome, Exception> {
          override fun onResult(result: AdSelectionOutcome) {
            completer.set(result)
          }

          override fun onError(error: Exception) {
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
    input: ReportImpressionRequest,
  ): ListenableFuture<Void?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
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
        })
      "reportImpression"
    }
  }

  /**
   * Invokes the `setAppInstallAdvertisers` method of [AdSelectionManager], and returns a Void
   * future
   */
  fun setAppInstallAdvertisers(
    input: SetAppInstallAdvertisersRequest,
  ): ListenableFuture<Void?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
      adSelectionManager.setAppInstallAdvertisers(
        input,
        executor,
        object : NullableOutcomeReceiver<Any?, java.lang.Exception?> {
          override fun onResult(result: Any?) {
            completer.set(null)
          }

          override fun onError(error: java.lang.Exception?) {
            completer.setException(error!!)
          }
        })
      "setAppInstallAdvertisers"
    }
  }

  fun reportInteraction(
    request: ReportEventRequest,
  ): ListenableFuture<Void?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
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
        })
      "reportInteraction"
    }
  }

  /**
   * Invokes the `updateAdCounterHistogram` method of [AdSelectionManager], and returns
   * a Void future.
   */
  fun updateAdCounterHistogram(
    updateAdCounterHistogramRequest: UpdateAdCounterHistogramRequest,
  ): ListenableFuture<Void?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
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
        })
      "updateAdCounterHistogram"
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