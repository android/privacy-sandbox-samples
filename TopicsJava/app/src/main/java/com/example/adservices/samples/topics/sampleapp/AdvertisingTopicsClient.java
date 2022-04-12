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
package com.example.adservices.samples.topics.sampleapp;

import android.adservices.exceptions.GetTopicsException;
import android.adservices.topics.GetTopicsRequest;
import android.adservices.topics.GetTopicsResponse;
import android.adservices.topics.TopicsManager;
import android.content.Context;
import android.os.OutcomeReceiver;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** This class is a helper class for making call to AdServices's TopicsManager.
 * It pass app's SdkName to TopicsManager's getTopics call and deliver result
 * (GetTopicsResponse if success or throw exception if error). */
public class AdvertisingTopicsClient {

    // SDK used by app, this will be used to construct GetTopicsRequest
    private String mSdkName;

    // AdService's TopicsManager will be used to retrieve topics assigned to this app
    private TopicsManager mTopicsManager;

    // context of current app
    private Context mContext;

    // Executor that will be used by TopicManager to retrieve topics assigned to this app
    private Executor mExecutor;

    // constructor for AdvertisingTopicsClient class
    private AdvertisingTopicsClient(
        @NonNull Context context, @NonNull Executor executor, @NonNull String sdkName) {
        mContext = context;
        mSdkName = sdkName;
        mExecutor = executor;
        mTopicsManager = mContext.getSystemService(TopicsManager.class);
    }

    /** Gets the SdkName. */
    @NonNull
    public String getSdkName() {
        return mSdkName;
    }

    /** Gets the context. */
    @NonNull
    public Context getContext() {
        return mContext;
    }

    /** Gets the worker executor. */
    @NonNull
    public Executor getExecutor() {
        return mExecutor;
    }

    /** Gets the topics. */
    public @NonNull ListenableFuture<GetTopicsResponse> getTopics() {
        return CallbackToFutureAdapter.getFuture(
            completer -> {
                mTopicsManager.getTopics(
                    new GetTopicsRequest.Builder().setSdkName(mSdkName).build(),
                    mExecutor,
                    new OutcomeReceiver<GetTopicsResponse, GetTopicsException>() {
                        @Override
                        public void onResult(@NonNull GetTopicsResponse result) {
                            completer.set(result);
                        }
                        @Override
                        public void onError(@NonNull GetTopicsException error) {
                            completer.setException(error);
                        }
                    });
                // This value is used only for debug purposes: it will be used in toString()
                // of returned future or error cases.
                return "getTopics";
            });
    }

    /** Builder class. */
    public static final class Builder {
        private String mSdkName;
        private Context mContext;
        private Executor mExecutor;

        /** Empty-arg constructor with an empty body for Builder */
        public Builder() {}

        /** Sets the context. */
        public @NonNull AdvertisingTopicsClient.Builder setContext(@NonNull Context context) {
            mContext = context;
            return this;
        }

        /** Sets the SdkName. */
        public @NonNull Builder setSdkName(@NonNull String sdkName) {
            mSdkName = sdkName;
            return this;
        }

        /**
         * Sets the worker executor.
         *
         * <p>If an executor is not provided, the AdvertisingTopicsClient default executor will be
         * used.
         *
         * @param executor the worker executor used to run heavy background tasks.
         */
        @NonNull
        public Builder setExecutor(@NonNull Executor executor) {
            Objects.requireNonNull(executor);
            mExecutor = executor;
            return this;
        }

        /** Builds a {@link AdvertisingTopicsClient} instance */
        public @NonNull AdvertisingTopicsClient build() {
            if (mExecutor == null) {
                mExecutor = Executors.newCachedThreadPool();
            }
            return new AdvertisingTopicsClient(mContext, mExecutor, mSdkName);
        }
    }
}