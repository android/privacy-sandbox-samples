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

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.privacysandbox.client.R
import com.existing.sdk.BannerAd
import com.existing.sdk.ExistingSdk
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    /** Container for rendering content from the SDK. */
    private lateinit var bannerAd: BannerAd

    private val existingSdk = ExistingSdk(this)

    /** A spinner for selecting the size of the file created in the sandbox. */
    private lateinit var fileSizeSpinner: Spinner

    /** Represents a file size that can be selected in the UI. */
    private data class FileSize(val sizeInMb: Int) {
        /** Called when FileSize is shown in the spinner. */
        override fun toString() = "$sizeInMb MB"
    }

    private val fileSizes = listOf(
        FileSize(3),
        FileSize(9),
        FileSize(18),
    )

    /** A spinner for selecting the type of ad being requested. */
    private lateinit var adTypeSpinner: Spinner
    private val adTypes = listOf("Banner", "WebView Banner")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bannerAd = findViewById(R.id.banner_ad)

        findViewById<Button>(R.id.initialize_sdk_button).setOnClickListener {
            onInitializeSkButtonPressed()
        }
        findViewById<Button>(R.id.create_file_button).setOnClickListener {
            onCreateFileButtonPressed()
        }
        findViewById<Button>(R.id.request_banner_button).setOnClickListener {
            onRequestBannerButtonPressed()
        }

        fileSizeSpinner = findViewById<Spinner>(R.id.create_file_size_spinner).apply {
            adapter = ArrayAdapter(this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                fileSizes,
            )
        }

        adTypeSpinner = findViewById<Spinner>(R.id.request_ad_spinner).apply {
            adapter = ArrayAdapter(
                this@MainActivity, android.R.layout.simple_spinner_dropdown_item, adTypes)
        }
    }

    private fun onInitializeSkButtonPressed() = lifecycleScope.launch {
        if (!existingSdk.initialize()) {
            makeToast("Failed to initialize SDK")
        } else {
            makeToast("Initialized SDK!")
        }
    }

    private fun onRequestBannerButtonPressed() = lifecycleScope.launch {
        // Apps can allow or deny activity launches as they happen. In this example we are
        // surfacing a checkbox that controls the launches. In production apps could disable
        // launches whenever they feel SDKs shouldn't be launching activities (in the middle of
        // certain game scenes, video playback, etc).
        val launchSdkActivity = {
            if (findViewById<CheckBox>(R.id.sdk_activity_launch_checkbox).isChecked) {
                true
            } else {
                makeToast("SDK tried to launch an activity, but it was denied.")
                false
            }
        }
        val loadWebView = adTypes[adTypeSpinner.selectedItemPosition].contains("WebView")
        bannerAd.loadAd(this@MainActivity, PACKAGE_NAME, launchSdkActivity, loadWebView)
    }

    private fun onCreateFileButtonPressed() {
        val fileSize = fileSizes[fileSizeSpinner.selectedItemPosition]

        lifecycleScope.launch {
            val success = existingSdk.createFile(fileSize.sizeInMb)
            if (success == null) {
                makeToast("Please load the SDK first!")
                return@launch
            }
            makeToast(success)
        }
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
    }
}
