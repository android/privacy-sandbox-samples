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
package com.example.provider;

import android.app.sdksandbox.SandboxedSdkContext;
import android.app.sdksandbox.SandboxedSdkProvider;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import java.util.concurrent.Executor;

/*
 * This class works as an entry point for the sandbox to interact with the SDK.
 *
 * This class should be populated inside the AndroidManifest file.
 */
public class SdkProviderImpl extends SandboxedSdkProvider {

  @Override
  public void initSdk(SandboxedSdkContext sandboxedSdkContext, Bundle params,
      Executor executor, InitSdkCallback initSdkCallback) {
      executor.execute(() -> initSdkCallback.onInitSdkFinished(new Bundle()));
  }

  @Override
  public View getView(Context windowContext, Bundle bundle) {
    WebView webView = new WebView(windowContext);
    webView.loadUrl("https://google.com");
    return webView;
  }

  @Override
  public void onExtraDataReceived(@NonNull Bundle bundle) {
  }

}
