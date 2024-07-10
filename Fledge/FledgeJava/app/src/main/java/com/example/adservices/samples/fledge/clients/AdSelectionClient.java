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

import static com.example.adservices.samples.fledge.SdkExtensionsHelpers.VersionCompatUtil.isTestableVersion;

import android.adservices.adselection.AdSelectionConfig;
import android.adservices.adselection.AdSelectionFromOutcomesConfig;
import android.adservices.adselection.AdSelectionManager;
import android.adservices.adselection.AdSelectionOutcome;
import android.adservices.adselection.GetAdSelectionDataOutcome;
import android.adservices.adselection.GetAdSelectionDataRequest;
import android.adservices.adselection.PersistAdSelectionResultRequest;
import android.adservices.adselection.ReportEventRequest;
import android.adservices.adselection.ReportImpressionRequest;
import android.adservices.adselection.UpdateAdCounterHistogramRequest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.OutcomeReceiver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.example.adservices.samples.fledge.sampleapp.MainActivity;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.Executor;

/** The ad selection client. */
@RequiresApi(api = 34)
public class AdSelectionClient {
  private final AdSelectionManager mAdSelectionManager;
  private final Executor mExecutor;

  private AdSelectionClient(@NonNull Context context, @NonNull Executor executor) {
    mExecutor = executor;
    mAdSelectionManager = AdSelectionManager.get(context);
  }

