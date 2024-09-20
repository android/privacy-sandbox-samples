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
package com.mediateeadapter.implementation

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import com.example.api.MediateeAdapterInterface
import com.mediatee.api.SdkServiceFactory

class MediateeAdapterInterfaceImpl(private val context: Context) : MediateeAdapterInterface {

    private var mediateeInstance: com.mediatee.api.SdkService? = null

    /** Name of the SDK to be loaded. */
    private val mediateeSdkName = "com.mediatee.sdk"

    override suspend fun getBannerAd(
        appPackageName: String,
        activityLauncher: SdkActivityLauncher,
        isWebViewBannerAd: Boolean
    ): Bundle {
        loadMediateeSdk()
        val newRequest: com.mediatee.api.SdkBannerRequest =
            com.mediatee.api.SdkBannerRequest(context.packageName, isWebViewBannerAd)
        return mediateeInstance!!.getBanner(newRequest).toCoreLibInfo(context)
    }

    override suspend fun showFullscreenAd(activityLauncher: SdkActivityLauncher) {
        loadMediateeSdk()
        return mediateeInstance!!.getFullscreenAd().show(activityLauncher)
    }

    private suspend fun loadMediateeSdk() {
        if (mediateeInstance == null) {
            val controller = SdkSandboxControllerCompat.from(context)
            val sandboxedSdk = controller.loadSdk(mediateeSdkName, Bundle.EMPTY)
            mediateeInstance = SdkServiceFactory.wrapToSdkService(sandboxedSdk.getInterface()!!)
        }
    }
}