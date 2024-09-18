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
package com.example.implementation

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import com.example.R
import com.example.api.FullscreenAd
import com.example.api.SdkBannerRequest
import com.example.api.SdkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import androidx.privacysandbox.ui.core.SandboxedSdkViewUiInfo
import androidx.privacysandbox.ui.core.SessionObserver
import androidx.privacysandbox.ui.core.SessionObserverContext
import androidx.privacysandbox.ui.core.SessionObserverFactory
import com.example.api.MediateeAdapterInterface
import com.mediatee.api.SdkServiceFactory
import com.example.api.SdkSandboxedUiAdapter

class SdkServiceImpl(private val context: Context) : SdkService {
    override suspend fun getMessage(): String = "Hello from Privacy Sandbox!"

    private val tag = "ExampleSdk"

    private var remoteInstance: com.mediatee.api.SdkService? = null

    private var inAppMediateeAdapter: MediateeAdapterInterface? = null

    /** Name of the SDK to be loaded. */
    private val mediateeSdkName = "com.mediatee.sdk"

    override suspend fun createFile(sizeInMb: Int): String {
        val path = Paths.get(
            context.applicationContext.dataDir.path, "file.txt"
        )
        withContext(Dispatchers.IO) {
            Files.deleteIfExists(path)
            Files.createFile(path)
            val buffer = ByteArray(sizeInMb * 1024 * 1024)
            Files.write(path, buffer)
        }

        val file = File(path.toString())
        val actualFileSize: Long = file.length() / (1024 * 1024)
        return "Created $actualFileSize MB file successfully"
    }

    override suspend fun getBanner(
        request: SdkBannerRequest,
        mediationType: String
    ): SdkSandboxedUiAdapter? {
        if (mediationType == context.getString(R.string.mediation_option_none)) {
            val bannerAdAdapter = SdkSandboxedUiAdapterImpl(context, request, null)
            bannerAdAdapter.addObserverFactory(SessionObserverFactoryImpl())
            return bannerAdAdapter
        }
        try {
            if (remoteInstance == null) {
                val controller = SdkSandboxControllerCompat.from(context)
                // Runtime enabled Mediator SDK can load another SDK in the SDK Runtime or if it is
                // already loaded they may get the SDK binder from controller#getSandboxedSdks.
                val sandboxedSdk = controller.loadSdk(mediateeSdkName, Bundle.EMPTY)
                remoteInstance = SdkServiceFactory.wrapToSdkService(sandboxedSdk.getInterface()!!)
            }

            val newRequest: com.mediatee.api.SdkBannerRequest =
                com.mediatee.api.SdkBannerRequest(context.packageName, request.isWebViewBannerAd)
            return SdkSandboxedUiAdapterImpl(
                context,
                request,
                remoteInstance!!.getBanner(newRequest)
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to load SDK, error code: $e", e)
            return null
        }
    }

    override suspend fun getFullscreenAd(mediationType: String): FullscreenAd {
        if (mediationType == context.getString(R.string.mediation_option_re_re)) {
            try {
                if (remoteInstance == null) {
                    val controller = SdkSandboxControllerCompat.from(context)
                    val sandboxedSdk = controller.loadSdk(mediateeSdkName, Bundle.EMPTY)
                    remoteInstance =
                        SdkServiceFactory.wrapToSdkService(sandboxedSdk.getInterface()!!)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to load SDK, error code: $e", e)
            }
        }
        val fullscreenAd = FullscreenAdImpl(context, mediationType)
        fullscreenAd.setRuntimeMediateeSdkService(remoteInstance)
        fullscreenAd.setInAppMediateeAdapter(inAppMediateeAdapter)
        return fullscreenAd
    }

    override fun registerInAppMediateeAdapter(mediateeAdapter: MediateeAdapterInterface) {
        inAppMediateeAdapter = mediateeAdapter
    }
}

/**
 * A factory for creating [SessionObserver] instances.
 *
 * This class provides a way to create observers that can monitor the lifecycle of UI sessions
 * and receive updates about UI container changes.
 */
private class SessionObserverFactoryImpl : SessionObserverFactory {
    override fun create(): SessionObserver {
        return SessionObserverImpl()
    }

    /**
     * An implementation of [SessionObserver] that logs session lifecycle events and UI container
     * information.
     */
    private inner class SessionObserverImpl : SessionObserver {
        override fun onSessionOpened(sessionObserverContext: SessionObserverContext) {
            Log.i("SessionObserver", "onSessionOpened $sessionObserverContext")
        }

        /**
         * Called when the UI container associated with a session changes.
         *
         * @param uiContainerInfo A Bundle containing information about the UI container,
         * including on-screen geometry, width, height, and opacity.
         */
        override fun onUiContainerChanged(uiContainerInfo: Bundle) {
            val sandboxedSdkViewUiInfo = SandboxedSdkViewUiInfo.fromBundle(uiContainerInfo)
            val onScreen = sandboxedSdkViewUiInfo.onScreenGeometry
            val width = sandboxedSdkViewUiInfo.uiContainerWidth
            val height = sandboxedSdkViewUiInfo.uiContainerHeight
            val opacity = sandboxedSdkViewUiInfo.uiContainerOpacityHint
            Log.i("SessionObserver", "UI info: " +
                    "On-screen geometry: $onScreen, width: $width, height: $height," +
                    " opacity: $opacity")
        }

        override fun onSessionClosed() {
            Log.i("SessionObserver", "onSessionClosed")
        }
    }
}
