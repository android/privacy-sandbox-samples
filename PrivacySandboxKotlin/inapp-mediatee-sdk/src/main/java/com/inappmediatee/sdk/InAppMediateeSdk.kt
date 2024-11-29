package com.inappmediatee.sdk

import android.content.Context
import android.content.Intent
import android.view.View
import android.webkit.WebView
import com.inappmediatee.R

class InAppMediateeSdk(private val context: Context) {

    private val webViewUrl = "https://www.google.com"

    fun loadBannerAd(isWebViewBannerAd: Boolean) : View {
        if (isWebViewBannerAd) {
            val webview = WebView(context)
            webview.loadUrl(webViewUrl)
            return webview
        }
        return View.inflate(context, R.layout.banner, null)
    }

    fun loadFullscreenAd() {
      // All the heavy logic to load fullscreen Ad that Mediatee needs to perform goes here.
    }

    fun showFullscreenAd() {
        val intent = Intent(context, LocalActivity::class.java)
        context.startActivity(intent)
    }
}