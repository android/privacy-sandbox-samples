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

package com.example.adservices.samples.fledge.SdkExtensionsHelpers;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.ext.SdkExtensions;

public class VersionCompatUtil {
  @SuppressLint("InlinedApi")
  public static boolean isTPlusWithMinAdServicesVersion(int minVersion) {
    return Build.VERSION.SDK_INT >= 33 &&
        SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= minVersion;
  }

  public static boolean isSWithMinExtServicesVersion(int minVersion) {
    return (Build.VERSION.SDK_INT == 31 || Build.VERSION.SDK_INT == 32) &&
        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= minVersion;
  }

  public static boolean isTestableVersion(int minAdServicesVersion, int minExtServicesVersion) {
    return isTPlusWithMinAdServicesVersion(minAdServicesVersion) ||
        isSWithMinExtServicesVersion(minExtServicesVersion);
  }
}
