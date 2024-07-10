package com.example.adservices.samples.fledge.sampleapp

import android.adservices.common.AdSelectionSignals
import android.adservices.customaudience.AddCustomAudienceOverrideRequest
import android.annotation.SuppressLint
import com.google.common.collect.ImmutableList

@SuppressLint("NewApi")
class RemoteOverridesConfigFile(
    val overrides: ImmutableList<AddCustomAudienceOverrideRequest>,
    val trustedScoringSignals: AdSelectionSignals,
    val scoringLogic: String
) {
    fun hasOverrides(): Boolean {
        return !overrides.isEmpty() && trustedScoringSignals !== AdSelectionSignals.EMPTY
    }
}
