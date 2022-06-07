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

package com.example.measurement.sampleapp.view.base

import android.adservices.AdServicesApiUtil
import android.adservices.measurement.MeasurementManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.android.support.DaggerFragment
import javax.inject.Inject

/*
* BaseFragment
* This is the Base Fragment that provides common elements.
* */
open class BaseFragment : DaggerFragment() {

  /*
  * navController
  * This is an instance of the navigation controller.
  * */
  private lateinit var navController: NavController

  @Inject
  lateinit var viewModelFactory: ViewModelProvider.Factory

  /*
  * provideViewModel
  * This is a helper method to provide view models
  * */
  inline fun <reified VM : ViewModel> provideViewModel() =
    ViewModelProvider(this, viewModelFactory)[VM::class.java]

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    navController = NavHostFragment.findNavController(this)
  }
}