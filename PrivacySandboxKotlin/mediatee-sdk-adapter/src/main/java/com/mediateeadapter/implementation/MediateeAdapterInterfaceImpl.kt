package com.mediateeadapter.implementation

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import com.example.api.MediateeAdapterInterface
import com.mediatee.api.SdkServiceFactory

class MediateeAdapterInterfaceImpl(private val context: Context) : MediateeAdapterInterface {

    private var mediateeInstance: com.mediatee.api.SdkService? = null

    /** Name of the SDK to be loaded. */
    private val mediateeSdkName = "com.mediatee.sdk"

    override suspend fun getBannerAd(
        appPackageName: String,
        activityLauncher: SdkActivityLauncher,
        isWebViewBannerAd: Boolean
    ): Bundle {
        loadMediateeSdk()
        val newRequest: com.mediatee.api.SdkBannerRequest =
            com.mediatee.api.SdkBannerRequest(context.packageName, isWebViewBannerAd)
        return mediateeInstance!!.getBanner(newRequest).toCoreLibInfo(context)
    }

    override suspend fun showFullscreenAd(activityLauncher: SdkActivityLauncher) {
        loadMediateeSdk()
        return mediateeInstance!!.getFullscreenAd().show(activityLauncher)
    }

    private suspend fun loadMediateeSdk() {
        if (mediateeInstance == null) {
            val controller = SdkSandboxControllerCompat.from(context)
            val sandboxedSdk = controller.loadSdk(mediateeSdkName, Bundle.EMPTY)
            mediateeInstance = SdkServiceFactory.wrapToSdkService(sandboxedSdk.getInterface()!!)
        }
    }
}