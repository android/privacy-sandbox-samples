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
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import com.runtimeenabled.api.SdkServiceFactory
import com.mediateeadapter.api.AbstractSandboxedSdkProviderCompat
import com.mediateeadapter.api.SdkService

/** Provides an [SdkService] implementation when the SDK is loaded. */
class SdkProvider : AbstractSandboxedSdkProviderCompat() {

    private val mediatorSdkName = "com.runtimeenabled.sdk"
    private val mediateeSdkName = "com.mediatee.sdk"

    private var mediatorInstance: com.runtimeenabled.api.SdkService? = null
    private var mediateeInstance: com.mediatee.api.SdkService? = null

    /**
     * Returns the [SdkService] implementation. Called when the SDK is loaded.
     *
     * This method signature (and the [AbstractSandboxedSdkProviderCompat] class) is generated by
     * the Privacy Sandbox API Compiler plugin as the entry point for the app/SDK communication.
     */
    override fun createSdkService(context: Context): SdkService = SdkServiceImpl()

    /**
     * Does the work needed for the SDK to start handling requests. SDK should do any work to be
     * ready to handle upcoming requests.
     *
     * This function is called by the SDK sandbox after it loads the SDK.
     *
     * Mediatee is initialised when Adapter is initialised.
     *
     * For Runtime-enabled Mediatee Adapter, when the adapter is loaded in the Mediator, the
     * [MediateeAdapterInterface] will be registered with the Mediator.
     */
    override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
        registerWithMediator()
        return super.onLoadSdk(params)
    }

    /** Registers [MediateeAdapterInterface] with the Mediator. */
    private fun registerWithMediator() {
        val controller = SdkSandboxControllerCompat.from(checkNotNull(context))
        var mediatorSdk: SandboxedSdkCompat? = null
        var mediateeSdk: SandboxedSdkCompat? = null

        // Get mediatorSdk and mediateeSdk from SdkSandboxController#getSandboxedSdks.
        // Since the adapter is loaded from Mediator when initialise() API is called, Mediator sdk
        // should be present in already loaded sdks.
        // mediateeSdk is loaded before the adapter loadSdk call is made, so it should be present
        // in already loaded sdks.
        for (loadedSandboxedSdk in controller.getSandboxedSdks()) {
            val needToLoadMediator =
                mediatorSdk == null && loadedSandboxedSdk.getSdkInfo()?.name == mediatorSdkName
            val needToLoadMediatee =
                mediateeSdk == null && loadedSandboxedSdk.getSdkInfo()?.name == mediateeSdkName
            if (needToLoadMediator) {
                mediatorSdk = loadedSandboxedSdk
            }
            if (needToLoadMediatee) {
                mediateeSdk = loadedSandboxedSdk
            }
            if (mediatorSdk != null && mediateeSdk != null) {
                break
            }
        }
        mediatorInstance =
            SdkServiceFactory.wrapToSdkService(checkNotNull(mediatorSdk?.getInterface()))
        mediateeInstance =
            com.mediatee.api.SdkServiceFactory.wrapToSdkService(checkNotNull(mediateeSdk?.getInterface()))
        // Register MediateeAdapterInterface with Mediator.
        val mediateeAdapterInterfaceImpl = MediateeAdapterInterfaceImpl(
            checkNotNull(context) { "Adapter can't be registered with Mediator!" },
            checkNotNull(mediateeInstance) { "Mediatee Sdk is not loaded!" })
        mediatorInstance?.registerMediateeAdapter(mediateeAdapterInterfaceImpl)
    }
}
