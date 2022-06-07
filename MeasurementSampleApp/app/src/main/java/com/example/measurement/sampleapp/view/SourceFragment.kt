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
package com.example.measurement.sampleapp.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import com.example.measurement.sampleapp.databinding.FragmentSourceBinding
import com.example.measurement.sampleapp.view.base.BaseFragment
import com.example.measurement.sampleapp.viewmodel.MainViewModel
import com.example.measurement.sampleapp.viewmodel.MeasurementViewModel


import android.widget.Toast
import com.example.measurement.sampleapp.R

/*
* SourceFragment
* This fragment has two buttons to test registerSource.
* 1. Register Click Event
* 2. Register View Event
* */
class SourceFragment : BaseFragment() {

  /*
  * measurementViewModel
  * This is the Measurement ViewModel reference that handles communication to the measurement api.
  * */
  private val measurementViewModel by lazy { provideViewModel<MeasurementViewModel>() }

  /*
  * mainViewModel
  * This is the Main ViewModel reference that handles storage operations.
  * */
  private val mainViewModel by lazy { provideViewModel<MainViewModel>() }

  /*
  * binding
  * This is the view binding reference to access view elements.
  * */
  private lateinit var binding: FragmentSourceBinding

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    binding = FragmentSourceBinding.inflate(inflater, container, false)

    binding.sourceAdIdInput.setText(mainViewModel.getSourceRegistrationId())
    binding.sourceAdIdInput.doOnTextChanged { text, _,_,_ -> mainViewModel.setSourceRegistrationId(text.toString()) }

    binding.registerCtcButton.setOnTouchListener { _, event ->
      if (event.action == MotionEvent.ACTION_DOWN) {
        measurementViewModel.registerSource(event, mainViewModel.getServerUrl(), mainViewModel.getSourceRegistrationId())
        Toast.makeText(requireContext(), R.string.registering, Toast.LENGTH_SHORT).show()
      }
      false
    }

    binding.registerVtcButton.setOnClickListener {
      measurementViewModel.registerSource(null,  mainViewModel.getServerUrl(), mainViewModel.getSourceRegistrationId())
    }
    return binding.root
  }

}