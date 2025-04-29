/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.runtimeaware.sdk

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.privacysandbox.activity.client.createManagedSdkActivityLauncher
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import com.runtimeenabled.api.SdkBannerRequest

class BannerAd(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    // This method could divert a percentage of requests to a sandboxed SDK and fallback to
    // existing ad logic. For this runtimeenabled, we send all requests to the sandboxed SDK as long as it
    // exists.
    suspend fun loadAd(
        baseActivity: AppCompatActivity,
        clientMessage: String,
        allowSdkActivityLaunch: () -> Boolean,
        shouldLoadWebView: Boolean,
        mediationType: String) {
        val bannerAd = getBannerAdFromRuntimeEnabledSdkIfExists(
            baseActivity,
            clientMessage,
            allowSdkActivityLaunch,
            shouldLoadWebView,
            mediationType
        )
        if (bannerAd != null) {
            val sandboxedSdkView = SandboxedSdkView(context)
            addViewToLayout(sandboxedSdkView)
            sandboxedSdkView.setAdapter(bannerAd)
            return
        }

        val textView = TextView(context)
        textView.text = "Ad from SDK in the app"
        addViewToLayout(textView)
    }

    private suspend fun getBannerAdFromRuntimeEnabledSdkIfExists(
        baseActivity: AppCompatActivity,
        message: String,
        allowSdkActivityLaunch: () -> Boolean,
        shouldLoadWebView: Boolean,
        mediationType: String
    ): SandboxedUiAdapter? {
        if (!ExistingSdk.isSdkLoaded()) {
            return null
        }

        val launcher = baseActivity.createManagedSdkActivityLauncher(allowSdkActivityLaunch)
        val request = SdkBannerRequest(message, launcher, shouldLoadWebView)
        // Get the SandboxedUiAdapter from the Bundle.
        return SandboxedUiAdapterFactory.createFromCoreLibInfo(
            checkNotNull(
                ExistingSdk.loadSdkIfNeeded(
                    context
                )?.getBanner(request, mediationType)
            ) { "No banner Ad received from ad SDK!" })
    }

    private fun addViewToLayout(view: View) {
        removeAllViews()
        view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        super.addView(view)
    }
}
