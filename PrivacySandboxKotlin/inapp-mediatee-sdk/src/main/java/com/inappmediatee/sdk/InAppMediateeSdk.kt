package com.inappmediatee.sdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import com.example.api.InAppMediateeSdkInterface

class InAppMediateeSdk(private val context: Context) : InAppMediateeSdkInterface {

    private val url = "https://www.google.com/"

    override suspend fun getInterstitial() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(context, browserIntent, null)
    }

}