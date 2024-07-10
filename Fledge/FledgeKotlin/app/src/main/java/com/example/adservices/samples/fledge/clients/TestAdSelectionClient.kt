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

import android.adservices.adselection.AdSelectionManager
import android.adservices.adselection.AddAdSelectionFromOutcomesOverrideRequest
import android.adservices.adselection.AddAdSelectionOverrideRequest
import android.adservices.adselection.RemoveAdSelectionFromOutcomesOverrideRequest
import android.adservices.adselection.RemoveAdSelectionOverrideRequest
import android.adservices.adselection.TestAdSelectionManager
import android.content.Context
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.Objects
import java.util.concurrent.Executor


/** This is the Override Ad Selection Client */
@RequiresApi(api = 34)
class TestAdSelectionClient
private constructor(
  mContext: Context,
  private val mExecutor: Executor,
) {
  private val mTestAdSelectionManager: TestAdSelectionManager

  /**
   * Invokes the {@code overrideAdSelectionConfigRemoteInfo} method of {@link AdSelectionManager},
   * and returns a Void future
   *
   * <p>This method is only available when Developer mode is enabled and the app is debuggable.
   */
  fun overrideAdSelectionConfigRemoteInfo(
    request: AddAdSelectionOverrideRequest): ListenableFuture<Void?>? {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?>
      ->
      mTestAdSelectionManager.overrideAdSelectionConfigRemoteInfo(
        request,
        mExecutor,
        object : OutcomeReceiver<Any?, java.lang.Exception> {
          override fun onResult(p0: Any?) {
            completer.set(null)
          }

          override fun onError(error: java.lang.Exception) {
            completer.setException(error)
          }
        }
      )
      "overrideAdSelectionConfigRemoteInfo"
    }
  }

  /**
   * Invokes the {@code removeAdSelectionConfigRemoteInfoOverride} method of {@link
   * AdSelectionManager}, and returns a Void future
   *
   * <p>This method is only available when Developer mode is enabled and the app is debuggable.
   */
  fun removeAdSelectionConfigRemoteInfoOverride(
    request: RemoveAdSelectionOverrideRequest,
  ): ListenableFuture<Void?>? {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?>
      ->
      mTestAdSelectionManager.removeAdSelectionConfigRemoteInfoOverride(
        request,
        mExecutor,
        object : OutcomeReceiver<Any?, java.lang.Exception> {
          override fun onResult(p0: Any?) {
            completer.set(null)
          }

          override fun onError(error: java.lang.Exception) {
            completer.setException(error)
          }
        }
      )
      "removeAdSelectionConfigRemoteInfoOverride"
    }
  }

  /**
   * Invokes the {@code removeAdSelectionConfigRemoteInfoOverride} method of {@link
   * AdSelectionManager}, and returns a Void future
   *
   * <p>This method is only available when Developer mode is enabled and the app is debuggable.
   */
  fun resetAllAdSelectionConfigRemoteOverrides(): ListenableFuture<Void?>? {
    return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Void?>
      ->
      mTestAdSelectionManager.resetAllAdSelectionConfigRemoteOverrides(
        mExecutor,
        object : OutcomeReceiver<Any?, Exception> {
          override fun onResult(p0: Any?) {
            completer.set(null)
          }

          override fun onError(error: Exception) {
            completer.setException(error)
          }
        }
      )
      "resetAllAdSelectionConfigRemoteOverrides"
    }
  }

  /** Builder class */
  class Builder
    /** Empty-arg constructor with an empty body for Builder */
  {
    private var mContext: Context? = null
    private var mExecutor: Executor? = null

    /** Sets the context. */
    fun setContext(context: Context): TestAdSelectionClient.Builder {
      Objects.requireNonNull(context)
      mContext = context
      return this
    }

    /**
     * Sets the worker executor.
     *
     * @param executor the worker executor used to run heavy background tasks.
     */
    fun setExecutor(executor: Executor): TestAdSelectionClient.Builder {
      Objects.requireNonNull(executor)
      mExecutor = executor
      return this
    }

    /**
     * Builds the Ad Selection Client.
     *
     * @throws NullPointerException if {@code mContext} is null or if {@code mExecutor} is null
     */
    fun build(): TestAdSelectionClient {
      Objects.requireNonNull(mContext)
      Objects.requireNonNull(mExecutor)
      return TestAdSelectionClient(mContext!!, mExecutor!!)
    }
  }

  init {
    mTestAdSelectionManager =
     AdSelectionManager.get(mContext).testAdSelectionManager
  }
}
