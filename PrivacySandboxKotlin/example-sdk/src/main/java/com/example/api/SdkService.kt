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
package com.example.api

import android.os.Bundle
import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface SdkService {

    /**
     * App has to call this API after loadSdk("mediator") call.
     * Mediatee and Adapter SDKs are loaded by the Mediator when this API is called.
     */
    suspend fun initialise()

    suspend fun getMessage(): String

    suspend fun createFile(sizeInMb: Int): String

    suspend fun getBanner(request: SdkBannerRequest, mediationType: String): Bundle?

    suspend fun getFullscreenAd(mediationType: String): FullscreenAd

    /**
     * Registers the Runtime-enabled mediatee adapter so that it can be used by the Mediator later
     * to show ads.
     */  
    fun registerMediateeAdapter(mediateeAdapter: MediateeAdapterInterface)

    /**
     * Registers the In-App mediatee adapter so that it can be used by the Mediator later
     * to show ads.
     */
    fun registerInAppMediateeAdapter(mediateeAdapter: MediateeAdapterInterface)
}
