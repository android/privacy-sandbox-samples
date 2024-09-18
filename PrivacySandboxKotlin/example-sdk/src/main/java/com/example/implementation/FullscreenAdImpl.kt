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
package com.example.implementation

import android.content.Context
import android.os.Build
import android.os.RemoteException
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import com.example.R
import com.example.api.FullscreenAd
import com.example.api.MediateeAdapterInterface

class FullscreenAdImpl(private val sdkContext: Context,
                       private val mediationType: String
) : FullscreenAd {

    private val webView = WebView(sdkContext)
    private val controller = SdkSandboxControllerCompat.from(sdkContext)

    private var mediateeSdk: com.mediatee.api.SdkService? = null
    private var inAppMediateeAdapter: MediateeAdapterInterface? = null

    init {
        initializeSettings(webView.settings)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                return false
            }
        }
        webView.loadUrl(WEB_VIEW_LINK)
    }

    /**
     * Shows ad in a new Activity.
     *
     * For mediationType == RUNTIME_MEDIATEE, Runtime mediatee uses the [SdkActivityLauncher] passed
     * to it to open new activity and show its ad.
     * For mediationType == INAPP_MEDIATEE, In-App mediatee ignores the [SdkActivityLauncher] passed
     * to it and opens a new activity that is declared in its manifest.
     */
    override suspend fun show(activityLauncher: SdkActivityLauncher) {
        if (mediationType == sdkContext.getString(R.string.mediation_option_re_re)) {
            if (mediateeSdk == null) {
                throw RemoteException("Mediatee SDK not loaded!")
            }
            // Activity Launcher to be used to load interstitial ad will be passed from
            // mediator to mediatee SDK.
            mediateeSdk!!.getFullscreenAd().show(activityLauncher)
        } else if (mediationType == sdkContext.getString(R.string.mediation_option_re_inapp)) {
            if (inAppMediateeAdapter == null) {
                throw RemoteException("In App Mediatee SDK not registered with mediator SDK!")
            }
            // In App mediatee declares its own activity in its manifest (statically linked to the
            // app), which opens in the app process. ActivityLauncher is passed from mediator is
            // ignored at the Adapter.
            inAppMediateeAdapter!!.showFullscreenAd(activityLauncher)
        } else {
            val handler = object : SdkSandboxActivityHandlerCompat {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onActivityCreated(activityHolder: ActivityHolder) {
                    val activityHandler = ActivityHandler(activityHolder, webView)
                    activityHandler.buildLayout()

                    ViewCompat.setOnApplyWindowInsetsListener(activityHolder.getActivity().window.decorView) { view, windowInsets ->
                        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                        view.updatePadding(top = insets.top)
                        WindowInsetsCompat.CONSUMED
                    }
                }
            }

            val token = controller.registerSdkSandboxActivityHandler(handler)
            val launched = activityLauncher.launchSdkActivity(token)
            if (!launched) controller.unregisterSdkSandboxActivityHandler(handler)
        }
    }

    fun setRuntimeMediateeSdkService(mediateeSdk: com.mediatee.api.SdkService?) {
        this.mediateeSdk = mediateeSdk
    }

    fun setInAppMediateeAdapter(inAppMediateeAdapter: MediateeAdapterInterface?) {
        this.inAppMediateeAdapter = inAppMediateeAdapter
    }

    private fun initializeSettings(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.setGeolocationEnabled(true)
        settings.setSupportZoom(true)
        settings.databaseEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
    }

    companion object {
        private const val WEB_VIEW_LINK = "https://developer.android.com/"
    }
}