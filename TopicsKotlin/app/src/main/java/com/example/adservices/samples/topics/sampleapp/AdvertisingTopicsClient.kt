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
package com.example.adservices.samples.topics.sampleapp

import android.adservices.topics.TopicsManager
import android.adservices.topics.GetTopicsResponse
import androidx.concurrent.futures.CallbackToFutureAdapter
import android.adservices.topics.GetTopicsRequest
import android.os.OutcomeReceiver
import android.adservices.exceptions.GetTopicsException
import android.content.Context

import com.google.common.util.concurrent.ListenableFuture
import java.util.Objects
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** This class is a helper class for making call to AdServices's TopicsManager.
 * It pass app's SdkName to TopicsManager's getTopics call and deliver result
 * (GetTopicsResponse if success or throw exception if error). */
class AdvertisingTopicsClient private constructor(
  /** Gets the context.  */
  val context: Context,
  /** Gets the worker executor.  */
  val executor: Executor,
  /** Gets the SdkName.  */
  val sdkName: String
) {
  private val mTopicsManager: TopicsManager

  /** Gets the topics.  */
  val topics: ListenableFuture<GetTopicsResponse?>
    get() = CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<GetTopicsResponse?> ->
      mTopicsManager.getTopics(
        GetTopicsRequest.Builder().setSdkName(sdkName).build(),
        executor,
        object : OutcomeReceiver<GetTopicsResponse, GetTopicsException> {
          override fun onResult(result: GetTopicsResponse) {
            completer.set(result)
          }

          override fun onError(error: GetTopicsException) {
            completer.setException(error)
          }
        })
      "getTopics"
    }

  /** Builder class.  */
  class Builder
  /** Empty-arg constructor with an empty body for Builder  */
  {
    private var mSdkName: String? = null
    private var mContext: Context? = null
    private var mExecutor: Executor? = null

    /** Sets the context.  */
    fun setContext(context: Context): Builder {
      mContext = context
      return this
    }

    /** Sets the SdkName.  */
    fun setSdkName(sdkName: String): Builder {
      mSdkName = sdkName
      return this
    }

    /**
     * Sets the worker executor.
     *
     *
     * If an executor is not provided, the AdvertisingTopicsClient default executor will be
     * used.
     *
     * @param executor the worker executor used to run heavy background tasks.
     */
    fun setExecutor(executor: Executor): Builder {
      Objects.requireNonNull(executor)
      mExecutor = executor
      return this
    }

    /** Builds a [AdvertisingTopicsClient] instance  */
    fun build(): AdvertisingTopicsClient {
      if (mExecutor == null) {
        mExecutor = Executors.newCachedThreadPool()
      }
      return AdvertisingTopicsClient(mContext!!, mExecutor!!, mSdkName!!)
    }
  }

  init {
    mTopicsManager = context.getSystemService(TopicsManager::class.java)
  }
}