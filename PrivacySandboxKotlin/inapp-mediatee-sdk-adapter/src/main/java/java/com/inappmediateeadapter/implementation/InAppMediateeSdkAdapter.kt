package com.inappmediateeadapter.implementation

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import com.inappmediatee.sdk.InAppMediateeSdk
import com.example.api.MediateeAdapterInterface
import java.com.inappmediateeadapter.implementation.SandboxedUiAdapterImpl

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
        return SandboxedUiAdapterImpl(inAppMediateeSdk.loadBannerAd(isWebViewBannerAd))
            .toCoreLibInfo(context)
    }

    override suspend fun loadFullscreenAd() {
        inAppMediateeSdk.loadFullscreenAd()
    }

    override suspend fun showFullscreenAd(activityLauncher: SdkActivityLauncher) {
        // SdkActivityLauncher is ignored since In-App mediatee does not need it in its
        // showFullscreenAd() call.
        // In-app mediatee declares an Activity in its manifest that is used to show the ad,
        // SdkActivityLauncher is not used.
        inAppMediateeSdk.showFullscreenAd()
    }
}