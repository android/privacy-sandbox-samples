/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.runtimeenabled.implementation

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.DelegatingSandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionData
import androidx.privacysandbox.ui.provider.AbstractSandboxedUiAdapter
import com.runtimeenabled.R
import com.runtimeenabled.api.SdkBannerRequest
import com.runtimeenabled.api.SdkSandboxedUiAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlin.random.Random

/**
 * Implementation of [SdkSandboxedUiAdapter] that handles banner ad requests.
 *
 * This class extends [AbstractSandboxedUiAdapter] and provides the functionality to open
 * UI sessions. The usage of [AbstractSandboxedUiAdapter] simplifies the implementation.
 *
 * @param sdkContext The context of the SDK.
 * @param request The banner ad request.
 * @param mediateeAdapter The UI adapter for a mediatee SDK, if applicable.
 */
class SdkSandboxedUiAdapterImpl(
    private val sdkContext: Context,
    private val request: SdkBannerRequest,
    private val mediateeAdapter: SandboxedUiAdapter?
) : AbstractSandboxedUiAdapter(), SdkSandboxedUiAdapter {
    /**
     * Opens a new session to display remote UI.
     * The session will handle notifications from and to the client.
     * We consider the client the owner of the SandboxedSdkView.
     *
     @param context The client's context.
     * @param sessionData Constants related to the session, such as the presentation id.
     * @param initialWidth The initial width of the adapter's view.
     * @param initialHeight The initial height of the adapter's view.
     * @param isZOrderOnTop Whether the session's view should be drawn on top of other views.
     * @param clientExecutor The executor to use for client callbacks.
     * @param client A UI adapter representing the client of this single session.
     */
    override fun openSession(
        context: Context,
        sessionData: SessionData,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient
    ) {
        val session = SdkUiSession(clientExecutor, sdkContext, request, mediateeAdapter)
        clientExecutor.execute {
            client.onSessionOpened(session)
        }
    }
}

/**
 * Implementation of [SandboxedUiAdapter.Session], used for banner ad requests.
 * This class extends [AbstractSandboxedUiAdapter.AbstractSession] to provide the functionality in
 * cohesion with [AbstractSandboxedUiAdapter]
 *
 * @param clientExecutor The executor to use for client callbacks.
 * @param sdkContext The context of the SDK.
 * @param request The banner ad request.
 * @param mediateeAdapter The UI adapter for a mediatee SDK, if applicable.
 */
private class SdkUiSession(
    clientExecutor: Executor,
    private val sdkContext: Context,
    private val request: SdkBannerRequest,
    private val mediateeSandboxedUiAdapter: SandboxedUiAdapter?
) : AbstractSandboxedUiAdapter.AbstractSession() {

    private val controller = SdkSandboxControllerCompat.from(sdkContext)

    /** A scope for launching coroutines in the client executor. */
    private val scope = CoroutineScope(clientExecutor.asCoroutineDispatcher() + Job())

    private val urls = listOf(
        "https://github.com", "https://developer.android.com/"
    )

    override val view: View = getAdView()

    private fun getAdView() : View {
        if (mediateeSandboxedUiAdapter != null) {
            // The Mediator (runtime-enabled-sdk) view contains a SandboxedSdkView that is being populated
            // with the ad view from the Runtime enabled Mediatee, which runs in the same process
            // as the Mediator. The view also has an overlay from the Mediator sdk. This will be
            // sent to the Publisher as a SandboxedUiAdapter by the Mediator.
            return View.inflate(sdkContext, R.layout.banner, null).apply {
                val adLayout = findViewById<LinearLayout>(R.id.ad_layout)
                adLayout.removeView(findViewById(R.id.click_ad_header))
                val textView = findViewById<TextView>(R.id.banner_header_view)
                textView.text =
                    context.getString(R.string.banner_ad_label, request.appPackageName)
                val ssv = SandboxedSdkView(context)
                ssv.setAdapter(mediateeSandboxedUiAdapter)
                adLayout.addView(ssv)
            }
        }
        if (request.isWebViewBannerAd) {
            val webview = WebView(sdkContext)
            webview.loadUrl(urls[Random.nextInt(urls.size)])
            return webview
        }
        return View.inflate(sdkContext, R.layout.banner, null).apply {
            val textView = findViewById<TextView>(R.id.banner_header_view)
            textView.text =
                context.getString(R.string.banner_ad_label, request.appPackageName)

            setOnClickListener {
                launchActivity()
            }
        }
    }

    override fun close() {
        // Notifies that the client has closed the session. It's a good opportunity to dispose
        // any resources that were acquired to maintain the session.
        scope.cancel()
    }

    override fun notifyConfigurationChanged(configuration: Configuration) {
        // Notifies that the device configuration has changed and affected the app.
    }

    override fun notifyResized(width: Int, height: Int) {
        // Notifies that the size of the presentation area in the app has changed.
    }

    override fun notifyUiChanged(uiContainerInfo: Bundle) {
        // Notify the session when the presentation state of its UI container has changed.
    }

    override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
        // Notifies that the Z order has changed for the UI associated by this session.
    }

    private fun launchActivity() = scope.launch {
        val handler = object : SdkSandboxActivityHandlerCompat {
            override fun onActivityCreated(activityHolder: ActivityHolder) {
                val contentView = View.inflate(sdkContext, R.layout.full_screen, null)
                contentView.findViewById<WebView>(R.id.full_screen_ad_webview).apply {
                    loadUrl(urls[Random.nextInt(urls.size)])
                }
                activityHolder.getActivity().setContentView(contentView)
            }
        }

        val token = controller.registerSdkSandboxActivityHandler(handler)
        val launched = request.activityLauncher.launchSdkActivity(token)
        if (!launched) controller.unregisterSdkSandboxActivityHandler(handler)
    }
}
