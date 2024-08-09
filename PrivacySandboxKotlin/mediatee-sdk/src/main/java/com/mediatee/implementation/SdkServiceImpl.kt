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
import com.mediatee.api.FullscreenAd
import com.mediatee.api.SdkBannerRequest
import com.mediatee.api.SdkService

class SdkServiceImpl(private val context: Context) : SdkService {
    override suspend fun getBanner(request: SdkBannerRequest) =
        SdkSandboxedUiAdapterImpl(context, request)

    override suspend fun getFullscreenAd() : FullscreenAd = FullscreenAdImpl(context)
}
