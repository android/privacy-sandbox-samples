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
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.example.measurement.sampleapp.R
import com.example.measurement.sampleapp.databinding.FragmentTriggerBinding
import com.example.measurement.sampleapp.view.base.BaseFragment
import com.example.measurement.sampleapp.viewmodel.MainViewModel
import com.example.measurement.sampleapp.viewmodel.MeasurementViewModel

/*
* TriggerFragment
* This fragment contains a button to test the registerTrigger.
* */
class TriggerFragment : BaseFragment() {

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

  private lateinit var binding: FragmentTriggerBinding

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentTriggerBinding.inflate(inflater, container, false)

    binding.triggerConvIdInput.setText(mainViewModel.getConversionRegistrationId())
    binding.triggerConvIdInput.doOnTextChanged { text, _,_,_ -> mainViewModel.setConvId(text.toString()) }

    binding.registerTriggerButton.setOnClickListener {
      measurementViewModel.registerTrigger(mainViewModel.getServerTriggerUrl(), mainViewModel.getConversionRegistrationId())
      Toast.makeText(requireContext(), R.string.registering, Toast.LENGTH_SHORT).show()
    }

    return binding.root
  }
}