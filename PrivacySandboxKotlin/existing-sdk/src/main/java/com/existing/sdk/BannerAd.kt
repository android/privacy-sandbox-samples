package com.existing.sdk

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.privacysandbox.activity.client.createSdkActivityLauncher
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import com.example.api.SdkBannerRequest

class BannerAd(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    // This method could divert a percentage of requests to a sandboxed SDK and fallback to
    // existing ad logic. For this example, we send all requests to the sandboxed SDK as long as it
    // exists.
    suspend fun loadAd(
        baseActivity: AppCompatActivity,
        clientMessage: String,
        allowSdkActivityLaunch: () -> Boolean,
        shouldLoadWebView: Boolean) {
        val bannerAd = getBannerAdFromRuntimeEnabledSdkIfExists(
                            baseActivity, clientMessage, allowSdkActivityLaunch, shouldLoadWebView)
        if (bannerAd != null) {
            val sandboxedSdkView = SandboxedSdkView(context)
            addViewToLayout(sandboxedSdkView)
            sandboxedSdkView.setAdapter(bannerAd)
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
        shouldLoadWebView: Boolean
    ): SandboxedUiAdapter? {
        if (!ExistingSdk.isSdkLoaded()) {
            return null
        }

        val launcher = baseActivity.createSdkActivityLauncher(allowSdkActivityLaunch)
        val request = SdkBannerRequest(message, launcher, shouldLoadWebView)
        return ExistingSdk.loadSdkIfNeeded(context)?.getBanner(request);
    }

    private fun addViewToLayout(view: View) {
        removeAllViews()
        view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        super.addView(view)
    }
}
