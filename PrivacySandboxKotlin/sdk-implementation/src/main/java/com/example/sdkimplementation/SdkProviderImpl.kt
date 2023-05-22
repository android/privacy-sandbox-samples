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
package com.example.sdkimplementation;

import android.annotation.SuppressLint
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SandboxedSdkProvider
import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import android.view.View
import android.webkit.WebView
import com.example.exampleaidllibrary.ISdkApi
import kotlin.random.Random


/*
 * This class works as an entry point for the sandbox to interact with the SDK.
 *
 * This class should be populated inside the AndroidManifest file.
 */
@SuppressLint("NewApi")
class SdkProviderImpl : SandboxedSdkProvider() {

    private val EXTRA_SDK_SDK_ENABLED_KEY = "sdkSdkCommEnabled"

    @SuppressLint("Override")
    override fun onLoadSdk(params: Bundle): SandboxedSdk {
        return SandboxedSdk(SdkApi(context!!))
    }

    @SuppressLint("Override")
    override fun getView(windowContext: Context, bundle: Bundle, width: Int, height: Int): View {
        val mSdkSdkCommEnabled: String = bundle.getString(EXTRA_SDK_SDK_ENABLED_KEY, null)
        return if (mSdkSdkCommEnabled == null) {
            val webView = WebView(windowContext)
            webView.loadUrl("https://google.com")
            webView
        } else {
            TestView(windowContext, context!!, mSdkSdkCommEnabled)
        }
    }

    private class TestView internal constructor(windowContext: Context?,
                                                private val mSdkContext: Context,
                                                private val mSdkToSdkCommEnabled: String) : View(windowContext) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.textSize = 50f
            val random = Random(0)
            var message: String? = null
            if (!TextUtils.isEmpty(mSdkToSdkCommEnabled)) {
                val mediateeSdk: SandboxedSdk = try {
                    // get message from another sandboxed SDK
                    val sandboxedSdks = mSdkContext
                        .getSystemService(SdkSandboxController::class.java)
                        .sandboxedSdks
                    sandboxedSdks.stream()
                        .filter { s: SandboxedSdk ->
                            s.sharedLibraryInfo
                                .name
                                .contains(MEDIATEE_SDK)
                        }
                        .findAny()
                        .get()
                } catch (e: Exception) {
                    throw IllegalStateException("Error in sdk-sdk communication ", e)
                }
                try {
                    val binder = mediateeSdk.getInterface()
                    val sdkApi = ISdkApi.Stub.asInterface(binder)
                    message = sdkApi.getMessage()
                } catch (e: RemoteException) {
                    throw IllegalStateException(e)
                }
            } else {
                message = "Sdk to sdk communication cannot be done"
            }
            val c: Int = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            canvas.drawColor(c)
            canvas.drawText(message!!, 75f, 75f, paint)
        }

        companion object {
            private val MEDIATEE_SDK: CharSequence = "com.example.mediatee.provider"
        }
    }
}