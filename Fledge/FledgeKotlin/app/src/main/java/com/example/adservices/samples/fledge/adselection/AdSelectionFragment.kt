package com.example.adservices.samples.fledge.adselection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.adservices.samples.fledge.sampleapp.databinding.FragmentAdSelectionBinding
import com.example.adservices.samples.fledge.util.EventLogManager
import kotlinx.coroutines.launch

class AdSelectionFragment : Fragment() {

  private val viewModel: AdSelectionViewModel by viewModels()

  private lateinit var binding: FragmentAdSelectionBinding
  private lateinit var eventLog: EventLogManager

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    binding = FragmentAdSelectionBinding.inflate(inflater, container, false)

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    eventLog = EventLogManager(binding.eventLog)

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.adSelectionId.collect { adSelectionId ->
            binding.adSelectionId.text = adSelectionId?.toString() ?: "<ad selection ID>"
          }
        }
        launch {
          viewModel.isServerAuction.collect { isServerAuction ->
            binding.auctionCheckbox.isChecked = isServerAuction
            setupRunAdSelectionButton(isServerAuction)
          }
        }
      }
    }

    setupAuctionCheckbox()
    setupReportImpressionButton()
    setupUpdateAdCounterHistogramButton()
    setupReportClickEventButton()
  }

  private fun setupAuctionCheckbox() {
    binding.auctionCheckbox.setOnCheckedChangeListener { _, isServerAuction ->
      viewModel.updateIsServerAuction(isServerAuction)
    }
  }

  private fun setupRunAdSelectionButton(isAuctionServerEnabled: Boolean) {
    eventLog.writeEvent("Auction Server set to $isAuctionServerEnabled")
    binding.adSelectionButton.setOnClickListener {
      viewModel.runAdSelection(
        isAuctionServerEnabled = isAuctionServerEnabled,
        statusReceiver = eventLog::writeEvent,
        renderUriReceiver = binding.adSpace::setText
      )
    }
  }

  private fun setupReportImpressionButton() {
    binding.reportImpressionButton.setOnClickListener {
      viewModel.reportImpression(eventLog::writeEvent)
    }
  }

  private fun setupUpdateAdCounterHistogramButton() {
    binding.updateAdCounterHistogramButton.setOnClickListener {
      viewModel.updateAdCounterHistogram(eventLog::writeEvent)
    }
  }

  private fun setupReportClickEventButton() {
    binding.reportClickEventButton.setOnClickListener {
      viewModel.reportClickEvent(
        binding.clickEventDataInput.text.toString(),
        eventLog::writeEvent
      )
    }
  }
}