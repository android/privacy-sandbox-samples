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
package com.example.implementation

import android.content.Context
import com.example.api.FullscreenAd
import com.example.api.SdkBannerRequest
import com.example.api.SdkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class SdkServiceImpl(private val context: Context) : SdkService {
    override suspend fun getMessage(): String = "Hello from Privacy Sandbox!"

    override suspend fun createFile(sizeInMb: Int): String {
        val path = Paths.get(
            context.applicationContext.dataDir.path, "file.txt"
        )
        withContext(Dispatchers.IO) {
            Files.deleteIfExists(path)
            Files.createFile(path)
            val buffer = ByteArray(sizeInMb * 1024 * 1024)
            Files.write(path, buffer)
        }

        val file = File(path.toString())
        val actualFileSize: Long = file.length() / (1024 * 1024)
        return "Created $actualFileSize MB file successfully"
    }

    override suspend fun getBanner(request: SdkBannerRequest) =
        SdkSandboxedUiAdapterImpl(context, request)

    override suspend fun getFullscreenAd() : FullscreenAd {
        return FullscreenAdImpl(context)
    }

}
