package com.example.implementation

import android.content.Context
import android.webkit.WebView
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat

class InterstitialAd(private val context: Context) {

    private val controller = SdkSandboxControllerCompat.from(context)

    private val url = "https://github.com"

    suspend fun showAd(activityLauncher: SdkActivityLauncher) {
        val handler = object : SdkSandboxActivityHandlerCompat {
            override fun onActivityCreated(activityHolder: ActivityHolder) {
                val webview = WebView(context)
                webview.loadUrl(url)
                activityHolder.getActivity().setContentView(webview)
            }
        }

        val token = controller.registerSdkSandboxActivityHandler(handler)
        val launched = activityLauncher.launchSdkActivity(token)
        if (!launched) controller.unregisterSdkSandboxActivityHandler(handler)
    }
}