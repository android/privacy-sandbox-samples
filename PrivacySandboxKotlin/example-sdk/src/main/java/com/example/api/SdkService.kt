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

import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface SdkService {
    suspend fun getMessage(): String

    suspend fun createFile(sizeInMb: Int): String

    suspend fun getBanner(request: SdkBannerRequest, mediationType: String): SdkSandboxedUiAdapter?

    suspend fun getFullscreenAd(mediationType: String): FullscreenAd

    /**
     * Registers the RE mediatee adapter.
     *
     * For the RE Adapter case, Adapter is initialised and registered with the mediator when 
     * Mediator is initialised.
     */  
    fun registerMediateeAdapter(mediateeAdapter: MediateeAdapterInterface)

    /**
     * Registers the In-App mediatee adapter.
     *
     * In-App Adapter is initialised and registered with the mediator from the app. This is
     * unlike the pre-Rubidium world where the App does not directly communicate with mediatee.
     * After In-App mediatee transitions to run in Runtime process, this will not be done by the
     * app.
     */
    fun registerInAppMediateeAdapter(mediateeAdapter: MediateeAdapterInterface)
}
