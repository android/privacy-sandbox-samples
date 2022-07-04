/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.example.provider

import android.annotation.SuppressLint
import android.app.sdksandbox.SandboxedSdkProvider
import android.app.sdksandbox.SandboxedSdkContext
import android.os.Bundle
import java.util.concurrent.Executor
import android.app.sdksandbox.SandboxedSdkProvider.InitSdkCallback
import android.content.Context
import java.lang.Runnable
import android.view.View
import android.webkit.WebView

/*
 * This class works as an entry point for the sandbox to interact with the SDK.
 *
 * This class should be populated inside the AndroidManifest file.
 */
@SuppressLint("NewApi")
class SdkProviderImpl : SandboxedSdkProvider() {

    @SuppressLint("Override")
    override fun initSdk(
        sandboxedSdkContext: SandboxedSdkContext, params: Bundle,
        executor: Executor, initSdkCallback: InitSdkCallback
    ) {
        executor.execute { initSdkCallback.onInitSdkFinished(Bundle()) }
    }

    @SuppressLint("Override")
    override fun getView(windowContext: Context, bundle: Bundle): View {
        val webView = WebView(windowContext)
        webView.loadUrl("https://developer.android.com/privacy-sandbox")
        return webView
    }

    @SuppressLint("Override")
    override fun onDataReceived(bundle: Bundle, dataReceivedCallback : DataReceivedCallback) {
        dataReceivedCallback.onDataReceivedSuccess(Bundle())
    }
}