package com.example.adservices.samples.fledge.protectedsignals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.adservices.samples.fledge.sampleapp.databinding.FragmentSignalsBinding
import com.example.adservices.samples.fledge.util.EventLogManager

class ProtectedSignalsFragment : Fragment() {
  private val viewModel: ProtectedSignalsViewModel by viewModels()

  private lateinit var binding: FragmentSignalsBinding
  private lateinit var eventLog: EventLogManager

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    binding = FragmentSignalsBinding.inflate(inflater, container, false)

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    eventLog = EventLogManager(binding.eventLog)

    binding.updateSignalsButton.setOnClickListener {
      viewModel.updateSignals(
        urlInput = binding.urlInput.text.toString(),
        statusReceiver = eventLog::writeEvent
      )
    }
  }
}