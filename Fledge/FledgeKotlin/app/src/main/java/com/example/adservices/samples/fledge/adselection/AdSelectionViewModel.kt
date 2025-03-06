package com.example.adservices.samples.fledge.adselection

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.privacysandbox.ads.adservices.adselection.AdSelectionConfig
import androidx.privacysandbox.ads.adservices.adselection.GetAdSelectionDataRequest
import androidx.privacysandbox.ads.adservices.adselection.PersistAdSelectionResultRequest
import androidx.privacysandbox.ads.adservices.adselection.ReportEventRequest
import androidx.privacysandbox.ads.adservices.adselection.ReportImpressionRequest
import androidx.privacysandbox.ads.adservices.adselection.UpdateAdCounterHistogramRequest
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.common.FrequencyCapFilters
import com.example.adservices.samples.fledge.adselection.config.OnDeviceAdSelectionJsonConfigLoader
import com.example.adservices.samples.fledge.adselection.config.ServerAdSelectionJsonConfig
import com.example.adservices.samples.fledge.adselection.config.ServerAdSelectionJsonConfigLoader
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.google.common.io.BaseEncoding
import java.util.function.Consumer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdSelectionViewModel : ViewModel() {

  private val adSelectionClient = AdSelectionClient()
  private val biddingAuctionServerClient = BiddingAuctionServerClient()

  private var adSelectionConfig: AdSelectionConfig? = null
  private var serverAdSelectionJsonConfig: ServerAdSelectionJsonConfig? = null

  private val _adSelectionId = MutableStateFlow<Long?>(null)
  val adSelectionId = _adSelectionId.asStateFlow()

  private val _isServerAuction = MutableStateFlow(true)
  val isServerAuction = _isServerAuction.asStateFlow()

  fun runAdSelection(
    isAuctionServerEnabled: Boolean,
    statusReceiver: Consumer<String>,
    renderUriReceiver: Consumer<String>,
  ) {
    viewModelScope.launch {
      if (isAuctionServerEnabled) {
        runAdSelectionOnAuctionServer(statusReceiver, renderUriReceiver)
      } else {
        runAdSelectionOnDevice(statusReceiver, renderUriReceiver)
      }
    }
  }

  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  fun reportImpression(statusReceiver: Consumer<String>) {
    if (adSelectionId.value == null) statusReceiver.accept("You should run an auction first.")

    val request = if (isServerAuction.value) {
      ReportImpressionRequest(adSelectionId.value!!)
    } else {
      ReportImpressionRequest(adSelectionId.value!!, adSelectionConfig!!)
    }

    viewModelScope.launch {
      adSelectionClient.reportImpression(request, statusReceiver)
    }
  }

  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  fun updateAdCounterHistogram(statusReceiver: Consumer<String>) {
    if (adSelectionId.value == null) statusReceiver.accept("You should run an auction first.")

    val request = UpdateAdCounterHistogramRequest(
      adSelectionId = adSelectionId.value!!,
      adEventType = FrequencyCapFilters.AD_EVENT_TYPE_CLICK,
      callerAdTech = if (isServerAuction.value) adSelectionConfig!!.seller else AdTechIdentifier(
        serverAdSelectionJsonConfig!!.seller.toString())
    )

    viewModelScope.launch {
      adSelectionClient.updateAdCounterHistogram(request, statusReceiver)
    }
  }

  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  fun reportClickEvent(
    interactionDataInput: String,
    statusReceiver: Consumer<String>,
  ) {
    if (adSelectionId.value == null) statusReceiver.accept("You should run an auction first.")

    val request = ReportEventRequest(
      adSelectionId = adSelectionId.value!!,
      eventKey = "click",
      eventData = interactionDataInput,
      reportingDestinations = ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER or
        ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER
    )

    viewModelScope.launch {
      adSelectionClient.reportEvent(request, statusReceiver)
    }
  }

  @OptIn(ExperimentalFeatures.Ext8OptIn::class, ExperimentalFeatures.Ext10OptIn::class)
  fun runAdSelectionOnDevice(
    statusReceiver: Consumer<String>,
    renderUriReceiver: Consumer<String>,
  ) {
    viewModelScope.launch {
      try {
        adSelectionConfig = OnDeviceAdSelectionJsonConfigLoader().load(statusReceiver)
        statusReceiver.accept("Running on-device ad selection")
        Log.i(TAG, "Running on-device ad selection")

        try {
          val adSelectionOutcome =
            adSelectionClient.selectAds(adSelectionConfig!!, statusReceiver)

          if(adSelectionOutcome.hasOutcome() && adSelectionOutcome.renderUri != Uri.EMPTY) {
            renderUriReceiver.accept("Would display ad from ${adSelectionOutcome.renderUri}")
            _adSelectionId.emit(adSelectionOutcome.adSelectionId)

            val updateAdCounterHistogramRequest = UpdateAdCounterHistogramRequest(
              adSelectionId = adSelectionOutcome.adSelectionId,
              adEventType = FrequencyCapFilters.AD_EVENT_TYPE_IMPRESSION,
              callerAdTech = adSelectionConfig!!.seller
            )
            adSelectionClient.updateAdCounterHistogram(updateAdCounterHistogramRequest, statusReceiver)

            reportImpressionAfterAuction(adSelectionOutcome.adSelectionId, statusReceiver)
          } else {
            renderUriReceiver.accept("Ad selection failed, no ad to display")
          }
        } catch (e: Exception) {
          renderUriReceiver.accept("Ad selection failed, no ad to display")
        }
      } catch (e: Exception) {
        statusReceiver.accept("Skipped running on-device ad selection")
        Log.e(TAG, "Skipped running on-device ad selection")
      }
    }
  }

  @OptIn(ExperimentalFeatures.Ext10OptIn::class)
  fun runAdSelectionOnAuctionServer(
    statusReceiver: Consumer<String>,
    renderUriReceiver: Consumer<String>,
  ) {
    viewModelScope.launch {
      try {
        serverAdSelectionJsonConfig = ServerAdSelectionJsonConfigLoader().load(statusReceiver)
        statusReceiver.accept("Running ad selection on the auction server")
        Log.i(TAG, "Running ad selection on the auction server")

        try {
          serverAdSelectionJsonConfig?.run {
            val getAdSelectionDataRequest = if(coordinatorOriginUri == Uri.EMPTY) {
              GetAdSelectionDataRequest(seller)
            } else {
              GetAdSelectionDataRequest(seller, coordinatorOriginUri)
            }

            val getAdSelectionDataOutcome = adSelectionClient.getAdSelectionData(
              getAdSelectionDataRequest, statusReceiver)

            val selectAdsResponse = biddingAuctionServerClient.runServerAuction(
              getAdSelectionDataOutcome = getAdSelectionDataOutcome,
              auctionConfig = getServerAuctionConfig(),
              sellerSfeUri = sellerSfeUri.toString(),
              statusReceiver = statusReceiver
            )

            val persistAdSelectionResultRequest = PersistAdSelectionResultRequest(
              adSelectionId = getAdSelectionDataOutcome.adSelectionId,
              seller = seller,
              adSelectionResult = BaseEncoding.base64()
                .decode(selectAdsResponse.auctionResultCiphertext!!)
            )
            val adSelectionOutcome = adSelectionClient.persistAdSelectionResult(
              persistAdSelectionResultRequest, statusReceiver
            )

            if (adSelectionOutcome.hasOutcome() && adSelectionOutcome.renderUri != Uri.EMPTY) {
              renderUriReceiver.accept("Would display ad from ${adSelectionOutcome.renderUri}")
              _adSelectionId.emit(adSelectionOutcome.adSelectionId)
              reportImpressionAfterAuction(adSelectionOutcome.adSelectionId, statusReceiver)
            } else {
              renderUriReceiver.accept("Ad selection failed, no ad to display")
            }
          }
        } catch (e: Exception) {
          renderUriReceiver.accept("Ad selection failed, no ad to display")
        }
      } catch (e: Exception) {
        statusReceiver.accept("Skipped running ad selection on the auction server")
        Log.e(TAG, "Skipped running ad selection on the auction server")
      }
    }
  }

  @OptIn(ExperimentalFeatures.Ext8OptIn::class)
  private suspend fun reportImpressionAfterAuction(adSelectionId: Long, statusReceiver: Consumer<String>) {
    val request = if (isServerAuction.value) {
      ReportImpressionRequest(adSelectionId)
    } else {
      ReportImpressionRequest(adSelectionId, adSelectionConfig!!)
    }

    adSelectionClient.reportImpression(
      request = request,
      statusReceiver = statusReceiver,
      onSuccess = {
        val reportEventRequest = ReportEventRequest(
          adSelectionId = adSelectionId,
          eventKey = "view",
          eventData = "{\"viewTimeSeconds\":1}",
          reportingDestinations = ReportEventRequest.FLAG_REPORTING_DESTINATION_SELLER or
            ReportEventRequest.FLAG_REPORTING_DESTINATION_BUYER
        )
        adSelectionClient.reportEvent(reportEventRequest, statusReceiver)
      })
  }

  fun updateIsServerAuction(value: Boolean) {
    viewModelScope.launch {
      _isServerAuction.emit(value)
    }
  }
}