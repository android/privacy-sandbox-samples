package com.mediatee.implementation

import android.content.Context
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.privacysandbox.activity.core.SdkActivityLauncher
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import com.mediatee.api.FullscreenAd


class FullscreenAdImpl(private val sdkContext: Context) : FullscreenAd {

    private val webView = WebView(sdkContext)
    private val controller = SdkSandboxControllerCompat.from(sdkContext)

    init {
        initializeSettings(webView.settings)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                return false
            }
        }
        webView.loadUrl(WEB_VIEW_LINK)
    }

    override suspend fun show(activityLauncher: SdkActivityLauncher) {
        val handler = object : SdkSandboxActivityHandlerCompat {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onActivityCreated(activityHolder: ActivityHolder) {
                val activityHandler = ActivityHandler(activityHolder, webView)
                activityHandler.buildLayout()

                ViewCompat.setOnApplyWindowInsetsListener(activityHolder.getActivity().window.decorView) { view, windowInsets ->
                    val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.updatePadding(top = insets.top)
                    WindowInsetsCompat.CONSUMED
                }
            }
        }

        val token = controller.registerSdkSandboxActivityHandler(handler)
        val launched = activityLauncher.launchSdkActivity(token)
        if (!launched) controller.unregisterSdkSandboxActivityHandler(handler)
    }

    private fun initializeSettings(settings: WebSettings) {
        settings.javaScriptEnabled = true
        settings.setGeolocationEnabled(true)
        settings.setSupportZoom(true)
        settings.databaseEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
    }

    companion object {
        private const val WEB_VIEW_LINK = "https://github.com"
    }
}
