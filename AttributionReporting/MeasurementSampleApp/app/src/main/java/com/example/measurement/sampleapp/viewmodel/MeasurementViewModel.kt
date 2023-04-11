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
package com.example.measurement.sampleapp.viewmodel

import android.adservices.measurement.MeasurementManager
import android.net.Uri
import android.util.Log
import android.view.InputEvent
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import java.util.concurrent.Executors
import javax.inject.Inject

/*
* MeasurementViewModel
* This is the Measurement viewModel that handles communication to the Measurement APIs.
* */
class MeasurementViewModel @Inject constructor(private val measurementManager: MeasurementManager)
  : ViewModel() {

  /*
  * registerSource
  * This method invokes the registerSource Measurement API.
  * */
   fun registerSource(inputEvent: InputEvent?, serverUrl: String, adId: String){
     Log.d("adservices", "registerSource")
     measurementManager.registerSource(Uri.parse("$serverUrl?ad_id=$adId"),
                                       inputEvent, /* executor = */ null, /* callback = */ null)
   }

  /*
  * registerTrigger
  * This method invokes the registerTrigger Measurement API.
  * */
  fun registerTrigger(serverUrl: String, convId: String){
    Log.d("adservices","registerTrigger")
    measurementManager.registerTrigger(Uri.parse("$serverUrl?conv_id=$convId"),
                                   /* executor = */ null, /* callback = */ null)
  }

}