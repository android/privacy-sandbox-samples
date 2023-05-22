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
package com.example.sdkimplementation

import android.content.Context
import android.os.RemoteException
import com.example.exampleaidllibrary.ISdkApi
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class SdkApi(sdkContext: Context) : ISdkApi.Stub() {
    private var mContext: Context? = null

    init {
        mContext = sdkContext
    }

    @Throws(RemoteException::class)
    override fun createFile(sizeInMb: Int): String? {
        return try {
            val path = Paths.get(
                    mContext!!.applicationContext.dataDir.path, "file.txt")
            Files.deleteIfExists(path)
            Files.createFile(path)
            val buffer = ByteArray(sizeInMb * 1024 * 1024)
            Files.write(path, buffer)

            val file = File(path.toString())
            val actualFileSize: Long = file.length() / (1024 * 1024)
            "Created $actualFileSize MB file successfully"
        } catch (e: IOException) {
            throw RemoteException(e.message)
        }
    }

    override fun getMessage(): String? {
        return "Message from sdk in the sandbox process"
    }
}
