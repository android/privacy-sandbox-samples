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
package com.example.client

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.RequestSurfacePackageException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.app.sdksandbox.SdkSandboxManager.EXTRA_DISPLAY_ID
import android.app.sdksandbox.SdkSandboxManager.EXTRA_HEIGHT_IN_PIXELS
import android.app.sdksandbox.SdkSandboxManager.EXTRA_HOST_TOKEN
import android.app.sdksandbox.SdkSandboxManager.EXTRA_SURFACE_PACKAGE
import android.app.sdksandbox.SdkSandboxManager.EXTRA_WIDTH_IN_PIXELS
import android.app.sdksandbox.SdkSandboxManager.SdkSandboxLifecycleCallback
import android.content.DialogInterface
import android.os.*
import android.text.InputType
import android.util.Log
import android.view.SurfaceControlViewHost.SurfacePackage
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.exampleaidllibrary.ISdkApi
import com.example.privacysandbox.client.R

@SuppressLint("NewApi")
class MainActivity : AppCompatActivity() {
    /**
     * Button to load the SDK to the sandbox.
     */
    private lateinit var mLoadSdkButton: Button

    /**
     * Button to request a SurfacePackage from sandbox which remotely render a webview.
     */
    private lateinit var mRequestWebViewButton: Button

    /**
     * Button to create a file inside sandbox.
     */
    private lateinit var mCreateFileButton: Button

    /**
     * An instance of SdkSandboxManager which contains APIs to communicate with the sandbox.
     */
    private lateinit var mSdkSandboxManager: SdkSandboxManager

    /**
     * The SurfaceView which will be used by the client app to show the SurfacePackage
     * going to be rendered by the sandbox.
     */
    private lateinit var mClientView: SurfaceView

    /**
     * This object is going to be set when SDK is successfully loaded. It is a wrapper for the
     * public SDK API Binder object defined by SDK by implementing the AIDL file from
     * example-aidl-library module.
     */
    private lateinit var mSandboxedSdk : SandboxedSdk

    private var mSdkLoaded = false


