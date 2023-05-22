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

import android.annotation.SuppressLint;
import android.app.sdksandbox.SandboxedSdk;
import android.app.sdksandbox.SandboxedSdkProvider;
import android.app.sdksandbox.sdkprovider.SdkSandboxController;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.example.exampleaidllibrary.ISdkApi;

import java.util.List;
import java.util.Random;

/*
 * This class works as an entry point for the sandbox to interact with the SDK.
 *
 * This class should be populated inside the AndroidManifest file.
 */
@SuppressLint("NewApi")
public class SdkProviderImpl extends SandboxedSdkProvider {

  private static final String EXTRA_SDK_SDK_ENABLED_KEY = "sdkSdkCommEnabled";

  @SuppressLint("Override")
  @Override
  public SandboxedSdk onLoadSdk(Bundle params) {
    return new SandboxedSdk(new SdkApi(getContext()));
  }

  @SuppressLint("Override")
  @Override
  public View getView(Context windowContext, Bundle params, int width, int height) {
    final String mSdkSdkCommEnabled = params.getString(EXTRA_SDK_SDK_ENABLED_KEY, null);
    if(mSdkSdkCommEnabled == null) {
      WebView webView = new WebView(windowContext);
      webView.loadUrl("https://google.com");
      return webView;
    }
    else {
      return new TestView(windowContext, getContext(), mSdkSdkCommEnabled);
    }
  }

  private static class TestView extends View {

    private static final CharSequence MEDIATEE_SDK = "com.example.mediatee.provider";
    private Context mSdkContext;
    private String mSdkToSdkCommEnabled;

    TestView(Context windowContext,
            Context sdkContext,
            String sdkSdkCommEnabled) {
      super(windowContext);
      mSdkContext = sdkContext;
      mSdkToSdkCommEnabled = sdkSdkCommEnabled;
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);

      Paint paint = new Paint();
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(Color.WHITE);
      paint.setTextSize(50);
      Random random = new Random();
      String message = null;

      if (!TextUtils.isEmpty(mSdkToSdkCommEnabled)) {
        SandboxedSdk mediateeSdk;
        try {
          // get message from another sandboxed SDK
          List<SandboxedSdk> sandboxedSdks =
                  mSdkContext
                          .getSystemService(SdkSandboxController.class)
                          .getSandboxedSdks();
          mediateeSdk =
                  sandboxedSdks.stream()
                          .filter(
                                  s ->
                                          s.getSharedLibraryInfo()
                                                  .getName()
                                                  .contains(MEDIATEE_SDK))
                          .findAny()
                          .get();
        } catch (Exception e) {
          throw new IllegalStateException("Error in sdk-sdk communication ", e);
        }
        try {
          IBinder binder = mediateeSdk.getInterface();
          ISdkApi sdkApi = ISdkApi.Stub.asInterface(binder);
          message = sdkApi.getMessage();
        } catch (RemoteException e) {
          throw new IllegalStateException(e);
        }
      } else {
        message = "Sdk to sdk communication cannot be done";
      }
      int c = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
      canvas.drawColor(c);
      canvas.drawText(message, 75, 75, paint);
    }
  }
}
