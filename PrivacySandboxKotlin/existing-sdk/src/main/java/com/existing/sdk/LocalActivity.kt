package com.existing.sdk

import android.app.Activity
import android.os.Bundle

class LocalActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.local_full_screen_ad)
    }
}