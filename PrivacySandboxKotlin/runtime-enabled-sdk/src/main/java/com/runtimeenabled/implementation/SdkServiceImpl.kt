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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.core.DelegatingSandboxedUiAdapter
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import com.runtimeenabled.R
import com.runtimeenabled.api.FullscreenAd
import com.runtimeenabled.api.SdkBannerRequest
import com.runtimeenabled.api.SdkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import androidx.privacysandbox.ui.core.SandboxedSdkViewUiInfo
import androidx.privacysandbox.ui.core.SessionObserver
import androidx.privacysandbox.ui.core.SessionObserverContext
import androidx.privacysandbox.ui.core.SessionObserverFactory
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import com.runtimeenabled.api.MediateeAdapterInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SdkServiceImpl(private val context: Context) : SdkService {

    private var inAppMediateeAdapter: MediateeAdapterInterface? = null
    private var mediateeAdapter: MediateeAdapterInterface? = null

    private val adapterSdkName = "com.mediateeadapter.sdk"
    private val mediateeSdkName = "com.mediatee.sdk"
    private val tag = "RuntimeEnabledSdk"

    override suspend fun initialise() {
        val sandboxController = SdkSandboxControllerCompat.from(context)
        sandboxController.loadSdk(mediateeSdkName, Bundle.EMPTY)
        // Adapter should only be loaded after Mediatee is loaded.
        sandboxController.loadSdk(adapterSdkName, Bundle.EMPTY)
    }

    override suspend fun getMessage(): String = "Hello from Runtime-enabled SDK!"

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

    // We return a Bundle here, not an interface that extends SandboxedUiAdapter. This is because
    // for in app mediatees, the SandboxedUiAdapter received from the mediatee is directly returned
    // by the mediator to the app, without any wrapper, to avoid nested remote rendering. Since
    // this will need to be returned in a Bundle (one SDK cannot use a shim object defined by
    // another SDK), return type for getBanner will always be a Bundle.
    @OptIn(ExperimentalFeatures.DelegatingAdapterApi::class)
    override suspend fun getBanner(
        request: SdkBannerRequest,
        mediationType: String
    ): Bundle? {
        if (mediationType == context.getString(R.string.mediation_option_refresh_mediated_ads)) {
            val runtimeMediateeBanner = SandboxedUiAdapterFactory.createFromCoreLibInfo(checkNotNull(
                mediateeAdapter?.getBannerAd(
                    request.appPackageName,
                    request.activityLauncher,
                    request.isWebViewBannerAd
                )
            ) { "No banner Ad received from mediatee!" })
            // DelegatingSandboxedUiAdapter enables updating delegate from which different ads
            // can be served without the client's involvement.
            val delegatingAdapter = DelegatingSandboxedUiAdapter(
                SdkSandboxedUiAdapterImpl(
                    context,
                    request,
                    runtimeMediateeBanner
                ).toCoreLibInfo(context)
            )
            // Launches a function to refresh the ad after a few seconds
            CoroutineScope(Dispatchers.IO).launch {
                updateDelegateAfterSomeDelay(request, delegatingAdapter);
            }
            return delegatingAdapter.toCoreLibInfo(context)
        }
        if (mediationType == context.getString(R.string.mediation_option_none)) {
            val bannerAdAdapter = SdkSandboxedUiAdapterImpl(context, request, null)
            bannerAdAdapter.addObserverFactory(SessionObserverFactoryImpl())
            return bannerAdAdapter.toCoreLibInfo(context)
        }
        // For In-app mediatee, SandboxedUiAdapter returned by mediatee is not wrapped, it is
        // directly returned to app. This is to avoid nested remote rendering.
        // There is no overlay in this case for this reason.
        if (mediationType == context.getString(R.string.mediation_option_inapp_mediatee)) {
            return inAppMediateeAdapter?.getBannerAd(
                        request.appPackageName,
                        request.activityLauncher,
                        request.isWebViewBannerAd
                    )
        }
        return SdkSandboxedUiAdapterImpl(
            context,
            request,
            SandboxedUiAdapterFactory.createFromCoreLibInfo(checkNotNull(
                mediateeAdapter?.getBannerAd(
                    request.appPackageName,
                    request.activityLauncher,
                    request.isWebViewBannerAd
                )
            ) { "No banner Ad received from mediatee!" })
        ).toCoreLibInfo(context)
    }

    private suspend fun updateDelegateAfterSomeDelay(
        request: SdkBannerRequest,
        delegatingAdapter: DelegatingSandboxedUiAdapter
    ) {
        delay(10000)
        val inAppMediateeBanner = inAppMediateeAdapter?.getBannerAd(
            request.appPackageName,
            request.activityLauncher,
            request.isWebViewBannerAd
        )
        // Refresh the ad to show ads from another mediatee
        if (inAppMediateeBanner != null) {
            delegatingAdapter.updateDelegate(inAppMediateeBanner)
        }
    }

    override suspend fun getFullscreenAd(mediationType: String): FullscreenAd {
        if (mediationType == context.getString(R.string.mediation_option_none)) {
            return FullscreenAdImpl(context, null, false)
        }
        val adapter: MediateeAdapterInterface?
        if (mediationType == context.getString(R.string.mediation_option_inapp_mediatee)) {
            inAppMediateeAdapter
                ?: throw RemoteException("In-App mediatee SDK not registered with mediator SDK!")
            adapter = inAppMediateeAdapter
        } else {
            mediateeAdapter
                ?: throw RemoteException("Mediatee SDK not registered with mediator SDK!")
            adapter = mediateeAdapter
        }
        adapter?.loadFullscreenAd()
        return FullscreenAdImpl(context, adapter, true)
    }

    override fun registerMediateeAdapter(mediateeAdapter: MediateeAdapterInterface) {
        this.mediateeAdapter = mediateeAdapter
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
