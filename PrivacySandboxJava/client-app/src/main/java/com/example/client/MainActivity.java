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
import android.app.sdksandbox.SdkSandboxManager.SdkSandboxLifecycleCallback;
import android.app.sdksandbox.SendDataException;
import android.app.sdksandbox.SendDataResponse;
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

import com.example.myaidllibrary.ISdkApi;
import com.example.privacysandbox.client.R;
import java.util.concurrent.Executor;

@SuppressLint("NewApi")
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
        mCreateFileButton = findViewById(R.id.create_file_button);

        registerLoadCodeProviderButton();
        registerRequestWebViewButton();
        registerCreateFileButton();
    }

    /**
     * Register the callback action after once mLoadSdkButton got clicked.
     */
    @RequiresApi(api = 33)
    private void registerLoadCodeProviderButton() {
        mLoadSdkButton.setOnClickListener(v -> {
          // Register for sandbox death event.
          mSdkSandboxManager.addSdkSandboxLifecycleCallback(
              Runnable::run, new SdkSandboxLifecycleCallbackImpl());

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
            if (!mSdkLoaded) {
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

                    // Let SDK know the size of file we want it to create.
                    final Bundle params = new Bundle();
                    params.putString("method", "createFile");
                    params.putInt("sizeInMb", sizeInMb);
                    mSdkSandboxManager.sendData(SDK_NAME, params, Runnable::run,
                        new OutcomeReceiver<SendDataResponse, SendDataException>() {
                            @Override
                            public void onResult(SendDataResponse response) {
                                makeToast(response
                                            .getExtraInformation()
                                            .getString("message", "Something went wrong"));
                            }

                            @Override
                            public void onError(SendDataException error) {
                                makeToast("File creation failed: " + error.getMessage());
                            }
                        });
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
     * A callback for tracking events regarding loading of an SDK.
     */
    @RequiresApi(api = 33)
    private class LoadSdkCallbackImpl implements OutcomeReceiver<SandboxedSdk, LoadSdkException> {
        /**
         * This notifies client application that the requested SDK is successfully loaded.
         *
         * @param sandboxedSdk a {@link SandboxedSdk} is returned from the sandbox to the app.
         */
        @SuppressLint("Override")
        @Override
        public void onResult(SandboxedSdk sandboxedSdk) {
            log("SDK is loaded");
            makeToast("Loaded successfully!");
            mSdkLoaded = true;

            IBinder binder = sandboxedSdk.getInterface();
            ISdkApi sdkApi = ISdkApi.Stub.asInterface(binder);

            // Send some message to the SDK if needed.
            try {
                sdkApi.sayHello("Hi! The binder was successfully received.");
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            // Send some data to the SDK if needed. This will soon be deprecated.
            mSdkSandboxManager.sendData(SDK_NAME, new Bundle(), Runnable::run,
                new SendDataCallbackImpl());
        }

        /**
         * This notifies client application that the requested Sdk failed to be loaded.
         *
         * @param error a {@link LoadSdkException} containing the details of failing to load the
         *              SDK.
         */
        @SuppressLint("Override")
        @Override
        public void onError(LoadSdkException error) {
            log("onLoadSdkFailure(" + error.getLoadSdkErrorCode() + "): " + error.getMessage());
            makeToast("Load SDK Failed! " + error.getMessage());
        }
    }

    /**
     * A callback for tracking Sdk Sandbox lifecycle events.
     */
    @RequiresApi(api = 33)
    private class SdkSandboxLifecycleCallbackImpl implements SdkSandboxLifecycleCallback {
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

    /**
     * A callback for tracking sending of data to an SDK.
     */
    @RequiresApi(api = 33)
    private class SendDataCallbackImpl
                implements OutcomeReceiver<SendDataResponse, SendDataException> {
        /**
         * This notifies the client application that sending data to the SDK has completed
         * successfully.
         *
         * @param response a {@link SendDataResponse} containing a bundle of data returned from the
         *                 SDK to the App.
         */
        @SuppressLint("Override")
        @Override
        public void onResult(SendDataResponse response) {
            log("onSendDataSuccess: " + response.getExtraInformation());
            makeToast("Sent data successfully!");
        }

        /**
         * This notifies client application that sending data to an SDK has failed.
         *
         * @param error a {@link SendDataException} containing the details of failing to send data
         *              to the SDK.
         */
        @SuppressLint("Override")
        @Override
        public void onError(SendDataException error) {
            log("onSendDataError(" + error.getSendDataErrorCode() + "): " + error.getMessage());
            makeToast("Send data to SDK failed!" + error.getMessage());
        }
    }

    private void makeToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void log(String message) {
        Log.e(TAG, message);
    }
}
