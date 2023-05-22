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

import android.content.Context;
import android.os.RemoteException;

import com.example.exampleaidllibrary.ISdkApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SdkApi extends ISdkApi.Stub {
    private Context mContext;

    public SdkApi(Context context) {
        mContext = context;
    }

    @Override
    public String createFile(int sizeInMb) throws RemoteException {
        try {
            final Path path = Paths.get(mContext.getDataDir().getPath(), "file.txt");
            Files.deleteIfExists(path);
            Files.createFile(path);
            final byte[] buffer = new byte[sizeInMb * 1024 * 1024];
            Files.write(path, buffer);

            final File file = new File(path.toString());
            final long actualFileSize = file.length() / (1024 * 1024);
            return "Created " + actualFileSize + " MB file successfully";
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public String getMessage() {
        return "Message from sdk in the sandbox process";
    }
}