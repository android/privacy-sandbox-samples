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

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.client.SdkSandboxProcessDeathCallbackCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.ui.client.createSdkActivityLauncher
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import com.example.api.SdkBannerRequest
import com.example.api.SdkService
import com.example.api.SdkServiceFactory
import com.example.privacysandbox.client.R
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), SdkSandboxProcessDeathCallbackCompat {
    /**
     * An [SdkSandboxManagerCompat], used to communicate with the sandbox and load SDKs.
     */
    private lateinit var sandboxManager: SdkSandboxManagerCompat

    /** Container for rendering content from the Privacy Sandbox. */
    private lateinit var sandboxedView: SandboxedSdkView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sandboxManager = SdkSandboxManagerCompat.from(applicationContext)
        sandboxManager.addSdkSandboxProcessDeathCallback(mainExecutor, this)

        sandboxedView = findViewById(R.id.sandbox_view)

        findViewById<Button>(R.id.load_sdk_button).setOnClickListener {
            onLoadSkButtonPressed()
        }
        findViewById<Button>(R.id.create_file_button).setOnClickListener {
            onCreateFileButtonPressed()
        }
        findViewById<Button>(R.id.request_banner_button).setOnClickListener {
            onRequestBannerButtonPressed()
        }
    }

    private fun onLoadSkButtonPressed() = lifecycleScope.launch {
        try {
            val sdk = loadSdk()
            makeToast("Message from SDK: ${sdk.getMessage()}")
        } catch (error: LoadSdkCompatException) {
            makeToast("Failed to load first SDK: " + error.message)
            Log.e(TAG, "Failed to load first SDK: " + error.message)
        }
    }

    private fun onRequestBannerButtonPressed() = lifecycleScope.launch {
        val sdk = sdkServiceOrNull()
        if (sdk == null) {
            makeToast("Load SDK first.")
            return@launch
        }
        // Create an SDK activity launcher that always approves a launch.
        val launcher = createSdkActivityLauncher { true }

        val request = SdkBannerRequest(PACKAGE_NAME, launcher)
        sandboxedView.setAdapter(sdk.getBanner(request))
    }

    private fun onCreateFileButtonPressed() {
        // Show dialog to collect the size of storage
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Set file size in MB")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        builder.setView(input)
        builder.setPositiveButton("Create") { _, _ ->
            val sizeInMb = input.text.toString().toIntOrNull()
            if (sizeInMb == null) {
                makeToast("Please provide positive integer value")
                return@setPositiveButton
            }

            lifecycleScope.launch {
                val sdk = sdkServiceOrNull()
                if (sdk == null) {
                    makeToast("Please load the SDK first!")
                    return@launch
                }
                Log.i(TAG, "Creating file inside sandbox.")
                makeToast(sdk.createFile(sizeInMb))
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    /**
     * Notifies the client application that the SDK sandbox has died. The sandbox could die for
     * various reasons, for example, due to memory pressure on the system, or a crash in the
     * sandbox.
     *
     * The system will automatically restart the sandbox process if it died due to a crash.
     * However, the state of the sandbox will be lost - so any SDKs that were loaded previously
     * would have to be loaded again, using [SdkSandboxManagerCompat.loadSdk] to continue using
     * them.
     * If this method is called you should clear any references previously returned by the SDK.
     */
    override fun onSdkSandboxDied() {
        makeToast("Sdk Sandbox process died")
    }

    /** Fetches the [SdkService] if the SDK was loaded. Returns null otherwise. */
    private fun sdkServiceOrNull(): SdkService? {
        val loadedSdk = sandboxManager.getSandboxedSdks().find { it.getSdkInfo()?.name == SDK_NAME }
        return loadedSdk?.run { SdkServiceFactory.wrapToSdkService(getInterface()!!) }
    }

    private suspend fun loadSdk(): SdkService {
        // First we need to check if the SDK is already loaded. If it is we just return it.
        // The sandbox manager will throw an exception if we try to load an SDK that is already
        // loaded.
        val loadedSdk = sdkServiceOrNull()
        if (loadedSdk != null) return loadedSdk

        val sandboxedSdk = sandboxManager.loadSdk(SDK_NAME, Bundle.EMPTY)
        return SdkServiceFactory.wrapToSdkService(sandboxedSdk.getInterface()!!)
    }

    private fun makeToast(message: String) {
        runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        private const val TAG = "SandboxClient"

        /**
         * Package name of this app. This is something that the SDK might use the identify this
         * particular app client.
         *
         * (Note that in this particular sample it's used to build the banner view label).
         */
        private const val PACKAGE_NAME = "com.example.privacysandbox.client"

        /**
         * Name of the SDK to be loaded.
         *
         * (needs to be the one defined in example-sdk-bundle/build.gradle)
         */
        private const val SDK_NAME = "com.example.sdk"
    }
}
