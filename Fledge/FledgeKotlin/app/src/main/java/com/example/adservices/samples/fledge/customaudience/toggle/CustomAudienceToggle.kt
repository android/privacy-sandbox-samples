package com.example.adservices.samples.fledge.customaudience.toggle

import androidx.annotation.StringRes
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience
import androidx.privacysandbox.ads.adservices.customaudience.FetchAndJoinCustomAudienceRequest
import com.example.adservices.samples.fledge.sampleapp.R

sealed class CustomAudienceToggle(open val label: String, @StringRes val labelPrefixResId: Int) {
  data class JoinCustomAudienceToggle(override val label: String, val customAudience: CustomAudience) :
    CustomAudienceToggle(label, R.string.join_ca_toggle)

  @OptIn(ExperimentalFeatures.Ext10OptIn::class)
  data class FetchAndJoinCustomAudienceToggle(
    override val label: String,
    val request: FetchAndJoinCustomAudienceRequest,
  ) : CustomAudienceToggle(label, R.string.fetch_and_join_ca_toggle)
}