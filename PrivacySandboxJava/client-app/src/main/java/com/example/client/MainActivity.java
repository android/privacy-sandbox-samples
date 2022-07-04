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
package com.example.client;

import android.annotation.SuppressLint;
import android.app.sdksandbox.SdkSandboxManager;
import android.app.sdksandbox.SdkSandboxManager.LoadSdkCallback;
import android.app.sdksandbox.SdkSandboxManager.RequestSurfacePackageCallback;
import android.app.sdksandbox.SdkSandboxManager.SendDataCallback;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceControlViewHost.SurfacePackage;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.example.privacysandbox.client.R;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SandboxClient";
    /**
     * Name of the SDK to be loaded.
     */
    private static final String SDK_NAME = "com.example.privacysandbox.provider";
    /**
     * Button to load the SDK to the sandbox.
     */
    private Button mLoadSdkButton;
    /**
     * Button to request a SurfacePackage from sandbox which remotely render a webview.
     */
    private Button mRequestWebViewButton;
    /**
     * An instance of SdkSandboxManager which contains APIs to communicate with the sandbox.
     */
    private SdkSandboxManager mSdkSandboxManager;
    /**
     * The SurfaceView which will be used by the client app to show the SurfacePackage
     * going to be rendered by the sandbox.
     */
    private SurfaceView mClientView;

    private boolean mSdkLoaded = false;

    @RequiresApi(api = 33)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSdkSandboxManager = getApplicationContext().getSystemService(
            SdkSandboxManager.class);

        mClientView = findViewById(R.id.rendered_view);
        mClientView.setZOrderOnTop(true);

        mLoadSdkButton = findViewById(R.id.load_sdk_button);
        mRequestWebViewButton = findViewById(R.id.request_webview_button);

        registerLoadCodeProviderButton();
        registerRequestWebViewButton();
    }

    /**
     * Register the callback action after once mLoadSdkButton got clicked.
     */
    @RequiresApi(api = 33)
    private void registerLoadCodeProviderButton() {
        mLoadSdkButton.setOnClickListener(v -> {
          log("Attempting to load sandbox SDK");
          final LoadSdkCallbackImpl callback = new LoadSdkCallbackImpl();
          mSdkSandboxManager.loadSdk(
              SDK_NAME, new Bundle(), Runnable::run, callback);
        });
    }

    /**
     * Register the callback action after once mRequestWebViewButton got clicked.
     */
    @RequiresApi(api = 33)
    private void registerRequestWebViewButton() {
        mRequestWebViewButton.setOnClickListener(v -> {
          if (!mSdkLoaded) {
            makeToast("Please load the SDK first!");
            return;
          }
          log("Getting SurfacePackage.");
          new Handler(Looper.getMainLooper()).post(() -> {
            Bundle bundle = new Bundle();
            mSdkSandboxManager.requestSurfacePackage(
                SDK_NAME, getDisplay().getDisplayId(),
                mClientView.getWidth(), mClientView.getHeight(), bundle,
                Runnable::run, new RequestSurfacePackageCallbackImpl());
              });
        });
    }

    /**
     * A callback for tracking events regarding loading of an SDK.
     */
    @RequiresApi(api = 33)
    private class LoadSdkCallbackImpl implements LoadSdkCallback {
        private LoadSdkCallbackImpl() {}

        /**
         * This notifies client application that the requested SDK is successfully loaded.
         *
         * @param params list of params returned from Sdk to the App.
         */
        @SuppressLint("Override")
        @Override
        public void onLoadSdkSuccess(Bundle params) {
            log("onLoadSdkSuccess: " + params);
            makeToast("Loaded successfully!");
            mSdkLoaded = true;
            // Send some data to the SDK if needed.
            mSdkSandboxManager.sendData(SDK_NAME, new Bundle(), Runnable::run,
                new SendDataCallbackImpl());
        }

        /**
         * This notifies client application that the requested Sdk is failed to be loaded.
         *
         * @param errorCode int code for the error
         * @param errorMessage a String description of the error
         */
        @SuppressLint("Override")
        @Override
        public void onLoadSdkFailure(int errorCode, String errorMessage) {
            log("onLoadSdkFailure(" + errorCode + "): " + errorMessage);
            makeToast("Load SDK Failed!" + errorMessage);
        }
    }

    /**
     * A callback for tracking a request for a surface package from an SDK.
     */
    @RequiresApi(api = 33)
    private class RequestSurfacePackageCallbackImpl implements RequestSurfacePackageCallback {
        /**
         * This notifies client application that {@link SurfacePackage}
         * is ready to remote render view from the SDK.
         *
         * @param surfacePackage the requested surface package by
         *            {@link SdkSandboxManager#requestSurfacePackage(String, int, int, int, Bundle,
         *                                              Executor, RequestSurfacePackageCallback)}
         * @param surfacePackageId a unique id for the {@link SurfacePackage} {@code surfacePackage}
         * @param params list of params returned from Sdk to the App.
         */
        @SuppressLint("Override")
        @Override
        public void onSurfacePackageReady(SurfacePackage surfacePackage,
            int surfacePackageId, Bundle params) {
            log("Surface package ready: " + params);
            makeToast("Surface Package Rendered!");
            new Handler(Looper.getMainLooper()).post(() -> {
                log("Setting surface package in the client view");
                mClientView.setChildSurfacePackage(surfacePackage);
                mClientView.setVisibility(View.VISIBLE);
            });
        }

        /**
         * This notifies client application that requesting {@link SurfacePackage} has failed.
         *
         * @param errorCode int code for the error
         * @param errorMessage a String description of the error
         */
        @SuppressLint("Override")
        @Override
        public void onSurfacePackageError(int errorCode, String errorMessage) {
            log("onSurfacePackageError" + errorCode + "): " + errorMessage);
            makeToast("Surface Package Failed!" + errorMessage);
        }
    }

    /**
     * A callback for tracking sending of data to an SDK.
     */
    @RequiresApi(api = 33)
    private class SendDataCallbackImpl implements SendDataCallback {
        private SendDataCallbackImpl() {}

        /**
         * This notifies the client application that sending data to the SDK has completed
         * successfully.
         *
         * @param params list of params returned from Sdk to the App.
         */
        @SuppressLint("Override")
        @Override
        public void onSendDataSuccess(Bundle params) {
            log("onSendDataSuccess: " + params);
            makeToast("Sent data successfully!");
        }

        /**
         * This notifies client application that sending data to an SDK has failed.
         *
         * @param errorCode int code for the error
         * @param errorMessage a String description of the error
         */
        @SuppressLint("Override")
        @Override
        public void onSendDataError(int errorCode, String errorMessage) {
            log("onSendDataError(" + errorCode + "): " + errorMessage);
            makeToast("Send data to SDK failed!" + errorMessage);
        }
    }

    private void makeToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void log(String message) {
        Log.e(TAG, message);
    }
}
