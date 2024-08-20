package com.existing.sdk

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.privacysandbox.activity.client.createSdkActivityLauncher
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.SandboxedSdkViewUiInfo
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionObserver
import androidx.privacysandbox.ui.core.SessionObserverContext
import androidx.privacysandbox.ui.core.SessionObserverFactory
import com.example.api.SdkBannerRequest

class BannerAd(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    // This method could divert a percentage of requests to a sandboxed SDK and fallback to
    // existing ad logic. For this example, we send all requests to the sandboxed SDK as long as it
    // exists.
    suspend fun loadAd(
        baseActivity: AppCompatActivity,
        clientMessage: String,
        allowSdkActivityLaunch: () -> Boolean,
        shouldLoadWebView: Boolean,
        shouldLoadMediatedAd: Boolean) {
        val bannerAd = getBannerAdFromRuntimeEnabledSdkIfExists(
            baseActivity,
            clientMessage,
            allowSdkActivityLaunch,
            shouldLoadWebView,
            shouldLoadMediatedAd
        )
        if (bannerAd != null) {
            val sandboxedSdkView = SandboxedSdkView(context)
            addViewToLayout(sandboxedSdkView)
            sandboxedSdkView.setAdapter(bannerAd)
            bannerAd.addObserverFactory(SessionObserverFactoryImpl())
            return
        }

        val textView = TextView(context)
        textView.text = "Ad from SDK in the app"
        addViewToLayout(textView)
    }

    private suspend fun getBannerAdFromRuntimeEnabledSdkIfExists(
        baseActivity: AppCompatActivity,
        message: String,
        allowSdkActivityLaunch: () -> Boolean,
        shouldLoadWebView: Boolean,
        shouldLoadMediatedAd: Boolean
    ): SandboxedUiAdapter? {
        if (!ExistingSdk.isSdkLoaded()) {
            return null
        }

        val launcher = baseActivity.createSdkActivityLauncher(allowSdkActivityLaunch)
        val request = SdkBannerRequest(message, launcher, shouldLoadWebView)
        return ExistingSdk.loadSdkIfNeeded(context)?.getBanner(request, shouldLoadMediatedAd)
    }

    private fun addViewToLayout(view: View) {
        removeAllViews()
        view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        super.addView(view)
    }
}

private class SessionObserverFactoryImpl : SessionObserverFactory {
    override fun create(): SessionObserver {
        return SessionObserverImpl()
    }

    private inner class SessionObserverImpl : SessionObserver {
        override fun onSessionOpened(sessionObserverContext: SessionObserverContext) {
            Log.i("Test", "onSessionOpened $sessionObserverContext")
        }

        override fun onUiContainerChanged(uiContainerInfo: Bundle) {
            val sandboxedSdkViewUiInfo = SandboxedSdkViewUiInfo.fromBundle(uiContainerInfo)
            val onScreen = sandboxedSdkViewUiInfo.onScreenGeometry
            val width = sandboxedSdkViewUiInfo.uiContainerWidth
            val height = sandboxedSdkViewUiInfo.uiContainerHeight
            val opacity = sandboxedSdkViewUiInfo.uiContainerOpacityHint
            Log.i("Test", "UI info" +
                    "on-screen $onScreen, width $width, height $height, opacity $opacity")
        }

        override fun onSessionClosed() {
            Log.i("Test", "onSessionClosed")
        }
    }
}
