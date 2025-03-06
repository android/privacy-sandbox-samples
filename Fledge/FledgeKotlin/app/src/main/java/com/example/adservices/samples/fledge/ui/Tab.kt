package com.example.adservices.samples.fledge.ui

import androidx.fragment.app.Fragment
import com.example.adservices.samples.fledge.adselection.AdSelectionFragment
import com.example.adservices.samples.fledge.customaudience.CustomAudienceFragment
import com.example.adservices.samples.fledge.protectedsignals.ProtectedSignalsFragment
import com.google.common.collect.ImmutableList

enum class Tab(val title: String, val fragment: Fragment) {
  CUSTOM_AUDIENCE("Custom Audience", CustomAudienceFragment()),
  PROTECTED_SIGNALS("Protected App Signals", ProtectedSignalsFragment()),
  AD_SELECTION("Ad Selection", AdSelectionFragment());

  companion object {
    val mainActivityTabList: ImmutableList<Tab> = ImmutableList.of(
      CUSTOM_AUDIENCE,
      PROTECTED_SIGNALS,
      AD_SELECTION
    )
  }
}