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
package com.runtimeenabled.api

import android.os.Bundle
import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface SdkService {

    /** Loads Mediatee and Adapter SDKs. */
    suspend fun initialise()

    suspend fun getMessage(): String

    suspend fun createFile(sizeInMb: Int): String

    /**
     *  Returns a Bundle containing a SandboxedUiAdapter binder.
     *
     * We return a Bundle here, not an interface that extends SandboxedUiAdapter. This is because
     * for in app mediatees, the SandboxedUiAdapter received from the mediatee is directly returned
     * by the mediator to the app, without any wrapper, to avoid nested remote rendering. Since
     * this will need to be returned in a Bundle (one SDK cannot use a shim object defined by
     * another SDK), return type for getBanner will always be a Bundle.
     */
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