    @RequiresApi(api = 33)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSdkSandboxManager = applicationContext.getSystemService(
            SdkSandboxManager::class.java
        )
        mClientView = findViewById(R.id.rendered_view)
        mClientView.setZOrderOnTop(true)
        mLoadSdkButton = findViewById(R.id.load_sdk_button)
        mRequestWebViewButton = findViewById(R.id.request_webview_button)
        mCreateFileButton = findViewById(R.id.create_file_button)
        registerLoadCodeProviderButton()
        registerRequestWebViewButton()
        registerCreateFileButton()
    }

    /**
     * Register the callback action after once mLoadSdkButton got clicked.
     */
    @RequiresApi(api = 33)
    private fun registerLoadCodeProviderButton() {
        mLoadSdkButton.setOnClickListener { _: View? ->
            // Register for sandbox death event.
            mSdkSandboxManager.addSdkSandboxLifecycleCallback(
                { obj: Runnable -> obj.run() }, SdkSandboxLifecycleCallbackImpl())
            log("Attempting to load sandbox SDK")
            val callback = LoadSdkCallbackImpl()
            mSdkSandboxManager.loadSdk(
                SDK_NAME, Bundle(), { obj: Runnable -> obj.run() }, callback
            )
        }
    }

    /**
     * Register the callback action after once mRequestWebViewButton got clicked.
     */
    @RequiresApi(api = 33)
    private fun registerRequestWebViewButton() {
        mRequestWebViewButton.setOnClickListener {
            if (!mSdkLoaded) {
                makeToast("Please load the SDK first!")
                return@setOnClickListener
            }
            log("Getting SurfacePackage.")
            Handler(Looper.getMainLooper()).post {
                val params = Bundle()
                params.putInt(EXTRA_WIDTH_IN_PIXELS, mClientView.getWidth())
                params.putInt(EXTRA_HEIGHT_IN_PIXELS, mClientView.getHeight())
                params.putInt(EXTRA_DISPLAY_ID, getDisplay()?.getDisplayId()!!)
                params.putBinder(EXTRA_HOST_TOKEN, mClientView.getHostToken())
                mSdkSandboxManager.requestSurfacePackage(
                    SDK_NAME, params, { obj: Runnable -> obj.run() }, RequestSurfacePackageCallbackImpl())
            }
        }
    }

    /**
     * Register the callback action after once mCreateFileButton got clicked.
     */
    @RequiresApi(api = 33)
    private fun registerCreateFileButton() {
        mCreateFileButton.setOnClickListener { _ ->
            if (!mSdkLoaded) {
                makeToast("Please load the SDK first!")
                return@setOnClickListener
            }
            log("Creating file inside sandbox.")

            // Show dialog to collect the size of storage
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Set size in MB")
            val input = EditText(this)
            input.setInputType(InputType.TYPE_CLASS_NUMBER)
            builder.setView(input)
            builder.setPositiveButton("Create", object : DialogInterface.OnClickListener {

                override fun onClick(dialog: DialogInterface?, which: Int) {
                    var sizeInMb = -1
                    try {
                        sizeInMb = Integer.parseInt(input.getText().toString())
                    } catch (ignore: Exception) {
                    }
                    if (sizeInMb <= 0) {
                        makeToast("Please provide positive integer value")
                        return
                    }

                    val binder: IBinder? = mSandboxedSdk.getInterface()
                    val sdkApi = ISdkApi.Stub.asInterface(binder)

                    try {
                        val response: String = sdkApi.createFile(sizeInMb)
                        makeToast(response)
                    } catch (e: RemoteException) {
                        throw RuntimeException(e)
                    }
                }
            })
            builder.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {

                override fun onClick(dialog: DialogInterface, which: Int) {
                    dialog.cancel()
                }
            })
            builder.show()
        }
    }

    /**
     * A callback for tracking events regarding loading an SDK.
     */
    @RequiresApi(api = 33)
    private inner class LoadSdkCallbackImpl() : OutcomeReceiver<SandboxedSdk, LoadSdkException> {
        /**
         * This notifies client application that the requested SDK is successfully loaded.
         *
         * @param sandboxedSdk a [SandboxedSdk] is returned from the sandbox to the app.
         */
        @SuppressLint("Override")
        override fun onResult(sandboxedSdk: SandboxedSdk) {
            log("SDK is loaded")
            makeToast("Loaded successfully!")
            mSdkLoaded = true
            mSandboxedSdk = sandboxedSdk;
        }

        /**
         * This notifies client application that the requested Sdk failed to be loaded.
         *
         * @param error a [LoadSdkException] containing the details of failing to load the
         * SDK.
         */
        @SuppressLint("Override")
        override fun onError(error: LoadSdkException) {
            log("onLoadSdkFailure(" + error.getLoadSdkErrorCode().toString() + "): " + error.message)
            makeToast("Load SDK Failed! " + error.message)
        }
    }

    /**
     * A callback for tracking Sdk Sandbox lifecycle events.
     */
    @RequiresApi(api = 33)
    private inner class SdkSandboxLifecycleCallbackImpl() : SdkSandboxLifecycleCallback {
        /**
         * Notifies the client application that the SDK sandbox has died. The sandbox could die for
         * various reasons, for example, due to memory pressure on the system, or a crash in the
         * sandbox.
         *
         * The system will automatically restart the sandbox process if it died due to a crash.
         * However, the state of the sandbox will be lost - so any SDKs that were loaded previously
         * would have to be loaded again, using [SdkSandboxManager.loadSdk] to continue using them.
         */
        @SuppressLint("Override")
        override fun onSdkSandboxDied() {
            makeToast("Sdk Sandbox process died")
        }
    }

    /**
     * A callback for tracking a request for a surface package from an SDK.
     */
    @RequiresApi(api = 33)
    private inner class RequestSurfacePackageCallbackImpl() :
        OutcomeReceiver<Bundle?, RequestSurfacePackageException?> {
        /**
         * This notifies client application that [SurfacePackage]
         * is ready to remote render view from the SDK.
         *
         * @param response a [Bundle] which should contain the key EXTRA_SURFACE_PACKAGE with
         * a value of [SurfacePackage] response.
         */
        @SuppressLint("Override")
        override fun onResult(response: Bundle) {
            log("Surface package ready")
            makeToast("Surface Package Rendered!")
            Handler(Looper.getMainLooper()).post {
                log("Setting surface package in the client view")
                val surfacePackage: SurfacePackage? = response.getParcelable(
                    EXTRA_SURFACE_PACKAGE, SurfacePackage::class.java)
                mClientView.setChildSurfacePackage(surfacePackage!!)
                mClientView.setVisibility(View.VISIBLE)
            }
        }

        /**
         * This notifies client application that requesting [SurfacePackage] has failed.
         *
         * @param error a [RequestSurfacePackageException] containing the details of failing
         * to request the surface package.
         */
        @SuppressLint("Override")
        override fun onError(error: RequestSurfacePackageException) {
            log("onSurfacePackageError" + error.getRequestSurfacePackageErrorCode()
                .toString() + "): "
                  + error.message)
            makeToast("Surface Package Failed! " + error.message)
        }
    }

    private fun makeToast(message: String) {
        runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
    }

    private fun log(message: String) {
        Log.e(TAG, message)
    }

    companion object {
        private const val TAG = "SandboxClient"

        /**
         * Name of the SDK to be loaded.
         */
        private const val SDK_NAME = "com.example.privacysandbox.provider"
    }
}
