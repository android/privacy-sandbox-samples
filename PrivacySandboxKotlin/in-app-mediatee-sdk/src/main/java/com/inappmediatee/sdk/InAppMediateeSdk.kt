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
package com.inappmediatee.sdk

import android.content.Context
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import com.example.api.IMediateeSdkInterface

class InAppMediateeSdk(private val context: Context) : IMediateeSdkInterface.Stub() {

    private var inAppMediateeSdkRegistered = false

    suspend fun register() {
        // Register the In-app SDK if not already registered.
        // An [SdkSandboxManagerCompat], used to communicate with the sandbox and load SDKs.
        if (!inAppMediateeSdkRegistered) {
            val sandboxManagerCompat = SdkSandboxManagerCompat.from(context)
            sandboxManagerCompat.registerAppOwnedSdkSandboxInterface(
                AppOwnedSdkSandboxInterfaceCompat(
                    "com.inappmediatee.sdk",
                    /*version=*/ 0,
                    this
                )
            )
            inAppMediateeSdkRegistered = true
        }
    }

    suspend fun unregister() {
        // Unregister the In-app SDK if registered.
        // An [SdkSandboxManagerCompat], used to communicate with the sandbox and load SDKs.
        if (inAppMediateeSdkRegistered) {
            val sandboxManagerCompat = SdkSandboxManagerCompat.from(context)
            sandboxManagerCompat.unregisterAppOwnedSdkSandboxInterface(
                "com.inappmediatee.sdk"
            )
            inAppMediateeSdkRegistered = false
        }
    }
}
