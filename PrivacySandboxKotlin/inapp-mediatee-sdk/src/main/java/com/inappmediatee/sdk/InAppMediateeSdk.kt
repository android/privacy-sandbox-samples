package com.inappmediatee.sdk

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.webkit.WebView
import android.widget.TextView

class InAppMediateeSdk(private val context: Context) {

    private val webViewUrl = "https://www.google.com"
    private val bannerAdMsg = "Ad from In-app Mediatee SDK"

    fun loadBannerAd(isWebViewBannerAd: Boolean) : View {
        if (isWebViewBannerAd) {
            val webview = WebView(context)
            webview.loadUrl(webViewUrl)
            return webview
        }
        val textView = TextView(context)
        textView.text = bannerAdMsg
        textView.setTextColor(Color.WHITE)
        return textView
    }

    fun loadFullscreenAd() {
      // All the heavy logic to load fullscreen Ad that Mediatee needs to perform goes here.
    }

    fun showFullscreenAd() {
        val intent = Intent(context, LocalActivity::class.java)
        context.startActivity(intent)
    }
}