  /**
   * Invokes the {@code selectAds} method of {@link AdSelectionManager}, and returns a future with
   * {@link AdSelectionOutcome} if succeeds, or an {@link Exception} if fails.
   */
  @NonNull
  public ListenableFuture<AdSelectionOutcome> selectAds(
      @NonNull AdSelectionConfig adSelectionConfig) {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mAdSelectionManager.selectAds(
              adSelectionConfig,
              mExecutor,
              new OutcomeReceiver<AdSelectionOutcome, Exception>() {

                @Override
                public void onResult(@NonNull AdSelectionOutcome result) {
                  completer.set(
                      new AdSelectionOutcome.Builder()
                          .setAdSelectionId(result.getAdSelectionId())
                          .setRenderUri(result.getRenderUri())
                          .build());
                }

                @Override
                public void onError(@NonNull Exception error) {
                  completer.setException(error);
                }
              });
          return "Ad Selection";
        });
  }

  /**
   * Invokes the {@code selectAds} method of {@link AdSelectionManager}, and returns a future with
   * {@link AdSelectionOutcome} if succeeds, or an {@link Exception} if fails.
   */
  @NonNull
  @SuppressLint("NewApi")
  public ListenableFuture<AdSelectionOutcome> selectAds(
      @NonNull AdSelectionFromOutcomesConfig config) {
    if (!isTestableVersion(10, 10)) {
      Log.w(MainActivity.TAG, "Unsupported SDK Extension: Ad Selection From Outcomes Config requires 10, skipping");
      return Futures.immediateFailedFuture(
          new IllegalStateException("Unsupported SDK Extension: Ad Selection From Outcomes Config requires 10, skipping")
      );
    }
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mAdSelectionManager.selectAds(
              config,
              mExecutor,
              new OutcomeReceiver<AdSelectionOutcome, Exception>() {
                @Override
                public void onResult(@Nullable AdSelectionOutcome result) {
                  completer.set(result);
                }

                @Override
                public void onError(@NonNull Exception error) {
                  completer.setException(error);
                }
              });
          return "Ad Selection from outcomes";
        });
  }

  /**
   * Invokes the {@code reportImpression} method of {@link AdSelectionManager}, and returns a Void
   * future
   */
  @NonNull
  public ListenableFuture<Void> reportImpression(@NonNull ReportImpressionRequest input) {
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mAdSelectionManager.reportImpression(
              input,
              mExecutor,
              new OutcomeReceiver<Object, Exception>() {
                @Override
                public void onResult(@NonNull Object ignoredResult) {
                  completer.set(null);
                }

                @Override
                public void onError(@NonNull Exception error) {
                  completer.setException(error);
                }
              });
          return "reportImpression";
        });
  }

  /**
   * Invokes the {@code reportEvent} method of {@link AdSelectionManager}, and returns a Void future
   */
  @SuppressLint("NewApi")
  @NonNull
  public ListenableFuture<Void> reportEvent(@NonNull ReportEventRequest request) {
    if (!isTestableVersion(8, 9)) {
      Log.w(MainActivity.TAG, "Unsupported SDK Extension: Event reporting requires 8 for T+ or 9 for S-, skipping");
      return Futures.immediateVoidFuture();
    }

    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mAdSelectionManager.reportEvent(
              request,
              mExecutor,
              new OutcomeReceiver<Object, Exception>() {
                @Override
                public void onResult(@NonNull Object ignoredResult) {
                  completer.set(null);
                }

                @Override
                public void onError(@NonNull Exception error) {
                  completer.setException(error);
                }
              });
          return "reportEvent";
        });
  }

  /**
   * Invokes the {@code updateAdCounterHistogram} method of {@link AdSelectionManager}, and returns
   * a Void future.
   */
  @SuppressLint("NewApi")
  @NonNull
  public ListenableFuture<Void> updateAdCounterHistogram(
      @NonNull UpdateAdCounterHistogramRequest updateAdCounterHistogramRequest) {
    if (!isTestableVersion(8, 9)) {
      Log.w(
          MainActivity.TAG,
          "Unsupported SDK Extension: Ad counter histogram update requires 8 for T+ or 9 for S-, skipping");
      return Futures.immediateVoidFuture();
    }

    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mAdSelectionManager.updateAdCounterHistogram(
              updateAdCounterHistogramRequest,
              mExecutor,
              new OutcomeReceiver<Object, Exception>() {
                @Override
                public void onResult(@NonNull Object ignoredResult) {
                  completer.set(null);
                }

                @Override
                public void onError(@NonNull Exception error) {
                  completer.setException(error);
                }
              });
          return "updateAdCounterHistogram";
        });
  }

  /**
   * Invokes {@link AdSelectionManager#getAdSelectionData}, and returns a GetAdSelectionDataOutcome future.
   */
  @NonNull
  @SuppressLint({"MissingPermission", "NewApi"})
  public ListenableFuture<GetAdSelectionDataOutcome> getAdSelectionData(
      @NonNull GetAdSelectionDataRequest request) {
    if (!isTestableVersion(10, 10)) {
      Log.w(
          MainActivity.TAG,
          "Unsupported SDK Extension: Get Ad Selection Data requires 10, skipping");
      return Futures.immediateFailedFuture(
          new IllegalStateException("Unsupported SDK Extension: Get Ad Selection Data requires 10, skipping")
      );
    }
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mAdSelectionManager.getAdSelectionData(
              request,
              mExecutor,
              new OutcomeReceiver<GetAdSelectionDataOutcome, Exception>() {
                @Override
                public void onResult(@NonNull GetAdSelectionDataOutcome result) {
                  completer.set(result);
                }

                @Override
                public void onError(@NonNull Exception error) {
                  completer.setException(error);
                }
              });
          return "getAdSelectionData";
        });
  }

  /**
   * Invokes {@link AdSelectionManager#persistAdSelectionResult} and returns an AdSelectionOutcome future.
   */
  @NonNull
  @SuppressLint({"MissingPermission", "NewApi"})
  public ListenableFuture<AdSelectionOutcome> persistAdSelectionResult(
      @NonNull PersistAdSelectionResultRequest request) {
    if (!isTestableVersion(10, 10)) {
      Log.w(
          MainActivity.TAG,
          "Unsupported SDK Extension: Persist Ad Selection Result requires 10, skipping");
      return Futures.immediateFailedFuture(
          new IllegalStateException("Unsupported SDK Extension: Persist Ad Selection Result requires 10, skipping")
      );
    }
    return CallbackToFutureAdapter.getFuture(
        completer -> {
          mAdSelectionManager.persistAdSelectionResult(
              request,
              mExecutor,
              new OutcomeReceiver<AdSelectionOutcome, Exception>() {
                @Override
                public void onResult(@NonNull AdSelectionOutcome result) {
                  completer.set(result);
                }

                @Override
                public void onError(@NonNull Exception error) {
                  completer.setException(error);
                }
              });
          return "persistAdSelectionResult";
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
    public AdSelectionClient.Builder setContext(@NonNull Context context) {
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
    public AdSelectionClient.Builder setExecutor(@NonNull Executor executor) {
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
    public AdSelectionClient build() {
      Objects.requireNonNull(mContext);
      Objects.requireNonNull(mExecutor);

      return new AdSelectionClient(mContext, mExecutor);
    }
  }
}
