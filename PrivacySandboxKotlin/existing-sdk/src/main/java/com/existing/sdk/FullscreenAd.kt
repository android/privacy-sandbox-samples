package com.existing.sdk

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.privacysandbox.activity.client.createSdkActivityLauncher
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import com.example.api.FullscreenAd

class FullscreenAd(private val sdkFullscreenAd: FullscreenAd) {
    suspend fun show(baseActivity: AppCompatActivity, allowSdkActivityLaunch: () -> Boolean) {
        val activityLauncher = baseActivity.createSdkActivityLauncher(allowSdkActivityLaunch)
        sdkFullscreenAd.show(activityLauncher)
    }

    companion object {
        // This method could divert a percentage of requests to a sandboxed SDK and fallback to
        // existing ad logic. For this example, we send all requests to the sandboxed SDK as long as
        // it exists.
        suspend fun create(context: Context): com.existing.sdk.FullscreenAd {
            if (ExistingSdk.isSdkLoaded()) {
                val remoteFullscreenAd = ExistingSdk.loadSdkIfNeeded(context)?.getFullscreenAd()
                if (remoteFullscreenAd != null)
                    return FullscreenAd(remoteFullscreenAd)
            }
            return FullscreenAd(LocalFullscreenAdImpl(context))
        }
    }

    internal class LocalFullscreenAdImpl(private val context: Context) : FullscreenAd {
        override suspend fun show(activityLauncher: SdkActivityLauncher) {
            val intent = Intent(context, LocalActivity::class.java)
            context.startActivity(intent)
        }
    }
}
