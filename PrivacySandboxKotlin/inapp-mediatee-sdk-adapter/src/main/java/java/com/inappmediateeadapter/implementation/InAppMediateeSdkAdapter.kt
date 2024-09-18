package com.inappmediateeadapter.implementation

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import com.inappmediatee.sdk.InAppMediateeSdk
import com.example.api.MediateeAdapterInterface

/**
 * Adapter class that implements the interface declared by the Mediator.
 *
 * This is loaded by and registered with Mediator from existing-sdk (RA_SDK).
 */
class InAppMediateeSdkAdapter(private val context: Context): MediateeAdapterInterface {

    private val inAppMediateeSdk = InAppMediateeSdk(context)

    override suspend fun getBannerAd(
        appPackageName: String,
        activityLauncher: SdkActivityLauncher,
        isWebViewBannerAd: Boolean
    ): Bundle {
        TODO("Not yet implemented")
    }

    override suspend fun showFullscreenAd(activityLauncher: SdkActivityLauncher) {
        // SdkActivityLauncher is ignored.
        inAppMediateeSdk.showFullscreenAd()
    }
}