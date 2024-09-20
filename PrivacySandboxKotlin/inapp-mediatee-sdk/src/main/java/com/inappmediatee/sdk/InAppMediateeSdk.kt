package com.inappmediatee.sdk

import android.content.Context
import android.content.Intent

class InAppMediateeSdk(private val context: Context) {

    fun showFullscreenAd() {
        val intent = Intent(context, LocalActivity::class.java)
        context.startActivity(intent)
    }
}