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

import android.adservices.common.AdSelectionSignals;
import android.adservices.common.AdTechIdentifier;
import android.adservices.customaudience.CustomAudience;
import android.adservices.customaudience.CustomAudienceManager;
import android.adservices.customaudience.FetchAndJoinCustomAudienceRequest;
import android.adservices.customaudience.JoinCustomAudienceRequest;
import android.adservices.customaudience.LeaveCustomAudienceRequest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.OutcomeReceiver;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executor;

/** The custom audience client. */
@RequiresApi(api = 34)
public class CustomAudienceClient {
  private final CustomAudienceManager mCustomAudienceManager;
  private final Executor mExecutor;

  private CustomAudienceClient(@NonNull Context context, @NonNull Executor executor) {
    mExecutor = executor;
    mCustomAudienceManager =CustomAudienceManager.get(context);
  }

  /** Join custom audience. */
  @NonNull
  public ListenableFuture<Void> joinCustomAudience(CustomAudience customAudience) {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          JoinCustomAudienceRequest request =
              new JoinCustomAudienceRequest.Builder().setCustomAudience(customAudience).build();
          mCustomAudienceManager.joinCustomAudience(
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
          return "joinCustomAudience";
        });
  }

  /** Fetch and Join custom audience. */
  @NonNull
  @SuppressLint("NewApi")
  public ListenableFuture<Void> fetchAndJoinCustomAudience(
      FetchAndJoinCustomAudienceRequest fetchAndJoinCustomAudienceRequest) {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mCustomAudienceManager.fetchAndJoinCustomAudience(
              fetchAndJoinCustomAudienceRequest,
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
          return "fetchAndJoinCustomAudience";
        });
  }

  /** Leave custom audience. */
  @NonNull
  public ListenableFuture<Void> leaveCustomAudience(@NonNull AdTechIdentifier buyer, @NonNull String name) {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          LeaveCustomAudienceRequest request =
              new LeaveCustomAudienceRequest.Builder().setBuyer(buyer).setName(name).build();
          mCustomAudienceManager.leaveCustomAudience(
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
          return "leaveCustomAudience";
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
    public Builder setContext(@NonNull Context context) {
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
    public Builder setExecutor(@NonNull Executor executor) {
      Objects.requireNonNull(executor);
      mExecutor = executor;
      return this;
    }

    /** Builds a {@link CustomAudienceClient} instance */
    @NonNull
    public CustomAudienceClient build() {
      Objects.requireNonNull(mContext);
      Objects.requireNonNull(mExecutor);

      return new CustomAudienceClient(mContext, mExecutor);
    }
  }
}
