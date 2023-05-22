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

import static android.app.sdksandbox.SdkSandboxManager.EXTRA_DISPLAY_ID;
import static android.app.sdksandbox.SdkSandboxManager.EXTRA_HEIGHT_IN_PIXELS;
import static android.app.sdksandbox.SdkSandboxManager.EXTRA_HOST_TOKEN;
import static android.app.sdksandbox.SdkSandboxManager.EXTRA_SURFACE_PACKAGE;
import static android.app.sdksandbox.SdkSandboxManager.EXTRA_WIDTH_IN_PIXELS;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.sdksandbox.LoadSdkException;
import android.app.sdksandbox.RequestSurfacePackageException;
import android.app.sdksandbox.SandboxedSdk;
import android.app.sdksandbox.SdkSandboxManager;
import android.app.sdksandbox.SdkSandboxManager.SdkSandboxProcessDeathCallback;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;
import android.view.SurfaceControlViewHost.SurfacePackage;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.example.exampleaidllibrary.ISdkApi;
import com.example.privacysandbox.client.R;
import java.util.concurrent.Executor;

@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SandboxClient";
    /**
     * Name of the privacy sandbox SDK to be loaded.
     */
    private static final String SDK_NAME = "com.example.privacysandbox.provider";
    /**
     * Name of the mediated sandbox SDK to be loaded.
     */
    private static final String MEDIATEE_SDK_NAME = "com.example.mediatee.provider";
    /**
     * Button to load the SDKs to the sandbox.
     */
    private Button mLoadSdksButton;
    /**
     * Button to request a SurfacePackage from sandbox which remotely render a webview.
     */
    private Button mRequestWebViewButton;
    /**
     * Button to create a file inside sandbox.
     */
    private Button mCreateFileButton;
    /**
     * An instance of SdkSandboxManager which contains APIs to communicate with the sandbox.
     */
    private SdkSandboxManager mSdkSandboxManager;
    /**
     * The SurfaceView which will be used by the client app to show the SurfacePackage
     * going to be rendered by the sandbox.
     */
    private SurfaceView mClientView;
    /**
     * This object is going to be set when SDK is successfully loaded. It is a wrapper for the
     * public SDK API Binder object defined by SDK by implementing the AIDL file from
     * example-aidl-library module.
     */
    private SandboxedSdk mSandboxedSdk;

    private boolean mSdksLoaded = false;
    private boolean mSdkToSdkCommEnabled = false;

    @RequiresApi(api = 33)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSdkSandboxManager = getApplicationContext().getSystemService(
                SdkSandboxManager.class);

        mClientView = findViewById(R.id.rendered_view);
        mClientView.setZOrderOnTop(true);

        mLoadSdksButton = findViewById(R.id.load_sdk_button);
        mRequestWebViewButton = findViewById(R.id.request_webview_button);
        mCreateFileButton = findViewById(R.id.create_file_button);

        registerLoadCodeProviderButton();
        registerRequestWebViewButton();
        registerCreateFileButton();
        registerSdkToSdkButton();
    }

    /**
     * Register the callback action using {@link OutcomeReceiver} after once mLoadSdksButton got clicked.
     */
    @RequiresApi(api = 33)
    private void registerLoadCodeProviderButton() {
        mLoadSdksButton.setOnClickListener(v -> {
            if (mSdksLoaded) {
                resetStateForLoadSdkButton();
                return;
            }

            // Register for sandbox death event.
            mSdkSandboxManager.addSdkSandboxProcessDeathCallback(
                    Runnable::run, new SdkSandboxProcessDeathCallbackImpl());

            Bundle params = new Bundle();
            OutcomeReceiver<SandboxedSdk, LoadSdkException> mediateeReceiver =
                    new OutcomeReceiver<SandboxedSdk, LoadSdkException>() {
                        @Override
                        public void onResult(SandboxedSdk sandboxedSdk) {
                            makeToast("All SDKs Loaded successfully!");
                            Log.i(TAG, "All SDKs Loaded successfully!");
                            mSdksLoaded = true;
                            refreshLoadSdksButtonText();
                        }

                        @Override
                        public void onError(LoadSdkException error) {
                            makeToast("Failed to load all SDKs: " + error.getMessage());
                            Log.e(TAG, "Failed to load all SDKs: " + error.getMessage());
                        }
                    };
            OutcomeReceiver<SandboxedSdk, LoadSdkException> receiver =
                    new OutcomeReceiver<SandboxedSdk, LoadSdkException>() {
                        @Override
                        public void onResult(SandboxedSdk sandboxedSdk) {
                            mSandboxedSdk = sandboxedSdk;
                            mSdkSandboxManager.loadSdk(
                                    MEDIATEE_SDK_NAME,
                                    params,
                                    Runnable::run,
                                    mediateeReceiver);
                        }

                        @Override
                        public void onError(LoadSdkException error) {
                            makeToast("Failed to load first SDK: " + error.getMessage());
                            Log.e(TAG, "Failed to load first SDK: " + error.getMessage());
                        }
                    };
            Log.i(TAG, "Loading SDKs " + SDK_NAME + " and " + MEDIATEE_SDK_NAME);
            mSdkSandboxManager.loadSdk(SDK_NAME, params, Runnable::run, receiver);
        });
    }

    /**
     * Unload the SDKs and reset the state of button to Load SDKs
     */
    private void resetStateForLoadSdkButton() {
        mSdkSandboxManager.unloadSdk(SDK_NAME);
        mSdkSandboxManager.unloadSdk(MEDIATEE_SDK_NAME);
        mLoadSdksButton.setText("Load SDKs");
        mSdksLoaded = false;
    }

    /**
     * Refresh the state of Load SDKs button to either Load SDKs or
     * Unload SDKs according to {@value mSdksLoaded}
     */
    private void refreshLoadSdksButtonText() {
        if (mSdksLoaded) {
            mLoadSdksButton.post(() -> mLoadSdksButton.setText("Unload SDKs"));
        } else {
            mLoadSdksButton.post(() -> mLoadSdksButton.setText("Load SDKs"));
        }
    }

    /**
     * Register the callback action after once mRequestWebViewButton got clicked.
     */
    @RequiresApi(api = 33)
    private void registerRequestWebViewButton() {
        mRequestWebViewButton.setOnClickListener(v -> {
            if (!mSdksLoaded) {
                makeToast("Please load the SDK first!");
                return;
            }

            log("Getting SurfacePackage.");
            new Handler(Looper.getMainLooper()).post(() -> {
                Bundle params = new Bundle();
                params.putInt(EXTRA_WIDTH_IN_PIXELS, mClientView.getWidth());
                params.putInt(EXTRA_HEIGHT_IN_PIXELS, mClientView.getHeight());
                params.putInt(EXTRA_DISPLAY_ID, getDisplay().getDisplayId());
                params.putBinder(EXTRA_HOST_TOKEN, mClientView.getHostToken());
                mSdkSandboxManager.requestSurfacePackage(
                        SDK_NAME, params, Runnable::run, new RequestSurfacePackageCallbackImpl());
            });
        });
    }

    /**
     * Register the callback action after once mCreateFileButton got clicked.
     */
    @RequiresApi(api = 33)
    private void registerCreateFileButton() {
        mCreateFileButton.setOnClickListener(v -> {
            if (!mSdksLoaded) {
                makeToast("Please load the SDK first!");
                return;
            }
            log("Creating a file inside sandbox.");

            // Show dialog to collect the size of storage
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Set size in MB");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int sizeInMb = -1;
                    try {
                        sizeInMb = Integer.parseInt(input.getText().toString());
                    } catch (Exception ignore) {}
                    if (sizeInMb <= 0) {
                        makeToast("Please provide positive integer value");
                        return;
                    }

                    IBinder binder = mSandboxedSdk.getInterface();
                    ISdkApi sdkApi = ISdkApi.Stub.asInterface(binder);
                    try {
                        String response = sdkApi.createFile(sizeInMb);
                        makeToast(response);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        });
    }

    /**
     * Creates a bundle with required parameters for SDK-SDK communication
     */
    private Bundle getRequestSurfacePackageParams(SurfaceView surfaceView) {
        Bundle params = new Bundle();
        final String EXTRA_SDK_SDK_ENABLED_KEY = "sdkSdkCommEnabled";
        params.putInt(EXTRA_WIDTH_IN_PIXELS, surfaceView.getWidth());
        params.putInt(EXTRA_HEIGHT_IN_PIXELS, surfaceView.getHeight());
        params.putInt(EXTRA_DISPLAY_ID, getDisplay().getDisplayId());
        params.putBinder(EXTRA_HOST_TOKEN, surfaceView.getHostToken());
        params.putString(EXTRA_SDK_SDK_ENABLED_KEY, "SDK_IN_SANDBOX");
        return params;
    }

    /**
     * Register the communication between SDKs {@value SDK_NAME} and {@value MEDIATEE_SDK_NAME}
     */
    private void registerSdkToSdkButton() {
        // Button for SDK-SDK communication.
        final Button mSdkToSdkCommButton = findViewById(R.id.enable_sdk_sdk_button);
        mSdkToSdkCommButton.setOnClickListener(
                v -> {
                    mSdkToSdkCommEnabled = !mSdkToSdkCommEnabled;
                    if (mSdkToSdkCommEnabled) {
                        mSdkToSdkCommButton.setText("Disable SDK to SDK comm");
                        makeToast("Sdk to Sdk Comm Enabled");
                        final SurfaceView view = mClientView;
                        mSdkSandboxManager.requestSurfacePackage(
                                SDK_NAME,
                                getRequestSurfacePackageParams(view),
                                Runnable::run,
                                new RequestSurfacePackageCallbackImpl());
                    } else {
                        mSdkToSdkCommButton.setText("Enable SDK to SDK comm");
                        makeToast("Sdk to Sdk Comm Disabled");
                    }
                });
    }

    /**
     * A callback for tracking Sdk Sandbox process death event.
     */
    @RequiresApi(api = 33)
    private class SdkSandboxProcessDeathCallbackImpl implements SdkSandboxProcessDeathCallback {
        /**
         * Notifies the client application that the SDK sandbox has died. The sandbox could die for
         * various reasons, for example, due to memory pressure on the system, or a crash in the
         * sandbox.
         *
         * The system will automatically restart the sandbox process if it died due to a crash.
         * However, the state of the sandbox will be lost - so any SDKs that were loaded previously
         * would have to be loaded again, using {@link SdkSandboxManager#loadSdk(String, Bundle,
         * Executor, OutcomeReceiver)} to continue using them.
         */
        @SuppressLint("Override")
        @Override
        public void onSdkSandboxDied() {
            makeToast("Sdk Sandbox process died");
        }
    }

    /**
     * A callback for tracking a request for a surface package from an SDK.
     */
    @RequiresApi(api = 33)
    private class RequestSurfacePackageCallbackImpl
            implements OutcomeReceiver<Bundle, RequestSurfacePackageException> {
        /**
         * This notifies client application that {@link SurfacePackage}
         * is ready to remote render view from the SDK.
         *
         * @param response a {@link Bundle} which should contain the key EXTRA_SURFACE_PACKAGE with
         * a value of {@link SurfacePackage} response.
         */
        @SuppressLint("Override")
        @Override
        public void onResult(Bundle response) {
            log("Surface package ready");
            makeToast("Surface Package Rendered!");
            new Handler(Looper.getMainLooper()).post(() -> {
                log("Setting surface package in the client view");
                SurfacePackage surfacePackage = response.getParcelable(
                        EXTRA_SURFACE_PACKAGE, SurfacePackage.class);
                mClientView.setChildSurfacePackage(surfacePackage);
                mClientView.setVisibility(View.VISIBLE);
            });
        }

        /**
         * This notifies client application that requesting {@link SurfacePackage} has failed.
         *
         * @param error a {@link RequestSurfacePackageException} containing the details of failing
         *              to request the surface package.
         */
        @SuppressLint("Override")
        @Override
        public void onError(RequestSurfacePackageException error) {
            log("onSurfacePackageError" + error.getRequestSurfacePackageErrorCode() + "): "
                    + error.getMessage());
            makeToast("Surface Package Failed! " + error.getMessage());
        }
    }

    private void makeToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void log(String message) {
        Log.e(TAG, message);
    }
}
