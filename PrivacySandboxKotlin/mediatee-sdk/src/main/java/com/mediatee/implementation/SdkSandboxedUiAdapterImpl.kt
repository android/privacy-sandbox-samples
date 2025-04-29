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
package com.mediatee.implementation

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionData
import com.mediatee.api.SdkBannerRequest
import com.mediatee.api.SdkSandboxedUiAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import java.util.concurrent.Executor
import kotlin.random.Random

class SdkSandboxedUiAdapterImpl(
    private val sdkContext: Context,
    private val request: SdkBannerRequest
) : SdkSandboxedUiAdapter {
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
        val session = SdkUiSession(clientExecutor, sdkContext, request)
        clientExecutor.execute {
            client.onSessionOpened(session)
        }
    }
}

private class SdkUiSession(
    clientExecutor: Executor,
    private val sdkContext: Context,
    private val request: SdkBannerRequest
) : SandboxedUiAdapter.Session {

    /** A scope for launching coroutines in the client executor. */
    private val scope = CoroutineScope(clientExecutor.asCoroutineDispatcher() + Job())

    private val urls = listOf(
        "https://github.com", "https://developer.android.com/"
    )

    private val bannerAdMsg = "Rendered from Runtime Enabled Mediatee SDK"

    override val signalOptions: Set<String> = setOf()

    override val view: View = getAdView()

    private fun getAdView() : View {
        if (request.isWebViewBannerAd) {
            val webview = WebView(sdkContext)
            webview.loadUrl(urls[Random.nextInt(urls.size)])
            return webview
        }
        val textView = TextView(sdkContext)
        textView.text = bannerAdMsg
        return textView
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

    override fun notifySessionRendered(supportedSignalOptions: Set<String>) {
        TODO("Not yet implemented")
    }

    override fun notifyUiChanged(uiContainerInfo: Bundle) {
        // Notify the session when the presentation state of its UI container has changed.
    }

    override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
        // Notifies that the Z order has changed for the UI associated by this session.
    }
}
