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

import android.adservices.customaudience.CustomAudience
import android.adservices.customaudience.CustomAudienceManager
import android.adservices.customaudience.JoinCustomAudienceRequest
import android.adservices.customaudience.LeaveCustomAudienceRequest
import android.adservices.exceptions.AdServicesException
import android.content.Context
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.Objects
import java.util.concurrent.Executor

/**
 * The custom audience client.
 */
@RequiresApi(api = 34)
class CustomAudienceClient private constructor(
  context: Context,
  private val executor: Executor
) {
  private val customAudienceManager: CustomAudienceManager

  /** Join custom audience.  */
  fun joinCustomAudience(customAudience: CustomAudience?): ListenableFuture<Void?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
      val request = JoinCustomAudienceRequest.Builder()
        .setCustomAudience(customAudience!!)
        .build()
      customAudienceManager.joinCustomAudience(
        request,
        executor,
        object : NullableOutcomeReceiver<Void?, AdServicesException?> {
          override fun onResult(result: Void?) {
            completer.set(null)
          }

          override fun onError(error: AdServicesException?) {
            completer.setException(error!!)
          }
        })
      "joinCustomAudience"
    }
  }


  /** Leave custom audience.  */
  fun leaveCustomAudience(
    owner: String, buyer: String, name: String
  ): ListenableFuture<Void?> {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?> ->
      val request = LeaveCustomAudienceRequest.Builder()
        .setOwner(owner)
        .setBuyer(buyer)
        .setName(name)
        .build()
      customAudienceManager.leaveCustomAudience(
        request,
        executor,
        object : NullableOutcomeReceiver<Void?, AdServicesException?> {
          override fun onResult(result: Void?) {
            completer.set(null)
          }

          override fun onError(error: AdServicesException?) {
            completer.setException(error!!)
          }
        })
      "leaveCustomAudience"
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

    /** Builds a [CustomAudienceClient] instance  */
    fun build(): CustomAudienceClient {
      Objects.requireNonNull(mContext)
      Objects.requireNonNull(mExecutor)
      return CustomAudienceClient(mContext!!, mExecutor!!)
    }
  }

  init {
    customAudienceManager = context.getSystemService(
      CustomAudienceManager::class.java)
  }
}