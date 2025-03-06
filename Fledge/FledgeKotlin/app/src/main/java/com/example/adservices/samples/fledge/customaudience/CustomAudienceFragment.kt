package com.example.adservices.samples.fledge.customaudience

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adservices.samples.fledge.customaudience.toggle.CustomAudienceToggleAdapter
import com.example.adservices.samples.fledge.sampleapp.databinding.FragmentCustomAudienceBinding
import com.example.adservices.samples.fledge.util.EventLogManager
import kotlinx.coroutines.launch

class CustomAudienceFragment : Fragment() {
  private val viewModel: CustomAudienceViewModel by viewModels()

  private lateinit var binding: FragmentCustomAudienceBinding
  private lateinit var eventLog: EventLogManager
  private lateinit var toggleAdapter: CustomAudienceToggleAdapter

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    binding = FragmentCustomAudienceBinding.inflate(inflater, container, false)

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    eventLog = EventLogManager(binding.eventLog)

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.buyerBaseUri.collect { buyerBaseUri ->
            binding.baseUrlInput.setText(buyerBaseUri.toString())
          }
        }
        launch {
          viewModel.toggles.collect { toggles ->
            toggleAdapter.submitList(toggles)
          }
        }
      }
    }

    viewModel.loadCustomAudienceBuyerConfig(eventLog::writeEvent)
    setupToggleRecyclerView()
    setupUpdateButton()
  }

  private fun setupToggleRecyclerView() {
    toggleAdapter = CustomAudienceToggleAdapter(
      statusReceiver = eventLog::writeEvent,
      listener = viewModel.toggleListener
    )

    binding.toggleRecyclerView.layoutManager = LinearLayoutManager(context)
    binding.toggleRecyclerView.adapter = toggleAdapter
  }

  private fun setupUpdateButton() {
    binding.updateButton.setOnClickListener {
      viewModel.updateBuyerBaseUri(binding.baseUrlInput.text.toString(), eventLog::writeEvent)
    }
  }
}