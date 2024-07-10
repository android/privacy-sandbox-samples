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
