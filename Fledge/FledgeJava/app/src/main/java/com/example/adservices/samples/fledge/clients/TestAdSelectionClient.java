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

import android.adservices.adselection.AdSelectionManager;
import android.adservices.adselection.AddAdSelectionOverrideRequest;
import android.adservices.adselection.RemoveAdSelectionOverrideRequest;
import android.adservices.adselection.TestAdSelectionManager;
import android.content.Context;
import android.os.OutcomeReceiver;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.Executor;

/** This is the Overrides Ad Selection Client  */
public class TestAdSelectionClient {
  private TestAdSelectionManager mTestAdSelectionManager;
  private Context mContext;
  private Executor mExecutor;

  private TestAdSelectionClient(@NonNull Context context, @NonNull Executor executor) {
    mContext = context;
    mExecutor = executor;
    mTestAdSelectionManager =
        mContext.getSystemService(AdSelectionManager.class).getTestAdSelectionManager();
  }

  /**
   * Invokes the {@code overrideAdSelectionConfigRemoteInfo} method of {@link AdSelectionManager},
   * and returns a Void future
   *
   * <p>This method is only available when Developer mode is enabled and the app is debuggable.
   */
  @NonNull
  public ListenableFuture<Void> overrideAdSelectionConfigRemoteInfo(
      @NonNull AddAdSelectionOverrideRequest request) {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mTestAdSelectionManager.overrideAdSelectionConfigRemoteInfo(
              request,
              mExecutor,
              new OutcomeReceiver<Object, Exception>() {

                @Override
                public void onResult(Object ignoredResult) {
                  completer.set(null);
                }

                @Override
                public void onError(@NonNull Exception error) {
                  completer.setException(error);
                }
              });
          return "overrideAdSelectionConfigRemoteInfo";
        });
  }

  /**
   * Invokes the {@code removeAdSelectionConfigRemoteInfoOverride} method of {@link
   * AdSelectionManager}, and returns a Void future
   *
   * <p>This method is only available when Developer mode is enabled and the app is debuggable.
   */
  @NonNull
  public ListenableFuture<Void> removeAdSelectionConfigRemoteInfoOverride(
      @NonNull RemoveAdSelectionOverrideRequest request) {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mTestAdSelectionManager.removeAdSelectionConfigRemoteInfoOverride(
              request,
              mExecutor,
              new OutcomeReceiver<Object, Exception>() {

                @Override
                public void onResult(Object ignoredResult) {
                  completer.set(null);
                }

                @Override
                public void onError(@NonNull Exception error) {
                  completer.setException(error);
                }
              });
          return "removeAdSelectionConfigRemoteInfoOverride";
        });
  }

  /**
   * Invokes the {@code removeAdSelectionConfigRemoteInfoOverride} method of {@link
   * AdSelectionManager}, and returns a Void future
   *
   * <p>This method is only available when Developer mode is enabled and the app is debuggable.
   */
  @NonNull
  public ListenableFuture<Void> resetAllAdSelectionConfigRemoteOverrides() {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mTestAdSelectionManager.resetAllAdSelectionConfigRemoteOverrides(
              mExecutor,
              new OutcomeReceiver<Object, Exception>() {

                @Override
                public void onResult(Object ignoredResult) {
                  completer.set(null);
                }

                @Override
                public void onError(@NonNull Exception error) {
                  completer.setException(error);
                }
              });
          return "resetAllAdSelectionConfigRemoteOverrides";
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
    public TestAdSelectionClient.Builder setContext(@NonNull Context context) {
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
    public TestAdSelectionClient.Builder setExecutor(@NonNull Executor executor) {
      Objects.requireNonNull(executor);

      mExecutor = executor;
      return this;
    }

    /**
     * Builds the Ad Selection Client.
     *
     * @throws NullPointerException if {@code mContext} is null or if {@code mExecutor} is null
     */
    @NonNull
    public TestAdSelectionClient build() {
      Objects.requireNonNull(mContext);
      Objects.requireNonNull(mExecutor);

      return new TestAdSelectionClient(mContext, mExecutor);
    }
  }
}
