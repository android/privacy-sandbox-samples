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
import android.app.sdksandbox.SdkSandboxManager
import android.app.sdksandbox.SdkSandboxManager.LoadSdkCallback
import android.app.sdksandbox.SdkSandboxManager.RequestSurfacePackageCallback
import android.app.sdksandbox.SdkSandboxManager.SendDataCallback
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.SurfaceView
import android.view.SurfaceControlViewHost.SurfacePackage
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import com.example.client.MainActivity
import com.example.privacysandbox.client.R

import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

import java.util.concurrent.Executor
import java.lang.Runnable

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
                val bundle = Bundle()
                val callback = RequestSurfacePackageCallbackImpl()
                mSdkSandboxManager.requestSurfacePackage(
                    SDK_NAME, display!!.displayId,
                    mClientView.width, mClientView.height, bundle,
                    { obj: Runnable -> obj.run() }, callback
                )
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

                    // Let SDK know the size of file we want it to create.
                    val params = Bundle()
                    params.putString("method", "createFile")
                    params.putInt("sizeInMb", sizeInMb)
                    mSdkSandboxManager.sendData(SDK_NAME, params, Runnable::run,
                                                object : SendDataCallback {
                                                    override fun onSendDataSuccess(params: Bundle) {
                                                         makeToast(params.getString(
                                                             "message", "Something went wrong"));
                                                    }
                                                    override fun onSendDataError(
                                                        errorCode: Int,errorMessage: String) {
                                                        makeToast("File creation failed: " +
                                                                    "$errorMessage")
                                                    }
                                                })
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
    private inner class LoadSdkCallbackImpl() : LoadSdkCallback {
        /**
         * This notifies client application that the requested SDK is successfully loaded.
         *
         * @param params list of params returned from Sdk to the App.
         */
        @SuppressLint("Override")
        override fun onLoadSdkSuccess(params: Bundle) {
            log("onLoadSdkSuccess: $params")
            makeToast("Loaded successfully!")
            mSdkLoaded = true
            // Send data to the SDK if needed
            val callback = SendDataCallbackImpl()
            mSdkSandboxManager.sendData(SDK_NAME, Bundle(), { obj: Runnable -> obj.run() },
                                        callback)
        }

        /**
         * This notifies client application that the requested Sdk is failed to be loaded.
         *
         * @param errorCode int code for the error
         * @param errorMessage a String description of the error
         */
        @SuppressLint("Override")
        override fun onLoadSdkFailure(errorCode: Int, errorMessage: String) {
            log("onLoadSdkFailure($errorCode): $errorMessage")
            makeToast("Load SDK Failed!$errorMessage")
        }
    }

    /**
     * A callback for tracking a request for a surface package from an SDK.
     */
    @RequiresApi(api = 33)
    private inner class RequestSurfacePackageCallbackImpl() : RequestSurfacePackageCallback {
        /**
         * This notifies client application that [SurfacePackage]
         * is ready to remote render view from the SDK.
         *
         * @param surfacePackage the requested surface package by
         * [SdkSandboxManager.requestSurfacePackage]
         * @param surfacePackageId a unique id for the [SurfacePackage] `surfacePackage`
         * @param params list of params returned from Sdk to the App.
         */
        @SuppressLint("Override")
        override fun onSurfacePackageReady(
            surfacePackage: SurfacePackage,
            surfacePackageId: Int, params: Bundle
        ) {
            log("Surface package ready: $params")
            makeToast("Surface Package Rendered!")
            Handler(Looper.getMainLooper()).post {
                log("Setting surface package in the client view")
                mClientView.setChildSurfacePackage(surfacePackage)
                mClientView.visibility = View.VISIBLE
            }
        }

        /**
         * This notifies client application that requesting [SurfacePackage] has failed.
         *
         * @param errorCode int code for the error
         * @param errorMessage a String description of the error
         */
        @SuppressLint("Override")
        override fun onSurfacePackageError(errorCode: Int, errorMessage: String) {
            log("onSurfacePackageError$errorCode): $errorMessage")
            makeToast("Surface Package Failed!$errorMessage")
        }
    }

    /**
     * A callback for tracking events regarding sending of data to an SDK.
     */
    @RequiresApi(api = 33)
    private inner class SendDataCallbackImpl() : SendDataCallback {
        /**
         * This notifies the client application that sending data to the SDK has completed
         * successfully.
         *
         * @param params list of params returned from Sdk to the App.
         */
        @SuppressLint("Override")
        override fun onSendDataSuccess(params: Bundle) {
            log("onSendDataSuccess: $params")
            makeToast("Sent data successfully!")
        }

        /**
         * This notifies client application that sending data to an SDK has failed.
         *
         * @param errorCode int code for the error
         * @param errorMessage a String description of the error
         */
        @SuppressLint("Override")
        override fun onSendDataError(errorCode: Int, errorMessage: String) {
            log("onSendDataError($errorCode): $errorMessage")
            makeToast("Send data to SDK Failed!$errorMessage")
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
