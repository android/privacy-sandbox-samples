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

import android.annotation.SuppressLint;
import android.app.sdksandbox.SandboxedSdkContext;
import android.app.sdksandbox.SandboxedSdkProvider;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

/*
 * This class works as an entry point for the sandbox to interact with the SDK.
 *
 * This class should be populated inside the AndroidManifest file.
 */
@SuppressLint("NewApi")
public class SdkProviderImpl extends SandboxedSdkProvider {

  SandboxedSdkContext mContext;

  @SuppressLint("Override")
  @Override
  public void initSdk(SandboxedSdkContext sandboxedSdkContext, Bundle params,
      Executor executor, InitSdkCallback initSdkCallback) {
      mContext = sandboxedSdkContext;
      executor.execute(() -> initSdkCallback.onInitSdkFinished(new Bundle()));
  }

  @SuppressLint("Override")
  @Override
  public View getView(Context windowContext, Bundle bundle) {
    WebView webView = new WebView(windowContext);
    webView.loadUrl("https://developer.android.com/privacy-sandbox");
    return webView;
  }

  @SuppressLint("Override")
  @Override
  public void onDataReceived(@NonNull Bundle bundle,
      DataReceivedCallback dataReceivedCallback) {
    if (bundle.isEmpty()) {
       dataReceivedCallback.onDataReceivedSuccess(new Bundle());
       return;
    }

    try {
      final String methodName = bundle.getString("method", "");
      switch (methodName) {
        case "createFile":
          final int sizeInMb = bundle.getInt("sizeInMb");
          final Bundle result = createFile(sizeInMb);
          dataReceivedCallback.onDataReceivedSuccess(result);
          break;
        default:
          dataReceivedCallback.onDataReceivedError("Unknown method name");
      }
    } catch (Throwable e) {
       dataReceivedCallback.onDataReceivedError("Failed process data: " + e.getMessage());
    }
  }

  private Bundle createFile(int sizeInMb) throws IOException {
    final Path path = Paths.get(mContext.getDataDir().getPath(), "file.txt");
    Files.deleteIfExists(path);
    Files.createFile(path);
    try (RandomAccessFile file = new RandomAccessFile(path.toString(), "rw")){
      file.setLength(sizeInMb * 1024 * 1024);
    }
    final Bundle result = new Bundle();
    result.putString("message", "Created " + sizeInMb + " MB file successfully");
    return result;
  }
}
