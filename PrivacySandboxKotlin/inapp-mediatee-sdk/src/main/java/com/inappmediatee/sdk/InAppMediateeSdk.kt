package com.inappmediatee.sdk

import android.content.Context
import android.content.Intent

class InAppMediateeSdk(private val context: Context) {

    fun loadFullscreenAd() {
      // This could contain more logic. For our sample this is empty.
    }

    fun showFullscreenAd() {
        val intent = Intent(context, LocalActivity::class.java)
        context.startActivity(intent)
    }
}