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
import android.os.Bundle
import com.example.adservices.samples.topics.sampleapp.R
import android.adservices.topics.TopicsManager
import android.adservices.topics.GetTopicsRequest
import android.os.OutcomeReceiver
import android.adservices.topics.GetTopicsResponse
import android.adservices.exceptions.GetTopicsException
import android.util.Log
import android.view.View
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Android application activity for testing Topics API by sending a call to
 * the 'getTopics()' function onResume. When a result is received it will be displayed
 * on screen in a text box, as well as displaying a text box showing what the current
 * package name is for the application. This project can be build with 11 different
 * flavors, each of which will assign a different package name corresponding to a
 * different suite of possible Topics.
 */
class MainActivity : AppCompatActivity() {
  //TextView to display results from getTopics call
  private lateinit var results: TextView;

  //TextView to display current package name which influences returned topics
  private lateinit var packageNameDisplay: TextView;

  //On app creation setup view as well as assign variables for TextViews to display results
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    results = findViewById<View>(R.id.textView) as TextView
    packageNameDisplay = findViewById<View>(R.id.textView2) as TextView
  }

  //On Application Resume, call getTopics code. This can be used to facilitate automating population of topics data
  override fun onResume() {
    super.onResume()
    packageNameDisplay.text = baseContext.packageName
    TopicGetter()
  }

  //TopicGetter holds all of the setup and code for creating a TopicsManager and getTopics call
  fun TopicGetter() {
    val mContext = baseContext
    val mTopicsManager = mContext.getSystemService(
      TopicsManager::class.java)
    val mExecutor: Executor = Executors.newCachedThreadPool()
    val mGetTopicsRequest = GetTopicsRequest.Builder().setSdkName("com.example.adtech")
    mTopicsManager.getTopics(mGetTopicsRequest.build(), mExecutor,
                             mCallback as OutcomeReceiver<GetTopicsResponse, GetTopicsException>)
  }

  //onResult is called when getTopics successfully comes back with an answer
  var mCallback: OutcomeReceiver<*, *> =
    object : OutcomeReceiver<GetTopicsResponse, GetTopicsException> {
      override fun onResult(result: GetTopicsResponse) {
        val topicsResult = result.topics
        for (i in topicsResult.indices) {
          Log.i("Topic", topicsResult[i])
          if (results.isEnabled) {
            results.text = topicsResult[i]
          }
        }
        if (topicsResult.size == 0) {
          Log.i("Topic", "Returned Empty")
          if (results.isEnabled) {
            results.text = "Returned Empty"
          }
        }
      }

      //onError should not be returned, even invalid topics callers should simply return empty
      override fun onError(error: GetTopicsException) {
        // Handle error
        Log.i("Topic", "Experienced an Error, and did not return successfully")
      }
    }
}