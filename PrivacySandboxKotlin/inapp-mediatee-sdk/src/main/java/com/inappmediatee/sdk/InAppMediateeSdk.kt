package com.inappmediatee.sdk

import android.content.Context
import android.content.Intent
import com.example.api.InAppMediateeSdkInterface

class InAppMediateeSdk(private val context: Context) : InAppMediateeSdkInterface {

    override suspend fun show() {
        val intent = Intent(context, LocalActivity::class.java)
        context.startActivity(intent)
    }
}