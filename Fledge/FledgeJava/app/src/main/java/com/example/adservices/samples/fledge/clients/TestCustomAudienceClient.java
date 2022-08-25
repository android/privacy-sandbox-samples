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
package com.example.adservices.samples.fledge.clients;

import android.adservices.customaudience.AddCustomAudienceOverrideRequest;
import android.adservices.customaudience.CustomAudienceManager;
import android.adservices.customaudience.RemoveCustomAudienceOverrideRequest;
import android.adservices.customaudience.TestCustomAudienceManager;
import android.content.Context;
import android.os.OutcomeReceiver;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Client for override APIs.
 */
public class TestCustomAudienceClient {
  private final TestCustomAudienceManager mTestCustomAudienceManager;
  private final Context mContext;
  private final Executor mExecutor;

  private TestCustomAudienceClient(
      @NonNull Context context, @NonNull Executor executor) {
    mContext = context;
    mExecutor = executor;
    mTestCustomAudienceManager =
        mContext.getSystemService(CustomAudienceManager.class)
            .getTestCustomAudienceManager();
  }

  /**
   * Invokes the {@code overrideCustomAudienceRemoteInfo} method of {@link CustomAudienceManager},
   * and returns a Void future
   */
  @NonNull
  public ListenableFuture<Void> overrideCustomAudienceRemoteInfo(
      @NonNull AddCustomAudienceOverrideRequest request) {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mTestCustomAudienceManager.overrideCustomAudienceRemoteInfo(
              request,
              mExecutor,
              new OutcomeReceiver<Object, Exception>() {
                @Override
                public void onResult(Object ignoredResult) {
                  completer.set(null);
                }

                @Override
                public void onError(Exception error) {
                  completer.setException(error);
                }
              });
          // This value is used only for debug purposes: it will be used in toString()
          // of returned future or error cases.
          return "overrideCustomAudienceRemoteInfo";
        });
  }

  /**
   * Invokes the {@code removeCustomAudienceRemoteInfoOverride} method of {@link
   * CustomAudienceManager}, and returns a Void future
   */
  @NonNull
  public ListenableFuture<Void> removeCustomAudienceRemoteInfoOverride(
      @NonNull RemoveCustomAudienceOverrideRequest request) {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mTestCustomAudienceManager.removeCustomAudienceRemoteInfoOverride(
              request,
              mExecutor,
              new OutcomeReceiver<Object, Exception>() {
                @Override
                public void onResult(Object ignoredResult) {
                  completer.set(null);
                }

                @Override
                public void onError(Exception error) {
                  completer.setException(error);
                }
              });
          // This value is used only for debug purposes: it will be used in toString()
          // of returned future or error cases.
          return "removeCustomAudienceRemoteInfoOverride";
        });
  }

  /**
   * Invokes the {@code resetAllCustomAudienceOverrides} method of {@link CustomAudienceManager},
   * and returns a Void future
   */
  @NonNull
  public ListenableFuture<Void> resetAllCustomAudienceOverrides() {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mTestCustomAudienceManager.resetAllCustomAudienceOverrides(
              mExecutor,
              new OutcomeReceiver<Object, Exception>() {
                @Override
                public void onResult(Object ignoredResult) {
                  completer.set(null);
                }

                @Override
                public void onError(Exception error) {
                  completer.setException(error);
                }
              });
          // This value is used only for debug purposes: it will be used in toString()
          // of returned future or error cases.
          return "resetAllCustomAudienceOverrides";
        });
  }

  /** Builder class. */
  public static final class Builder {
    private Context mContext;
    private Executor mExecutor;

    /** Empty-arg constructor with an empty body for Builder */
    public Builder() {}

    /** Sets the context. */
    @NonNull
    public TestCustomAudienceClient.Builder setContext(@NonNull Context context) {
      Objects.requireNonNull(context);
      mContext = context;
      return this;
    }

    /**
     * Sets the worker executor.
     *
     * @param executor the worker executor used to run heavy background tasks.
     */
    @NonNull
    public TestCustomAudienceClient.Builder setExecutor(@NonNull Executor executor) {
      Objects.requireNonNull(executor);
      mExecutor = executor;
      return this;
    }

    /** Builds a {@link TestCustomAudienceClient} instance */
    @NonNull
    public TestCustomAudienceClient build() {
      Objects.requireNonNull(mContext);
      Objects.requireNonNull(mExecutor);

      return new TestCustomAudienceClient(mContext, mExecutor);
    }
  }
}
