/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mediatee.implementation

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder

/* This class creates the layout of the activity that shows the ad. */
class ActivityHandler(
    private val activityHolder: ActivityHolder,
    private val adView: View,
) {
    private var activity = activityHolder.getActivity()
    private var backControlButton: Button? = null
    private var destroyActivityButton: Button? = null
    private var openLandingPage: Button? = null

    fun buildLayout() {
        val layout = buildLayoutProgrammatically()
        registerBackControlButton()
        registerDestroyActivityButton()
        registerOpenLandingPageButton()
        registerLifecycleListener()
    }

    /**
     * Building the activity layout programmatically.
     */
    private fun buildLayoutProgrammatically(): ViewGroup {
        val mainLayout = LinearLayout(activity)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.layoutParams =
            ViewGroup.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

        backControlButton = Button(activity)
        backControlButton!!.text = DISABLE_BACK_NAVIGATION
        mainLayout.addView(backControlButton)

        destroyActivityButton = Button(activity)
        destroyActivityButton!!.text = DESTROY_ACTIVITY
        mainLayout.addView(destroyActivityButton)

        openLandingPage = Button(activity)
        openLandingPage!!.text = OPEN_LANDING_PAGE
        mainLayout.addView(openLandingPage)

        if (adView.parent != null) {
            (adView.parent as ViewGroup).removeView(adView)
        }
        adView.layoutParams = ViewGroup.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        mainLayout.addView(adView)

        activity.setContentView(mainLayout)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return mainLayout
    }

    private fun registerBackControlButton() {
        val disabler = BackNavigationDisabler(
            activityHolder.getOnBackPressedDispatcher(), backControlButton!!
        )
        backControlButton!!.setOnClickListener { disabler.toggle() }
    }

    private fun registerDestroyActivityButton() {
        destroyActivityButton!!.setOnClickListener { activity.finish() }
    }

    private fun registerOpenLandingPageButton() {
        openLandingPage!!.setOnClickListener {
            val visitUrl = Intent(Intent.ACTION_VIEW)
            visitUrl.setData(Uri.parse(LANDING_PAGE_URL))
            visitUrl.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(visitUrl)
        }
    }

    private fun registerLifecycleListener() {
        activityHolder.lifecycle.addObserver(LocalLifecycleObserver())
    }

    private fun makeToast(message: String) {
        activity.runOnUiThread { Toast.makeText(activity, message, Toast.LENGTH_SHORT).show() }
    }

    inner class BackNavigationDisabler(
        private val dispatcher: OnBackPressedDispatcher,
        private val backButton: Button
    ) {
        private val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                makeToast("Can not go back!")
            }
        }

        private var backNavigationDisabled = false // default is back enabled.

        @Synchronized
        fun toggle() {
            if (backNavigationDisabled) {
                onBackPressedCallback.remove()
                backButton.text = DISABLE_BACK_NAVIGATION
            } else {
                dispatcher.addCallback(onBackPressedCallback)
                backButton.text = ENABLE_BACK_NAVIGATION
            }
            backNavigationDisabled = !backNavigationDisabled
        }
    }

    inner class LocalLifecycleObserver : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            makeToast("Current activity state is: $event")
        }
    }

    companion object {
        private const val DISABLE_BACK_NAVIGATION = "Disable Back Navigation"
        private const val ENABLE_BACK_NAVIGATION = "Enable Back Navigation"
        private const val DESTROY_ACTIVITY = "Destroy Activity"
        private const val OPEN_LANDING_PAGE = "Open Landing Page"
        private const val LANDING_PAGE_URL = "https://www.google.com"
    }
}