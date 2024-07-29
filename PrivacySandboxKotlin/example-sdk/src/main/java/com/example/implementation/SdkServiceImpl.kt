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
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import com.example.api.SdkBannerRequest
import com.example.api.SdkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import com.mediatee.api.SdkServiceFactory
import com.example.api.SdkSandboxedUiAdapter

class SdkServiceImpl(private val context: Context) : SdkService {
    override suspend fun getMessage(): String = "Hello from Privacy Sandbox!"

    private val tag = "ExampleSdk"

    private var remoteInstance: com.mediatee.api.SdkService? = null

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
        shouldLoadMediatedAd: Boolean
    ): SdkSandboxedUiAdapter? {
        if (!shouldLoadMediatedAd) {
            return SdkSandboxedUiAdapterImpl(context, request, null)
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

    override suspend fun getInterstitial(
        activityLauncher: SdkActivityLauncher,
        requestMediatedAd: Boolean
    ) {
        if (!requestMediatedAd) {
            InterstitialAd(context).showAd(activityLauncher)
        } else {
            try {
                if (remoteInstance == null) {
                    val controller = SdkSandboxControllerCompat.from(context)
                    val sandboxedSdk = controller.loadSdk(mediateeSdkName, Bundle.EMPTY)
                    remoteInstance =
                        SdkServiceFactory.wrapToSdkService(sandboxedSdk.getInterface()!!)
                }

                // Activity Launcher to be used to load interstitial ad will be passed from
                // mediator to mediatee SDK.
                remoteInstance!!.getInterstitial(activityLauncher)
            } catch (e: Exception) {
                Log.e(tag, "Failed to load SDK, error code: $e", e)
            }
        }
    }
}
