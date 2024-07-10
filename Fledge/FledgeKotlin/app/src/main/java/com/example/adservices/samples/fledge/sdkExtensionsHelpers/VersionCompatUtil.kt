package com.example.adservices.samples.fledge.sdkExtensionsHelpers

import android.annotation.SuppressLint
import android.os.Build
import android.os.ext.SdkExtensions

object VersionCompatUtil {
  @SuppressLint("InlinedApi")
  fun isTPlusWithMinAdServicesVersion(minVersion: Int): Boolean {
    return Build.VERSION.SDK_INT >= 33 &&
        SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= minVersion
  }

  fun isSWithMinExtServicesVersion(minVersion: Int): Boolean {
    return (Build.VERSION.SDK_INT == 31 || Build.VERSION.SDK_INT == 32) &&
        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= minVersion
  }

  fun isTestableVersion(minAdServicesVersion: Int, minExtServicesVersion: Int): Boolean {
    return isTPlusWithMinAdServicesVersion(minAdServicesVersion) ||
        isSWithMinExtServicesVersion(minExtServicesVersion)
  }
}
