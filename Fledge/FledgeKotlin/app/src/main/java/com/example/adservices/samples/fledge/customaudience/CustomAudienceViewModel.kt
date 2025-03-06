package com.example.adservices.samples.fledge.customaudience

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience
import androidx.privacysandbox.ads.adservices.customaudience.FetchAndJoinCustomAudienceRequest
import com.example.adservices.samples.fledge.app.FledgeApplication.Companion.TAG
import com.example.adservices.samples.fledge.customaudience.config.CustomAudienceBuyerConfigFileLoader
import com.example.adservices.samples.fledge.customaudience.config.CustomAudienceConfigFileLoader
import com.example.adservices.samples.fledge.customaudience.toggle.CustomAudienceToggle
import com.example.adservices.samples.fledge.customaudience.toggle.CustomAudienceToggle.FetchAndJoinCustomAudienceToggle
import com.example.adservices.samples.fledge.customaudience.toggle.CustomAudienceToggle.JoinCustomAudienceToggle
import com.example.adservices.samples.fledge.customaudience.toggle.CustomAudienceToggleListener
import java.net.MalformedURLException
import java.util.function.Consumer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomAudienceViewModel : ViewModel() {

  private val client = CustomAudienceClient()

  @OptIn(ExperimentalFeatures.Ext10OptIn::class)
  val toggleListener = object : CustomAudienceToggleListener {
    override fun onSwitchToggle(
      toggle: CustomAudienceToggle,
      isChecked: Boolean,
      statusReceiver: Consumer<String>,
    ) {
      when (toggle) {
        is JoinCustomAudienceToggle -> onCustomAudienceSwitchToggle(
          customAudience = toggle.customAudience,
          isChecked = isChecked,
          statusReceiver = statusReceiver
        )

        is FetchAndJoinCustomAudienceToggle -> onFetchCustomAudienceSwitchToggle(
          request = toggle.request,
          isChecked = isChecked,
          statusReceiver = statusReceiver
        )
      }
    }
  }

  private val _buyerBaseUri = MutableStateFlow(Uri.EMPTY)
  val buyerBaseUri = _buyerBaseUri.asStateFlow()

  private val _toggles = MutableStateFlow<List<CustomAudienceToggle>>(listOf())
  val toggles = _toggles.asStateFlow()

  fun loadCustomAudienceBuyerConfig(statusReceiver: Consumer<String>) {
    val config = CustomAudienceBuyerConfigFileLoader().load(statusReceiver)
    updateBuyerBaseUri(config?.buyerBaseUri, statusReceiver)
  }

  fun updateBuyerBaseUri(buyerBaseUriString: String?, statusReceiver: Consumer<String>) {
    viewModelScope.launch {
      try {
        val uri = getValidatedUri(buyerBaseUriString)
        _buyerBaseUri.emit(uri)
        loadCustomAudienceConfig(uri, statusReceiver)
        statusReceiver.accept("Loaded custom audiences using $uri as the buyer base uri")
        Log.e(TAG, "Loaded custom audiences using $uri as the buyer base uri")
      } catch (e: Exception) {
        statusReceiver.accept("Failed to load custom audiences: ${e.message}")
        Log.e(TAG, "Failed to custom audiences: ${e.message}")
      }
    }
  }

  @OptIn(ExperimentalFeatures.Ext10OptIn::class)
  private fun loadCustomAudienceConfig(buyerBaseUri: Uri, statusReceiver: Consumer<String>) {
    val customAudienceConfig = CustomAudienceConfigFileLoader().load(buyerBaseUri, statusReceiver)
    val toggles = mutableListOf<CustomAudienceToggle>()

    for (customAudience in customAudienceConfig.customAudiences) {
      toggles.add(
        JoinCustomAudienceToggle(
          customAudience.key,
          customAudience.value
        )
      )
    }
    for (request in customAudienceConfig.fetchAndJoinCustomAudiences) {
      toggles.add(
        FetchAndJoinCustomAudienceToggle(
          request.key,
          request.value
        )
      )
      viewModelScope.launch {
        _toggles.emit(toggles)
      }
    }
  }

  fun onCustomAudienceSwitchToggle(
    customAudience: CustomAudience,
    isChecked: Boolean,
    statusReceiver: Consumer<String>
  ) {
    viewModelScope.launch {
      if (isChecked) {
        client.joinCustomAudience(customAudience, statusReceiver)
      } else {
        client.leaveCustomAudience(customAudience.name, customAudience.buyer, statusReceiver)
      }
    }
  }

  @OptIn(ExperimentalFeatures.Ext10OptIn::class)
  fun onFetchCustomAudienceSwitchToggle(
    request: FetchAndJoinCustomAudienceRequest,
    isChecked: Boolean,
    statusReceiver: Consumer<String>
  ) {
    viewModelScope.launch {
      if (isChecked) {
        client.fetchAndJoinCustomAudience(request, statusReceiver)
      } else {
        client.leaveCustomAudience(
          request.name!!,
          AdTechIdentifier(request.fetchUri.host!!),
          statusReceiver
        )
      }
    }
  }

  @Throws(MalformedURLException::class)
  private fun getValidatedUri(uriString: String?): Uri {
    val uri = Uri.parse(uriString)
    if (uri.scheme == null || uri.host == null) {
      throw IllegalArgumentException("$uriString is not a valid uri.")
    }
    return uri
  }
}