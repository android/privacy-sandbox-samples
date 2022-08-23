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
package com.example.sdk_implementation;

import android.annotation.SuppressLint
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SandboxedSdkProvider
import android.os.Binder
import android.os.Bundle
import android.content.Context
import android.view.View
import android.webkit.WebView
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor

/*
 * This class works as an entry point for the sandbox to interact with the SDK.
 *
 * This class should be populated inside the AndroidManifest file.
 */
@SuppressLint("NewApi")
class SdkProviderImpl : SandboxedSdkProvider() {

    @SuppressLint("Override")
    override fun onLoadSdk(params: Bundle): SandboxedSdk {
        return SandboxedSdk(Binder())
    }

    @SuppressLint("Override")
    override fun getView(windowContext: Context, bundle: Bundle, width: Int, height: Int): View {
        val webView = WebView(windowContext)
        webView.loadUrl("https://developer.android.com/privacy-sandbox")
        return webView
    }

    @SuppressLint("Override")
    override fun onDataReceived(bundle: Bundle, dataReceivedCallback: DataReceivedCallback) {
        if (bundle.isEmpty()) {
            dataReceivedCallback.onDataReceivedSuccess(Bundle())
            return
        }
        try {
            val methodName: String = bundle.getString("method", "")
            when (methodName) {
                "createFile" -> {
                    val sizeInMb: Int = bundle.getInt("sizeInMb")
                    val result: Bundle = createFile(sizeInMb)
                    return dataReceivedCallback.onDataReceivedSuccess(result)
                }
                else -> {
                    dataReceivedCallback.onDataReceivedError("Unknown method name")
                }
            }
        } catch (e: Throwable) {
            dataReceivedCallback.onDataReceivedError("Failed process data: " + e.message)
        }
    }

    @Throws(IOException::class)
    private fun createFile(sizeInMb: Int): Bundle {
        val path: Path = Paths.get(getContext()?.getApplicationContext()?.getDataDir()?.getPath(), "file.txt")
        Files.deleteIfExists(path)
        Files.createFile(path)
        RandomAccessFile(path.toString(), "rw").use { file -> file.setLength(sizeInMb * 1024L * 1024L) }
        val result: Bundle = Bundle();
        result.putString("message", "Created " + sizeInMb + " MB file successfully");
        return result
    }
}