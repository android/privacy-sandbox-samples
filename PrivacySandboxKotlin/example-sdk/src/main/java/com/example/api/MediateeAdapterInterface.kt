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
package com.example.api

import android.os.Bundle
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import androidx.privacysandbox.tools.PrivacySandboxCallback

/**
 * Common interface to be implemented by Adapters.
 *
 * RE Adapters will register this interface with the Mediator.
 * In-App Adapters will be initialised and registered with Mediator from the App.
 *
 * This interface will then be used by the Mediator to communicate with the Mediatees to show ads.
 */
@PrivacySandboxCallback
interface MediateeAdapterInterface {
    suspend fun getBannerAd(
        appPackageName: String,
        activityLauncher: SdkActivityLauncher,
        isWebViewBannerAd: Boolean
    ): Bundle

    suspend fun showFullscreenAd(activityLauncher: SdkActivityLauncher)
}