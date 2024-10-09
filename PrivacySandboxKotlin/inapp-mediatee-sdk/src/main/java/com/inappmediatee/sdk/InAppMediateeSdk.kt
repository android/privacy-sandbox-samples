package com.inappmediatee.sdk

import android.content.Context
import android.content.Intent

class InAppMediateeSdk(private val context: Context) {

    fun loadFullscreenAd() {
      // All the heavy logic to load fullscreen Ad that Mdiatee needs to perform goes here.
    }

    fun showFullscreenAd() {
        val intent = Intent(context, LocalActivity::class.java)
        context.startActivity(intent)
    }
}