/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.example.adservices.samples.topics.sampleapp

import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.example.adservices.samples.topics.sampleapp.AdvertisingTopicsClient
import android.os.Bundle
import com.example.adservices.samples.topics.sampleapp.MainActivity
import android.adservices.topics.GetTopicsResponse
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.example.adservices.samples.topics.sampleapp.databinding.ActivityMainBinding
import java.lang.StringBuilder
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Android application activity for testing Topics API by providing a button in UI that initiate
 * user's interaction with Topics Manager in the background. Response from Topics API will be shown
 * in the app as text as well as toast message. In case anything goes wrong in this process, error
 * message will also be shown in toast to suggest the Exception encountered.
 */
class MainActivity : AppCompatActivity() {
  // Once click on this button, the call to AdServices will be triggered
  private var mTopicsClientButton: Button? = null

  // Topics get from the call to AdServices will be shown here
  private var mResultTextView: TextView? = null

  // Helper class which make call to AdService's TopicsManager
  // and get Topics for this app
  private var mAdvertisingTopicsClient: AdvertisingTopicsClient? = null

  // View binding for MainActivity to ease interactions with views
  private var binding: ActivityMainBinding? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(
      layoutInflater)
    val view: View = binding!!.root
    setContentView(view)
    mTopicsClientButton = binding!!.topicsClientButton
    mResultTextView = binding!!.textView
    mAdvertisingTopicsClient = AdvertisingTopicsClient.Builder()
      .setContext(this)
      .setSdkName(mSdkName)
      .setExecutor(CALLBACK_EXECUTOR)
      .build()
    registerGetTopicsButton()
  }

  // Register Topics Client Button so that every time people click on this
  // button, a call call to AdService's TopicsManager will be triggered and
  // app can get topics associated with it
  private fun registerGetTopicsButton() {
    mTopicsClientButton!!.setOnClickListener { v: View? ->
      runOnUiThread(Runnable {
        try {
          val result = mAdvertisingTopicsClient!!.topics.get()
          val topics = result!!.topics.joinToString(SPACE)
          val text = "Topics are $topics"
          mResultTextView!!.text = text
          makeToast(text)
        } catch (e: ExecutionException) {
          makeToast(e.message)
        } catch (e: InterruptedException) {
          makeToast(e.message)
        }
      })
    }
  }

  private fun makeToast(message: String?) {
    runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show() }
  }

  companion object {
    // Executor to be used by AdvertisingTopicsClient
    private val CALLBACK_EXECUTOR: Executor = Executors.newCachedThreadPool()

    // String containing one space to be used to split topic results
    private const val SPACE = " "

    // Name of SDK used by this app. In reality one app can have several SDK
    private const val mSdkName = "SdkName"
  }
}