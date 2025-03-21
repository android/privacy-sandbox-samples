package com.inappmediatee.sdk

import android.app.Activity
import android.os.Bundle
import com.inappmediatee.R

/** This activity is declared in the manifest of the In-app mediatee. */
class LocalActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.full_screen_ad)
    }
}